package c3h8.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A property with support of delegation a value to other properties.
 * Each time when a value of delegate property is changes it will be delegated to
 * all subscribers.<p>
 *
 * <pre>
 * public class Author {
 *    public final DelegateProperty&lt;String&gt; id = new DelegateProperty&lt;String&gt;(null);
 *    // ...
 * }
 *
 * public class Book {
 *    public final Property&lt;String&gt; authorId = new Property&lt;String&gt;();
 *    // ...
 *    public Book(Author ofAuthor) {
 *       authorId.id.subscribe(this.authorId);
 *    }
 * }
 * </pre>
 * Delegate property stores weak references to all subscribers so it is not necessary to unsubscribe
 * before subscriber becomes unreachable.<p>
 * Delegate property supports loops. You can tie several properties into a loop with {@link #subscribe}
 * and setting value to any of them will cause the value to be setted to all properties in a loop.
 * @param Value value of the property.
 * @author Volodymyr Frolov
 */
public class DelegateProperty<Value> extends Property<Value> {

    private Lock subscribersGuard = new ReentrantLock();
    private Set<Property<Value>> subscribers = Collections.newSetFromMap(new WeakHashMap<Property<Value>, Boolean>());

    /**
     * Get set of subscribers.
     * @return Set of all subscribed properties.
     */
    protected Set<Property<Value>> getSubscribers() {
        return subscribers;
    }

    private ThreadLocal<Boolean> isLoop = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    /**
     * Create new delegate property object.
     * @param value Initial value of delegate property.
     */
    public DelegateProperty(Value value) {
        super(value);
    }

    /**
     * Set a new value for delegate property as well as all property subscribers.
     * @param value new value of a property.
     */
    @Override
    public void set(Value value) {
        if (this.isLoop.get()) {
            // Prevent calling delegates in a loop.
            return;
        }

        try {
            this.isLoop.set(true);

            super.set(value);
            try {
                subscribersGuard.lock();
                for(Property<Value> property : this.subscribers) {
                    property.set(value);
                }

                // Hash code of all values has been changed after setter.
                // All values should be re-added back to subscribers set.
                Set<Property<Value>> subscribersCache = Collections.newSetFromMap(new WeakHashMap<Property<Value>, Boolean>());
                subscribersCache.addAll(this.subscribers);
                this.subscribers.clear();
                this.subscribers.addAll(subscribersCache);
            }
            finally {
                subscribersGuard.unlock();
            }
        }
        finally {
            this.isLoop.set(false);
        }
    }

    /**
     * Subscribe to a changing value of the property. Subscribers may be tied in a loop.
     * The value of the property will be setted to subscriber directly after subscription and
     * each time when it will be changed after that.
     * @param property subscriber.
     * @return {@code true} if subscriber wasn't subscribed to this property yet. Otherwise {@code false}.
     */
    public boolean subscribe(Property<Value> property) {
        boolean result;
        try {
            property.set(this.get());
            subscribersGuard.lock();
            result = this.subscribers.add(property);
        }
        finally {
            subscribersGuard.unlock();
        }
        return result;
    }

    /**
     * Unsubscribe from a property.
     * @param property subscriber.
     * @return {@code true} if subscriber was subscribed to this property and sucessfuly
     * unscubscribed. Otherwise {@code false}.
     */
    public boolean unsubscribe(Property<Value> property) {
        boolean result;
        try {
            subscribersGuard.lock();
            result = this.subscribers.remove(property);
        }
        finally {
            subscribersGuard.unlock();
        }
        return result;
    }

    /**
     * Compare that instances of {@code DelegateProperty} belongs to the same property class
     * and have the same value and subscribers.
     * @param object Any other {@code object} to compare this property with.
     * @return {@code true} if {@code object} represents property of the same type with
     * the same value. Otherwise {@code false}.
     */
    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) {
            return false;
        }

        DelegateProperty property = (DelegateProperty)object;
        return this.subscribers.equals(property.getSubscribers());
    }

    /**
     * Hash code of a delegate property.
     * @return Hash code of a property.
     */
    @Override
    public int hashCode() {
        // only value affects has code of a property
        return super.hashCode();
    }
}
