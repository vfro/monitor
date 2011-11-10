package vfro.java.util;

public class FluentProperty<Owner, Value> extends Property<Value> {

	private Owner owner;

	public FluentProperty(Owner owner, Value value) {
		super(value);
		this.owner = owner;
	}

	public Owner with(Value value) {
		this.set(value);
		return this.owner;
	}
}
