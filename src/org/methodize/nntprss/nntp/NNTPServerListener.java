package org.methodize.nntprss.nntp;

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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;


/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: NNTPServerListener.java,v 1.4 2003/09/28 20:23:52 jasonbrome Exp $
 */
public class NNTPServerListener extends Thread {

	private Logger log = Logger.getLogger(NNTPServerListener.class);

	private ServerSocket serverSocket = null;
	private boolean active = true;
	private NNTPServer nntpServer = null;

	public NNTPServerListener(NNTPServer nntpServer, int port) throws Exception {
		serverSocket = new ServerSocket(port);
		this.nntpServer = nntpServer;
	}

	public NNTPServerListener(NNTPServer nntpServer, int port, InetAddress address) throws Exception {
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
