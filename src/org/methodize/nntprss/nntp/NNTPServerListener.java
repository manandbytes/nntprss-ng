package org.methodize.nntprss.nntp;

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

import java.io.IOException;
import java.net.*;

import org.apache.log4j.Logger;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: NNTPServerListener.java,v 1.10 2007/12/17 04:14:52 jasonbrome Exp $
 */
public class NNTPServerListener extends Thread {

    private static final Logger log = Logger.getLogger(NNTPServerListener.class);

    private volatile boolean active = true;

    private final ServerSocket serverSocket;
    private final NNTPServer nntpServer;

    public NNTPServerListener(NNTPServer nntpServer, int port)
        throws Exception {
    	try {
    		serverSocket = new ServerSocket(port);
    	} catch(BindException be) {
    		if(port <= 1024) {
    			throw new Exception("Bind exception establishing NNTP server socket on port " + port
    					+ " - if running on Unix, are you running as root?");
    		} else {
    			throw be;
    		}
    	}
        this.nntpServer = nntpServer;
    }

    public NNTPServerListener(
        NNTPServer nntpServer,
        int port,
        InetAddress address)
        throws Exception {
        serverSocket = new ServerSocket(port, 0, address);
        this.nntpServer = nntpServer;
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
            try {
                Socket clientSocket = serverSocket.accept();

                clientSocket.setTcpNoDelay(true);

                if (log.isInfoEnabled()) {
                    log.info(
                        "NNTP Client connection from "
                            + clientSocket.getInetAddress().getHostAddress());
                    //					log.info(
                    //						"NNTP Client connection from "
                    //							+ clientSocket.getInetAddress());
                }

                nntpServer.handleConnection(clientSocket);
            } catch (IOException ie) {
                // FIXME what to do???
            }
        }
    }

}
