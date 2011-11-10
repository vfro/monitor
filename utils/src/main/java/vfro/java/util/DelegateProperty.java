package vfro.java.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class DelegateProperty<Value> extends Property<Value> {

	private Set<Property<Value>> subscribers = Collections.newSetFromMap(new WeakHashMap<Property<Value>, Boolean>());
	private Set<Property<Value>> subscribersCache = Collections.newSetFromMap(new WeakHashMap<Property<Value>, Boolean>());

	private ThreadLocal<Boolean> isLoop = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	public DelegateProperty(Value value) {
		super(value);
	}

	@Override
	public void set(Value value) {
		if (isLoop.get()) {
			// Prevent calling delegates in a loop.
			return;
		}

		try {
			isLoop.set(true);

			super.set(value);
			for(Property<Value> property : this.subscribers) {
				property.set(value);
			}

			// Hash code of all values has been changed after setter.
			// All values should be re-added back to subscribers set.
			this.subscribersCache.addAll(subscribers);
			this.subscribers.clear();
			this.subscribers.addAll(subscribersCache);
			this.subscribersCache.clear();
		}
		finally {
			isLoop.set(false);
		}
	}

	public boolean subscribe(Property<Value> property) {
		boolean result = subscribers.add(property);
		property.set(this.get());
		return result;
	}

	public boolean unsubscribe(Property<Value> property) {
		return subscribers.remove(property);
	}
}
