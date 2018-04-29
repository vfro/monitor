package com.github.vfro;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * `Sandbox` class allows threads to create a local copy of monitored entity and
 * then Compare and Swap the mutated or changed entity back to monitor. The
 * `Entity` must be `Cloneable` in order to create its local copy.
 *
 * `Sandbox` instance is not reentrant. Each thread must create a local
 * instance of `Sandbox`.

 * @param <Entity> monitored entity type.
 */
@SuppressWarnings("LocalVariableHidesMemberVariable")
public final class Sandbox<Entity extends Cloneable> implements Cloneable {

    private WeakReference<Entity> check;
    private Entity entity;
    private boolean pushResult;

    @SuppressWarnings({"unchecked", "UseSpecificCatch"})
    private static <Entity> Entity cloneValue(final Entity entity) {
        Entity result = null;
        try {
            Method clone = entity.getClass().getDeclaredMethod("clone");
            result = (Entity) clone.invoke(entity);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot clone instance of " + entity.getClass().getName(), e
            );
        }
        return result;
    }

    /**
     * Create a new instance of Sandbox with null as its monitored entity.
     */
    public Sandbox() {
        this.entity = null;
        this.check = null;
        this.pushResult = false;
    }

    /**
     * Create new instance of Sandbox with monitored entity.
     *
     * @param entity monitored entity.
     */
    public Sandbox(final Entity entity) {
        this.entity = entity;
        this.check = null;
    }

    /**
     * Create new instance of Sandbox and pull its entity from monitor.
     *
     * @param monitor initial entity is pulled from the monitor.
     * @see com.github.vfro.Monitor
     */
    public Sandbox(final Monitor<Entity> monitor) {
        super();
        this.pull(monitor);
    }

    /**
     * Access monitored entity. If the entity was previously pulled from
     * {@link Monitor} then the copy of it is returned. The copy can be mutated
     * without locking the monitor.
     *
     * @return monitored entity.
     * @see #set(java.lang.Cloneable)
     */
    public Entity get() {
        return this.entity;
    }

    /**
     * Assign a new monitored entity to Sandbox.
     *
     * @param entity monitored entity.
     */
    public void set(final Entity entity) {
        this.entity = entity;
    }

    /**
     * Access monitored entity in the monitor, assign its clone to sandbox and
     * return the clone of obtained entity.
     *
     * Read access is used to obtain the entity. The entity is cloned before
     * assigning it to sandbox, so later it could be mutated without locking the
     * monitor.
     *
     * @param monitor the monitor to pull entity from.
     * @return clone of monitored entity.
     */
    public Entity pull(final Monitor<Entity> monitor) {
        monitor.read(
                value -> {
                    this.set(cloneValue(value));
                    this.check = new WeakReference<>(value);
                });

        return this.entity;
    }

    private boolean push(
            final Monitor<Entity> monitor,
            final boolean force,
            final boolean byReference) {
        monitor.write(value -> {
            this.pushResult = false;
            if (force
                    || byReference
                        && this.check != null
                        && this.check.get() == value
                    || !byReference
                        && this.check != null
                        && this.check.get().equals(value)) {
                this.pushResult = true;
                return this.entity;
            }
            return value;
        });

        if (this.pushResult) {
            check = new WeakReference<>(entity);
            return true;
        }
        return false;
    }

    /**
     * Unconditionally change monitored entity of the monitor.
     *
     * @param monitor the monitor to set the entity to.
     */
    void push(final Monitor<Entity> monitor) {
        this.push(monitor, true, false);
    }

    /**
     * Compare monitored entity with the sandbox copy by reference and set it
     * to monitor if the reference is the same.
     *
     * @param monitor the monitor to set the entity to.
     * @return true if comparison was successful and entity was set to monitor,
     * or false otherwise.
     */
    public boolean casByReference(final Monitor<Entity> monitor) {
        return this.push(monitor, false, true);
    }

    /**
     * Compare monitored entity with the sandbox copy by value and set it
     * to monitor if the reference is the same.
     *
     * @param monitor the monitor to set the entity to.
     * @return true if comparison was successful and entity was set to monitor,
     * or false otherwise.
     */
    public boolean casByValue(final Monitor<Entity> monitor) {
        return this.push(monitor, false, false);
    }

    /**
     * Convert sandbox entity to string.
     *
     * @return String representation of sandbox entity.
     */
    @Override
    public String toString() {
        return this.entity.toString();
    }

    /**
     * Check if sandbox entity is equal to other`s sandbox entity.
     *
     * @param same the other sandbox.
     * @return true if the entity in other sandbox is the same.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object same) {
        if (same == null
                || !this.getClass().equals(same.getClass())) {
            return false;
        }
        return this.entity.equals(((Sandbox<Entity>) same).entity);
    }

    /**
     * Return hash code of sandbox entity.
     *
     * @return hash code of sandbox entity.
     */
    @Override
    public int hashCode() {
        return this.entity.hashCode();
    }

    /**
     * Clone Sandbox.
     *
     * @return New instance of Sandbox identical to this.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Sandbox<Entity> clone() {
        Sandbox<Entity> same
                = new Sandbox<>(Sandbox.cloneValue(this.entity));
        same.check = this.check;
        return same;
    }
}
