package c3h8.util;

/**
 * A property with support of both fluent interface and ability to delegate
 * a value.
 * @param Owner object that have property field.
 * @param Value value of the property.
 * @author Volodymyr Frolov
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

    /**
    /**
     * Compare that instances of {@code FluentDelegateProperty} belongs to the same
     * property class and have the same value, owner and subscribers.
     * @param object Any other {@code object} to compare this property with.
     * @return {@code true} if {@code object} represents property of the same type with
     * the same value. Otherwise {@code false}.
     */
    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) {
            return false;
        }

        FluentDelegateProperty property = (FluentDelegateProperty)object;
        return this.owner == property.with();
    }

    /**
     * Hash code of a fluent delegate property.
     * @return Hash code of a property.
     */
    @Override
    public int hashCode() {
        // only value affects has code of a property
        return super.hashCode();
    }
}
