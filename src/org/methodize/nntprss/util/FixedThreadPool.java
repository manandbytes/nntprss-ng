package org.methodize.nntprss.util;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002, 2003 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * mail:  Methodize Solutions
 *        PO Box 3865
 *        Grand Central Station
 *        New York NY 10163
 * 
 * This file is part of nntp//rss
 * 
 * nntp//rss is free software; you can redistribute it 
 * and/or modify it under the terms of the GNU General 
 * Public License as published by the Free Software Foundation; 
 * either version 2 of the License, or (at your option) any 
 * later version.
 *
 * This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE.  See the GNU General Public License for more 
 * details.
 *
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
 * Boston, MA  02111-1307  USA
 * ----------------------------------------------------- */

import java.util.List;
import java.util.ArrayList;

/**
 * @author Jason Brome <jason@methodize.org>
 */


public class FixedThreadPool {

	private ThreadGroup threadGroup = null;
	private List pool = new ArrayList();
	private boolean shutdown = false;
	private String threadName;

	private class WorkerThread extends Thread {
		private boolean shutdown = false;
		private Runnable task;

		public WorkerThread(
			ThreadGroup threadGroup,
			String threadName) {
			super(threadGroup, (Runnable) null, threadName);
		}

		public void kill() {
			shutdown = true;
			interrupt();
		}

		public void run(Runnable runnable) {
			task = runnable;
			synchronized (this) {
				notifyAll();
			}
		}

		public void run() {
			while (!shutdown) {

				while (task == null) {
					try {
						synchronized (this) {
							wait();
						}
					} catch (InterruptedException ex) {
						if (shutdown) {
							return;
						}
					}
				}

				try {
					task.run();
				} catch (Throwable t) {
				}

				task = null;
				if (shutdown) {
					return;
				}

				synchronized (pool) {
					pool.add(this);
					pool.notifyAll();
				}
			}
		}
	}

	// Constructor

	public FixedThreadPool(String name, String threadName, int maxThreads) {
		// Thread groups enable easier debugging in certain IDE,
		// so lets assign our pool threads to a specific group

		if (name != null) {
			threadGroup = new ThreadGroup(name);
		} else {
			threadGroup = new ThreadGroup("anonymous");
		}

		if (threadName != null) {
			this.threadName = threadName;
		} else {
			this.threadName = "STP-anonymous";
		}

		for (int i = 0; i < maxThreads; i++) {
			WorkerThread worker = new WorkerThread(threadGroup, threadName);
			pool.add(worker);
			worker.start();
		}
	}

	
	public void shutdown() {
		shutdown = true;
		Object[] workers = pool.toArray();
		for (int i = 0; i < workers.length; i++) {
			((WorkerThread) workers[i]).kill();
		}

		synchronized (pool) {
			pool.clear();
			pool.notifyAll();
		}
	}


	public void run(Runnable task) {
		WorkerThread worker = null;
		synchronized (pool) {
			while (pool.isEmpty()) {

				if (shutdown) {
					return;
				}
				try {
					pool.wait();
				} catch (InterruptedException ex) {
					return;
				}
			}

			worker = (WorkerThread) pool.remove(0);
		}
		worker.run(task);
	}
}
