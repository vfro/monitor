package c3h8.java.util;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class MonitorSimpleTest {
	public MonitorSimpleTest() {
	}

	@Test
	public void monitorConstructorReadAccess() {
		final String monitorValue = "Test";
		Monitor<String> monitor = new Monitor<String>(monitorValue);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(monitorValue == value, "Test Monitor.readAccess() with value passed into constructor.");
				return null;
			}
		});
	}

	@Test
	public void monitorSetterReadAccess() {
		final String monitorValue = "Test";
		Monitor<String> monitor = new Monitor<String>(null);
		monitor.set(monitorValue);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(monitorValue == value, "Test Monitor.readAccess() with value passed into setter.");
				return null;
			}
		});
	}

	@Test
	public void monitorWriteReadAccess() {
		final String monitorValue = "Test";
		Monitor<String> monitor = new Monitor<String>(null);
		monitor.writeAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				return monitorValue;
			}
		});
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(monitorValue == value, "Test Monitor.readAccess() with value from write access.");
				return null;
			}
		});
	}

	@Test
	public void monitorWriteAccessChecker() throws InterruptedException {
		final String initialValue = "initial";
		final String modifiedValue = "modified";
		Monitor<String> monitor = new Monitor<String>(initialValue);

		monitor.writeAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					return modifiedValue;
				}
			}, new Checker<String>() {
				@Override
				public boolean check(String value) {
					return value == initialValue;
				}
			});

		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(modifiedValue == value, "Test Monitor.writeAccess(Checker) without wait.");
				return null;
			}
		});
	}

	@Test
	public void monitorNanosecondsReadTest() throws InterruptedException {
		Monitor<String> monitor = new Monitor<String>(null);
		long result = monitor.readAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					fail("Never reach read access for false checer.");
					return null;
				}
			}, new Checker<String>() {
				@Override
				public boolean check(String value) {
					return false;
				}
			}, 1000);
		assertTrue(result <= 0, "Test Monitor.readAccess interrupt wait for nanoseconds.");
	}

	@Test
	public void monitorNanosecondsWriteTest() throws InterruptedException {
		Monitor<String> monitor = new Monitor<String>(null);
		long result = monitor.writeAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					fail("Never reach write access for false checer.");
					return null;
				}
			}, new Checker<String>() {
				@Override
				public boolean check(String value) {
					return false;
				}
			}, 1000);
		assertTrue(result <= 0, "Test Monitor.writeAccess interrupt wait for nanoseconds.");
	}

	@Test
	public void monitorTimeunitReadTest() throws InterruptedException {
		Monitor<String> monitor = new Monitor<String>(null);
		boolean result = monitor.readAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					fail("Never reach read access for false checer.");
					return null;
				}
			}, new Checker<String>() {
				@Override
				public boolean check(String value) {
					return false;
				}
			}, 10, TimeUnit.MILLISECONDS);
		assertFalse(result, "Test Monitor.readAccess interrupt wait for milliseconds.");
	}

	@Test
	public void monitorTimeunitWriteTest() throws InterruptedException {
		Monitor<String> monitor = new Monitor<String>(null);
		boolean result = monitor.writeAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					fail("Never reach write access for false checer.");
					return null;
				}
			}, new Checker<String>() {
				@Override
				public boolean check(String value) {
					return false;
				}
			}, 10, TimeUnit.MILLISECONDS);
		assertFalse(result, "Test Monitor.writeAccess interrupt wait for milliseconds.");
	}
}
