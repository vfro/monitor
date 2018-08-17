package com.github.vfro;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Monitor is a synchronization construct that allows threads to have both
 * thread-safe access to underlying monitored object or value (hereinafter
 * monitored entity) and wait for it to mutate (if it is a mutable object) or
 * change (if it is an immutable value) to a desirable state.
 *
 * <pre>
 * Monitor&lt;Queue&lt;String&gt;&gt; outputQueue =
 *    new Monitor&lt;&gt;(new LinkedList&lt;String&gt;());
 *
 * // ... monitoring thread
 * while(true) {
 *    // wait for a new string in queue
 *    outputQueue.write(
 *       queue -&gt; {
 *             System.out.println(queue.pull());
 *
 *             // write access lambda must return its argument
 *             // to preserve reference to the queue
 *             return queue;
 *          },
 *
 *       // wake up when the queue is not empty
 *       queue -&gt; !queue.isEmpty()
 *    );
 * }
 *
 * // ... somewhere in some other thread
 * outputQueue.write(
 *    queue -&gt; {
 *          queue.add("Hello Monitor!");
 *          return queue;
 *       }
 * );
 * </pre>
 *
 * @param <Entity> monitored entity type.
 */
@SuppressWarnings("LocalVariableHidesMemberVariable")
public class Monitor<Entity> {

    private Entity entity;
    private final Lock readLock;
    private final Lock writeLock;
    private final Condition condition;

    /**
     * Get access to monitored entity without any synchronization.
     *
     * @return the monitored entity.
     * @see #setEntity(java.lang.Object)
     */
    protected Entity getEntity() {
        return this.entity;
    }

    /**
     * Modify monitored entity directly without any synchronization.
     *
     * @param entity the new monitored entity.
     * @see #set(java.lang.Object)
     * @see #getEntity()
     */
    protected void setEntity(final Entity entity) {
        this.entity = entity;
    }

    /**
     * Get a lock which is used for shared access.
     *
     * @return read access lock.
     * @see #getWriteLock()
     */
    protected final Lock getReadLock() {
        return this.readLock;
    }

    /**
     * Get lock which is used for exclusive access.
     *
     * @return write access lock.
     * @see #getReadLock()
     */
    protected final Lock getWriteLock() {
        return this.writeLock;
    }

    /**
     * Get a condition variable which is signaled after mutation or change of
     * monitored entity.
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

        TimeTracker(final long time, final TimeUnit unit) {
            this.time = time;
            this.unit = unit;
            this.origin = systemTimer(unit);
        }

        boolean hasMoreTime() {
            return this.timeLeft() > 0;
        }

        long timeLeft() {
            final long timer = systemTimer(this.unit);
            return this.origin
                    - timer + toSystemTimerUnits(this.time, this.unit);
        }

        static TimeUnit systemUnit(final TimeUnit unit) {
            if (unit == TimeUnit.NANOSECONDS
                    || unit == TimeUnit.MICROSECONDS) {
                return TimeUnit.NANOSECONDS;
            }
            return TimeUnit.MILLISECONDS;
        }

        private static long systemTimer(final TimeUnit unit) {
            if (unit == TimeUnit.NANOSECONDS
                    || unit == TimeUnit.MICROSECONDS) {
                return System.nanoTime();
            }
            return System.currentTimeMillis();
        }

        private static long toSystemTimerUnits(
                final long time,
                final TimeUnit unit) {
            return systemUnit(unit).convert(time, unit);
        }
    };

    private boolean accessByTime(
            final UnaryOperator<Entity> operator,
            final Predicate<Entity> predicate,
            final long time, final TimeUnit unit,
            final boolean isWrite
    ) throws InterruptedException {
        final TimeTracker timeTracker = new TimeTracker(time, unit);
        Lock aquiredLock = null;

        try {
            Lock tryLock = this.getWriteLock();
            if (tryLock.tryLock(time, TimeTracker.systemUnit(unit))) {
                aquiredLock = tryLock;
            } else {
                return false;
            }

            // Concurrent predicates examine monitored entity exclusively.
            while (!predicate.test(this.getEntity())) {
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
                this.setEntity(operator.apply(this.getEntity()));
                this.condition.signalAll();
            } else {
                // Write lock used for evaluating predicate
                // now must be downgraded.
                this.getReadLock().lock();
                aquiredLock.unlock();
                aquiredLock = this.getReadLock();
                operator.apply(this.getEntity());
            }
        } finally {
            if (aquiredLock != null) {
                aquiredLock.unlock();
            }
        }
        return true;
    }

    private void accessByPredicate(
            final UnaryOperator<Entity> operator,
            final Predicate<Entity> predicate,
            final boolean isWrite
    ) throws InterruptedException {
        Lock aquiredLock = null;
        try {
            final Lock tryLock = this.getWriteLock();
            tryLock.lockInterruptibly();
            aquiredLock = tryLock;

            // Concurrent predicates examine monitored entity exclusively.
            while (!predicate.test(this.getEntity())) {
                this.condition.await();
            }

            if (isWrite) {
                this.setEntity(operator.apply(this.getEntity()));
                this.condition.signalAll();
            } else {
                // Write lock used for evaluating predicate
                // now must be downgraded.
                final Lock downgradedLock = this.getReadLock();
                downgradedLock.lockInterruptibly();
                aquiredLock.unlock();
                aquiredLock = downgradedLock;
                operator.apply(this.getEntity());
            }
        } finally {
            if (aquiredLock != null) {
                aquiredLock.unlock();
            }
        }
    }

    /**
     * Create new instance of Monitor initialized by monitored entity.
     *
     * @param entity monitored entity.
     */
    public Monitor(final Entity entity) {
        this.entity = entity;
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
        this.condition = this.writeLock.newCondition();
    }

    /**
     * Create new instance of Monitor initialized by an entity and custom locks.
     *
     * Custom locks must be re-entrant and support lock downgrading (Acquiring
     * read lock inside write lock, and release write lock after that).
     *
     * @param entity monitored entity.
     * @param readLock Custom read lock.
     * @param writeLock Custom write lock. It may be the same instance as
     * {@code readLock}.
     * @see java.util.concurrent.locks.ReentrantReadWriteLock
     */
    protected Monitor(
            final Entity entity,
            final Lock readLock,
            final Lock writeLock) {
        this.entity = entity;
        this.readLock = readLock;
        this.writeLock = writeLock;
        this.condition = this.writeLock.newCondition();
    }

    /**
     * Access monitored entity in shared mode.
     *
     * @param consumer access monitored entity. The consumer must not mutate
     * monitored entity.
     * @see #read(java.util.function.Consumer, java.util.function.Predicate)
     * @see #read(java.util.function.Consumer, java.util.function.Predicate,
     * long, java.util.concurrent.TimeUnit)
     */
    public final void read(final Consumer<Entity> consumer) {
        final Lock lock = this.getReadLock();
        try {
            lock.lock();
            consumer.accept(this.getEntity());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for monitored entity to mutate or change to a desirable state and
     * access it in shared mode.
     *
     * @param consumer access monitored entity. The consumer must not mutate
     * monitored entity.
     * @param predicate a desirable state.
     * @throws InterruptedException if the current thread is interrupted.
     * @see #read(java.util.function.Consumer)
     * @see #read(java.util.function.Consumer, java.util.function.Predicate,
     * long, java.util.concurrent.TimeUnit)
     */
    public final void read(
            final Consumer<Entity> consumer,
            final Predicate<Entity> predicate
    ) throws InterruptedException {
        accessByPredicate(
                entity -> {
                    consumer.accept(entity);
                    return entity;
                },
                predicate, false
        );
    }

    /**
     * Wait for monitored entity to mutate or change to a desirable state and
     * access it in shared mode.
     *
     * @param consumer access monitored entity. The consumer must not mutate
     * monitored entity.
     * @param predicate a desirable state.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitored entity has been accessed or
     * {@code false} if the waiting time detectably elapsed before return from
     * the method.
     * @throws InterruptedException if the current thread is interrupted.
     * @see #read(java.util.function.Consumer)
     * @see #read(java.util.function.Consumer, java.util.function.Predicate)
     */
    public final boolean read(
            final Consumer<Entity> consumer,
            final Predicate<Entity> predicate,
            final long time, final TimeUnit unit
    ) throws InterruptedException {
        return this.accessByTime(
                entity -> {
                    consumer.accept(entity);
                    return entity;
                },
                predicate, time, unit, false
        );
    }

    /**
     * Access monitored entity in exclusive mode.
     *
     * Return value of operator becomes new monitored entity.
     *
     * @param operator exclusively access monitored entity. The operator may
     * mutate monitored entity or replace it with a new one.
     * @see #write(java.util.function.UnaryOperator, java.util.function.Predicate)
     * @see #write(java.util.function.UnaryOperator, java.util.function.Predicate,
     * long, java.util.concurrent.TimeUnit)
     */
    public final void write(final UnaryOperator<Entity> operator) {
        final Lock lock = this.getWriteLock();
        try {
            lock.lock();
            this.setEntity(operator.apply(this.getEntity()));
            this.condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for monitored entity to mutate or change to a desirable state and
     * access it in exclusive mode.
     *
     * Return value of operator becomes new monitored entity.
     *
     * @param operator exclusively access monitored entity. The operator may
     * mutate monitored entity or replace it with a new one.
     * @param predicate a desirable state.
     * @throws InterruptedException if the current thread is interrupted.
     * @see #write(java.util.function.UnaryOperator)
     * @see #write(java.util.function.UnaryOperator, java.util.function.Predicate,
     * long, java.util.concurrent.TimeUnit)
     */
    public final void write(
            final UnaryOperator<Entity> operator,
            final Predicate<Entity> predicate
    ) throws InterruptedException {
        this.accessByPredicate(operator, predicate, true);
    }

    /**
     * Wait for monitored entity to mutate or change to a desirable state and
     * access it in exclusive mode.
     *
     * Return value of operator becomes new monitored entity.
     *
     * @param operator exclusively access monitored entity. The operator may
     * mutate monitored entity or replace it with a new one.
     * @param predicate a desirable state.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitored entity has been accessed or
     * {@code false} if the waiting time detectably elapsed before return from
     * the method.
     * @throws InterruptedException if the current thread is interrupted.
     * @see #write(java.util.function.UnaryOperator)
     * @see #write(java.util.function.UnaryOperator, java.util.function.Predicate)
     */
    public final boolean write(
            final UnaryOperator<Entity> operator,
            final Predicate<Entity> predicate,
            final long time, final TimeUnit unit
    ) throws InterruptedException {
        return this.accessByTime(operator, predicate, time, unit, true);
    }

    /**
     * Change monitored entity.
     *
     * @param entity new monitored entity.
     * @see #swap(java.lang.Object)
     */
    public final void set(final Entity entity) {
        final Lock lock = this.getWriteLock();
        try {
            lock.lock();
            this.setEntity(entity);
            this.condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Change monitored entity and return the previous value.
     *
     * @param entity new monitored entity.
     * @return previous monitored entity.
     * @see #set(java.lang.Object)
     */
    public final Entity swap(final Entity entity) {
        Entity previousEntity;
        final Lock lock = this.getWriteLock();
        try {
            lock.lock();
            previousEntity = this.getEntity();
            this.setEntity(entity);
            this.condition.signalAll();
        } finally {
            lock.unlock();
        }
        return previousEntity;
    }
}
