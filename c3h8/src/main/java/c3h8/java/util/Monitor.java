package c3h8.java.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Monitor is a special kind of property which can be used for concurent
 * access to a value with and ability to wait until the value becomes into
 * some certain state (defined by a {@link Checker}).
 */
public class Monitor<Value> {
	private Value value;

	private Condition checkEvent = null;

	public final Property<Lock> rlock = new Property<Lock>(null);

	public final Property<Lock> wlock = new Property<Lock>(null) {
		@Override
		public void set(Lock lock) {
			Monitor.this.checkEvent = lock.newCondition();
			super.set(lock);
		}
	};

	/**
	 * Create new instance of a monitor initialized by specified value.
	 * @param value initial value of a monitor.
	 */
	public Monitor(Value value) {
		this.value = value;

		ReadWriteLock rwlock = new ReentrantReadWriteLock();
		this.rlock.set(rwlock.readLock());
		this.wlock.set(rwlock.writeLock());
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
		Lock lockedObject = null;
		try {
			lockedObject = this.wlock.get();
			lockedObject.lock();
			while (!checker.check(this.value)) {
				this.checkEvent.await();
			}

			this.rlock.get().lock();
			lockedObject.unlock();
			lockedObject = this.rlock.get();

			accessor.access(this.value);
		}
		finally {
			lockedObject.unlock();
		}
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		Lock lockedObject = null;
		try {
			lockedObject = this.wlock.get();
			lockedObject.lock();
			while (!checker.check(this.value)) {
				if (!this.checkEvent.await(time, unit)) {
					return false;
				}
			}

			this.rlock.get().lock();
			lockedObject.unlock();
			lockedObject = this.rlock.get();

			accessor.access(this.value);
		}
		finally {
			lockedObject.unlock();
		}
		return true;
	}

	public long readAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		Lock lockedObject = null;
		long result = 0;
		try {
			lockedObject = this.wlock.get();
			lockedObject.lock();
			while (!checker.check(this.value)) {
				result = this.checkEvent.awaitNanos(nanosTimeout);
				if (result <= 0) {
					return result;
				}
			}

			this.rlock.get().lock();
			lockedObject.unlock();
			lockedObject = this.rlock.get();

			accessor.access(this.value);
		}
		finally {
			lockedObject.unlock();
		}
		return result;
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		Lock lockedObject = null;
		try {
			lockedObject = this.wlock.get();
			lockedObject.lock();

			while (!checker.check(this.value)) {
				if (!this.checkEvent.awaitUntil(deadline)) {
					return false;
				}
			}

			this.rlock.get().lock();
			lockedObject.unlock();
			lockedObject = this.rlock.get();

			accessor.access(this.value);
		}
		finally {
			lockedObject.unlock();
		}
		return true;
	}

	public void writeAccess(Accessor<Value> accessor, Checker<Value> checker)
		throws InterruptedException {
		try {
			this.wlock.get().lock();
			while (!checker.check(this.value)) {
				this.checkEvent.await();
			}
			this.value = accessor.access(this.value);
			this.checkEvent.signalAll();
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
				if (!this.checkEvent.await(time, unit)) {
					return false;
				}
			}
			this.value = accessor.access(this.value);
			this.checkEvent.signalAll();
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
				result = this.checkEvent.awaitNanos(nanosTimeout);
				if (result <= 0) {
					return result;
				}
			}
			this.value = accessor.access(this.value);
			this.checkEvent.signalAll();
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
				if (!this.checkEvent.awaitUntil(deadline)) {
					return false;
				}
			}
			this.value = accessor.access(this.value);
			this.checkEvent.signalAll();
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
			this.checkEvent.signalAll();
		}
		finally {
			this.wlock.get().unlock();
		}
	}

	public void set(Value value) {
		try {
			this.wlock.get().lock();
			this.value = value;
			this.checkEvent.signalAll();
		}
		finally {
			this.wlock.get().unlock();
		}
	}
}
