package vfro.java.util;

public interface IFluentProperty<Owner, Value> {
	Owner with(Value value);
}
