package org.methodize.nntprss.admin;

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

import java.io.InputStream;
import java.util.Properties;

import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.rss.ChannelManager;
import org.methodize.nntprss.util.AppConstants;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AdminServer.java,v 1.3 2003/03/22 16:26:48 jasonbrome Exp $
 */
public class AdminServer {

	private HttpServer httpServer;
	private ChannelManager channelManager;
	private NNTPServer nntpServer;
	private int port;
	public static final String SERVLET_CTX_RSS_MANAGER = "rss.manager";
	public static final String SERVLET_CTX_NNTP_SERVER = "nntp.server";

	public static final String REALM_NAME = "nntprss-realm";


	public AdminServer(ChannelManager channelManager, NNTPServer nntpServer) {
		this.channelManager = channelManager;
		this.nntpServer = nntpServer;
	}

	public void configure(Document config) throws Exception {

		Element rootElm = config.getDocumentElement();
		Element adminConfig = (Element)rootElm.getElementsByTagName("admin").item(0);
		port = Integer.parseInt(adminConfig.getAttribute("port"));

		httpServer = new HttpServer();

// Check for user realm properties file
// If it exists, use security.
		InputStream userRealmConfig = this.getClass().getResourceAsStream("/" + AppConstants.USERS_CONFIG);
		boolean useSecurity = false;
		if(userRealmConfig != null) {
			useSecurity = true;
			HashUserRealm userRealm = new HashUserRealm(REALM_NAME);
			userRealm.load(AppConstants.USERS_CONFIG);
			httpServer.addRealm(userRealm);
		}

		
		HttpContext context = httpServer.getContext("/");
		WebApplicationHandler handler = new WebApplicationHandler();

		if(useSecurity) {
			context.setRealmName(REALM_NAME);
			context.setAuthenticator(
				new BasicAuthenticator());
			context.addHandler(new SecurityHandler());
			context.addSecurityConstraint("/",
				new SecurityConstraint("Admin",
					"*"));
		}
		
		context.setAttribute(SERVLET_CTX_RSS_MANAGER, channelManager);
		context.setAttribute(SERVLET_CTX_NNTP_SERVER, nntpServer);

		handler.addServlet("/", AdminServlet.class.getName());
		context.addHandler(handler);


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
