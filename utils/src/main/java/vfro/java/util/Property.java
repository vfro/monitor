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

	@Override
	public boolean equals(Object object) {
		if (this.value == null) {
			return false;
		} else if (object == null) {
			return false;
		} else if (this == object) {
			return true;
		} else if (object.getClass() != this.getClass()) {
			return false;
		}

		Property property = (Property)object;
		return value.equals(property.get());
	}

	@Override
	public int hashCode() {
		return value == null ? 0 : value.hashCode();
	}
}
