package org.methodize.nntprss.nntp;

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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;


/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */
public class NNTPServerListener extends Thread {

	private Logger log = Logger.getLogger(NNTPServerListener.class);

	private ServerSocket serverSocket = null;
	private static final int NNTP_PORT = 119;
	private boolean active = true;
	private NNTPServer nntpServer = null;

	public NNTPServerListener(NNTPServer nntpServer) throws Exception {
		serverSocket = new ServerSocket(NNTP_PORT);
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
						"NNTP Client connection");
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
