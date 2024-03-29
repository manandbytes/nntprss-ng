package org.methodize.nntprss.util;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2007 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * mail:  Jason Brome
 *        PO Box 222-WOB
 *        West Orange
 *        NJ 07052-0222
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

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: FixedThreadPool.java,v 1.10 2007/12/17 04:17:04 jasonbrome Exp $
 */

public class FixedThreadPool {

    private static final Logger log = Logger.getLogger(FixedThreadPool.class);
	
    private final ThreadGroup threadGroup;
    private final List pool = new ArrayList();
    private final String threadName;

    private volatile boolean shutdown = false;

    private class WorkerThread extends Thread {
        private volatile boolean shutdown = false;
        private Runnable task;

        public WorkerThread(ThreadGroup threadGroup, String threadName) {
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
                } catch (Exception e) {
                    if (log.isEnabledFor(Priority.WARN)) {
                        log.warn(
                            "Exception thrown in FixedThreadPool.Worker during task execution",
                            e);
                    }
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

        this.threadGroup = new ThreadGroup(name != null ? name : "anonymous");
        this.threadName = threadName != null ? threadName : "STP-anonymous";

        for (int i = 0; i < maxThreads; i++) {
            WorkerThread worker =
                new WorkerThread(this.threadGroup, this.threadName + " #" + i);
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
