package c3h8.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FluentCollectionPropertyTest {

	public FluentCollectionPropertyTest() {
	}

	@Test
	public void fluentCollectionPropertyConstructor() {
		LinkedList<String> values = new LinkedList<String>();
		Collections.addAll(values, "1", "2", "3");

		FluentCollectionProperty<FluentCollectionPropertyTest, String, List<String>> property =
			new FluentCollectionProperty<FluentCollectionPropertyTest, String, List<String>>(this, values);
		assertTrue(property.get() == values, "Test FluentCollectionProperty constructor.");
	}

	@Test
	public void fluentCollectionPropertyWithArray() {
		LinkedList<String> values = new LinkedList<String>();
		Collections.addAll(values, "1", "2", "3");

		FluentCollectionProperty<FluentCollectionPropertyTest, String, List<String>> property =
			new FluentCollectionProperty<FluentCollectionPropertyTest, String, List<String>>(this, new LinkedList<String>());

		FluentCollectionPropertyTest withResult = property.withAll("1", "2", "3");
		assertEquals(property.get(), values, "Test FluentCollectionProperty.withAll(T...) method.");
		assertTrue(withResult == this, "Test FluentCollectionProperty.withAll(T...) method return value.");
	}

	@Test
	public void fluentCollectionPropertyWithCollection() {
		LinkedList<String> values = new LinkedList<String>();
		Collections.addAll(values, "1", "2", "3");

		Set<String> addValues = new TreeSet<String>();
		Collections.addAll(addValues, "1", "2", "3");

		FluentCollectionProperty<FluentCollectionPropertyTest, String, List<String>> property =
			new FluentCollectionProperty<FluentCollectionPropertyTest, String, List<String>>(this, new LinkedList<String>());

		FluentCollectionPropertyTest withResult = property.withAll(addValues);
		assertEquals(property.get(), values, "Test FluentCollectionProperty.withAll(Collection<T>) method.");
		assertTrue(withResult == this, "Test FluentCollectionProperty.withAll(Collection<T>) method return value.");
	}
}
