package c3h8.java.util;

import java.util.Collection;
import java.util.Collections;

public class FluentCollectionDelegateProperty<Owner, Value, Container extends Collection<Value>>
	extends FluentDelegateProperty<Owner, Container>
	implements IFluentCollectionProperty<Owner, Value, Container> {

	public FluentCollectionDelegateProperty(Owner owner, Container values) {
		super(owner, values);
	}

	@Override
	public Owner withAll(Value... values) {
		Collections.addAll(this.get(), values);
		this.set(this.get());
		return this.with();
	}

	@Override
	public Owner withAll(Collection<? extends Value> values) {
		this.get().addAll(values);
		this.set(this.get());
		return this.with();
	}
}
