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
import java.io.InputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.methodize.nntprss.rss.db.ChannelManagerDAO;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.SimpleThreadPool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: NNTPServer.java,v 1.3 2003/03/22 16:28:02 jasonbrome Exp $
 */

public class NNTPServer {

	private Logger log = Logger.getLogger(NNTPServer.class);

	private NNTPServerListener listener = null;
	private SimpleThreadPool simpleThreadPool;
	private int listenerPort;

	private static final int MAX_NNTP_CLIENT_THREADS = 5;

	private int contentType = AppConstants.CONTENT_TYPE_MIXED;
	private boolean secure = false;

	private ChannelManagerDAO channelManagerDAO;

	private Map users = new HashMap();

	public NNTPServer() throws Exception {
		simpleThreadPool =
			new SimpleThreadPool(
				"NNTP Client Handlers",
				"NNTP Client Thread",
				20);
		channelManagerDAO = ChannelManagerDAO.getChannelManagerDAO();
	}

	public void configure(Document config) {
		// TODO configure Maximum concurrent threads etc
		Element rootElm = config.getDocumentElement();
		Element adminConfig =
			(Element) rootElm.getElementsByTagName("nntp").item(0);
		listenerPort = Integer.parseInt(adminConfig.getAttribute("port"));

		// Load DB persisted configuration
		channelManagerDAO.loadConfiguration(this);

		if (log.isInfoEnabled()) {
			log.info("NNTP server listener port = " + listenerPort);
		}

		InputStream userConfig =
			this.getClass().getResourceAsStream(
				"/" + AppConstants.USERS_CONFIG);
		if (userConfig != null) {
			// Load users...
			try {
				Properties props = new Properties();
				props.load(userConfig);
				Enumeration enum = props.propertyNames();
				while (enum.hasMoreElements()) {
					String user = (String) enum.nextElement();
					users.put(user, props.getProperty(user));
				}

				if (log.isInfoEnabled()) {
					log.info("Loaded NNTP user configuration");
				}

			} catch (IOException ie) {
				log.error("Error loading users", ie);
			}
		}

	}

	public void start() throws Exception {
		if (listener == null) {
			listener = new NNTPServerListener(this, listenerPort);
		}
		listener.start();
	}

	public void shutdown() {
		listener.shutdown();
	}

	void handleConnection(Socket clientConnection) {
		simpleThreadPool.run(new ClientHandler(this, clientConnection));
	}

	public int getContentType() {
		return contentType;
	}

	public void setContentType(int contentType) {
		this.contentType = contentType;
	}

	public void saveConfiguration() {
		channelManagerDAO.saveConfiguration(this);
	}

	/**
	 * Returns secure.
	 * @return boolean
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * Sets secure.
	 * @param secure The secure to set
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isValidUser(String user, String password) {
		boolean valid = false;

		String actualPassword = (String) users.get(user);
		if (actualPassword != null && actualPassword.equals(password)) {
			valid = true;
		}

		return valid;
	}

}
