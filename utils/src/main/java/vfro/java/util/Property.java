package vfro.java.util;

public class Property<Value> {
	private Value value;

	public Property(Value value) {
		this.value = value;
	}

	public Value get() {
		return this.value;
	}

	public void set(Value value) {
		this.value = value;
	}
}
