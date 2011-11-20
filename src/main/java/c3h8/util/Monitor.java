package c3h8.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Monitor is a special kind of property which can be used for concurent
 * access a value and ability to wait until the value becomes into
 * some certain state (defined by a {@link Checker}).<p>
 * <pre>
 * Monitor&lt;Queue&lt;String&gt;&gt; outputQueue = new Monitor&lt;Queue&lt;String&gt;&gt;(new LinkedList&lt;String&gt;());
 *
 * // ...
 * while(true) {
 *    // Wait until some string will be added to a queue and print this into System.out
 *    outputQueue.writeAccess(
 *       new Accessor&lt;&lt;Queue&lt;String&gt;&gt;&gt;() {
 *          &#064;Override
 *          public Queue&lt;String&gt; access(Queue&lt;String&gt; queue) {
 *             System.out.println(queue.pull());
 *
 *             // Write access method must return its parameter to preserve
 *             // link to a queue
 *             return queue;
 *          }
 *       },
 *       new Checker&lt;&lt;Queue&lt;String&gt;&gt;&gt;() {
 *          &#064;Override
 *          public boolean check(Queue&lt;String&gt; queue) {
 *             return !queue.empty();
 *          }
 *       }
 *    );
 * }
 *
 * // Some other thread
 * outputQueue.writeAccess(
 *    new Accessor&lt;&lt;Queue&lt;String&gt;&gt;&gt;() {
 *       &#064;Override
 *       public Queue&lt;String&gt; access(Queue&lt;String&gt; queue) {
 *          queue.add("Hello Monitor!");
 *          return queue;
 *       }
 * );
 * </pre>
 * @param Value value of the monitor.
 * @author Volodymyr Frolov
 */
public class Monitor<Value> {
    /**
     * All descendants of {@code Monitor} class will have access to a property
     * that stores {@code Monitor} value with without synchronization.
     */
    final protected Property<Value> rawValue;

    private Condition checkEvent = null;

    private static class TimeTracker {
        private long origin;

        private long time;
        private TimeUnit unit;

        TimeTracker(long time, TimeUnit unit) {
            this.time = time;
            this.unit = unit;
            this.origin = systemTimer(unit);
        }

        boolean hasMoreTime() {
            return this.timeLeft() > 0;
        }

        long timeLeft() {
            long timer = systemTimer(this.unit);
            return this.origin - timer + toSystemTimerUnits(this.time, this.unit);
        }

        static TimeUnit systemUnit(TimeUnit unit) {
            if (unit == TimeUnit.NANOSECONDS || unit == TimeUnit.MICROSECONDS) {
                return TimeUnit.NANOSECONDS;
            }
            return TimeUnit.MILLISECONDS;
        }

        private static long systemTimer(TimeUnit unit) {
            if (unit == TimeUnit.NANOSECONDS || unit == TimeUnit.MICROSECONDS) {
                return System.nanoTime();
            }
            return System.currentTimeMillis();
        }

        private static long toSystemTimerUnits(long time, TimeUnit unit) {
            return systemUnit(unit).convert(time, unit);
        }
    };

    private boolean accessByTime(
            Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit, boolean isWrite
        ) throws InterruptedException {
        TimeTracker timeTracker = new TimeTracker(time, unit);
        Lock aquiredLock = null;

        long result = 0;

        try {
            Lock tryLock = this.wlock.get();
            if (tryLock.tryLock(time, TimeTracker.systemUnit(unit))) {
                aquiredLock = tryLock;
            } else {
                return false;
            }

            while (!checker.check(this.rawValue.get())) {
                while (!this.checkEvent.await(timeTracker.timeLeft(), TimeTracker.systemUnit(unit))) {
                    if (!timeTracker.hasMoreTime()) {
                        return false;
                    }
                }
            }

            if (isWrite) {
                this.rawValue.set(accessor.access(this.rawValue.get()));
                this.checkEvent.signalAll();
            } else {
                this.rlock.get().lock();
                aquiredLock.unlock();
                aquiredLock = this.rlock.get();
                accessor.access(this.rawValue.get());
            }
        }
        finally {
            if (aquiredLock != null) {
                aquiredLock.unlock();
            }
        }
        return true;
    }

    private void accessByChecker(
            Accessor<Value> accessor, Checker<Value> checker, boolean isWrite
        ) throws InterruptedException {
        Lock lockedObject = this.wlock.get();
        try {
            lockedObject.lockInterruptibly();
            while (!checker.check(this.rawValue.get())) {
                this.checkEvent.await();
            }

            if (isWrite) {
                this.rawValue.set(accessor.access(this.rawValue.get()));
                this.checkEvent.signalAll();
            } else {
                Lock downgradeLock = this.rlock.get();
                downgradeLock.lockInterruptibly();
                lockedObject.unlock();
                lockedObject = downgradeLock;
                accessor.access(this.rawValue.get());
            }
        }
        finally {
            lockedObject.unlock();
        }
    }

    /**
     * A lock which is used for read access synchronization. Read/Write lock
     * must be reenterant and support lock downgrading (Acquiring read lock inside
     * write lock, and release write lock after that).
     *
     * By default the value of a property is a read lock of a {@code java.util.concurrent.locks.ReentrantReadWriteLock()}
     * instance.
     */
    public final Property<Lock> rlock = new Property<Lock>(null);

    /**
     * A lock which is used for write access synchronization. Read/Write lock
     * must be reenterant and support lock downgrading (Acquiring read lock inside
     * write lock, and release write lock after that).
     *
     * By default the value of a property is a read lock of a {@code java.util.concurrent.locks.ReentrantReadWriteLock()}
     * instance.
     */
    public final Property<Lock> wlock = new Property<Lock>(null) {
        @Override
        public void set(Lock lock) {
            Monitor.this.checkEvent = lock.newCondition();
            super.set(lock);
        }
    };

    /**
     * Constructor for descendants of Monitor class to allows redefining setters/getters
     * of rawValue property.
     */
    protected Monitor(Property<Value> rawValue, Lock readLock, Lock writeLock) {
        this.rawValue = rawValue;
        this.rlock.set(readLock);
        this.wlock.set(writeLock);
    }

    /**
     * Create new instance of a monitor initialized by specified value.
     * @param value initial value of a monitor.
     */
    public Monitor(Value value) {
        this.rawValue = new Property<Value>(value);
        ReadWriteLock rwlock = new ReentrantReadWriteLock();
        this.rlock.set(rwlock.readLock());
        this.wlock.set(rwlock.writeLock());
    }

    /**
     * Obtain a read access to monitor value.
     *
     * Return value of a {@code Accessor.access} is ignored.
     * @param accessor {@code Accessor} instance to obtain read access. The instance must
     * not change internal state of the value in {@code Accessor.access} method.
     */
    final public void readAccess(Accessor<Value> accessor) {
        Lock lockedObject = this.rlock.get();
        try {
            lockedObject.lock();
            accessor.access(this.rawValue.get());
        }
        finally {
            lockedObject.unlock();
        }
    }

    /**
     * Causes the current thread to wait until monitor becomes to a certain state defined by
     * {@link Checker} instance and access monitor value for read.
     *
     * Return value of a {@code Accessor.access} is ignored.
     * Checker instance must not change a state of the monitor value.
     * @param accessor {@code Accessor} instance to obtain read access. The instance must
     * not change internal state of the value in {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of the monitor value
     * when it should be accessed by Accessor.
     * @throws InterruptedException if the current thread is interrupted.
     */
    final public void readAccess(Accessor<Value> accessor, Checker<Value> checker)
        throws InterruptedException {
        accessByChecker(accessor, checker, false);
    }

    /**
     * Causes the current thread to wait until monitor becomes to a certain state defined by
     * {@link Checker} instance and access monitor value for read.
     *
     * Return value of a {@code Accessor.access} is ignored.
     * Checker instance must not change a state of the monitor value.
     * @param accessor {@code Accessor} instance to obtain read access. The instance must
     * not change internal state of the value in {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of the monitor value
     * when it should be accessed by Accessor.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitor value has been accessed or {@code false} if the waiting
     * time detectably elapsed before return from the method
     * @throws InterruptedException if the current thread is interrupted.
     */
    final public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
        throws InterruptedException {
        return this.accessByTime(accessor, checker, time, unit, false);
    }

    /**
     * Obtain a write access to monitor value.
     *
     * Return value of a {@code Accessor.access} becomes new value of a monitor property.
     * @param accessor {@code Accessor} instance to obtain write access. The instance may
     * change internal state of the value in {@code Accessor.access} method.
     */
    final public void writeAccess(Accessor<Value> accessor) {
        Lock lockedObject = this.wlock.get();
        try {
            lockedObject.lock();
            this.rawValue.set(accessor.access(this.rawValue.get()));
            this.checkEvent.signalAll();
        }
        finally {
            lockedObject.unlock();
        }
    }

    /**
     * Causes the current thread to wait until monitor becomes to a certain state defined by
     * {@link Checker} instance and access monitor value for write.
     *
     * Return value of a {@code Accessor.access} becomes new value of a monitor property.
     * Checker instance must not change a state of the monitor value.
     * @param accessor {@code Accessor} instance to obtain write access. The instance may
     * change internal state of the value in {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of the monitor value
     * when it should be accessed by Accessor.
     * @throws InterruptedException if the current thread is interrupted.
     */
    final public void writeAccess(Accessor<Value> accessor, Checker<Value> checker)
        throws InterruptedException {
        accessByChecker(accessor, checker, true);
    }

    /**
     * Causes the current thread to wait until monitor becomes to a certain state defined by
     * {@link Checker} instance and access monitor value for write.
     *
     * Return value of a {@code Accessor.access} becomes new value of a monitor property.
     * Checker instance must not change a state of the monitor value.
     * @param accessor {@code Accessor} instance to obtain write access. The instance may
     * change internal state of the value in {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of the monitor value
     * when it should be accessed by Accessor.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitor value has been accessed or {@code false} if the waiting
     * time detectably elapsed before return from the method
     * @throws InterruptedException if the current thread is interrupted.
     */
    final public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
        throws InterruptedException {
        return this.accessByTime(accessor, checker, time, unit, true);
    }

    /**
     * Set new value of the monitor.
     *
     * @param value new value of the monitor.
     * @return previous value.
     */
    final public Value set(Value value) {
        Value previousValue;
        Lock lockedObject = this.wlock.get();
        try {
            lockedObject.lock();
            previousValue = this.rawValue.get();
            this.rawValue.set(value);
            this.checkEvent.signalAll();
        }
        finally {
            lockedObject.unlock();
        }
        return previousValue;
    }
}
