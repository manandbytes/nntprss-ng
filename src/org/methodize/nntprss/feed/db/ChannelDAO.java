package org.methodize.nntprss.feed.db;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2004 Jason Brome.  All Rights Reserved.
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
import org.methodize.nntprss.feed.Category;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.nntp.NNTPServer;
import org.w3c.dom.Document;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelDAO.java,v 1.5 2004/01/04 21:14:23 jasonbrome Exp $
 */
public abstract class ChannelDAO {

	public static final int LIMIT_NONE = -1;

	static final int DBVERSION = 5;

	Logger log = Logger.getLogger(ChannelDAO.class);

	public abstract void shutdown();

	protected abstract void upgradeDatabase(int dbVersion);

	protected abstract void createTables();
	protected abstract void populateInitialChannels(Document config);

	public abstract void initialize(Document config) throws Exception;

	public abstract void loadConfiguration(ChannelManager channelManager);

	public abstract void loadConfiguration(NNTPServer nntpServer);

	public abstract Map loadChannels(ChannelManager channelManager);

	public abstract Map loadCategories();

	public abstract void addChannel(Channel channel);
	public abstract void addCategory(Category category);

	public abstract void addChannelToCategory(
		Channel channel,
		Category category);
	public abstract void removeChannelFromCategory(
		Channel channel,
		Category category);

	public abstract void updateChannel(Channel channel);

	public abstract void updateCategory(Category category);

	public abstract void deleteChannel(Channel channel);

	public abstract void deleteCategory(Category category);

	public abstract Item loadItem(Category category, int articleNumber);

	public abstract Item loadItem(Channel channel, int articleNumber);

	public abstract Item loadNextItem(
		Category category,
		int relativeArticleNumber);

	public abstract Item loadNextItem(
		Channel channel,
		int relativeArticleNumber);

	public abstract Item loadPreviousItem(
		Category category,
		int relativeArticleNumber);

	public abstract Item loadPreviousItem(
		Channel channel,
		int relativeArticleNumber);

	public abstract Item loadItem(Channel channel, String signature);

	/**
	 * Method loadItems.
	 * @param channel
	 * @param articleRange
	 * @param onlyHeaders
	 * @param limit Maximum number of items to return
	 * @return List
	 * 
	 * articleRange
	 * -1 = open ended search (all items from article number,
	 *    all items to article number)
	 */

	public abstract List loadItems(
		Category category,
		int[] articleRange,
		boolean onlyHeaders,
		int limit);

	public abstract List loadItems(
		Channel channel,
		int[] articleRange,
		boolean onlyHeaders,
		int limit);

	public abstract List loadArticleNumbers(Category category);

	/**
	 * Method loadArticleNumbers
	 * @param channel
	 * @return List
	 * 
	 * Supports NNTP listgroup command
	 */
	public abstract List loadArticleNumbers(Channel channel);

	public abstract void saveItem(Item item);

	public abstract void saveConfiguration(ChannelManager channelManager);

	public abstract void saveConfiguration(NNTPServer nntpServer);

	public abstract void deleteExpiredItems(
		Channel channel,
		Set currentItemSignatures);

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteItemsNotInSet(org.methodize.nntprss.feed.Channel, java.util.Set)
	 */
	public abstract void deleteItemsNotInSet(
		Channel channel,
		Set itemSignatures);

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#findNewItemSignatures(int, java.util.Set)
	 */
	public abstract Set findNewItemSignatures(
		Channel channel,
		Set itemSignatures);

}