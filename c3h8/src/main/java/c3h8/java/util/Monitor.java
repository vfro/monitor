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
	private ReadWriteLock rwlock = new ReentrantReadWriteLock();
	private Lock rlock = rwlock.readLock();
	private Lock wlock = rwlock.writeLock();

	private Condition rtouch = rlock.newCondition();
	private Condition wtouch = wlock.newCondition();

	// must be called only inside write lock
	private void touch() {
		this.wtouch.signalAll();
		try {
			this.rlock.lock();
			this.rtouch.signalAll();
		}
		finally {
			this.rlock.unlock();
		}
	}

	/**
	 * Create new instance of a monitor initialized by specified value.
	 * @param value initial value of a monitor.
	 */
	public Monitor(Value value) {
		this.value = value;
	}

	public void readAccess(Accessor<Value> accessor) {
		try {
			this.rlock.lock();
			accessor.access(this.value);
		}
		finally {
			this.rlock.unlock();
		}
	}

	public void readAccess(Accessor<Value> accessor, Checker<Value> checker)
		throws InterruptedException {
		try {
			this.rlock.lock();
			while (!checker.check(this.value)) {
				rtouch.await();
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.unlock();
		}
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		try {
			this.rlock.lock();
			while (!checker.check(this.value)) {
				if (!rtouch.await(time, unit)) {
					return false;
				}
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.unlock();
		}
		return true;
	}

	public long readAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		long result = 0;
		try {
			this.rlock.lock();
			while (!checker.check(this.value)) {
				result = rtouch.awaitNanos(nanosTimeout);
				if (result <= 0) {
					return result;
				}
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.unlock();
		}
		return result;
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		try {
			this.rlock.lock();
			while (!checker.check(this.value)) {
				if (!rtouch.awaitUntil(deadline)) {
					return false;
				}
			}
			accessor.access(this.value);
		}
		finally {
			this.rlock.unlock();
		}
		return true;
	}

	public void writeAccess(Accessor<Value> accessor, Checker<Value> checker)
		throws InterruptedException {
		try {
			this.wlock.lock();
			while (!checker.check(this.value)) {
				wtouch.await();
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.unlock();
		}
	}

	public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		try {
			this.wlock.lock();
			while (!checker.check(this.value)) {
				if (!wtouch.await(time, unit)) {
					return false;
				}
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.unlock();
		}
		return true;
	}

	public long writeAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		long result = 0;
		try {
			this.wlock.lock();
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
			this.wlock.unlock();
		}
		return result;
	}

	public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		try {
			this.wlock.lock();
			while (!checker.check(this.value)) {
				if (!wtouch.awaitUntil(deadline)) {
					return false;
				}
			}
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			this.wlock.unlock();
		}
		return true;
	}

	public void writeAccess(Accessor<Value> accessor) {
		try {
			this.wlock.lock();
			this.value = accessor.access(this.value);
			this.touch();
		}
		finally {
			rlock.unlock();
		}
	}

	public void set(Value value) {
		try {
			this.wlock.lock();
			this.value = value;
			this.touch();
		}
		finally {
			this.wlock.unlock();
		}
	}
}
