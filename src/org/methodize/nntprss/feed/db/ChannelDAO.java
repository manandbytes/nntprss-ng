package org.methodize.nntprss.feed.db;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.w3c.dom.Document;

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

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelDAO.java,v 1.1 2003/07/18 23:58:05 jasonbrome Exp $
 */
public interface ChannelDAO {

	public void initialize(Document config);
	
	public void loadConfiguration(ChannelManager channelManager);
	
	public void loadConfiguration(NNTPServer nntpServer);
	
	public Map loadChannels();
	
	public void addChannel(Channel channel);
	
	public void updateChannel(Channel channel);
	
	public void deleteItemsNotInSet(Channel channel, Set itemSignatures);
	
	public void deleteChannel(Channel channel);
	
	public Item loadItem(Channel channel, int articleNumber);
	
	public Item loadNextItem(Channel channel, int relativeArticleNumber);
	
	public Item loadPreviousItem(Channel channel, int relativeArticleNumber);
	
	public Item loadItem(Channel channel, String signature);
	
	public List loadItems(
		Channel channel,
		int[] articleRange,
		boolean onlyHeaders);
		
	public List loadArticleNumbers(Channel channel);
	
	public void saveItem(Item item);
	
	public Set findNewItemSignatures(int channelId, Set itemSignatures);
	
	public void saveConfiguration(ChannelManager rssManager);
	
	public void saveConfiguration(NNTPServer nntpServer);

}