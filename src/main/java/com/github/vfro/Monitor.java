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
 *    // Wait for a new string added to the queue and print it
 *    outputQueue.write(
 *       queue -&gt; {
 *             System.out.println(queue.pull());
 *
 *             // Write access lambda must return the argument object
 *             // to preserve reference to the queue
 *             return queue;
 *          },
 *
 *       // Wake up when the queue is not empty
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
    protected void setEntity(Entity entity) {
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
            Function<Entity, Entity> function, Predicate<Entity> predicate,
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
                this.setEntity(function.apply(this.getEntity()));
                this.condition.signalAll();
            } else {
                // Write lock used for evaluating predicate
                // now must be downgraded.
                this.getReadLock().lock();
                aquiredLock.unlock();
                aquiredLock = this.getReadLock();
                function.apply(this.getEntity());
            }
        } finally {
            if (aquiredLock != null) {
                aquiredLock.unlock();
            }
        }
        return true;
    }

    private void accessByPredicate(
            Function<Entity, Entity> function,
            Predicate<Entity> predicate,
            boolean isWrite
    ) throws InterruptedException {
        Lock aquiredLock = null;
        try {
            Lock tryLock = this.getWriteLock();
            tryLock.lockInterruptibly();
            aquiredLock = tryLock;

            // Concurrent predicates examine monitored entity exclusively.
            while (!predicate.test(this.getEntity())) {
                this.condition.await();
            }

            if (isWrite) {
                this.setEntity(function.apply(this.getEntity()));
                this.condition.signalAll();
            } else {
                // Write lock used for evaluating predicate
                // now must be downgraded.
                Lock downgradedLock = this.getReadLock();
                downgradedLock.lockInterruptibly();
                aquiredLock.unlock();
                aquiredLock = downgradedLock;
                function.apply(this.getEntity());
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
    public Monitor(Entity entity) {
        this.entity = entity;
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
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
    protected Monitor(Entity entity, Lock readLock, Lock writeLock) {
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
    public final void read(Consumer<Entity> consumer) {
        Lock lock = this.getReadLock();
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
            Consumer<Entity> consumer, Predicate<Entity> predicate
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
            Consumer<Entity> consumer, Predicate<Entity> predicate,
            long time, TimeUnit unit
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
     * Return value of function becomes new monitored entity.
     *
     * @param function exclusively access monitored entity. The function may
     * mutate monitored entity or replace it with a new one.
     * @see #write(java.util.function.Function, java.util.function.Predicate)
     * @see #write(java.util.function.Function, java.util.function.Predicate,
     * long, java.util.concurrent.TimeUnit)
     */
    public final void write(Function<Entity, Entity> function) {
        Lock lock = this.getWriteLock();
        try {
            lock.lock();
            this.setEntity(function.apply(this.getEntity()));
            this.condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for monitored entity to mutate or change to a desirable state and
     * access it in exclusive mode.
     *
     * Return value of function becomes new monitored entity.
     *
     * @param function exclusively access monitored entity. The function may
     * mutate monitored entity or replace it with a new one.
     * @param predicate a desirable state.
     * @throws InterruptedException if the current thread is interrupted.
     * @see #write(java.util.function.Function)
     * @see #write(java.util.function.Function, java.util.function.Predicate,
     * long, java.util.concurrent.TimeUnit)
     */
    public final void write(
            Function<Entity, Entity> function,
            Predicate<Entity> predicate
    ) throws InterruptedException {
        this.accessByPredicate(function, predicate, true);
    }

    /**
     * Wait for monitored entity to mutate or change to a desirable state and
     * access it in exclusive mode.
     *
     * Return value of function becomes new monitored entity.
     *
     * @param function exclusively access monitored entity. The function may
     * mutate monitored entity or replace it with a new one.
     * @param predicate a desirable state.
     * @param time the maximum time to wait.
     * @param unit the time unit of the time argument.
     * @return {@code true} if the monitored entity has been accessed or
     * {@code false} if the waiting time detectably elapsed before return from
     * the method.
     * @throws InterruptedException if the current thread is interrupted.
     * @see #write(java.util.function.Function)
     * @see #write(java.util.function.Function, java.util.function.Predicate)
     */
    public final boolean write(
            Function<Entity, Entity> function,
            Predicate<Entity> predicate,
            long time, TimeUnit unit
    ) throws InterruptedException {
        return this.accessByTime(function, predicate, time, unit, true);
    }

    /**
     * Change monitored entity.
     *
     * @param entity new monitored entity.
     * @see #swap(java.lang.Object)
     */
    public final void set(Entity entity) {
        Lock lock = this.getWriteLock();
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
    public final Entity swap(Entity entity) {
        Entity previousEntity;
        Lock lock = this.getWriteLock();
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
