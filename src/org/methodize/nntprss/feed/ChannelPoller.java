package org.methodize.nntprss.feed;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2004 Jason Brome.  All Rights Reserved.
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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.util.FixedThreadPool;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelPoller.java,v 1.4 2004/10/26 01:13:33 jasonbrome Exp $
 */
public class ChannelPoller extends Thread {

    private Logger log = Logger.getLogger(ChannelPoller.class);

    private Map channels;
    private boolean active = true;
    //	private SimpleThreadPool simpleThreadPool;
    private FixedThreadPool fixedThreadPool;

    //	private static final int MAX_POLL_THREADS = 20;
    private static final int MAX_POLL_THREADS = 4;

    // Check pending polls every 30 seconds
    private static final int POLL_INTERVAL = 30 * 1000;

    public ChannelPoller(Map channels) {
        super("Channel Poller");
        this.channels = channels;
        fixedThreadPool =
            new FixedThreadPool(
                "Channel Poll Workers",
                "Channel Poll Worker",
                MAX_POLL_THREADS);
    }

	public ChannelPoller(Map channels, int threads) {
		super("Channel Poller");
		this.channels = channels;
		fixedThreadPool =
			new FixedThreadPool(
				"Channel Poll Workers",
				"Channel Poll Worker",
				threads);
	}

    public synchronized void shutdown() {
        active = false;
        fixedThreadPool.shutdown();
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

            try {
                // Moved channel iterator retrieval within loop...
                Iterator channelIter = channels.values().iterator();

                while (channelIter.hasNext() && active) {
                    Channel channel = (Channel) channelIter.next();
                    if (channel.isEnabled()) {
                        //						if(channel.isPolling()) {
                        //							channel.checkConnection();
                        //						}

                        if (channel.isAwaitingPoll()) {
                            fixedThreadPool.run(channel);
                        }
                    }
                }
            } catch (ConcurrentModificationException cme) {
                // Some channel management activity coincided with channel poll
                // FIXME implement thread-safe approach				
                if (log.isDebugEnabled()) {
                    log.debug(
                        "ConcurrentModificationException in Channel Poller");
                }
            } catch (Exception e) {
                if (log.isEnabledFor(Priority.WARN)) {
                    log.warn("Exception thrown in Channel Poller", e);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished checking feeds for poll action");
            }

            synchronized (this) {
                if (active) {
                    try {
                        // Check pending polls every 30 seconds
                        wait(POLL_INTERVAL);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

}
