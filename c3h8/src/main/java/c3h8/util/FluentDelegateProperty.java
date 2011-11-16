package c3h8.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A property with support of both fluent interface and ability to delegate
 * a value.
 */
public class FluentDelegateProperty<Owner, Value>
	extends DelegateProperty<Value>
	implements IFluentProperty<Owner, Value> {

	private Owner owner;

	/**
	 * Create new instance of fluent delegate property.
	 * @param owner owner object of a fluent property.
	 * @param value initial value of fluent property.
	 */
	public FluentDelegateProperty(Owner owner, Value value) {
		super(value);
		this.owner = owner;
	}

	/**
	 * Set new value to a property and link with owner fluently.
	 * @param value new value of a property.
	 * @return Owner of a property to link with.
	 */
	@Override
	public Owner with(Value value) {
		this.set(value);
		return this.owner;
	}

	/**
	 * Get an owner of a property.
	 * @return owner of a property which can be used as a return value for
	 * some other implementations of fluent interface properties.
	 */
	protected Owner with() {
		return this.owner;
	}
}
