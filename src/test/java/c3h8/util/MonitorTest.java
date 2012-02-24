package c3h8.util;

import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

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
        assertNull(monitor.set(monitorValue), "Test Monitor.set method returns previous value.");
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
    public void monitorWriteAccessChecker() throws InterruptedException {
        final String initialValue = "initial";
        final String modifiedValue = "modified";
        Monitor<String> monitor = new Monitor<String>(initialValue);

        monitor.writeAccess(
            new Accessor<String>() {
                @Override
                public String access(String value) {
                    assertTrue(value == initialValue, "Motitor has initial value before modification.");
                    return modifiedValue;
                }
            }, new Checker<String>() {
                @Override
                public boolean check(String value) {
                    assertTrue(value == initialValue, "Motitor has initial value inside checker.");
                    return value == initialValue;
                }
            });

        monitor.readAccess(new Accessor<String>() {
            @Override
            public String access(String value) {
                assertTrue(modifiedValue == value, "Monitor has changed value after modification.");
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
                    assertTrue(value == initialValue, "Motitor has initial value before modification.");
                    return modifiedValue;
                }
            }, new Checker<String>() {
                @Override
                public boolean check(String value) {
                    assertTrue(value == initialValue, "Motitor has initial value inside checker.");
                    return value == initialValue;
                }
            }, 1000, TimeUnit.MILLISECONDS);

        monitor.readAccess(new Accessor<String>() {
            @Override
            public String access(String value) {
                assertTrue(modifiedValue == value, "Monitor has changed value after modification.");
                return null;
            }
        });
    }

    @Test
    public void constructorWithLocks() {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock readLock = readWriteLock.readLock();
        Lock writeLock = readWriteLock.writeLock();
        
        Monitor<String> monitor = new Monitor<String>("", readLock, writeLock);
        assertEquals(monitor.getReadLock(), readLock, "Read lock is the same as passed to monitor constructor.");
        assertEquals(monitor.getWriteLock(), writeLock, "Write lock is the same as passed to monitor constructor.");
    }
}
