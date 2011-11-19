package c3h8.util;

import java.util.Collection;
import java.util.Collections;

/**
 * A property with support of fluent interface for collections and ability to delegate a value.
 * Delegation of a value will be triggered each time when fluent interface was used to insert values
 * into collection property.
 * @param Owner object that have property field.
 * @param Value value of the property.
 * @param Container collection of values.
 * @author Volodymyr Frolov
 */
public class FluentCollectionDelegateProperty<Owner, Value, Container extends Collection<Value>>
    extends FluentDelegateProperty<Owner, Container>
    implements IFluentCollectionProperty<Owner, Value, Container> {

    public FluentCollectionDelegateProperty(Owner owner, Container values) {
        super(owner, values);
    }

    /**
     * Add all values into property and link fluently with owner, trigger delegation.
     * @param values values to add to collection property.
     * @return owner of a property.
     */
    @Override
    public Owner withAll(Value... values) {
        Collections.addAll(this.get(), values);
        this.set(this.get());
        return this.with();
    }

    /**
     * Add all values from collection into property and link fluently with owner, trigger delegation.
     * @param values values to add to collection property.
     * @return owner of a property.
     */
    @Override
    public Owner withAll(Collection<? extends Value> values) {
        this.get().addAll(values);
        this.set(this.get());
        return this.with();
    }
}
