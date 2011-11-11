package c3h8.java.util;

/**
 * Property base class.
 * {@code Property} instances should be exposed as a public final fields of a class.
 * It gives potential ability to override set/get methods later but avoid defining
 * (and thus testing) getters and setters of each in each class.<p>
 *
 * {@code Property} class is supposed to be inherited by inner-classes inside pojo with
 * overriding of get and/or set methods to define custom getters/setters.<p>
 *
 * <pre>
 * public class Person {
 *    // Property with overrided setter and getter.
 *    public final Property&lt;String&gt; firstName = new Property&lt;String&gt;("") {
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
 *    public final Property&lt;String&gt; lastName = new Property&lt;String&gt;("");
 *    public final Property&lt;Calendar&gt; dateOfBirth = new Property&lt;Calendar&gt;(Calendar.getInstance());
 * }
 * </pre>
 *
 * @author Volodymyr Frolov &lt;frolov.volodymyr@gmail.com&gt;
 */
public class Property<Value> {
	private Value value;

	/**
	 * Constructor for {@code Property} which creates property instance initialized by 
	 * some specified value.
	 * There is intentionally no default constructor to
	 * force users of {@code Property} pass {@code null} at this one and thus notice that
	 * this may cause {@code NullPointerException} when using Value methods over getter.
	 * For lazy inialization of a {@code Property} pass {@code null} to this constructor
	 * and redefine getter to set some valid value if {@code supper.get()} returns {@code null}.
	 * @param value initial value of a property.
	 */
	public Property(Value value) {
		this.value = value;
	}

	/**
	 * Getter of the property value.
	 * This method is supposed to be overriden by any property which is going to have custom
	 * getter.
	 * @return value of a property.
	 */
	public Value get() {
		return this.value;
	}

	/**
	 * Setter of the property value.
	 * This method is supposed to be overriden by any property which is going to have custom
	 * setter.
	 * @param value new value of a property.
	 */
	public void set(Value value) {
		this.value = value;
	}

	/**
	 * Compare that properties are contained in the same property container class and have
	 * the same value.
	 * Any kind of {@code Property} childs should override this method.
	 * @param object Any other {@code object} to compare this property with.
	 * @return {@code true} if {@code object} represents property of the same type with
	 * the same value. Otherwise {@code false}.
	 */
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

	/**
	 * Hash code of a property.
	 * If {@code Property} has a not null value the hash code will be equals with hash code of
	 * the property value. Otherwise it will be {@code 0}.
	 */
	@Override
	public int hashCode() {
		return value == null ? 0 : value.hashCode();
	}
}
