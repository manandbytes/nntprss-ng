package org.methodize.nntprss.feed.db;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelManagerDAO.java,v 1.1 2003/07/18 23:58:05 jasonbrome Exp $
 */
public class ChannelManagerDAO implements ChannelDAO {

//	private static final int DBVERSION = 5;

	private Logger log = Logger.getLogger(ChannelManagerDAO.class);

	private static final ChannelManagerDAO channelManagerDAO = new ChannelManagerDAO();

// The actual DB specific channel DAO logic instance
	private ChannelDAO channelDAO = null;

	private ChannelManagerDAO() {
	}

	public static ChannelManagerDAO getChannelManagerDAO() {
		return channelManagerDAO;
	}

	public void initialize(Document config) {
		Element rootElm = config.getDocumentElement();
		Element dbConfig = (Element)rootElm.getElementsByTagName("db").item(0);
		String daoClass = dbConfig.getAttribute("daoClass");
		
		if(daoClass != null && daoClass.length() > 0) {
			try {
				channelDAO = (ChannelDAO)Class.forName(daoClass).newInstance();
			} catch(Exception e) {
				throw new RuntimeException("Problem initializing database class "
					+ daoClass 
					+ ", exception="
					+ e);
			}
		} else {
// Default to HSQLDB
			channelDAO = new HSqlDbChannelDAO();
		}

		channelDAO.initialize(config);
	}

	public void loadConfiguration(ChannelManager channelManager) {
		channelDAO.loadConfiguration(channelManager);
	}

	public void loadConfiguration(NNTPServer nntpServer) {
		channelDAO.loadConfiguration(nntpServer);
	}

	public Map loadChannels() {
		return channelDAO.loadChannels();
	}

	public void addChannel(Channel channel) {
		channelDAO.addChannel(channel);
	}

	public void updateChannel(Channel channel) {
		channelDAO.updateChannel(channel);
	}


	public void deleteChannel(Channel channel) {
		channelDAO.deleteChannel(channel);
	}

	public Item loadItem(Channel channel, int articleNumber) {
		return channelDAO.loadItem(channel, articleNumber);
	}


	public Item loadNextItem(Channel channel, int relativeArticleNumber) {
		return channelDAO.loadNextItem(channel, relativeArticleNumber);
	}
	
	public Item loadPreviousItem(Channel channel, int relativeArticleNumber) {
		return channelDAO.loadPreviousItem(channel, relativeArticleNumber);
	}

	public Item loadItem(Channel channel, String signature) {
		return channelDAO.loadItem(channel, signature);
	}

	public List loadItems(
		Channel channel,
		int[] articleRange,
		boolean onlyHeaders) {

		return channelDAO.loadItems(channel, articleRange, onlyHeaders);
	}


	public List loadArticleNumbers(
		Channel channel) {
		return channelDAO.loadArticleNumbers(channel);
	}


	public void saveItem(Item item) {
		channelDAO.saveItem(item);
	}

	public void saveConfiguration(ChannelManager channelManager) {
		channelDAO.saveConfiguration(channelManager);
	}


	public void saveConfiguration(NNTPServer nntpServer) {
		channelDAO.saveConfiguration(nntpServer);
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteItemsNotInSet(org.methodize.nntprss.feed.Channel, java.util.Set)
	 */
	public void deleteItemsNotInSet(Channel channel, Set itemSignatures) {
		channelDAO.deleteItemsNotInSet(channel, itemSignatures);
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#findNewItemSignatures(int, java.util.Set)
	 */
	public Set findNewItemSignatures(int channelId, Set itemSignatures) {
		return channelDAO.findNewItemSignatures(channelId, itemSignatures);
	}

}
