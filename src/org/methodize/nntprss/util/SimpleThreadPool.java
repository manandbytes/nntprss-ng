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

import java.util.Stack;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: SimpleThreadPool.java,v 1.2 2003/01/22 05:11:59 jasonbrome Exp $
 */
public class SimpleThreadPool {

// TODO implement clean shutdown

	private Stack threadStack = new Stack();
	private ThreadGroup threadGroup = null;
	private String threadName;
//private int threadCount = 0;
	private int maxThreads;

	public SimpleThreadPool(String name, String threadName, int maxThreads) {
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
		
		this.maxThreads = maxThreads;
	}

	public synchronized void run(Runnable obj) {
		WorkerThread worker = null;
// FIXME - Add pool limits
		if (threadStack.size() == 0) {
			worker = new WorkerThread(threadGroup, this, threadName);
			worker.run(obj);
			worker.start();
//threadCount++;
		} else {
			worker = (WorkerThread) threadStack.pop();
			worker.run(obj);
		}
	}

	public synchronized void makeThreadAvailable(WorkerThread thread) {
		if(threadStack.size() > maxThreads) {
			thread.end();
		} else {
			threadStack.push(thread);
		}
	}

}
