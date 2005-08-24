package org.methodize.nntprss.feed;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2005 Jason Brome.  All Rights Reserved.
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.log4j.Logger;
import org.methodize.nntprss.feed.db.ChannelManagerDAO;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.plugin.ItemProcessor;
import org.methodize.nntprss.plugin.PluginException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelManager.java,v 1.10 2005/08/24 23:12:10 jasonbrome Exp $
 */
public class ChannelManager implements Externalizable {

    public static final int EXTERNAL_VERSION = 1;
	private Logger log = Logger.getLogger(ChannelManager.class);

    private long pollingIntervalSeconds = 60 * 60;

    private String proxyServer = null;
    private int proxyPort = 0;
    private String proxyUserID = null;
    private String proxyPassword = null;
    private boolean useProxy = false;
    private boolean observeHttp301 = false;
	private int pollerThreads = 4;

    private Map channels;
    private Map categories;
    private static ChannelManager channelManager = new ChannelManager();
    private ChannelDAO channelDAO;
    private ChannelPoller channelPoller;

    private HostConfiguration hostConfig = null;
    //	private HttpState httpState = null;
    private MultiThreadedHttpConnectionManager httpConMgr;

	private ItemProcessor[] itemProcessors = null;

    private ChannelManager() {
        // Private constructor - singleton class
        channelDAO = ChannelManagerDAO.getChannelManagerDAO().getChannelDAO();
        hostConfig = new HostConfiguration();
        httpConMgr = new MultiThreadedHttpConnectionManager();
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

        channelDAO.loadConfiguration(this);

        updateProxyConfig();

        //		// Start feed poller...
        //		startPoller();

		// Get poller Configuration
		Element rootElm = config.getDocumentElement();
		NodeList cfgElms = rootElm.getElementsByTagName("poller");
		if(cfgElms != null && cfgElms.getLength() > 0)
		{
			Element pollerElm = (Element)cfgElms.item(0);
			String threadsStr = pollerElm.getAttribute("threads");
			if(threadsStr != null) {
				pollerThreads = Integer.parseInt(threadsStr);
			}
		}

		NodeList processorsElms = rootElm.getElementsByTagName("itemProcessors");
		if(processorsElms != null && processorsElms.getLength() > 0) {
			NodeList processorElms = ((Element)processorsElms.item(0)).getElementsByTagName("processor");
			if(processorElms != null && processorElms.getLength() > 0) {
				List processors = new ArrayList();
				for(int i = 0; i < processorElms.getLength(); i++) {
					Element processorElm = (Element)processorElms.item(i);

					String itemProcessorClassName = processorElm.getAttribute("class");
					if(itemProcessorClassName != null){
						try {
							Object itemProcessorObject = Class.forName(itemProcessorClassName).newInstance();
							if(!(itemProcessorObject instanceof ItemProcessor))
							{
								log.warn(itemProcessorClassName + " not instance of org.methodize.nntprss.plugin.ItemProcessor, skipping");
							}
							else
							{
								try {
									ItemProcessor itemProcessor = (ItemProcessor)itemProcessorObject;
									itemProcessor.initialize(processorElm);
									processors.add(itemProcessor);
								} catch(PluginException pe) {
									log.warn("Error initializing ItemProcessor plug-in: " + pe.getMessage() + ", skipping.", pe);
								}
							}
						} catch(ClassNotFoundException cnfe) {
							log.warn("Cannot find ItemProcessor class " + itemProcessorClassName, cnfe);
						} catch(IllegalAccessException iae) {
							log.warn("Error instantiating ItemProcessor class: " + iae.getMessage(), iae);
						} catch(InstantiationException ie) {
							log.warn("Error instantiating ItemProcessor class: " + ie.getMessage(), ie);
						}
					}
					
				}
				if(processors.size() > 0)
				{
					itemProcessors = (ItemProcessor[])processors.toArray(new ItemProcessor[0]);
				}
			}
		}

		// Load feeds...
		categories = channelDAO.loadCategories();
		channels = channelDAO.loadChannels(this);
    }

    public void addChannel(Channel channel) {
        channel.setCreated(new Date());
        channelDAO.addChannel(channel);
        channels.put(channel.getName(), channel);
    }

    public void addCategory(Category category) {
        category.setCreated(new Date());
        channelDAO.addCategory(category);
        categories.put(category.getName(), category);
    }

    public void deleteChannel(Channel channel) {
        channels.remove(channel.getName());

        if (channel.getCategory() != null) {
            Category category = channel.getCategory();
            category.removeChannel(channel);
        }
        channelDAO.deleteChannel(channel);
    }

    public void deleteCategory(Category category) {
        categories.remove(category.getName());
        channelDAO.deleteCategory(category);
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

    public Iterator categories() {
        return categories.values().iterator();
    }

    public Iterator groups() {
        Set groups = new HashSet();
        groups.addAll(channels.values());
        groups.addAll(categories.values());
        return groups.iterator();
    }

    public Channel channelByName(String name) {
        return (Channel) channels.get(name);
    }

    public Category categoryByName(String name) {
        return (Category) categories.get(name);
    }

    public ItemContainer groupByName(String name) {
        ItemContainer group = (ItemContainer) channels.get(name);
        if (group == null) {
            group = (ItemContainer) categories.get(name);
        }
        return group;
    }

    public Category categoryById(int id) {
        Category category = null;
        if(categories != null) // Null check for jdbm migrate
        {
	        Iterator categoryIter = categories.values().iterator();
	        while (categoryIter.hasNext()) {
	            Category nextCategory = (Category) categoryIter.next();
	            if (nextCategory.getId() == id) {
	                category = nextCategory;
	                break;
	            }
	        }
		}
        return category;
    }

    private void startPoller() {
        channelPoller = new ChannelPoller(channels, pollerThreads);
        channelPoller.start();
    }

    private void stopPoller() {
        channelPoller.shutdown();
    }

    public synchronized void repollAllChannels() {
        try {
            Iterator channelIter = channels.values().iterator();

            while (channelIter.hasNext()) {
                Channel channel = (Channel) channelIter.next();
                channel.setLastPolled(null);
                channel.setStatus(Channel.STATUS_OK);
            }

        } catch (ConcurrentModificationException cme) {
            // Just in case something else is modifying the channel structure...
        }
    }

    public void configureHttpClient(HttpClient client) {
        client.setHostConfiguration(hostConfig);

        if ((proxyUserID != null && proxyUserID.length() > 0)
            || (proxyPassword != null && proxyPassword.length() > 0)) {
            client.getState().setProxyCredentials(
                null,
                null,
                new UsernamePasswordCredentials(
                    proxyUserID,
                    (proxyPassword == null) ? "" : proxyPassword));
        } else {
            client.getState().setProxyCredentials(null, null, null);
        }
    }

    private void updateProxyConfig() {
        // Set proxy configuration, if necessary.
        if (useProxy && (proxyServer != null) && (proxyServer.length() > 0)) {
            //			System.setProperty("http.proxyHost", proxyServer);
            //			System.setProperty("http.proxyPort", Integer.toString(proxyPort));
            //			System.setProperty("http.proxySet", "true");

            // Set HttpClient proxy configuration
            hostConfig.setProxy(proxyServer, proxyPort);

            //            if ((proxyUserID != null && proxyUserID.length() > 0) ||
            //            	(proxyPassword != null && proxyPassword.length() > 0)) {
            //                Authenticator.setDefault(new Authenticator() {
            //                        protected PasswordAuthentication getPasswordAuthentication() {
            //                            return new PasswordAuthentication(proxyUserID,
            //                                (proxyPassword == null) ? new char[0]
            //                                                        : proxyPassword.toCharArray());
            //                        }
            //                    });
            //
            //				httpClient.getState().setProxyCredentials(null,
            //					new UsernamePasswordCredentials(proxyUserID,
            //                                (proxyPassword == null) ? ""
            //                                                        : proxyPassword));
            //
            //            } else {
            //            	Authenticator.setDefault(null);
            //            	
            //            	httpClient.getState().setProxyCredentials(null, null);
            //            }
        } else {
            //            System.setProperty("http.proxyHost", "");
            //            System.setProperty("http.proxyPort", "");
            //            System.setProperty("http.proxySet", "false");
            //            Authenticator.setDefault(null);

            hostConfig = new HostConfiguration();
            //           	httpClient.getState().setProxyCredentials(null, null);

        }
    }

    public void saveConfiguration() {
        channelDAO.saveConfiguration(this);

        // Update proxy configuration, if necessary.
        updateProxyConfig();

    }

    /**
     * Returns the channelDAO.
     * @return ChannelManagerDAO
     */
    public ChannelDAO getChannelDAO() {
        return channelDAO;
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

    /**
     * @return
     */
    public MultiThreadedHttpConnectionManager getHttpConMgr() {
        return httpConMgr;
    }

    /**
     * @return
     */
    public boolean isUseProxy() {
        return useProxy;
    }

    /**
     * @param b
     */
    public void setUseProxy(boolean b) {
        useProxy = b;
    }

    /**
     * @return
     */
    public boolean isObserveHttp301() {
        return observeHttp301;
    }

    /**
     * @param b
     */
    public void setObserveHttp301(boolean b) {
        observeHttp301 = b;
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        in.readInt();
        pollingIntervalSeconds = in.readLong();
        proxyServer = in.readUTF();
        proxyPort = in.readInt();
        proxyUserID = in.readUTF();
        proxyPassword = in.readUTF();
        useProxy = in.readBoolean();
        observeHttp301 = in.readBoolean();
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(EXTERNAL_VERSION);
        out.writeLong(pollingIntervalSeconds);
        out.writeUTF(proxyServer != null ? proxyServer : "");
        out.writeInt(proxyPort);
        out.writeUTF(proxyUserID != null ? proxyUserID : "");
        out.writeUTF(proxyPassword != null ? proxyPassword : "");
        out.writeBoolean(useProxy);
        out.writeBoolean(observeHttp301);
    }

    /**
     * @return
     */
    public ItemProcessor[] getItemProcessors() {
        return itemProcessors;
    }

}
