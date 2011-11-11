package c3h8.java.util;

import java.util.Collection;
import java.util.Collections;

/**
 * A property with support of fluent interface for a values of collection types.
 */
public class FluentCollectionProperty<Owner, Value, Container extends Collection<Value>>
	extends FluentProperty<Owner, Container>
	implements IFluentCollectionProperty<Owner, Value, Container> {

	private Owner owner;

	/**
	 * Create new instance of fluent collection property.
	 * @param owner owner object of a fluent property.
	 * @param values a container that will be used by a property to store
	 * all values at.
	 */
	public FluentCollectionProperty(Owner owner, Container values) {
		super(owner, values);
	}

	/**
	 * Add all values into property and link fluently with owner.
	 * @param values values to add to collection property.
	 * @return owner of a property.
	 */
	@Override
	public Owner withAll(Value... values) {
		Collections.addAll(this.get(), values);
		return this.with();
	}

	/**
	 * Add all values from collection into property and link fluently with owner.
	 * @param values values to add to collection property.
	 * @return owner of a property.
	 */
	@Override
	public Owner withAll(Collection<? extends Value> values) {
		this.get().addAll(values);
		return this.with();
	}
}
