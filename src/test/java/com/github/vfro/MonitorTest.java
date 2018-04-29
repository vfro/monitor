package com.github.vfro;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

@SuppressWarnings("StringEquality")
public class MonitorTest {

    public MonitorTest() {
    }

    @Test
    public void monitorConstructorRead() {
        final String value = "Value";
        Monitor<String> monitor = new Monitor<>(value);
        monitor.read(entity -> {
            assertTrue(value == entity, "Compare Monitor.read() with initial entity.");
        });
    }

    @Test
    public void monitorSwapRead() {
        final String oldValue = "Old Value";
        final String newValue = "New Value";
        Monitor<String> monitor = new Monitor<>(oldValue);
        assertTrue(monitor.swap(newValue) == oldValue, "Compare Monitor.swap() return value with initial entity.");
        monitor.read(entity -> {
            assertTrue(newValue == entity, "Compare Monitor.read() with Monitor.swap() argument.");
        });
    }

    @Test
    public void monitorSetRead() {
        final String value = "Value";
        Monitor<String> monitor = new Monitor<>(null);
        monitor.set(value);
        monitor.read(entity -> {
            assertTrue(value == entity, "Compare Monitor.read() with Monitor.set() argument.");
        });
    }

    @Test
    public void monitorWriteRead() {
        final String value = "Value";
        Monitor<String> monitor = new Monitor<>(null);
        monitor.write(entity -> value);
        monitor.read(entity -> {
            assertTrue(value == entity, "Compare Monitor.read() with Monitor.write() function return value.");
        });
    }

    @Test
    public void monitorWritePredicate() throws InterruptedException {
        final String oldValue = "Old Value";
        final String newValue = "New Value";
        Monitor<String> monitor = new Monitor<>(oldValue);

        monitor.write(
                entity -> {
                    assertTrue(entity == oldValue, "Motitor has initial entity before modification.");
                    return newValue;
                },
                entity -> {
                    assertTrue(entity == oldValue, "Motitor has initial entity inside predicate.");
                    return entity == oldValue;
                }
        );

        monitor.read(entity -> {
            assertTrue(newValue == entity, "Monitor has changed entity after modification.");
        });
    }

    @Test
    public void monitorWriteMillisPredicate() throws InterruptedException {
        final String oldValue = "Old Value";
        final String newValue = "New Value";
        Monitor<String> monitor = new Monitor<>(oldValue);

        monitor.write(
                entity -> {
                    assertTrue(entity == oldValue, "Motitor has initial entity before modification.");
                    return newValue;
                },
                entity -> {
                    assertTrue(entity == oldValue, "Motitor has initial entity inside predicate.");
                    return entity == oldValue;
                },
                1000, TimeUnit.MILLISECONDS);

        monitor.read(entity -> {
            assertTrue(newValue == entity, "Monitor has changed entity after modification.");
        });
    }

    @Test
    public void constructorWithLocks() {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock readLock = readWriteLock.readLock();
        Lock writeLock = readWriteLock.writeLock();

        Monitor<String> monitor = new Monitor<>("", readLock, writeLock);
        assertEquals(monitor.getReadLock(), readLock, "Read lock is the same as passed to monitor constructor.");
        assertEquals(monitor.getWriteLock(), writeLock, "Write lock is the same as passed to monitor constructor.");
    }
}
