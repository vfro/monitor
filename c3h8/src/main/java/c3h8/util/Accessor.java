package c3h8.util;

/**
 * Accessor is an interface which is used by {@link Monitor} to access
 * its value which cannot be directly returned from getter.
 */
public interface Accessor<Value> {
	/**
	 * Override this method to access the value of the monitor.
	 * When this method is called either shared or exclusive synchronization
	 * was already done.
	 * @param value The value of the monitor to access. This method should change
	 * the internal state of {@code value} only in case of write monitor access.
	 * In this case {@code value} should be used as a return value anyway.
	 * @return In case of write access the returned value will become a new value
	 * of a monitor. If write accessor changed the internal state of {@code value}
	 * object, it must return this parameter anyway.
	 * In case of read access the returned value is ingored and may be either {@code value}
         * or null.
	 */
	Value access(Value value);
}
