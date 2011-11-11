package c3h8.java.util;

import java.util.Collection;

public interface IFluentCollectionProperty<Owner, Value, Container extends Collection<Value>>
	extends IFluentProperty<Owner, Container> {
	Owner withAll(Value... value);
	Owner withAll(Collection<? extends Value> value);
}
