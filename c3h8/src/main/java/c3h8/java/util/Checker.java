package c3h8.java.util;

/**
 * Checker to determinate if {@link Monitor} value becomed to appropriate state for access.
 * In some cases {@link Accessor} should acquire access to a value of a {@link Monitor} only
 * after the value becomed into some certain state. A state of the {@link Monitor} value may
 * be checked by instances of this interface.
 * Monitor will call {@link #check} method each time when its value is changed
 * and allow access of its value only when {@code check} method returned {@code true}.
 */
public interface Checker<Value> {
	/**
	 * Check if a value of a {@link Monitor} becomed to appropriate state for access.
	 * It is legal to throw runtime exceptions from checker and the exception instance will
	 * be thrown from access methods.
         * @param value. Value of a {@link Monitor} to check. Checker must not change internal
	 * state of a {@code value} object. Otherwise all other accessors won't be able to track
	 * changes and wake up.
	 * @return {@code true} if {@link Monitor} becomed to appropriate state for access.
	 * Otherwise {@code false}.
	 */
	boolean check(Value value);
}
