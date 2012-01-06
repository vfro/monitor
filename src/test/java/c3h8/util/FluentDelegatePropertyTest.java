package c3h8.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class FluentDelegatePropertyTest {

    public FluentDelegatePropertyTest() {
    }

    @Test
    public void fluentDelegatePropertyConstructorGet() {
        String stringValue = "FluentProperty";
        FluentDelegateProperty<FluentDelegatePropertyTest, String> stringProperty =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue);
        assertTrue(stringValue == stringProperty.get(),
            "Test FluentDelegateProperty.get() method with value passed into FluentDelegateProperty constructor.");
    }

    @Test
    public void fluentDelegatePropertySetGet() {
        String stringValue = "FluentDelegateProperty";
        FluentDelegateProperty<FluentDelegatePropertyTest, String> stringProperty =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, null);
        stringProperty.set(stringValue);

        assertTrue(stringValue == stringProperty.get(),
            "Test FluentDelegateProperty.get() method with value passed into FluentDelegateProperty.set().");
    }

    @Test
    public void fluentDelegatePropertyWithGet() {
        String stringValue = "FluentDelegateProperty";
        FluentDelegateProperty<FluentDelegatePropertyTest, String> stringProperty =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, null);
        FluentDelegatePropertyTest withResult = stringProperty.with(stringValue);

        assertTrue(stringValue == stringProperty.get(),
            "Test FluentDelegateProperty.get() method with value passed into FluentDelegateProperty.with().");
        assertTrue(withResult == this, "Test FluentDelegateProperty.with() method return value.");
    }

    @Test
    public void fluentDelegatePropertyEqualsTheSame() {
        String stringValue = "FluentDelegateProperty";
        Property<String> subscriber = new Property<String>("subscriber");

        FluentDelegateProperty<FluentDelegatePropertyTest, String> property1 =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue);
        property1.subscribe(subscriber);

        FluentDelegateProperty<FluentDelegatePropertyTest, String> property2 =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue);
        property2.subscribe(subscriber);

        assertEquals(property1, property2, "Fluent delegate properties are equals if they have the same owner, value and subscribers");
    }

    @Test
    public void fluentPropertyNotEqualsDifferentValue() {
        String stringValue1 = "FluentDelegateProperty value1";
        String stringValue2 = "FluentDelegateProperty value2";
        Property<String> subscriber = new Property<String>("subscriber");

        FluentDelegateProperty<FluentDelegatePropertyTest, String> property1 =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue1);
        property1.subscribe(subscriber);

        FluentDelegateProperty<FluentDelegatePropertyTest, String> property2 =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue2);
        property2.subscribe(subscriber);

        assertNotEquals(property1, property2, "Fluent delegate properties are not equals if they have different values");
    }

    @Test
    public void fluentPropertyNotEqualsDifferentOwner() {
        String stringValue = "FluentDelegateProperty";
        String owner1 = "owner 1";
        String owner2 = "owner 2";
        Property<String> subscriber = new Property<String>("subscriber");

        FluentDelegateProperty<String, String> property1 =
            new FluentDelegateProperty<String, String>(owner1, stringValue);
        property1.subscribe(subscriber);

        FluentDelegateProperty<String, String> property2 =
            new FluentDelegateProperty<String, String>(owner2, stringValue);
        property2.subscribe(subscriber);

        assertNotEquals(property1, property2, "Fluent delegate properties are not equals if they have different owners");
    }

    @Test
    public void fluentPropertyNotEqualsDifferentSubscribers() {
        String stringValue = "FluentDelegateProperty";

        Property<String> subscriber1 = new Property<String>("subscriber");
        DelegateProperty<String> subscriber2 = new DelegateProperty<String>("subscriber");

        FluentDelegateProperty<FluentDelegatePropertyTest, String> property1 =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue);
        property1.subscribe(subscriber1);

        FluentDelegateProperty<FluentDelegatePropertyTest, String> property2 =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, stringValue);
        property2.subscribe(subscriber2);

        assertNotEquals(property1, property2, "Fluent delegate properties are not equals if they have different subscribers.");
    }

    @Test
    public void fluentDelegatePropertyHashCode() {
        String value = new String("Property");
        Property<String> property = new Property<String>(value);

        Property<String> subscriber = new Property<String>(value);
        FluentDelegateProperty<FluentDelegatePropertyTest, String> fluentDelegateProperty =
            new FluentDelegateProperty<FluentDelegatePropertyTest, String>(this, value);
        fluentDelegateProperty.subscribe(subscriber);

        assertEquals(fluentDelegateProperty.hashCode(), property.hashCode(), "Test FluentDelegatePropertyTest.hashCode() equals to Property.hashCode() has code.");
    }
}
