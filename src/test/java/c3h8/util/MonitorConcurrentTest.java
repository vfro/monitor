package c3h8.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MonitorConcurrentTest {

    MonitorConcurrentTest() {
    }

    @Test
    public void monitorConcurentReadWriteAccess()
            throws InterruptedException, BrokenBarrierException {
        final List<Object> errors = new LinkedList<>();
        final Monitor<String> monitor = new Monitor<>("");
        final CyclicBarrier barrier = new CyclicBarrier(3);

        Thread reader = new Thread(() -> {
            try {
                barrier.await();
                monitor.readAccess(
                        value -> {
                            if (!value.equals("reader-await")) {
                                errors.add("monitor reader predicate doesn't work.");
                            }
                        },
                        value -> value.equals("reader-await")
                );
            } catch (InterruptedException | BrokenBarrierException e) {
                errors.add(e);
            }
        }, "monitorConcurentReadWriteAccess.reader");

        Thread writer = new Thread(() -> {
            try {
                barrier.await();
                monitor.writeAccess(
                        value -> {
                            if (!value.equals("writer-await")) {
                                errors.add("monitor write predicate doesn't work.");
                            }
                            return "reader-await";
                        },
                        value -> value.equals("writer-await")
                );
            } catch (InterruptedException | BrokenBarrierException e) {
                errors.add(e);
            }
        }, "monitorConcurentReadWriteAccess.writer");

        reader.start();
        writer.start();

        barrier.await();
        monitor.set("writer-await");

        reader.join();
        writer.join();

        monitor.readAccess(value -> {
            assertEquals(value, "reader-await", "Test Monitor writeAccess/readAccess.");
        });
        assertEquals(errors.size(), 0, "Test monitor has no errors during writeAccess/readAccess.");
    }

    @Test
    public void calculateSum() throws InterruptedException, BrokenBarrierException {
        final List<Object> errors = new LinkedList<>();

        final Monitor<Stack<Integer>> sum = new Monitor<>(new Stack<>());
        int result = 0;
        for (int i = 0; i < 1000; i++) {
            result += i;
            final int item = i;
            sum.writeAccess(
                    stackSum -> {
                        stackSum.push(item);
                        return stackSum;
                    });
        }
        final int finalResult = result;

        final CyclicBarrier barrier = new CyclicBarrier(11);
        List<Thread> workers = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Thread worker
                    = new Thread(
                            () -> {
                                try {
                                    barrier.await();
                                    while (true) {
                                        sum.writeAccess(
                                                stackSum -> {
                                                    int value1 = stackSum.pop();
                                                    int value2 = stackSum.pop();
                                                    stackSum.push(value1 + value2);
                                                    return stackSum;
                                                },
                                                stackSum -> stackSum.size() >= 2
                                        );
                                    }
                                } catch (InterruptedException e) {
                                    // It is okay. Worker is interrupted.
                                } catch (BrokenBarrierException e) {
                                    errors.add(e);
                                }
                            }, "calculateSum.worker " + Integer.toString(i));
            worker.start();
            workers.add(worker);
        }

        barrier.await();
        sum.readAccess(
                stackSum -> {
                    assertEquals(finalResult, (int) stackSum.pop(), "Workers have calculated resuld correctly.");
                },
                stackSum -> stackSum.size() == 1
        );

        assertEquals(errors.size(), 0, "Test monitor has no errors during calculating sum.");
        workers.stream().forEach((worker) -> {
            worker.interrupt();
        });
    }
}
