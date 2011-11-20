package c3h8.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;

public class MonitorConcurrentTest {
    MonitorConcurrentTest() {
    }

    @Test
    public void monitorConcurentReadWriteAccess()
        throws InterruptedException, BrokenBarrierException {
        final List<Object> errors = new LinkedList<Object>();
        final Monitor<String> monitor = new Monitor<String>("");
        final CyclicBarrier barrier = new CyclicBarrier(3);

        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await();
                    monitor.readAccess(
                        new Accessor<String>() {
                            @Override
                            public String access(String value) {
                                if (!value.equals("reader-await")) {
                                    errors.add("monitor reader checker doesn't work.");
                                }
                                return value;
                            }
                        },

                        new Checker<String>() {
                            @Override
                            public boolean check(String value) {
                                return value.equals("reader-await");
                            }
                        }
                    );
                } catch(InterruptedException e) {
                    errors.add(e);
                } catch(BrokenBarrierException e) {
                    errors.add(e);
                }
            }
        }, "monitorConcurentReadWriteAccess.reader");

        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await();
                    monitor.writeAccess(
                        new Accessor<String>() {
                            @Override
                            public String access(String value) {
                                if (!value.equals("writer-await")) {
                                    errors.add("monitor write checker doesn't work.");
                                }
                                return "reader-await";
                            }
                        },
                        new Checker<String>() {
                            @Override
                            public boolean check(String value) {
                                return value.equals("writer-await");
                            }
                        }
                    );
                } catch(InterruptedException e) {
                    errors.add(e);
                } catch(BrokenBarrierException e) {
                    errors.add(e);
                }
            }
        }, "monitorConcurentReadWriteAccess.writer");

        reader.start();
        writer.start();

        barrier.await();
        monitor.set("writer-await");

        reader.join();
        writer.join();

        monitor.readAccess(new Accessor<String>() {
            @Override
            public String access(String value) {
                assertEquals(value, "reader-await", "Test Monitor writeAccess/readAccess.");
                return null;
            }
        });
        assertEquals(errors.size(), 0, "Test monitor has no errors during writeAccess/readAccess.");
    }

    @Test
    public void calculateSum() throws InterruptedException, BrokenBarrierException {
        final List<Object> errors = new LinkedList<Object>();

        final Monitor<Stack<Integer>> sum = new Monitor<Stack<Integer>>(new Stack<Integer>());
        int result = 0;
        for (int i = 0; i < 1000; i++) {
            result += i;
            final int item = i;
            sum.writeAccess(
                    new Accessor<Stack<Integer>>() {
                        @Override
                        public Stack<Integer> access(Stack<Integer> stackSum) {
                            stackSum.push(item);
                            return stackSum;
                        }
                });
        }
        final int finalResult = result;

        final CyclicBarrier barrier = new CyclicBarrier(11);
        List<Thread> workers = new LinkedList<Thread>();
        for (int i = 0; i < 10; i++) {
            Thread worker = 
                new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                barrier.await();
                                while(true) {
                                    sum.writeAccess(
                                            new Accessor<Stack<Integer>>() {
                                                @Override
                                                public Stack<Integer> access(Stack<Integer> stackSum) {
                                                    int value1 = stackSum.pop();
                                                    int value2 = stackSum.pop();
                                                    stackSum.push(value1 + value2);
                                                    return stackSum;
                                                }
                                            },
                                            new Checker<Stack<Integer>>() {
                                                @Override
                                                public boolean check(Stack<Integer> stackSum) {
                                                    return stackSum.size() >= 2;
                                                }
                                            }
                                        );
                                }
                            } catch(InterruptedException e) {
                                // It is okay. Worker is interrupted.
                            } catch(BrokenBarrierException e) {
                                errors.add(e);
                            }
                        }
            }, "calculateSum.worker " + Integer.valueOf(i).toString());
            worker.start();
            workers.add(worker);
        }

        barrier.await();
        sum.readAccess(
                new Accessor<Stack<Integer>>() {
                    @Override
                    public Stack<Integer> access(Stack<Integer> stackSum) {
                        int result = stackSum.pop();
                        assertEquals(finalResult, result, "Workers have calculated resuld correctly.");
                        return stackSum;
                    }
                },
                new Checker<Stack<Integer>>() {
                    @Override
                    public boolean check(Stack<Integer> stackSum) {
                        return stackSum.size() == 1;
                    }
                }
            );

        assertEquals(errors.size(), 0, "Test monitor has no errors during calculating sum.");
        for(Thread worker : workers) {
            worker.interrupt();
        }
    }
}
