package com.github.vfro;

import com.github.vfro.Monitor;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class MonitorTimePrecisionTest {

    private final static TimeUnit[] UNITS = {
        TimeUnit.SECONDS, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS
    };

    private final static long[] REPEAT_COUNTS = {
        3, 10, 100, 1000
    };

    private final static long[] DELAYS = {
        1, 10, 100, 1000
    };

    public MonitorTimePrecisionTest() {
    }

    static long toSystemUnits(long time, TimeUnit unit) {
        return unit.convert(time, systemUnit(unit));
    }

    static TimeUnit systemUnit(TimeUnit unit) {
        if (unit == TimeUnit.NANOSECONDS || unit == TimeUnit.MICROSECONDS) {
            return TimeUnit.NANOSECONDS;
        }
        return TimeUnit.MILLISECONDS;
    }

    private static long systemTimer(TimeUnit unit) {
        if (unit == TimeUnit.NANOSECONDS || unit == TimeUnit.MICROSECONDS) {
            return System.nanoTime();
        }
        return System.currentTimeMillis();
    }

    @Test
    public void monitorPrecisionReadTest() throws InterruptedException {
        for (int unit = 0; unit < UNITS.length; unit++) {
            for (int i = 0; i < REPEAT_COUNTS[unit]; i++) {
                Monitor<String> monitor = new Monitor<String>(null);
                long now = systemTimer(UNITS[unit]);
                boolean result = monitor.readAccess(
                        value -> {
                            fail("Never reach read access for false Predicate.");
                        },
                        x -> false,
                        DELAYS[unit], UNITS[unit]);
                assertFalse(result,
                        "Test Monitor.readAccess interrupt wait for units:" + UNITS[unit].toString());
                assertTrue(systemTimer(UNITS[unit]) >= toSystemUnits(DELAYS[unit], UNITS[unit]) + now,
                        "Test Monitor.readAccess wasn't interrupted earlier for units:" + UNITS[unit].toString());
            }
        }
    }

    @Test
    public void monitorPrecisionWriteTest() throws InterruptedException {
        for (int unit = 0; unit < UNITS.length; unit++) {
            for (int i = 0; i < REPEAT_COUNTS[unit]; i++) {
                Monitor<String> monitor = new Monitor<String>(null);
                long now = systemTimer(UNITS[unit]);
                boolean result = monitor.writeAccess(
                        value -> {
                            fail("Never reach read access for false Predicate.");
                            return null;
                        },
                        x -> false,
                        DELAYS[unit], UNITS[unit]);
                assertFalse(result,
                        "Test Monitor.writeAccess interrupt wait for units:" + UNITS[unit].toString());
                assertTrue(systemTimer(UNITS[unit]) >= toSystemUnits(DELAYS[unit], UNITS[unit]) + now,
                        "Test Monitor.writeAccess wasn't interrupted earlier for units:" + UNITS[unit].toString());
            }
        }
    }
}
