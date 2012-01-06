package c3h8.util;

/**
 * A {@link Property} with support of fluent interface.<p>
 * <pre>
 * public class Person {
 *    public final FluentProperty&lt;String&gt; firstName = new FluentProperty&lt;String&gt;(null);
 *    public final FluentProperty&lt;String&gt; lastName = new FluentProperty&lt;String&gt;(null);
 * }
 * Person person = new Person();
 * person.firstName.with("John")
 *       .lastName.with("Smith");
 * </pre>
 * @param Owner object that have property field.
 * @param Value value of the property.
 * @author Volodymyr Frolov
 */
public class FluentProperty<Owner, Value>
    extends Property<Value>
    implements IFluentProperty<Owner, Value> {

    private Owner owner;

    /**
     * Create new property with support of fluent interface. Assing an owner and
     * value to new property.
     * @param owner owner object of a fluent property.
     * @param value initial value of fluent property.
     */
    public FluentProperty(Owner owner, Value value) {
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
        return this.with();
    }

    /**
     * Get an owner of a property.
     * @return owner of a property which can be used as a return value for
     * some other implementations of fluent interface properties.
     */
    protected Owner with() {
        return this.owner;
    }

    /**
     * Compare that properties are contained in the same property container class have
     * the same value and owner.
     * Any kind of {@code Property} childs should override this method.
     * @param object Any other {@code object} to compare this fluent property with.
     * @return {@code true} if {@code object} represents fluent property of the same
     * type with the same value. Otherwise {@code false}.
     */
    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) {
            return false;
        }

        FluentProperty property = (FluentProperty)object;
        return this.owner == property.with();
    }

    /**
     * Hash code of a fluent property.
     * @return Hash code of a property.
     */
    @Override
    public int hashCode() {
        // only value affects has code of a property
        return super.hashCode();
    }
}
