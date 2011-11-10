package vfro.java.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class FluentDelegateProperty<Owner, Value>
	extends DelegateProperty<Value>
	implements IFluentProperty<Owner, Value> {

	private Owner owner;

	public FluentDelegateProperty(Owner owner, Value value) {
		super(value);
		this.owner = owner;
	}

	@Override
	public Owner with(Value value) {
		this.set(value);
		return this.owner;
	}

	protected Owner with() {
		return this.owner;
	}
}
