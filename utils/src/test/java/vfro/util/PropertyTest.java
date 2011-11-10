package vfro.java.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class PropertyTest {

	public PropertyTest() {
	}

	@Test
	public void propertyConstructorGet() {
		String stringValue = "Property";
		Property<String> stringProperty = new Property<String>(stringValue);
		assertTrue(stringValue == stringProperty.get(), "Test Property.get() method with value passed into Property constructor.");
	}

	@Test
	public void propertySetGet() {
		String stringValue = "Property";
		Property<String> stringProperty = new Property<String>(null);
		stringProperty.set(stringValue);

		assertTrue(stringValue == stringProperty.get(), "Test Property.get() method with value passed into Property.set().");
	}
}
