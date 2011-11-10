package vfro.java.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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

	@Test
	public void propertySameValuesEquals() {
		Property<String> stringProperty1 = new Property<String>("Property");
		Property<String> stringProperty2 = new Property<String>("Property");

		assertTrue(stringProperty1.equals(stringProperty2), "Test Property.equals() method with the same values.");
	}

	@Test
	public void propertyDifferentValuesEquals() {
		Property<String> stringProperty1 = new Property<String>("Property 1");
		Property<String> stringProperty2 = new Property<String>("Property 2");

		assertFalse(stringProperty1.equals(stringProperty2), "Test Property.equals() method with different values.");
	}

	@Test
	public void propertyDifferentPropertyClassesEquals() {
		Property<String> stringProperty1 = new Property<String>("Property");
		Property<String> stringProperty2 = new Property<String>("Property") {};

		assertFalse(stringProperty1.equals(stringProperty2), "Test Property.equals() method with different classes of properties.");
	}

	private static class Object1 {
		@Override
		public boolean equals(Object object) {
			return true;
		}
	}

	private static class Object2 {
		@Override
		public boolean equals(Object object) {
			return true;
		}
	}

	@Test
	public void propertyDifferentValueClassesEquals() {
		Property<Object1> property1 = new Property<Object1>(new Object1());
		Property<Object2> property2 = new Property<Object2>(new Object2());

		assertTrue(property1.equals(property2), "Test Property.equals() method with different classes of values.");
	}

	@Test
	public void propertyNullEquals() {
		Property<String> stringProperty1 = new Property<String>(null);
		Property<String> stringProperty2 = new Property<String>("Property");

		assertFalse(stringProperty1.equals(stringProperty2), "Test Property.equals() method with null owner value.");
		assertFalse(stringProperty2.equals(null), "Test Property.equals() method with null parameter value.");
		assertFalse(stringProperty2.equals(stringProperty1), "Test Property.equals() method with null parameter property value.");
	}

	@Test
	public void propertyHashCode() {
		String value = new String("Property");
		Property<String> property = new Property<String>(value);
		assertEquals(property.hashCode(), value.hashCode(), "Test Property.hashCode() equals to value has code.");
	}

	@Test
	public void propertyHashCodeForNull() {
		Property<String> property = new Property<String>(null);
		assertTrue(property.hashCode() == 0, "Test Property.hashCode() for null value.");
	}
}
