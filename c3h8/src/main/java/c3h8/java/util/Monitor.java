package c3h8.java.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Monitor is a kind of property which can be used for concurent access
 * to a property value with ability to wait until the value becomes into
 * some certain state (defined by a {@link Comparator}).
 */
public class Monitor<Value> {
	private Value value;

	private Condition rtouch = null;
	private Condition wtouch = null;

	// must be called only inside write lock
	private void touch() {
		this.wtouch.signalAll();
		try {
			this.rlock.get().lock();
			this.rtouch.signalAll();
		}
		finally {
			this.rlock.get().unlock();
		}
	}

	public final Property<Lock> rlock = new Property<Lock>(null) {
		@Override
		public void set(Lock lock) {
			Monitor.this.rtouch = lock.newCondition();
		}
	};

	public final Property<Lock> wlock = new Property<Lock>(null) {
		@Override
		public void set(Lock lock) {
			Monitor.this.wtouch = lock.newCondition();
		}
	};

	/**
	 * Create new instance of a monitor initialized by specified value.
	 * @param value initial value of a monitor.
	 */
	public Monitor(Value value) {
		this.value = value;
		ReadWriteLock rwlock = new ReentrantReadWriteLock();
		rlock.set(rwlock.readLock());
		wlock.set(rwlock.writeLock());
	}

	public void readAccess(Accessor<Value> accessor) {
		try {
			this.rlock.get().lock();
			accessor.access(this.value);
		}
		finally {
			this.rlock.get().unlock();
		}
	}

	public void readAccess(Accessor<Value> accessor, Checker<Value> checker)
		throws InterruptedException {
		try {
			this.rlock.get().lock();
			while (!checker.check(this.value)) {
				rtouch.await();
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.get().unlock();
		}
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		try {
			this.rlock.get().lock();
			while (!checker.check(this.value)) {
				if (!rtouch.await(time, unit)) {
					return false;
				}
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.get().unlock();
		}
		return true;
	}

	public long readAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		long result = 0;
		try {
			this.rlock.get().lock();
			while (!checker.check(this.value)) {
				result = rtouch.awaitNanos(nanosTimeout);
				if (result <= 0) {
					return result;
				}
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.get().unlock();
		}
		return result;
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		try {
			this.rlock.get().lock();
			while (!checker.check(this.value)) {
				if (!rtouch.awaitUntil(deadline)) {
					return false;
				}
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.get().unlock();
		}
		return true;
	}

	public void writeAccess(Accessor<Value> accessor, Checker<Value> checker)
		throws InterruptedException {
		try {
			this.wlock.get().lock();
			while (!checker.check(this.value)) {
				wtouch.await();
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.get().unlock();
		}
	}

	public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		try {
			this.wlock.get().lock();
			while (!checker.check(this.value)) {
				if (!wtouch.await(time, unit)) {
					return false;
				}
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.get().unlock();
		}
		return true;
	}

	public long writeAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		long result = 0;
		try {
			this.wlock.get().lock();
			while (!checker.check(this.value)) {
				result = wtouch.awaitNanos(nanosTimeout);
				if (result <= 0) {
					return result;
				}
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.get().unlock();
		}
		return result;
	}

	public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		try {
			this.wlock.get().lock();
			while (!checker.check(this.value)) {
				if (!wtouch.awaitUntil(deadline)) {
					return false;
				}
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.get().unlock();
		}
		return true;
	}

	public void writeAccess(Accessor<Value> accessor) {
		try {
			this.wlock.get().lock();
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			rlock.get().unlock();
		}
	}

	public void set(Value value) {
		try {
			this.wlock.get().lock();
			this.value = value;
			this.touch();
		}
		finally {
			this.wlock.get().unlock();
		}
	}
}
