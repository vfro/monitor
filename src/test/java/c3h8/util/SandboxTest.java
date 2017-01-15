package c3h8.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class SandboxTest {

    private static final class CloneableString implements Cloneable {

        private final String string;
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
        @SuppressWarnings("CloneDoesntCallSuperClone")
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
            CloneableString same = (CloneableString) obj;
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
        Sandbox<CloneableString> sandbox = new Sandbox<>(string);
        assertTrue(sandbox.get() == string, "Sandbox.get() returns the same value as passed to constructor.");
    }

    @Test
    public void sandboxSetGet() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<>();
        sandbox.set(string);
        assertTrue(sandbox.get() == string, "Sandbox.get() returns the same value as passed to constructor.");
        assertFalse(string.isCloneInvoked(), "Sandbox.set() does not invoke clone method for the argument.");
    }

    @Test
    public void sandboxPull() {
        CloneableString string = new CloneableString("string");
        Monitor<CloneableString> monitor = new Monitor<>(string);
        Sandbox<CloneableString> sandbox = new Sandbox<>();

        CloneableString pullString = sandbox.pull(monitor);

        assertTrue(string.isCloneInvoked(), "Sandbox.pull() invokes clone() method for the argument.");
        assertEquals(pullString, string, "Sandbox.pull() returns copy of the value from monitor.");
        assertTrue(pullString != string, "Sandbox.pull() returns not the same instance as pulled from monitor, but its copy.");
        assertTrue(sandbox.get() == pullString, "Sandbox.get() returns the same value as pulled from monitor.");
    }

    @Test
    public void sandboxPush() {
        CloneableString originalString = new CloneableString("original string");
        CloneableString otherString = new CloneableString("other string");
        CloneableString pushString = new CloneableString("push string");

        Monitor<CloneableString> monitor = new Monitor<>(originalString);
        Sandbox<CloneableString> sandboxPush = new Sandbox<>();
        Sandbox<CloneableString> sandboxPull = new Sandbox<>();

        sandboxPush.pull(monitor);
        monitor.set(otherString);
        sandboxPush.set(pushString);
        sandboxPush.push(monitor);

        sandboxPull.pull(monitor);
        assertEquals(sandboxPull.get(), pushString, "Sandbox.push() puts its value to monitor unconditionaly.");
    }

    @Test
    @SuppressWarnings("RedundantStringConstructorCall")
    public void sandboxCasByValue() {
        CloneableString originalString = new CloneableString(new String("original string"));
        CloneableString theSameString = new CloneableString(new String("original string"));
        CloneableString successfulCasString = new CloneableString("successful push string");
        CloneableString unsuccessfulCasString = new CloneableString("unsuccessful push string");

        Monitor<CloneableString> monitor = new Monitor<>(originalString);
        Sandbox<CloneableString> sandboxSuccessfulCas = new Sandbox<>();
        Sandbox<CloneableString> sandboxUnsuccessfulCas = new Sandbox<>();

        Sandbox<CloneableString> sandboxCheck = new Sandbox<>();

        sandboxSuccessfulCas.pull(monitor);
        sandboxSuccessfulCas.set(successfulCasString);

        sandboxUnsuccessfulCas.pull(monitor);
        sandboxUnsuccessfulCas.set(unsuccessfulCasString);
        monitor.set(theSameString);

        // Monitor contains identical string
        boolean successful = sandboxSuccessfulCas.casByValue(monitor);
        sandboxCheck.pull(monitor);

        assertTrue(successful, "Sandbox.casByValue() completed successfully for identical string.");
        assertEquals(sandboxCheck.get(), successfulCasString, "Sandbox.casByValue() puts its value to monitor.");

        // Monitor contains identical string
        boolean unsuccessful = sandboxUnsuccessfulCas.casByValue(monitor);
        sandboxCheck.pull(monitor);

        assertFalse(unsuccessful, "Sandbox.casByValue() completed unsuccessfully for non-identical string.");
    }

    @Test
    @SuppressWarnings("RedundantStringConstructorCall")
    public void sandboxCasByReference() {
        CloneableString originalString = new CloneableString("original string");
        CloneableString theSameString = new CloneableString(new String("original string"));
        CloneableString successfulCasString = new CloneableString("successful push string");
        CloneableString unsuccessfulCasString = new CloneableString("unsuccessful push string");

        Monitor<CloneableString> monitor = new Monitor<>(originalString);
        Sandbox<CloneableString> sandboxSuccessfulCas = new Sandbox<>();
        Sandbox<CloneableString> sandboxUnsuccessfulCas = new Sandbox<>();

        Sandbox<CloneableString> sandboxCheck = new Sandbox<>();

        sandboxSuccessfulCas.pull(monitor);
        sandboxSuccessfulCas.set(successfulCasString);

        sandboxUnsuccessfulCas.pull(monitor);
        sandboxUnsuccessfulCas.set(unsuccessfulCasString);

        // Monitor contains identical string
        boolean successful = sandboxSuccessfulCas.casByReference(monitor);
        sandboxCheck.pull(monitor);

        assertTrue(successful, "Sandbox.casByReference() completed successfully for identical string.");
        assertEquals(sandboxCheck.get(), successfulCasString, "Sandbox.casByReference() puts its value to monitor.");

        monitor.set(theSameString);

        // Monitor contains identical string
        boolean unsuccessful = sandboxUnsuccessfulCas.casByReference(monitor);
        sandboxCheck.pull(monitor);

        assertFalse(unsuccessful, "Sandbox.casByReference() completed unsuccessfully for non-identical string.");
    }

    @Test
    public void sandboxToString() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<>(string);
        assertEquals(sandbox.toString(), string.toString(), "Sandbox.toString() equals to Sandbox.get().toString().");
    }

    @Test
    public void sandboxEquals() {
        CloneableString stringOne = new CloneableString("string");
        Monitor<CloneableString> monitorOne = new Monitor<>(stringOne);
        Sandbox<CloneableString> sandboxOne = new Sandbox<>(monitorOne);

        CloneableString stringOneAndAHalf = new CloneableString("string one and a half");
        Monitor<CloneableString> monitorOneAndAHalf = new Monitor<>(stringOneAndAHalf);
        Sandbox<CloneableString> sandboxTwo = new Sandbox<>(monitorOneAndAHalf);

        CloneableString stringTwo = new CloneableString("string");
        sandboxTwo.set(stringTwo);

        assertEquals(sandboxOne, sandboxTwo, "Sandbox.equals() equals other Sandbox with the same value.");
    }

    @Test
    public void sandboxNotEquals() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<>(string);

        assertNotEquals(sandbox, null, "Sandbox.equals() doesn't equals to null.");
        assertNotEquals(sandbox, string, "Sandbox.equals() doesn't equals to value.");
    }

    @Test
    public void sandboxHashCode() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<>(string);
        assertEquals(sandbox.hashCode(), string.hashCode(), "Sandbox.hashCode() equals to Sandbox.get().hashCode().");
    }

    @Test
    public void sandboxClone() {
        CloneableString string = new CloneableString("string");
        Sandbox<CloneableString> sandbox = new Sandbox<>(string);
        Sandbox<CloneableString> clone = sandbox.clone();
        assertEquals(clone, sandbox, "Sandbox.clone() equals to itself.");
    }
}
