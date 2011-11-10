package vfro.java.util;

public class FluentProperty<Owner, Value>
	extends Property<Value>
	implements IFluentProperty<Owner, Value> {

	private Owner owner;

	public FluentProperty(Owner owner, Value value) {
		super(value);
		this.owner = owner;
	}

	@Override
	public Owner with(Value value) {
		this.set(value);
		return this.with();
	}

	protected Owner with() {
		return this.owner;
	}
}
