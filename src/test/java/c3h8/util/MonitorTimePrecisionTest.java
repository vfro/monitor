package c3h8.util;

import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class MonitorTimePrecisionTest {
    private final static Logger log = Logger.getLogger(MonitorTimePrecisionTest.class.getName());

    private final static TimeUnit[] units = {
        TimeUnit.SECONDS, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS
    };

    private final static long[] repeatCounts = {
        3, 100, 100, 1000
    };

    private final static long[] delays = {
        1, 100, 1000, 10000
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
        for (int unit = 0; unit < units.length; unit++) {
            for (int i = 0; i < repeatCounts[unit]; i++) {
                Monitor<String> monitor = new Monitor<String>(null);
                long now = systemTimer(units[unit]);
                boolean result = monitor.readAccess(
                    new Accessor<String>() {
                        @Override
                        public String access(String value) {
                            fail("Never reach read access for false Checker.");
                            return null;
                        }
                    }, new Checker<String>() {
                        @Override
                        public boolean check(String value) {
                            return false;
                        }
                    }, delays[unit], units[unit]);
                assertFalse(result,
                    "Test Monitor.readAccess interrupt wait for units:" + units[unit].toString());
                assertTrue(systemTimer(units[unit]) >= toSystemUnits(delays[unit], units[unit]) + now,
                    "Test Monitor.readAccess wasn't interrupted earlier for units:" + units[unit].toString());
            }
        }
    }

    @Test
    public void monitorPrecisionWriteTest() throws InterruptedException {
        for (int unit = 0; unit < units.length; unit++) {
            for (int i = 0; i < repeatCounts[unit]; i++) {
                Monitor<String> monitor = new Monitor<String>(null);
                long now = systemTimer(units[unit]);
                boolean result = monitor.writeAccess(
                    new Accessor<String>() {
                        @Override
                        public String access(String value) {
                            fail("Never reach read access for false Checker.");
                            return null;
                        }
                    }, new Checker<String>() {
                        @Override
                        public boolean check(String value) {
                            return false;
                        }
                    }, delays[unit], units[unit]);
                assertFalse(result,
                    "Test Monitor.writeAccess interrupt wait for units:" + units[unit].toString());
                assertTrue(systemTimer(units[unit]) >= toSystemUnits(delays[unit], units[unit]) + now,
                    "Test Monitor.writeAccess wasn't interrupted earlier for units:" + units[unit].toString());
            }
        }
    }
}
