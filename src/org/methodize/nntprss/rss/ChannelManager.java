package org.methodize.nntprss.rss;

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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.methodize.nntprss.rss.db.ChannelManagerDAO;
import org.w3c.dom.Document;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelManager.java,v 1.4 2003/03/22 16:30:23 jasonbrome Exp $
 */
public class ChannelManager {

	private long pollingIntervalSeconds = 60 * 60;
	
	private String proxyServer = null;
	private int proxyPort = 0;
    private String proxyUserID = null;
    private transient String proxyPassword = null;
   

	private Map channels;
	private static ChannelManager channelManager = new ChannelManager();
	private ChannelManagerDAO channelManagerDAO;
	private ChannelPoller channelPoller;

	private ChannelManager() {
		// Private constructor - singleton class
		channelManagerDAO = ChannelManagerDAO.getChannelManagerDAO();
	}

	public static ChannelManager getChannelManager() {
		return channelManager;
	}

	/**
	 * RSS Manager configuration
	 * 
	 * - Monitored RSS feeds
	 *   feed url, and matching newsgroup name
	 * 
	 * Once feeds are loaded, load existing persistent 
	 * store information about feeds
	 * 
	 */

	public void configure(Document config) {

		channelManagerDAO.loadConfiguration(this);

// Set proxy configuration, if necessary.
        if ((proxyServer != null) && (proxyServer.length() > 0)) {
			System.setProperty("http.proxyHost", proxyServer);
			System.setProperty("http.proxyPort", Integer.toString(proxyPort));
			System.setProperty("http.proxySet", "true");

            if ((proxyUserID != null && proxyUserID.length() > 0) ||
            	(proxyPassword != null && proxyPassword.length() > 0)) {
                Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyUserID,
                                (proxyPassword == null) ? new char[0]
                                                        : proxyPassword.toCharArray());
                        }
                    });
            } else {
            	Authenticator.setDefault(null);
            }
        } else {
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
            System.setProperty("http.proxySet", "false");
            Authenticator.setDefault(null);
		}

		// Load feeds...
		channels = channelManagerDAO.loadChannels();

//		// Start feed poller...
//		startPoller();

	}

	public void addChannel(Channel channel) {
		channel.setCreated(new Date());
		channelManagerDAO.addChannel(channel);
		channels.put(channel.getName(), channel);
	}

	public void deleteChannel(Channel channel) {
		channels.remove(channel.getName());
		channelManagerDAO.deleteChannel(channel);
	}


	public void start() {
		startPoller();
	}

	public void shutdown() {
		stopPoller();
	}

	public Iterator channels() {
		return channels.values().iterator();
	}

	public Channel channelByName(String name) {
		return (Channel) channels.get(name);
	}

	private void startPoller() {
		channelPoller = new ChannelPoller(channels);
		channelPoller.start();
	}

	private void stopPoller() {
		channelPoller.shutdown();
	}


	
	public void saveConfiguration() {
		channelManagerDAO.saveConfiguration(this);

// Update proxy configuration, if necessary.
		if(proxyServer != null && proxyServer.length() > 0) {
			System.setProperty("http.proxyHost", proxyServer);
			System.setProperty("http.proxyPort", Integer.toString(proxyPort));
			System.setProperty("http.proxySet", "true");

            if ((proxyUserID != null && proxyUserID.length() > 0) ||
            	(proxyPassword != null && proxyPassword.length() > 0)) {
                Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyUserID,
                                (proxyPassword == null) ? new char[0]
                                                        : proxyPassword.toCharArray());
                        }
                    });
            } else {
            	Authenticator.setDefault(null);
            }
            
		} else {
			Properties props = System.getProperties();
			System.setProperty("http.proxyHost", "");
			System.setProperty("http.proxyPort", "");
			System.setProperty("http.proxySet", "false");
            Authenticator.setDefault(null);
  		}

	}

	/**
	 * Returns the channelManagerDAO.
	 * @return ChannelManagerDAO
	 */
	public ChannelManagerDAO getChannelManagerDAO() {
		return channelManagerDAO;
	}

	/**
	 * Returns the pollingIntervalSeconds.
	 * @return long
	 */
	public long getPollingIntervalSeconds() {
		return pollingIntervalSeconds;
	}

	/**
	 * Sets the pollingIntervalSeconds.
	 * @param pollingIntervalSeconds The pollingIntervalSeconds to set
	 */
	public void setPollingIntervalSeconds(long pollingIntervalSeconds) {
		this.pollingIntervalSeconds = pollingIntervalSeconds;
	}

	/**
	 * Returns the proxyPort.
	 * @return int
	 */
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * Returns the proxyServer.
	 * @return String
	 */
	public String getProxyServer() {
		return proxyServer;
	}

	/**
	 * Sets the proxyPort.
	 * @param proxyPort The proxyPort to set
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * Sets the proxyServer.
	 * @param proxyServer The proxyServer to set
	 */
	public void setProxyServer(String proxyServer) {
		this.proxyServer = proxyServer;
	}

	/**
	 * Returns the proxyPassword.
	 * @return String
	 */
	public String getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * Returns the proxyUserID.
	 * @return String
	 */
	public String getProxyUserID() {
		return proxyUserID;
	}

	/**
	 * Sets the proxyPassword.
	 * @param proxyPassword The proxyPassword to set
	 */
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	/**
	 * Sets the proxyUserID.
	 * @param proxyUserID The proxyUserID to set
	 */
	public void setProxyUserID(String proxyUserID) {
		this.proxyUserID = proxyUserID;
	}

}
