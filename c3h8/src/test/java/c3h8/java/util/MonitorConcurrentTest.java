package c3h8.java.util;

import java.util.LinkedList;
import java.util.List;

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
		});

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
					return;
				} catch(BrokenBarrierException e) {
					errors.add(e);
				}
			}
		});

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

}
