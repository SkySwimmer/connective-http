package org.asf.connective.tasks;

/**
 * 
 * Internal async task object
 * 
 * @author Sky Swimmer
 *
 */
public class AsyncTask {
	private Runnable action;
	private boolean run;

	private Object lock = new Object();

	public AsyncTask(Runnable action) {
		this.action = action;
	}

	void run() {
		try {
			action.run();
		} finally {
			// Release
			synchronized (lock) {
				run = true;
				lock.notifyAll();
			}
		}
	}

	public boolean hasRun() {
		return run;
	}

	public void block() {
		if (run)
			return;
		synchronized (lock) {
			while (!run) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

}
