package c3h8.java.util;

public interface IFluentProperty<Owner, Value> {
	Owner with(Value value);
}
