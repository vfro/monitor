package com.github.vfro;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Sandbox can be used to access a copy of monitored entity.
 * It also provides Compare and Swap operation that can compare
 * monitored entities either by reference or by value.
 *
 * @author Volodymyr Frolov
 * @param <Entity> entity of the monitor.
 */
@SuppressWarnings("LocalVariableHidesMemberVariable")
public final class Sandbox<Entity extends Cloneable> implements Cloneable {

    private WeakReference<Entity> check;
    private Entity entity;
    private boolean pushResult;

    @SuppressWarnings({"unchecked", "UseSpecificCatch"})
    private static <Entity> Entity cloneValue(Entity entity) {
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
     * Create a new instance of Sandbox with null as its entity.
     */
    public Sandbox() {
        this.entity = null;
        this.check = null;
        this.pushResult = false;
    }

    /**
     * Create new instance of Sandbox and assign particular entity to it.
     *
     * @param value initial entity of the sandbox.
     */
    public Sandbox(Entity value) {
        this.entity = value;
        this.check = null;
    }

    /**
     * Create new instance of Sandbox and {@link #pull(com.github.vfro.Monitor)} its
 entity from monitor.
     *
     * @param monitor initial entity is pulled from the monitor.
     */
    public Sandbox(Monitor<Entity> monitor) {
        super();
        this.pull(monitor);
    }

    /**
     * Access entity help by Sandbox. If the entity was previously pulled from
 Monitor, then it is a copy of the entity contained in {@link Monitor}. The
     * copy can be changed without locking the monitor.
     *
     * @return entity held by Sandbox.
     */
    public Entity get() {
        return this.entity;
    }

    /**
     * Assign entity to Sandbox.
     *
     * @param value new entity for the sandbox.
     */
    public void set(Entity value) {
        this.entity = value;
    }

    /**
     * Obtain entity from the monitor, assign its clone to sandbox and return the
 clone of obtained entity. Read access is used to obtain the entity. The
 entity is cloned before assigning it to sandbox, so it could be changed
 without locking the monitor.
     *
     * @param monitor the monitor to take entity from.
     * @return the clone of entity obtained from monitor.
     */
    public Entity pull(Monitor<Entity> monitor) {
        monitor.read(
                value -> {
                    this.set(cloneValue(value));
                    this.check = new WeakReference<>(value);
                });

        return this.entity;
    }

    private boolean push(Monitor<Entity> monitor, final boolean force, final boolean byReference) {
        monitor.write(value -> {
                    this.pushResult = false;
                    if (force
                    || byReference && this.check != null && this.check.get() == value
                    || !byReference && this.check != null && this.check.get().equals(value)) {
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
     * Unconditionally set entity of the sandbox to monitor.
     *
     * @param monitor the monitor to set the entity to.
     */
    void push(Monitor<Entity> monitor) {
        this.push(monitor, true, false);
    }

    /**
     * Compare monitor`s entity with the sandbox entity by reference and set it to
 monitor if the reference is the same.
     *
     * @param monitor the monitor to set the entity to.
     * @return true if comparison was successful and entity was set to monitor,
 or false otherwise.
     */
    public boolean casByReference(Monitor<Entity> monitor) {
        return this.push(monitor, false, true);
    }

    /**
     * Compare monitor`s entity with the sandbox entity by entity and set it to
 monitor if the entity is the same.
     *
     * @param monitor the monitor to set the entity to.
     * @return true if comparison was successful and entity was set to monitor,
 or false otherwise.
     */
    public boolean casByValue(Monitor<Entity> monitor) {
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
     * @return true if the other`s sandbox entity is the same.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object same) {
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
