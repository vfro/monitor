package c3h8.java.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MonitorTest {
	public MonitorTest() {
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

		monitor.writeAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				return modifiedValue;
			}
		}, new Checker<String>() {
			@Override
			public boolean check(String value) {
				return value == initialValue;
			}
		}
		);

		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(modifiedValue == value, "Test Monitor.writeAccess(Checker) without wait.");
				return null;
			}
		});
	}
}
