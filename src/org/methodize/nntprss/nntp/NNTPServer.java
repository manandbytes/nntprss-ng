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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.feed.db.ChannelManagerDAO;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.SimpleThreadPool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: NNTPServer.java,v 1.6 2003/09/28 20:23:26 jasonbrome Exp $
 */

public class NNTPServer implements Externalizable {

	public static final int EXTERNAL_VERSION = 1;  

	private Logger log = Logger.getLogger(NNTPServer.class);

	private NNTPServerListener listener = null;
	private SimpleThreadPool simpleThreadPool;
	private int listenerPort;
	private InetAddress address = null;

	private static final int MAX_NNTP_CLIENT_THREADS = 5;

// 30 minute default timeout on NNTP client connections
	private static final int DEFAULT_CONNECTION_TIMEOUT = 
		30 * 60 * 1000;

	private int contentType = AppConstants.CONTENT_TYPE_MIXED;
	private boolean secure = false;
	private boolean footnoteUrls = true;
	private String hostName = null;

	private ChannelDAO channelDAO;

	private Map users = new HashMap();

	public NNTPServer() {
		channelDAO = ChannelManagerDAO.getChannelManagerDAO().getChannelDAO();
	}

	public void configure(Document config) {
		// TODO configure Maximum concurrent threads etc
		Element rootElm = config.getDocumentElement();
		Element adminConfig =
			(Element) rootElm.getElementsByTagName("nntp").item(0);
		listenerPort = Integer.parseInt(adminConfig.getAttribute("port"));

		Node addressNode = adminConfig.getAttributeNode("address");
		if(addressNode != null) {
			try {
				address = InetAddress.getByName(addressNode.getNodeValue());
			} catch(UnknownHostException uhe) {
				if(log.isEnabledFor(Priority.ERROR)) {
					log.error("nntp listener bind address unknown - binding listener against all interfaces",
						uhe);
				}
			}
		}

		// Load DB persisted configuration
		channelDAO.loadConfiguration(this);

		if (log.isInfoEnabled()) {
			if(address == null) {
				log.info("NNTP server listener port = " + listenerPort);
			} else {
				log.info("NNTP server listener port = " + listenerPort
					+ ", address = " + address.toString());
			}
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
		simpleThreadPool =
			new SimpleThreadPool(
				"NNTP Client Handlers",
				"NNTP Client Thread",
				20);

		if (listener == null) {
			if(address == null) {
				listener = new NNTPServerListener(this, listenerPort);
			} else {
				listener = new NNTPServerListener(this, listenerPort,
					address);
			}
		}
		listener.start();
	}

	public void shutdown() {
		listener.shutdown();
	}

	void handleConnection(Socket clientConnection) {
		try {
			clientConnection.setSoTimeout(DEFAULT_CONNECTION_TIMEOUT);
			simpleThreadPool.run(new ClientHandler(this, clientConnection));
		} catch(SocketException e) {
			if(log.isEnabledFor(Priority.ERROR)) {
				log.error("SocketException invoking setSoTimeout",
					e);
			}
		}
	}

	public int getContentType() {
		return contentType;
	}

	public void setContentType(int contentType) {
		this.contentType = contentType;
	}

	public void saveConfiguration() {
		channelDAO.saveConfiguration(this);
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

	/**
	 * Returns the listenerPort.
	 * @return int
	 */
	public int getListenerPort() {
		return listenerPort;
	}

	/**
	 * Returns the footnoteUrls.
	 * @return boolean
	 */
	public boolean isFootnoteUrls() {
		return footnoteUrls;
	}

	/**
	 * Sets the footnoteUrls.
	 * @param footnoteUrls The footnoteUrls to set
	 */
	public void setFootnoteUrls(boolean footnoteUrls) {
		this.footnoteUrls = footnoteUrls;
	}

	/**
	 * @return
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * @param string
	 */
	public void setHostName(String string) {
		hostName = string;
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {

		in.readInt();
		contentType = in.readInt();
		secure = in.readBoolean();
		footnoteUrls = in.readBoolean();
		hostName = in.readUTF();
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(EXTERNAL_VERSION);
		out.writeInt(contentType);
		out.writeBoolean(secure);
		out.writeBoolean(footnoteUrls);
		out.writeUTF(hostName != null ? hostName : "");
	}

}
