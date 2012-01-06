package c3h8.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class FluentPropertyTest {

    public FluentPropertyTest() {
    }

    @Test
    public void fluentPropertyConstructorGet() {
        String stringValue = "FluentProperty";
        FluentProperty<FluentPropertyTest, String> stringProperty = new FluentProperty<FluentPropertyTest, String>(this, stringValue);
        assertTrue(stringValue == stringProperty.get(), "Test FluentProperty.get() method with value passed into FluentProperty constructor.");
    }

    @Test
    public void fluentPropertySetGet() {
        String stringValue = "FluentProperty";
        FluentProperty<FluentPropertyTest, String> stringProperty = new FluentProperty<FluentPropertyTest, String>(this, null);
        stringProperty.set(stringValue);

        assertTrue(stringValue == stringProperty.get(), "Test FluentProperty.get() method with value passed into FluentProperty.set().");
    }

    @Test
    public void fluentPropertyWithGet() {
        String stringValue = "FluentProperty";
        FluentProperty<FluentPropertyTest, String> stringProperty = new FluentProperty<FluentPropertyTest, String>(this, null);
        FluentPropertyTest withResult = stringProperty.with(stringValue);

        assertTrue(stringValue == stringProperty.get(), "Test FluentProperty.get() method with value passed into FluentProperty.with().");
        assertTrue(withResult == this, "Test FluentProperty.with() method return value.");
    }

    @Test
    public void fluentPropertyEqualsTheSame() {
        String stringValue = "FluentProperty";
        FluentProperty<FluentPropertyTest, String> property1 = new FluentProperty<FluentPropertyTest, String>(this, stringValue);
        FluentProperty<FluentPropertyTest, String> property2 = new FluentProperty<FluentPropertyTest, String>(this, stringValue);
        assertEquals(property1, property2, "Fluent properties are equals if they have the same owner and value");
    }

    @Test
    public void fluentPropertyNotEqualsDifferentValue() {
        String stringValue1 = "FluentProperty value1";
        String stringValue2 = "FluentProperty value2";
        FluentProperty<FluentPropertyTest, String> property1 = new FluentProperty<FluentPropertyTest, String>(this, stringValue1);
        FluentProperty<FluentPropertyTest, String> property2 = new FluentProperty<FluentPropertyTest, String>(this, stringValue2);
        assertNotEquals(property1, property2, "Fluent properties are not equals if they have different values");
    }

    @Test
    public void fluentPropertyNotEqualsDifferentOwner() {
        String owner1 = "owner 1";
        String owner2 = "owner 2";
        String stringValue = "FluentProperty value1";
        FluentProperty<String, String> property1 = new FluentProperty<String, String>(owner1, stringValue);
        FluentProperty<String, String> property2 = new FluentProperty<String, String>(owner2, stringValue);
        assertNotEquals(property1, property2, "Fluent properties are not equals if they have different owner instances");
    }

    @Test
    public void fluentPropertyHashCode() {
        String value = new String("Property");
        Property<String> property = new Property<String>(value);
        FluentProperty<String, String> fluentProperty = new FluentProperty<String, String>("owner", value);

        assertEquals(fluentProperty.hashCode(), property.hashCode(), "Test FluentProperty.hashCode() equals to Property.hashCode() has code.");
    }
}
