package c3h8.java.util;

/**
 * Property base class. {@code Property} instances should be
 * exposed as a public final fields of a class. It gives potential
 * ability to override set/get methods later but avoid defining
 * (and thus testing) getters and setters of each in each class.<p>
 * <pre>
 * public class Person {
 *    // Property with overrided setter and getter.
 *    public final Property<String> firstName = new Property<String>("") {
 *       &#064;Override
 *       public String get() {
 *          System.out.println("Value of property Person.firstName has been accessed.");
 *          return super.get();
 *       }
 *
 *       &#064;Override
 *       public void set(String value) {
 *          super.set(value);
 *          System.out.println("Value of property Person.firstName has been changed.");
 *       }
 *    };
 *
 *    // Properties without overrided setter and getter, but with ability to define them later.
 *    public final Property<String> lastName = new Property<String>("");
 *    public final Property<Calendar> dateOfBirth = new Property<Calendar>(Calendar.getInstance());
 * }
 * </pre>
 *
 * @author Volodymyr Frolov <frolov.volodymyr@gmail.com>
 */
public class Property<Value> {
	private Value value;

	public Property(Value value) {
		this.value = value;
	}

	public Value get() {
		return this.value;
	}

	public void set(Value value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object object) {
		if (this.value == null) {
			return false;
		} else if (object == null) {
			return false;
		} else if (this == object) {
			return true;
		} else if (object.getClass() != this.getClass()) {
			return false;
		}

		Property property = (Property)object;
		return value.equals(property.get());
	}

	@Override
	public int hashCode() {
		return value == null ? 0 : value.hashCode();
	}
}
