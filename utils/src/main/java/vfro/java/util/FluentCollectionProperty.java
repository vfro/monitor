package vfro.java.util;

import java.util.Collection;
import java.util.Collections;

public class FluentCollectionProperty<Owner, Value, Container extends Collection<Value>>
	extends FluentProperty<Owner, Container>
	implements IFluentCollectionProperty<Owner, Value, Container> {

	private Owner owner;

	public FluentCollectionProperty(Owner owner, Container values) {
		super(owner, values);
	}

	@Override
	public Owner withAll(Value... values) {
		Collections.addAll(this.get(), values);
		return this.with();
	}

	@Override
	public Owner withAll(Collection<? extends Value> values) {
		this.get().addAll(values);
		return this.with();
	}
}
