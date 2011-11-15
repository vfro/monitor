package c3h8.java.util;

/**
 * Accessor is an interface which is used by monitor to access
 * its value which is cannot be directly returned from getter.
 */
public interface Accessor<Value> {
	Value access(Value value);
}
