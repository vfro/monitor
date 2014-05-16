package c3h8.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public final class Sandbox<Value extends Cloneable> implements Cloneable {

    private WeakReference<Value> check;
    private Value value;
    private boolean pushResult;

    @SuppressWarnings("unchecked")
    private static <Value> Value cloneValue(Value value) {
        Value result = null;
        try {
            Method clone = value.getClass().getDeclaredMethod("clone");
            result = (Value)clone.invoke(value);
        } catch(Throwable e) {
            throw new IllegalArgumentException(
                "Cannot clone instance of " + value.getClass().getName(), e
            );
        }
        return result;
    }

    public Sandbox() {
        this.value = null;
        this.check = null;
        this.pushResult = false;
    }

    public Sandbox(Value value) {
        this.value = value;
        this.check = null;
    }

    public Sandbox(Monitor<Value> monitor) {
        super();
        this.pull(monitor);
    }

    public Value get() {
        return this.value;
    }

    public void set(Value value) {
        this.value = value;
    }

    public Value pull(Monitor<Value> monitor) {
        monitor.readAccess(
            value -> {
                Sandbox.this.set(Sandbox.cloneValue(value));
                Sandbox.this.check = new WeakReference<Value>(value);
            });

        return this.value;
    }

    public boolean push(Monitor<Value> monitor) {
        return this.push(monitor, false);
    }

    public boolean push(Monitor<Value> monitor, final boolean force) {
        monitor.writeAccess(
            value -> {
                Sandbox.this.pushResult = false;
                if (force ||
                    (Sandbox.this.check != null && Sandbox.this.check.get() == value)) {
                    Sandbox.this.pushResult = true;
                    return Sandbox.this.value;
                }
                return value;
            });

        if (this.pushResult) {
            check = new WeakReference<Value>(value);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == null
            || !this.getClass().equals(obj.getClass())
           ) {
            return false;
        }
        Sandbox<Value> same = (Sandbox<Value>)obj;
        return this.value.equals(same.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public Sandbox<Value> clone() {
        Sandbox<Value> same =
            new Sandbox<Value>(Sandbox.cloneValue(this.value));
        same.check = this.check;
        return same;
    }
}
