package org.methodize.nntprss.feed;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.methodize.nntprss.feed.db.ChannelDAO;

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
 * @version $Id: Category.java,v 1.1 2003/10/24 02:33:18 jasonbrome Exp $
 */
public class Category extends ItemContainer implements Externalizable {

	public static final int EXTERNAL_VERSION = 1;  

	private int id;

	private Map channels = new HashMap();

	private ChannelManager channelManager;
	private ChannelDAO channelDAO;

	public Category() {
		initialize();
	}

	private void initialize() {
		channelManager = ChannelManager.getChannelManager();
		channelDAO = channelManager.getChannelDAO();
	}

	/**
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param i
	 */
	public void setId(int i) {
		id = i;
	}

	public void save() {
		// Update channel in database...
		channelDAO.updateCategory(this);
	}
	
	/* (non-Javadoc)
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
			in.readInt();
			id = in.readInt();
			name = in.readUTF();

			firstArticleNumber = in.readInt();
			lastArticleNumber = in.readInt();
			totalArticles = in.readInt();
			created = new Date(in.readLong());
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(EXTERNAL_VERSION);
		out.writeInt(id);
		out.writeUTF(name != null ? name : "");

		out.writeInt(firstArticleNumber);
		out.writeInt(lastArticleNumber);
		out.writeInt(totalArticles);		
		out.writeLong(created != null ? created.getTime() : 0);
	
	}

	/**
	 * @return
	 */
	public Map getChannels() {
		return channels;
	}

	/**
	 * @param map
	 */
	public void setChannels(Map map) {
		channels = map;
	}

	public synchronized int nextArticleNumber() {
		lastArticleNumber++;
		return lastArticleNumber;
	}

	public void removeChannel(Channel channel) {
		// Update channel in database...
		channelDAO.removeChannelFromCategory(channel, this);
		channels.remove(new Integer(channel.getId()));
	}
	
	public void addChannel(Channel channel) {
		// Update channel in database...
		channelDAO.addChannelToCategory(channel, this);
		channels.put(new Integer(channel.getId()), channel);
	}

}