package c3h8.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Monitor can be used for concurent access a value and ability to
 * wait until the value becomes into some certain state (defined by a
 * {@link Checker}).<p>
 * <pre>
 * Monitor&lt;Queue&lt;String&gt;&gt; outputQueue =
 *    new Monitor&lt;Queue&lt;String&gt;&gt;(new LinkedList&lt;String&gt;());
 *
 * // ...
 * while(true) {
 *    // Wait until some string is added to a queue
 *    // and printit into System.out
 *    outputQueue.writeAccess(
 *       new Accessor&lt;Queue&lt;String&gt;&gt;() {
 *          &#064;Override
 *          public Queue&lt;String&gt; access(Queue&lt;String&gt; queue) {
 *             System.out.println(queue.pull());
 *
 *             // Write access method must return the parameter object
 *             // to preserve reference to the queue
 *             return queue;
 *          }
 *       },
 *
 *       new Checker&lt;Queue&lt;String&gt;&gt;() {
 *          &#064;Override
 *          public boolean check(Queue&lt;String&gt; queue) {
 *             return !queue.isEmpty();
 *          }
 *       }
 *    );
 * }
 *
 * // Some other thread
 * outputQueue.writeAccess(
 *    new Accessor&lt;Queue&lt;String&gt;&gt;() {
 *       &#064;Override
 *       public Queue&lt;String&gt; access(Queue&lt;String&gt; queue) {
 *          queue.add("Hello Monitor!");
 *          return queue;
 *       }
 * );
 * </pre>
 * @param <Value> value of the monitor.
 */
public class Monitor<Value> {

    private Value value;
    private Lock readLock;
    private Lock writeLock;
    private Condition condition;

    /**
     * Get access to monitored value without synchronization.
     */
    protected Value getValue() {
        return this.value;
    }

    /**
     * Modify monitored value directly without synchronization.
     */
    protected void setValue(Value value) {
        this.value = value;
    }

    /**
     * Get a lock which is used for read access synchronization.
     */
    protected final Lock getReadLock() {
        return this.readLock;
    }

    /**
     * Get lock which is used for write access synchronization.
     */
    protected final Lock getWriteLock() {
        return this.writeLock;
    }

    /**
     * Get a condition variable which is signaled after state of monitired
     * value becomes changed.
     */
    protected final Condition getCondition() {
        return this.condition;
    }

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
            return this.origin -
                timer + toSystemTimerUnits(this.time, this.unit);
        }

        static TimeUnit systemUnit(TimeUnit unit) {
            if (unit == TimeUnit.NANOSECONDS
                || unit == TimeUnit.MICROSECONDS) {
                return TimeUnit.NANOSECONDS;
            }
            return TimeUnit.MILLISECONDS;
        }

        private static long systemTimer(TimeUnit unit) {
            if (unit == TimeUnit.NANOSECONDS
                || unit == TimeUnit.MICROSECONDS) {
                return System.nanoTime();
            }
            return System.currentTimeMillis();
        }

        private static long toSystemTimerUnits(long time, TimeUnit unit) {
            return systemUnit(unit).convert(time, unit);
        }
    };

    private boolean accessByTime(
            Accessor<Value> accessor, Checker<Value> checker,
            long time, TimeUnit unit, boolean isWrite
        ) throws InterruptedException {
        TimeTracker timeTracker = new TimeTracker(time, unit);
        Lock aquiredLock = null;

        try {
            Lock tryLock = this.getWriteLock();
            if (tryLock.tryLock(time, TimeTracker.systemUnit(unit))) {
                aquiredLock = tryLock;
            } else {
                return false;
            }

            while (!checker.check(this.getValue())) {
                while (!this.condition.await(
                        timeTracker.timeLeft(),
                        TimeTracker.systemUnit(unit)
                    )) {
                    if (!timeTracker.hasMoreTime()) {
                        return false;
                    }
                }
            }

            if (isWrite) {
                this.setValue(accessor.access(this.getValue()));
                this.condition.signalAll();
            } else {
                this.getReadLock().lock();
                aquiredLock.unlock();
                aquiredLock = this.getReadLock();
                accessor.access(this.getValue());
            }
        } finally {
            if (aquiredLock != null) {
                aquiredLock.unlock();
            }
        }
        return true;
    }

    private void accessByChecker(
            Accessor<Value> accessor, Checker<Value> checker, boolean isWrite
        ) throws InterruptedException {
        Lock lockedObject = null;
        try {
            Lock tryLock = this.getWriteLock();
            tryLock.lockInterruptibly();
            lockedObject = tryLock;
            while (!checker.check(this.getValue())) {
                this.condition.await();
            }

            if (isWrite) {
                this.setValue(accessor.access(this.getValue()));
                this.condition.signalAll();
            } else {
                Lock downgradeLock = this.getReadLock();
                downgradeLock.lockInterruptibly();
                lockedObject.unlock();
                lockedObject = downgradeLock;
                accessor.access(this.getValue());
            }
        } finally {
            if (lockedObject != null) {
                lockedObject.unlock();
            }
        }
    }

    /**
     * Create new instance of Monitor initialized by specified value.
     * @param value initial value of a monitor.
     */
    public Monitor(Value value) {
        this.value = value;
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
        this.condition = this.writeLock.newCondition();
    }

    /**
     * Create new instance of Monitor initialized by value and custom locks.
     *
     * Custom locks must be reenterant and support lock downgrading (Acquiring
     * read lock inside write lock, and release write lock after that).
     *
     * @param value initial value of a monitor.
     * @param readLock Custom read lock.
     * @param writeLock Custom write lock. It may be the same instance as
     * {@code readLock}.
     */
    protected Monitor(Value value, Lock readLock, Lock writeLock) {
        this.value = value;
        this.readLock = readLock;
        this.writeLock = writeLock;
        this.condition = this.writeLock.newCondition();
    }

    /**
     * Access monitor value for read (in shared mode).
     *
     * Return value of {@code Accessor.access} is ignored.
     * @param accessor {@code Accessor} instance to obtain read access.
     * The instance must not change internal state of value in
     * {@code Accessor.access} method.
     */
    public final void readAccess(Accessor<Value> accessor) {
        Lock lockedObject = this.getReadLock();
        try {
            lockedObject.lock();
            accessor.access(this.getValue());
        } finally {
            lockedObject.unlock();
        }
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain
     * state defined by {@link Checker} instance and access monitor value for
     * read (in shared mode).
     *
     * Return value of {@code Accessor.access} is ignored.
     * Checker instance must not change a state of monitored value.
     * @param accessor {@code Accessor} instance to obtain read access.
     * The instance must not change internal state of value in
     * {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of monitored
     * value when it should be accessed by Accessor.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final void readAccess(
            Accessor<Value> accessor, Checker<Value> checker
        ) throws InterruptedException {
        accessByChecker(accessor, checker, false);
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain
     * state defined by {@link Checker} instance and access monitor value for
     * read (in shared mode).
     *
     * Returned value of {@code Accessor.access} is ignored.
     * Checker instance must not change a state of monitored value.
     * @param accessor {@code Accessor} instance to obtain read access.
     * The instance must not change internal state of value in
     * {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of monitored
     * value when it should be accessed by Accessor.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitor value has been accessed or
     * {@code false} if the waiting time detectably elapsed before return
     * from the method.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final boolean readAccess(
            Accessor<Value> accessor, Checker<Value> checker,
            long time, TimeUnit unit
        ) throws InterruptedException {
        return this.accessByTime(accessor, checker, time, unit, false);
    }

    /**
     * Access monitor value for write (in exclusive mode).
     *
     * Return value of {@code Accessor.access} becomes new value of monitor.
     * @param accessor {@code Accessor} instance to obtain read access.
     * The instance must not change internal state of value in
     * {@code Accessor.access} method.
     */
    public final void writeAccess(Accessor<Value> accessor) {
        Lock lockedObject = this.getWriteLock();
        try {
            lockedObject.lock();
            this.setValue(accessor.access(this.getValue()));
            this.condition.signalAll();
        } finally {
            lockedObject.unlock();
        }
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain
     * state defined by {@link Checker} instance and access monitor value for
     * write (in exclusive mode).
     *
     * Return value of {@code Accessor.access} becomes new value of monitor.
     * Checker instance must not change a state of monitored value.
     * @param accessor {@code Accessor} instance to obtain write access.
     * The instance may change internal state of value in
     * {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of monitored
     * value when it should be accessed by Accessor.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final void writeAccess(
            Accessor<Value> accessor, Checker<Value> checker
        ) throws InterruptedException {
        accessByChecker(accessor, checker, true);
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain
     * state defined by {@link Checker} instance and access monitor value for
     * write (in exclusive mode).
     *
     * Return value of {@code Accessor.access} becomes new value of monitor.
     * Checker instance must not change a state of monitored value.
     * @param accessor {@code Accessor} instance to obtain write access.
     * The instance may change internal state of value in
     * {@code Accessor.access} method.
     * @param checker {@code Checker} instance to define a state of monitored
     * value when it should be accessed by Accessor.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitor value has been accessed or
     * {@code false} if the waiting time detectably elapsed before return
     * from the method.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final boolean writeAccess(
            Accessor<Value> accessor, Checker<Value> checker,
            long time, TimeUnit unit
        ) throws InterruptedException {
        return this.accessByTime(accessor, checker, time, unit, true);
    }

    /**
     * Set new value of monitor.
     *
     * @param value new value of monitor.
     * @return previous value of monitor.
     */
    public final Value set(Value value) {
        Value previousValue;
        Lock lockedObject = this.getWriteLock();
        try {
            lockedObject.lock();
            previousValue = this.getValue();
            this.setValue(value);
            this.condition.signalAll();
        } finally {
            lockedObject.unlock();
        }
        return previousValue;
    }
}
