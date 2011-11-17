package c3h8.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DelegatePropertyTest {
	public DelegatePropertyTest() {
	}

	@Test
	public void propertySubscribe() {
		String stringValue = "Value";

		Property<String> subscriber = new Property<String>(null);
		DelegateProperty<String> property = new DelegateProperty<String>(null);
		assertTrue(property.subscribe(subscriber), "Test DelegateProperty.subscribe() method with first delegation of a value.");

		property.set(stringValue);
		assertTrue(property.get() == stringValue, "Test DelegateProperty.get() method with value passed into DelegateProperty.set().");
		assertTrue(subscriber.get() == stringValue, "Test Property.get() method after value delegation.");

		assertFalse(property.subscribe(subscriber), "Test DelegateProperty.subscribe() method with re-delegation of a value.");
	}

	@Test
	public void propertySetOnSubscribe() {
		String stringValue = "Value";

		Property<String> subscriber = new Property<String>(null);
		DelegateProperty<String> property = new DelegateProperty<String>(stringValue);
		property.subscribe(subscriber);
		assertTrue(subscriber.get() == stringValue, "Test Property.get() just after DelegateProperty.subscribe().");
	}

	@Test
	public void propertyMultipleSubscribes() {
		String stringValue = "Value";

		Property<String> subscriber1 = new Property<String>(null);
		Property<String> subscriber2 = new Property<String>(null);
		DelegateProperty<String> property = new DelegateProperty<String>(null);

		property.subscribe(subscriber1);
		assertTrue(property.subscribe(subscriber2), "Test DelegateProperty.subscribe() method with second subscriber.");

		property.set(stringValue);
		assertTrue(subscriber1.get() == stringValue, "Test first subscriber Property.get() method after value delegation.");
		assertTrue(subscriber2.get() == stringValue, "Test second subscriber Property.get() method after value delegation.");
	}

	@Test
	public void propertyDelegationLoop() {
		String stringValue1 = "Value 1";
		String stringValue2 = "Value 2";

		DelegateProperty<String> property1 = new DelegateProperty<String>("First");
		DelegateProperty<String> property2 = new DelegateProperty<String>("Second");
		property1.subscribe(property2);
		property2.subscribe(property1);

		property1.set(stringValue1);
		assertTrue(property2.get() == stringValue1, "Test subscribe loop on a first property.");

		property2.set(stringValue2);
		assertTrue(property1.get() == stringValue2, "Test subscribe loop on a second property.");
	}
}
