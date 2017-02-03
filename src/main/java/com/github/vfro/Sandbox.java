package com.github.vfro;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Sandbox can be used to access a copy of value controlled by {@link Monitor}.
 * Also it provides compare and swap operations over monitor object. Compare and Swap operations
 * can compare values either by reference or by value.
 *
 * @author Volodymyr Frolov
 * @param <Value> value of the monitor.
 */
@SuppressWarnings("LocalVariableHidesMemberVariable")
public final class Sandbox<Value extends Cloneable> implements Cloneable {

    private WeakReference<Value> check;
    private Value value;
    private boolean pushResult;

    @SuppressWarnings({"unchecked", "UseSpecificCatch"})
    private static <Value> Value cloneValue(Value value) {
        Value result = null;
        try {
            Method clone = value.getClass().getDeclaredMethod("clone");
            result = (Value) clone.invoke(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot clone instance of " + value.getClass().getName(), e
            );
        }
        return result;
    }

    /**
     * Create new instance of Sandbox with null as its value.
     */
    public Sandbox() {
        this.value = null;
        this.check = null;
        this.pushResult = false;
    }

    /**
     * Create new instance of Sandbox and assign particular value to it.
     *
     * @param value initial value of the sandbox.
     */
    public Sandbox(Value value) {
        this.value = value;
        this.check = null;
    }

    /**
     * Create new instance of Sandbox and {@link #pull(com.github.vfro.Monitor)} its
     * value from monitor.
     *
     * @param monitor initial value is pulled from the monitor.
     */
    public Sandbox(Monitor<Value> monitor) {
        super();
        this.pull(monitor);
    }

    /**
     * Access value help by Sandbox. If the value was previously pulled from
     * Monitor, then it is a copy of the value contained in {@link Monitor}. The
     * copy can be changed without locking the monitor.
     *
     * @return value held by Sandbox.
     */
    public Value get() {
        return this.value;
    }

    /**
     * Assign value to Sandbox.
     *
     * @param value new value for the sandbox.
     */
    public void set(Value value) {
        this.value = value;
    }

    /**
     * Obtain value from the monitor, assign its clone to sandbox and return the
     * clone of obtained value. Read access is used to obtain the value. The
     * value is cloned before assigning it to sandbox, so it could be changed
     * without locking the monitor.
     *
     * @param monitor the monitor to take value from.
     * @return the clone of value obtained from monitor.
     */
    public Value pull(Monitor<Value> monitor) {
        monitor.readAccess(
                value -> {
                    this.set(cloneValue(value));
                    this.check = new WeakReference<>(value);
                });

        return this.value;
    }

    private boolean push(Monitor<Value> monitor, final boolean force, final boolean byReference) {
        monitor.writeAccess(
                value -> {
                    this.pushResult = false;
                    if (force
                    || byReference && this.check != null && this.check.get() == value
                    || !byReference && this.check != null && this.check.get().equals(value)) {
                        this.pushResult = true;
                        return this.value;
                    }
                    return value;
                });

        if (this.pushResult) {
            check = new WeakReference<>(value);
            return true;
        }
        return false;
    }

    /**
     * Unconditionally set value of the sandbox to monitor.
     *
     * @param monitor the monitor to set the value to.
     */
    void push(Monitor<Value> monitor) {
        this.push(monitor, true, false);
    }

    /**
     * Compare monitor`s value with the sandbox value by reference and set it to
     * monitor if the reference is the same.
     *
     * @param monitor the monitor to set the value to.
     * @return true if comparison was successful and value was set to monitor,
     * or false otherwise.
     */
    public boolean casByReference(Monitor<Value> monitor) {
        return this.push(monitor, false, true);
    }

    /**
     * Compare monitor`s value with the sandbox value by value and set it to
     * monitor if the value is the same.
     *
     * @param monitor the monitor to set the value to.
     * @return true if comparison was successful and value was set to monitor,
     * or false otherwise.
     */
    public boolean casByValue(Monitor<Value> monitor) {
        return this.push(monitor, false, false);
    }

    /**
     * Convert sandbox value to string.
     *
     * @return String representation of sandbox value.
     */
    @Override
    public String toString() {
        return this.value.toString();
    }

    /**
     * Check if sandbox value is equal to other`s sandbox value.
     *
     * @param same the other sandbox.
     * @return true if the other`s sandbox value is the same.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object same) {
        if (same == null
                || !this.getClass().equals(same.getClass())) {
            return false;
        }
        return this.value.equals(((Sandbox<Value>) same).value);
    }

    /**
     * Return hash code of sandbox value.
     *
     * @return hash code of sandbox value.
     */
    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    /**
     * Clone Sandbox.
     *
     * @return New instance of Sandbox identical to this.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Sandbox<Value> clone() {
        Sandbox<Value> same
                = new Sandbox<>(Sandbox.cloneValue(this.value));
        same.check = this.check;
        return same;
    }
}
