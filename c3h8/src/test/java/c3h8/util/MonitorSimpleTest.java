package c3h8.util;

import java.util.LinkedList;
import java.util.List;

import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

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
	public void monitorReadAccessCannotChangeValue() {
		final List<Object> accessed = new LinkedList<Object>();
		final String initialValue = "initial";
		final String modifiedValue = "modified";
		Monitor<String> monitor = new Monitor<String>(initialValue);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				accessed.add(value);
				return modifiedValue;
			}
		});
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(initialValue == value, "Test Monitor.readAccess() cannot change value.");
				return null;
			}
		});
		assertEquals(accessed.size(), 1, "Test Monitor.readAccess() accessed value properly");
		assertEquals(accessed.get(0), initialValue, "Test Monitor.readAccess() accessed proper value");
	}

	@Test
	public void monitorReadAccessWithCheckerCannotChangeValue() throws InterruptedException {
		final List<Object> accessed = new LinkedList<Object>();
		final String initialValue = "initial";
		final String modifiedValue = "modified";
		Monitor<String> monitor = new Monitor<String>(initialValue);
		monitor.readAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					accessed.add(value);
					return modifiedValue;
				}
			},
			new Checker<String>() {
				@Override
				public boolean check(String value) {
					return true;
				}
			}
		);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(initialValue == value, "Test Monitor.readAccess() with Checker cannot change value.");
				return null;
			}
		});
		assertEquals(accessed.size(), 1, "Test Monitor.readAccess() with Checker accessed value properly");
		assertEquals(accessed.get(0), initialValue, "Test Monitor.readAccess() with Checker accessed proper value");
	}

	@Test
	public void monitorReadAccessNanosCannotChangeValue() throws InterruptedException {
		final List<Object> accessed = new LinkedList<Object>();
		final String initialValue = "initial";
		final String modifiedValue = "modified";
		Monitor<String> monitor = new Monitor<String>(initialValue);
		monitor.readAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					accessed.add(value);
					return modifiedValue;
				}
			},
			new Checker<String>() {
				@Override
				public boolean check(String value) {
					return true;
				}
			}, 1000
		);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(initialValue == value, "Test Monitor.readAccess() with Nanoseconds cannot change value.");
				return null;
			}
		});
		assertEquals(accessed.size(), 1, "Test Monitor.readAccess() with Nanoseconds accessed value properly");
		assertEquals(accessed.get(0), initialValue, "Test Monitor.readAccess() with Nanoseconds accessed proper value");
	}

	@Test
	public void monitorReadAccessMillisCannotChangeValue() throws InterruptedException {
		final List<Object> accessed = new LinkedList<Object>();
		final String initialValue = "initial";
		final String modifiedValue = "modified";
		Monitor<String> monitor = new Monitor<String>(initialValue);
		monitor.readAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					accessed.add(value);
					return modifiedValue;
				}
			},
			new Checker<String>() {
				@Override
				public boolean check(String value) {
					return true;
				}
			}, 1000, TimeUnit.MILLISECONDS
		);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(initialValue == value, "Test Monitor.readAccess() with Milliseconds cannot change value.");
				return null;
			}
		});
		assertEquals(accessed.size(), 1, "Test Monitor.readAccess() with Milliseconds accessed value properly");
		assertEquals(accessed.get(0), initialValue, "Test Monitor.readAccess() with Milliseconds accessed proper value");
	}

	@Test
	public void monitorReadAccessDateCannotChangeValue() throws InterruptedException {
		final List<Object> accessed = new LinkedList<Object>();
		final String initialValue = "initial";
		final String modifiedValue = "modified";
		Monitor<String> monitor = new Monitor<String>(initialValue);
		monitor.readAccess(
			new Accessor<String>() {
				@Override
				public String access(String value) {
					accessed.add(value);
					return modifiedValue;
				}
			},
			new Checker<String>() {
				@Override
				public boolean check(String value) {
					return true;
				}
			}, new Date(Calendar.getInstance().getTimeInMillis() + 1000)
		);
		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(initialValue == value, "Test Monitor.readAccess() with Date cannot change value.");
				return null;
			}
		});
		assertEquals(accessed.size(), 1, "Test Monitor.readAccess() with Date ccessed value properly");
		assertEquals(accessed.get(0), initialValue, "Test Monitor.readAccess() with Date accessed proper value");
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
	public void monitorWriteAccessNanosChecker() throws InterruptedException {
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
			}, 1000);

		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(modifiedValue == value, "Test Monitor.writeAccess(Checker) without wait for Nanoseconds.");
				return null;
			}
		});
	}

	@Test
	public void monitorWriteAccessMillisChecker() throws InterruptedException {
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
			}, 100, TimeUnit.MILLISECONDS);

		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(modifiedValue == value, "Test Monitor.writeAccess(Checker) without wait for Milliseconds.");
				return null;
			}
		});
	}

	@Test
	public void monitorWriteAccessDateChecker() throws InterruptedException {
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
			}, new Date(Calendar.getInstance().getTimeInMillis() + 100));

		monitor.readAccess(new Accessor<String>() {
			@Override
			public String access(String value) {
				assertTrue(modifiedValue == value, "Test Monitor.writeAccess(Checker) without wait for Date.");
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
			}, 100, TimeUnit.MILLISECONDS);
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
			}, 100, TimeUnit.MILLISECONDS);
		assertFalse(result, "Test Monitor.writeAccess interrupt wait for milliseconds.");
	}

	@Test
	public void monitorDateReadTest() throws InterruptedException {
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
			}, new Date(Calendar.getInstance().getTimeInMillis() + 100));
		assertFalse(result, "Test Monitor.readAccess interrupt wait for Date.");
	}

	@Test
	public void monitorDateWriteTest() throws InterruptedException {
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
			}, new Date(Calendar.getInstance().getTimeInMillis() + 100));
		assertFalse(result, "Test Monitor.writeAccess interrupt wait for Date.");
	}
}
