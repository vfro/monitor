package com.github.vfro;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Monitor can be used for concurrent access to a value. It also provides an
 * ability to wait until the value becomes into some certain state.
 * <pre>
 * Monitor&lt;Queue&lt;String&gt;&gt; outputQueue =
 *    new Monitor&lt;&lt;String&gt;&gt;(new LinkedList&lt;String&gt;());
 *
 * // ...
 * while(true) {
 *    // Wait until some string is added to a queue
 *    // and printit into System.out
 *    outputQueue.writeAccess(
 *       queue -&gt; {
 *             System.out.println(queue.pull());
 *
 *             // Write access lambda must return the parameter object
 *             // to preserve reference to the queue
 *             return queue;
 *          },
 *       queue -&gt; !queue.isEmpty()
 *    );
 * }
 *
 * // Some other thread
 * outputQueue.writeAccess(
 *    queue -&gt; {
 *          queue.add("Hello Monitor!");
 *          return queue;
 *       }
 * );
 * </pre>
 *
 * @param <Value> value of the monitor.
 */
@SuppressWarnings("LocalVariableHidesMemberVariable")
public class Monitor<Value> {

    private Value value;
    private final Lock readLock;
    private final Lock writeLock;
    private final Condition condition;

    /**
     * Get access to monitored value without any synchronization.
     *
     * @return the monitored value.
     */
    protected Value getValue() {
        return this.value;
    }

    /**
     * Modify monitored value directly without any synchronization.
     *
     * @param value the new monitored value.
     */
    protected void setValue(Value value) {
        this.value = value;
    }

    /**
     * Get a lock which is used for read access.
     *
     * @return read access lock.
     */
    protected final Lock getReadLock() {
        return this.readLock;
    }

    /**
     * Get lock which is used for write access.
     *
     * @return write access lock.
     */
    protected final Lock getWriteLock() {
        return this.writeLock;
    }

    /**
     * Get a condition variable which is signaled after state of the monitored
     * value becomes changed.
     *
     * @return condition variable.
     */
    protected final Condition getCondition() {
        return this.condition;
    }

    private static class TimeTracker {

        private final long origin;
        private final long time;
        private final TimeUnit unit;

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
            return this.origin
                    - timer + toSystemTimerUnits(this.time, this.unit);
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
            Function<Value, Value> function, Predicate<Value> predicate,
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

            while (!predicate.test(this.getValue())) {
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
                this.setValue(function.apply(this.getValue()));
                this.condition.signalAll();
            } else {
                this.getReadLock().lock();
                aquiredLock.unlock();
                aquiredLock = this.getReadLock();
                function.apply(this.getValue());
            }
        } finally {
            if (aquiredLock != null) {
                aquiredLock.unlock();
            }
        }
        return true;
    }

    private void accessByPredicate(
            Function<Value, Value> function, Predicate<Value> predicate, boolean isWrite
    ) throws InterruptedException {
        Lock lockedObject = null;
        try {
            Lock tryLock = this.getWriteLock();
            tryLock.lockInterruptibly();
            lockedObject = tryLock;
            while (!predicate.test(this.getValue())) {
                this.condition.await();
            }

            if (isWrite) {
                this.setValue(function.apply(this.getValue()));
                this.condition.signalAll();
            } else {
                Lock downgradeLock = this.getReadLock();
                downgradeLock.lockInterruptibly();
                lockedObject.unlock();
                lockedObject = downgradeLock;
                function.apply(this.getValue());
            }
        } finally {
            if (lockedObject != null) {
                lockedObject.unlock();
            }
        }
    }

    /**
     * Create new instance of Monitor initialized by specified value.
     *
     * @param value initial value of the monitor.
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
     * Custom locks must be re-entrant and support lock downgrading (Acquiring
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
     * Access monitored value for read (in shared mode).
     *
     * @param consumer to obtain read access. The consumer must not change
     * internal state of monitored value.
     */
    public final void readAccess(Consumer<Value> consumer) {
        Lock lockedObject = this.getReadLock();
        try {
            lockedObject.lock();
            consumer.accept(this.getValue());
        } finally {
            lockedObject.unlock();
        }
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain state
     * and access monitored value for read (in shared mode).
     *
     * Predicate expression must not change a state of monitored value.
     *
     * @param consumer to obtain read access. Consumer must not change internal
     * state of monitored value.
     * @param predicate to define a state of monitored value.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final void readAccess(
            Consumer<Value> consumer, Predicate<Value> predicate
    ) throws InterruptedException {
        accessByPredicate(
                value -> {
                    consumer.accept(value);
                    return value;
                },
                predicate, false);
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain state
     * and access monitored value for read (in shared mode).
     *
     * Predicate instance must not change a state of monitored value.
     *
     * @param consumer to obtain read access. The instance must not change
     * internal state of monitored value.
     * @param predicate to define a state of monitored value.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitored value has been accessed or
     * {@code false} if the waiting time detectably elapsed before return from
     * the method.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final boolean readAccess(
            Consumer<Value> consumer, Predicate<Value> predicate,
            long time, TimeUnit unit
    ) throws InterruptedException {
        return this.accessByTime(
                value -> {
                    consumer.accept(value);
                    return value;
                },
                predicate, time, unit, false);
    }

    /**
     * Access monitored value for write (in exclusive mode).
     *
     * Return value of function becomes new value of monitor.
     *
     * @param function instance change monitored value. The function may change
     * internal state of monitored value.
     */
    public final void writeAccess(Function<Value, Value> function) {
        Lock lockedObject = this.getWriteLock();
        try {
            lockedObject.lock();
            this.setValue(function.apply(this.getValue()));
            this.condition.signalAll();
        } finally {
            lockedObject.unlock();
        }
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain state
     * and access monitored value for write (in exclusive mode).
     *
     * Return value of function becomes new value of monitor.
     *
     * @param function to obtain write access. The function may change internal
     * state of monitored value.
     * @param predicate to define a state of monitored value.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final void writeAccess(
            Function<Value, Value> function, Predicate<Value> predicate
    ) throws InterruptedException {
        accessByPredicate(function, predicate, true);
    }

    /**
     * Causes current thread to wait until monitor becomes to some certain state
     * and access monitored value for write (in exclusive mode).
     *
     * Return value of function becomes new value of monitor.
     *
     * @param function to obtain write access. The function may change internal
     * state of monitored value.
     * @param predicate to define a state of monitored value.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitored value has been accessed or
     * {@code false} if the waiting time detectably elapsed before return from
     * the method.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public final boolean writeAccess(
            Function<Value, Value> function, Predicate<Value> predicate,
            long time, TimeUnit unit
    ) throws InterruptedException {
        return this.accessByTime(function, predicate, time, unit, true);
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
