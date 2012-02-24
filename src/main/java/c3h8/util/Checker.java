package c3h8.util;

/**
 * Checker instance may be used to determinate when {@link Monitor} value
 * becomes to appropriate state for access.
 *
 * In some cases {@link Accessor} should acquire access to value of a
 * {@link Monitor} only after the value becomes into some certain state.
 * Checker instance may be used to define the state.
 *
 * Monitor calls {@link #check} method each time when its value have been
 * changed and allow access value only when {@code check} method returns
 * {@code true}.
 *
 * @param <Value> Type of monitored value.
 */
public interface Checker<Value> {
    /**
     * Check when value of a {@link Monitor} becomes to appropriate state for
     * access.
     *
     * Implementation of this method may throw any runtime exception which will
     * be passed through access methods of a {@link Monitor}.
     *
     * @param value Value of a {@link Monitor} to check. Checker must not
     * change internal state of {@code value} object. Otherwise all other
     * accessors aren't able to track changes and wake up at the proper time.
     *
     * @return {@code true} if {@link Monitor} becomes to appropriate state
     * for access. Otherwise {@code false}.
     */
    boolean check(Value value);
}
