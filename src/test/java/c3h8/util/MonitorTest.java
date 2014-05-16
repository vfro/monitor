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
        monitor.readAccess(value -> {
                assertTrue(monitorValue == value, "Test Monitor.readAccess() with value passed into constructor.");
            });
    }

    @Test
    public void monitorSetterReadAccess() {
        final String monitorValue = "Test";
        Monitor<String> monitor = new Monitor<String>(null);
        assertNull(monitor.set(monitorValue), "Test Monitor.set method returns previous value.");
        monitor.readAccess(value -> {
                assertTrue(monitorValue == value, "Test Monitor.readAccess() with value passed into setter.");
            });
    }

    @Test
    public void monitorWriteReadAccess() {
        final String monitorValue = "Test";
        Monitor<String> monitor = new Monitor<String>(null);
        monitor.writeAccess(value -> monitorValue);
        monitor.readAccess(value -> {
                assertTrue(monitorValue == value, "Test Monitor.readAccess() with value from write access.");
            });
    }

    @Test
    public void monitorWriteAccessPredicate() throws InterruptedException {
        final String initialValue = "initial";
        final String modifiedValue = "modified";
        Monitor<String> monitor = new Monitor<String>(initialValue);

        monitor.writeAccess(
            value -> {
                    assertTrue(value == initialValue, "Motitor has initial value before modification.");
                    return modifiedValue;
                },
            value -> {
                    assertTrue(value == initialValue, "Motitor has initial value inside predicate.");
                    return value == initialValue;
                }
            );

        monitor.readAccess(value -> {
                assertTrue(modifiedValue == value, "Monitor has changed value after modification.");
            });
    }

    @Test
    public void monitorWriteAccessMillispredicate() throws InterruptedException {
        final String initialValue = "initial";
        final String modifiedValue = "modified";
        Monitor<String> monitor = new Monitor<String>(initialValue);

        monitor.writeAccess(
            value -> {
                    assertTrue(value == initialValue, "Motitor has initial value before modification.");
                    return modifiedValue;
                },
            value -> {
                    assertTrue(value == initialValue, "Motitor has initial value inside predicate.");
                    return value == initialValue;
                },
            1000, TimeUnit.MILLISECONDS);

        monitor.readAccess(value -> {
                assertTrue(modifiedValue == value, "Monitor has changed value after modification.");
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
