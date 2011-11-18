package c3h8.util;

import java.util.Collection;

/**
 * The aim of {@code IFluentCollectionProperty} interface is to unify {@link FluentCollectionProperty}
 * and {@link FluentCollectionDelegateProperty} classes into one hierarchy.
 *
 * @author Volodymyr Frolov &lt;frolov.volodymyr@gmail.com&gt;
 */
public interface IFluentCollectionProperty<Owner, Value, Container extends Collection<Value>>
	extends IFluentProperty<Owner, Container> {
	/**
	 * Add all values into property which has collection type value and link fluently with owner.
	 * @param values values to add to collection property.
	 * @return owner of a property.
	 */
	Owner withAll(Value... values);

	/**
	 * Add all values from collection into property which has collection type value and link
	 * fluently with owner.
	 * @param values values to add to collection property.
	 * @return owner of a property.
	 */
	Owner withAll(Collection<? extends Value> values);
}