package org.methodize.nntprss.admin;

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

import org.methodize.nntprss.rss.ChannelManager;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */
public class AdminServer {

	private HttpServer httpServer;
	private ChannelManager channelManager;
	private int port;
	public static final String SERVLET_CTX_RSS_MANAGER = "rss.manager";

	public AdminServer(ChannelManager channelManager) {
		this.channelManager = channelManager;
	}

	public void configure(Document config) throws Exception {

		Element rootElm = config.getDocumentElement();
		Element adminConfig = (Element)rootElm.getElementsByTagName("admin").item(0);
		port = Integer.parseInt(adminConfig.getAttribute("port"));

		httpServer = new HttpServer();
		HttpContext context = httpServer.getContext("/");
		WebApplicationHandler handler = new WebApplicationHandler();
		handler.addServlet("/", AdminServlet.class.getName());
		context.addHandler(handler);
		context.setAttribute(SERVLET_CTX_RSS_MANAGER, channelManager);

		httpServer.addContext(context);

		HttpListener httpListener = new SocketListener();
		httpListener.setPort(port);
		httpServer.addListener(httpListener);

	}

	public void start() throws Exception {
		httpServer.start();
	}

	public void shutdown() {
		try {
			httpServer.stop();
		} catch (InterruptedException ie) {
			// FIXME log
		}
	}
}
