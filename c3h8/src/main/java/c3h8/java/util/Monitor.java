package c3h8.java.util;

import java.util.Date;
import java.util.Calendar;
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
	protected Property<Value> rawValue;

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
		this.rawValue = new Property<Value>(value);

		ReadWriteLock rwlock = new ReentrantReadWriteLock();
		this.rlock.set(rwlock.readLock());
		this.wlock.set(rwlock.writeLock());
	}

	public void readAccess(Accessor<Value> accessor) {
		try {
			this.rlock.get().lock();
			accessor.access(this.rawValue.get());
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
			while (!checker.check(this.rawValue.get())) {
				this.checkEvent.await();
			}

			this.rlock.get().lock();
			lockedObject.unlock();
			lockedObject = this.rlock.get();

			accessor.access(this.rawValue.get());
		}
		finally {
			lockedObject.unlock();
		}
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		return this.readAccess(accessor, checker, unit.toNanos(time)) > 0;
	}

	public boolean readAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		Calendar deadlineCalendar = Calendar.getInstance();
		deadlineCalendar.setTime(deadline);
		Calendar now = Calendar.getInstance();
		return readAccess(accessor, checker, deadlineCalendar.getTimeInMillis() - now.getTimeInMillis(), TimeUnit.MILLISECONDS);
	}

	public long readAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		long origin = System.nanoTime();
		long timeLeft = nanosTimeout;
		Lock aquiredLock = null;

		long result = 0;
		try {
			if (this.wlock.get().tryLock(timeLeft, TimeUnit.NANOSECONDS)) {
				aquiredLock = this.wlock.get();
			} else {
				return origin + nanosTimeout - System.nanoTime();
			}

			while (!checker.check(this.rawValue.get())) {
				timeLeft = origin + nanosTimeout - System.nanoTime();
				result = this.checkEvent.awaitNanos(timeLeft);
				if (result <= 0) {
					return result;
				}
			}

			this.rlock.get().lock();
			aquiredLock.unlock();
			aquiredLock = this.rlock.get();

			accessor.access(this.rawValue.get());
		}
		finally {
			aquiredLock.unlock();
		}
		return result;
	}

	public void writeAccess(Accessor<Value> accessor, Checker<Value> checker)
		throws InterruptedException {
		try {
			this.wlock.get().lock();
			while (!checker.check(this.rawValue.get())) {
				this.checkEvent.await();
			}
			this.rawValue.set(accessor.access(this.rawValue.get()));
			this.checkEvent.signalAll();
		}
		finally {
			this.wlock.get().unlock();
		}
	}

	public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, long time, TimeUnit unit)
		throws InterruptedException {
		return this.writeAccess(accessor, checker, unit.toNanos(time)) > 0;
	}

	public boolean writeAccess(Accessor<Value> accessor, Checker<Value> checker, Date deadline)
		throws InterruptedException {
		Calendar deadlineCalendar = Calendar.getInstance();
		deadlineCalendar.setTime(deadline);
		Calendar now = Calendar.getInstance();
		return writeAccess(accessor, checker, deadlineCalendar.getTimeInMillis() - now.getTimeInMillis(), TimeUnit.MILLISECONDS);
	}

	public long writeAccess(Accessor<Value> accessor, Checker<Value> checker, long nanosTimeout)
		throws InterruptedException {
		long origin = System.nanoTime();
		long timeLeft = nanosTimeout;
		Lock aquiredLock = null;

		long result = 0;

		try {
			if (this.wlock.get().tryLock(timeLeft, TimeUnit.NANOSECONDS)) {
				aquiredLock = this.wlock.get();
			} else {
				return origin + nanosTimeout - System.nanoTime();
			}

			while (!checker.check(this.rawValue.get())) {
				timeLeft = origin + nanosTimeout - System.nanoTime();
				result = this.checkEvent.awaitNanos(timeLeft);
				if (result <= 0) {
					return result;
				}
			}

			this.rawValue.set(accessor.access(this.rawValue.get()));
			this.checkEvent.signalAll();
		}
		finally {
			if (aquiredLock != null) {
				aquiredLock.unlock();
			}
		}
		return result;
	}

	public void writeAccess(Accessor<Value> accessor) {
		try {
			this.wlock.get().lock();
			this.rawValue.set(accessor.access(this.rawValue.get()));
			this.checkEvent.signalAll();
		}
		finally {
			this.wlock.get().unlock();
		}
	}

	public void set(Value value) {
		try {
			this.wlock.get().lock();
			this.rawValue.set(value);
			this.checkEvent.signalAll();
		}
		finally {
			this.wlock.get().unlock();
		}
	}
}
