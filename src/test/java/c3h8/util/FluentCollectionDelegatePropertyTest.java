package c3h8.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FluentCollectionDelegatePropertyTest {

    private static class SetTriggerChecker extends Property<List<String>> {
        private boolean setTriggered = false;

        public SetTriggerChecker() {
            super(null);
        }

        @Override
        public void set(List<String> value) {
            this.setTriggered = true;
            super.set(value);
        }

        public boolean isSetTriggered() {
            return this.setTriggered;
        }
    };
    
    public FluentCollectionDelegatePropertyTest() {
    }

    @Test
    public void fluentCollectionDelegatePropertyConstructor() {
        LinkedList<String> values = new LinkedList<String>();
        Collections.addAll(values, "1", "2", "3");

        FluentCollectionDelegateProperty<FluentCollectionDelegatePropertyTest, String, List<String>> property =
            new FluentCollectionDelegateProperty<FluentCollectionDelegatePropertyTest, String, List<String>>(this, values);
        assertTrue(property.get() == values, "Test FluentCollectionDelegateProperty constructor.");
    }

    @Test
    public void fluentCollectionDelegatePropertyWithArray() {
        LinkedList<String> values = new LinkedList<String>();
        Collections.addAll(values, "1", "2", "3");

        FluentCollectionDelegateProperty<FluentCollectionDelegatePropertyTest, String, List<String>> property =
            new FluentCollectionDelegateProperty<FluentCollectionDelegatePropertyTest, String, List<String>>(this, new LinkedList<String>());

        SetTriggerChecker collectionProperty = new SetTriggerChecker();
        property.subscribe(collectionProperty);

        FluentCollectionDelegatePropertyTest withResult = property.withAll("1", "2", "3");
        assertEquals(collectionProperty.get(), values, "Test FluentCollectionDelegateProperty.withAll(T...) method.");
        assertTrue(collectionProperty.isSetTriggered(), "Test FluentCollectionDelegateProperty.withAll(T...) triggers set() of subscribers.");
        assertTrue(withResult == this, "Test FluentCollectionDelegateProperty.withAll(T...) method return value.");
    }


    @Test
    public void fluentCollectionDelegatePropertyWithCollection() {
        LinkedList<String> values = new LinkedList<String>();
        Collections.addAll(values, "1", "2", "3");

        Set<String> addValues = new TreeSet<String>();
        Collections.addAll(addValues, "1", "2", "3");

        FluentCollectionDelegateProperty<FluentCollectionDelegatePropertyTest, String, List<String>> property =
            new FluentCollectionDelegateProperty<FluentCollectionDelegatePropertyTest, String, List<String>>(this, new LinkedList<String>());

        SetTriggerChecker collectionProperty = new SetTriggerChecker();
        property.subscribe(collectionProperty);

        FluentCollectionDelegatePropertyTest withResult = property.withAll(addValues);
        assertEquals(collectionProperty.get(), values, "Test FluentCollectionDelegateProperty.withAll(Collection<T>) method.");
        assertTrue(collectionProperty.isSetTriggered(), "Test FluentCollectionDelegateProperty.withAll(Collection<T>) triggers set() of subscribers.");
        assertTrue(withResult == this, "Test FluentCollectionDelegateProperty.withAll(Collection<T>) method return value.");
    }

}
