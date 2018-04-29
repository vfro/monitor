package com.github.vfro;

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
    public void monitorConcurentReadWrite() 
            throws InterruptedException, BrokenBarrierException {
        final List<Object> errors = new LinkedList<>();
        final Monitor<String> monitor = new Monitor<>("");
        final CyclicBarrier barrier = new CyclicBarrier(3);

        Thread reader = new Thread(() -> {
            try {
                barrier.await();
                monitor.read(
                        entity -> {
                            if (!entity.equals("reader-await")) {
                                errors.add("monitor reader predicate doesn't work.");
                            }
                        },
                        entity -> entity.equals("reader-await")
                );
            } catch (InterruptedException | BrokenBarrierException e) {
                errors.add(e);
            }
        }, "monitorConcurentReadWrite.reader");

        Thread writer = new Thread(() -> {
            try {
                barrier.await();
                monitor.write(
                        entity -> {
                            if (!entity.equals("writer-await")) {
                                errors.add("monitor write predicate doesn't work.");
                            }
                            return "reader-await";
                        },
                        entity -> entity.equals("writer-await")
                );
            } catch (InterruptedException | BrokenBarrierException e) {
                errors.add(e);
            }
        }, "monitorConcurentReadWrite.writer");

        reader.start();
        writer.start();

        barrier.await();
        monitor.set("writer-await");

        reader.join();
        writer.join();

        monitor.read(entity -> {
            assertEquals(entity, "reader-await", "Monitor final state after write/read.");
        });
        assertEquals(errors.size(), 0, "There were no exceptions during write/read.");
    }

    @Test
    public void calculateSum()
            throws InterruptedException, BrokenBarrierException {
        final List<Object> errors = new LinkedList<>();

        final Monitor<Stack<Integer>> sum = new Monitor<>(new Stack<>());
        int result = 0;
        for (int i = 0; i < 1000; i++) {
            result += i;
            final int item = i;
            sum.write(
                    stackSum -> {
                        stackSum.push(item);
                        return stackSum;
                    });
        }
        final int finalResult = result;

        final int THREADS_COUNT = 10;
        final CyclicBarrier barrier = new CyclicBarrier(THREADS_COUNT + 1);
        List<Thread> workers = new LinkedList<>();
        for (int i = 0; i < THREADS_COUNT; i++) {
            Thread worker
                    = new Thread(
                            () -> {
                                try {
                                    barrier.await();
                                    while (true) {
                                        sum.write(
                                                stackSum -> {
                                                    int entity1 = stackSum.pop();
                                                    int entity2 = stackSum.pop();
                                                    stackSum.push(entity1 + entity2);
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
        sum.read(
                stackSum -> {
                    assertEquals(finalResult, (int) stackSum.pop(), "Workers have calculated resulted sum correctly.");
                },
                stackSum -> stackSum.size() == 1
        );

        assertEquals(errors.size(), 0, "There were no exceptions during sum calculation.");
        workers.stream().forEach(Thread::interrupt);
    }
}
