package org.methodize.nntprss.rss;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002 Jason Brome.  All Rights Reserved.
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

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.methodize.nntprss.util.SimpleThreadPool;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */
public class ChannelPoller extends Thread {

	private Logger log = Logger.getLogger(ChannelPoller.class);

	private Map channels;
	private boolean active = true;
	private SimpleThreadPool simpleThreadPool;

	private static final int MAX_POLL_THREADS = 20;

	public ChannelPoller(Map channels) {
		super("Channel Poller");
		this.channels = channels;
		simpleThreadPool = new SimpleThreadPool("Channel Poll Workers", "Channel Poll Worker", MAX_POLL_THREADS);
	}

	public synchronized void shutdown() {
		active = false;
		this.notify();
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (active) {
			if (log.isDebugEnabled()) {
				log.debug("Checking feeds for poll action");
			}
			Iterator channelIter = channels.values().iterator();
			while (channelIter.hasNext() && active) {
				Channel channel = (Channel) channelIter.next();
				if (channel.isAwaitingPoll()) {
					simpleThreadPool.run(channel);
				}
			}

			if (log.isDebugEnabled()) {
				log.debug("Finished checking feeds for poll action");
			}

			synchronized (this) {
				if (active) {
					try {
						// Check pending polls every 30 seconds
						wait(30 * 1000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

}
