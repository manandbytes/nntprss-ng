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

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: WorkerThread.java,v 1.2 2003/01/22 05:12:15 jasonbrome Exp $
 */
public class WorkerThread extends Thread {

	private boolean active = true;
	private Runnable job = null;
	private SimpleThreadPool threadPool = null;

	public WorkerThread(
		ThreadGroup threadGroup,
		SimpleThreadPool threadPool,
		String threadName) {
		super(threadGroup, (Runnable) null, threadName);
		this.threadPool = threadPool;
	}

	public synchronized void run(Runnable newJob) {
		if (this.job != null) {
			throw new IllegalStateException("Thread is already handling job");
		} else {
			this.job = newJob;
			this.notify();
		}
	}

	public synchronized void end() {
		active = false;
		this.notify();
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (active) {
			// We have work to do?
			if (job != null) {
				try {
					job.run();
				} catch (Exception e) {
					// Need to log the exception..
				}
			}

			job = null;

			// Return thread to the pool
			threadPool.makeThreadAvailable(this);

			// Wait for next job
			synchronized (this) {
				while (active && job == null) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}

		}
	}

}
