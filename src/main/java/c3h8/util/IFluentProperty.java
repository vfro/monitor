package c3h8.util;

/**
 * The aim of {@code IFluentProperty} interface is to unify {@link FluentProperty} and
 * {@link FluentDelegateProperty} classes into one hierarchy.
 * @param Owner object that have property field.
 * @param Value value of the property.
 * @author Volodymyr Frolov
 */
public interface IFluentProperty<Owner, Value> {
    /**
     * Set a new {@code value} for a property and link owner fluently.<p>
     * Example:<p>
     * <pre>
     * Person.firstName.with("John")
     *    .lastName.with("Smith");
     * </pre>
     * @param value new value for a property.
     * @return link to property owner object.
     */
    Owner with(Value value);
}
