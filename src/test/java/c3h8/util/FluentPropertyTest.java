package c3h8.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

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
}
