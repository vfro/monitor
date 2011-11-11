package c3h8.java.util;

/**
 * The aim of IFluentProperty interface is to unify {@code FluentProperty} and
 * {@code FluentDelegateProperty} classes into one hierarchy.
 */
public interface IFluentProperty<Owner, Value> {
	/**
	 * Set a new {@code value} for a property and link owner fluently.<p>
	 * Example:<p>
	 * <pre>
	 * Person.lastName.with("Smith")
	 *       .firstName.with("John");
	 * </pre>
	 * @param value new value for a property.
	 * @return link to property owner object.
	 */
	Owner with(Value value);
}
