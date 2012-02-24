package c3h8.util;

/**
 * Value of a {@link Monitor} can be accessed only through Accessor instance.
 *
 * @param <Value> Type of monitored value.
 */
public interface Accessor<Value> {
    /**
     * Access value of a {@link Monitor} in shared (read) or exclusive
     * (write) mode. In write mode Accessor may change the state of value
     * and modify reference to monitored value.
     *
     * @param value Value of a {@link Monitor}. Implementaion of this method
     * should change internal state of {@code value} object only in write mode.
     *
     * @return In case of write access returned value will be used as new value
     * of monitor. {@code value} parameter may be returned to leave reference
     * on the monitored value the same. In case of read access returned value
     * is ingored.
     */
    Value access(Value value);
}
