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

import java.net.Socket;

import org.methodize.nntprss.util.SimpleThreadPool;
import org.w3c.dom.Document;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */

public class NNTPServer {

	private NNTPServerListener listener = null;
	private SimpleThreadPool simpleThreadPool;

	private static final int MAX_NNTP_CLIENT_THREADS = 20;

	public NNTPServer() throws Exception {
		simpleThreadPool =
			new SimpleThreadPool("NNTP Client Handlers", "NNTP Client Thread", 20);
		listener = new NNTPServerListener(this);
	}

	public void configure(Document config) {
		// TODO configure NNTP port...
		// TODO configure Maximum concurrent threads etc

	}

	public void start() {
		listener.start();
	}

	public void shutdown() {
		listener.shutdown();
	}

	void handleConnection(Socket clientConnection) {
		simpleThreadPool.run(new ClientHandler(clientConnection));
	}

}
