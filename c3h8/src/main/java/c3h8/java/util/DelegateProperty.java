package c3h8.java.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DelegateProperty<Value> extends Property<Value> {

	private Lock subscribersGuard = new ReentrantLock();
	private Set<Property<Value>> subscribers = Collections.newSetFromMap(new WeakHashMap<Property<Value>, Boolean>());

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
		if (this.isLoop.get()) {
			// Prevent calling delegates in a loop.
			return;
		}

		try {
			this.isLoop.set(true);

			super.set(value);
			try {
				subscribersGuard.lock();
				for(Property<Value> property : this.subscribers) {
					property.set(value);
				}

				// Hash code of all values has been changed after setter.
				// All values should be re-added back to subscribers set.
				Set<Property<Value>> subscribersCache = Collections.newSetFromMap(new WeakHashMap<Property<Value>, Boolean>());
				subscribersCache.addAll(this.subscribers);
				this.subscribers.clear();
				this.subscribers.addAll(subscribersCache);
			}
			finally {
				subscribersGuard.unlock();
			}
		}
		finally {
			this.isLoop.set(false);
		}
	}

	public boolean subscribe(Property<Value> property) {
		boolean result;
		try {
			subscribersGuard.lock();
			result = this.subscribers.add(property);
			property.set(this.get());
		}
		finally {
			subscribersGuard.unlock();
		}
		return result;
	}

	public boolean unsubscribe(Property<Value> property) {
		boolean result;
		try {
			subscribersGuard.lock();
			result = this.subscribers.remove(property);
		}
		finally {
			subscribersGuard.unlock();
		}
		return result;
	}
}
