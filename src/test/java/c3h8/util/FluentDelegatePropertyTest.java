package c3h8.util;

import org.testng.annotations.Test;

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
}
