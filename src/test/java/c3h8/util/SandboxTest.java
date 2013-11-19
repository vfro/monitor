package c3h8.util;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

public class SandboxTest {

    private static final class CloneableString implements Cloneable {
        private String string;
        private boolean cloneInvoked = false;

        public CloneableString(String string) {
            this.string = string;
        }

        public String get() {
            return this.string;
        }

        public boolean isCloneInvoked() {
            return this.cloneInvoked;
        }

        @Override
        public CloneableString clone() {
            this.cloneInvoked = true;
            return new CloneableString(this.string);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (obj == null || !this.getClass().equals(obj.getClass())) {
                return false;
            }
            CloneableString same = (CloneableString)obj;
            return this.string.equals(same.string);
        }

        @Override
        public int hashCode() {
            return this.string.hashCode();
        }

        @Override
        public String toString() {
            return this.string;
        }
    }

    @Test
    public void sandboxConstructorGet() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<CloneableString>(string);
        assertTrue(sandbox.get() == string, "Sandbox.get() returns the same value as passed to constructor.");
    }

    @Test
    public void sandboxSetGet() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<CloneableString>();
        sandbox.set(string);
        assertTrue(sandbox.get() == string, "Sandbox.get() returns the same value as passed to constructor.");
        assertFalse(string.isCloneInvoked(), "Sandbox.set() does not invoke clone method for the argument.");
    }

    @Test
    public void sandboxPull() {
        CloneableString string = new CloneableString("string");
        Monitor<CloneableString> monitor = new Monitor<CloneableString>(string);
        Sandbox<CloneableString> sandbox = new Sandbox<CloneableString>();

        CloneableString pullString = sandbox.pull(monitor);

        assertTrue(string.isCloneInvoked(), "Sandbox.pull() invokes clone() method for the argument.");
        assertEquals(pullString, string, "Sandbox.pull() returns copy of the value from monitor.");
        assertTrue(pullString != string, "Sandbox.pull() returns not the same instance as pulled from monitor, but its copy.");
        assertTrue(sandbox.get() == pullString, "Sandbox.get() returns the same value as pulled from monitor.");
    }

    @Test
    public void sandboxPush() {
        CloneableString originalString = new CloneableString("original string");
        CloneableString pushString = new CloneableString("push string");

        Monitor<CloneableString> monitor = new Monitor<CloneableString>(originalString);
        Sandbox<CloneableString> sandboxPush = new Sandbox<CloneableString>();
        Sandbox<CloneableString> sandboxPull = new Sandbox<CloneableString>();
        
        sandboxPush.pull(monitor);
        sandboxPush.set(pushString);
        boolean isPushed = sandboxPush.push(monitor);
        sandboxPull.pull(monitor);

        assertTrue(isPushed, "Sandbox.push() completed successfully.");
        assertEquals(sandboxPull.get(), pushString, "Sandbox.push() puts its value to monitor.");
    }

    @Test
    public void sandboxUnsuccessfulPush() {
        CloneableString originalString = new CloneableString("original string");
        CloneableString pushString = new CloneableString("push string");
        CloneableString unsuccessfulPushString = new CloneableString("unsuccessful push string");

        Monitor<CloneableString> monitor = new Monitor<CloneableString>(originalString);
        Sandbox<CloneableString> sandboxSuccessfulPush = new Sandbox<CloneableString>();
        Sandbox<CloneableString> sandboxUnsuccessfulPush = new Sandbox<CloneableString>();
        
        sandboxSuccessfulPush.pull(monitor);
        sandboxUnsuccessfulPush.pull(monitor);

        sandboxSuccessfulPush.set(pushString);
        sandboxSuccessfulPush.push(monitor);

        sandboxUnsuccessfulPush.set(unsuccessfulPushString);
        boolean isPushed = sandboxUnsuccessfulPush.push(monitor);

        assertFalse(isPushed, "Sandbox.push() completed unsuccessfully for updated monitor.");
        assertEquals(sandboxUnsuccessfulPush.get(), unsuccessfulPushString, "Sandbox value was not changed after unsuccessful push.");
    }

    @Test
    public void sandboxForcePush() {
        CloneableString originalString = new CloneableString("original string");
        CloneableString pushString = new CloneableString("push string");
        CloneableString forcePushString = new CloneableString("force push string");

        Monitor<CloneableString> monitor = new Monitor<CloneableString>(originalString);
        Sandbox<CloneableString> sandboxPush = new Sandbox<CloneableString>();
        Sandbox<CloneableString> sandboxForcePush = new Sandbox<CloneableString>();
        
        sandboxPush.pull(monitor);
        sandboxForcePush.pull(monitor);

        sandboxPush.set(pushString);
        sandboxPush.push(monitor);

        sandboxForcePush.set(forcePushString);
        boolean isPushed = sandboxForcePush.push(monitor, true);

        Sandbox<CloneableString> sandboxCheckPush = new Sandbox<CloneableString>();
        sandboxCheckPush.pull(monitor);

        assertTrue(isPushed, "Sandbox.push() completed successfully for forced push.");
        assertEquals(sandboxCheckPush.get(), forcePushString, "Sandbox value was pushed to monitor on forced push.");
    }

    @Test
    public void sandboxToString() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<CloneableString>(string);
        assertEquals(sandbox.toString(), string.toString(), "Sandbox.toString() equals to Sandbox.get().toString()");
    }

    @Test
    public void sandboxEquals() {
        CloneableString stringOne = new CloneableString("string");
        Monitor<CloneableString> monitorOne = new Monitor<CloneableString>(stringOne);
        Sandbox<CloneableString> sandboxOne = new Sandbox<CloneableString>(monitorOne);

        CloneableString stringOneAndAHalf = new CloneableString("string one and a half");
        Monitor<CloneableString> monitorOneAndAHalf = new Monitor<CloneableString>(stringOneAndAHalf);
        Sandbox<CloneableString> sandboxTwo = new Sandbox<CloneableString>(monitorOneAndAHalf);

        CloneableString stringTwo = new CloneableString("string");
        sandboxTwo.set(stringTwo);

        assertEquals(sandboxOne, sandboxTwo, "Sandbox.equals() equals other Sandbox with the same value");
    }

    @Test
    public void sandboxHashCode() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<CloneableString>(string);
        assertEquals(sandbox.hashCode(), string.hashCode(), "Sandbox.hashCode() equals to Sandbox.get().hashCode()");
    }

    @Test
    public void sandboxClone() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<CloneableString>(string);
        Sandbox<CloneableString> clone = sandbox.clone();
        assertEquals(clone, sandbox, "Sandbox.clone() equals to itself");
    }
}
