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

import java.util.Date;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Item.java,v 1.3 2003/01/27 22:41:42 jasonbrome Exp $
 */
public class Item {

	private int articleNumber;
	private String signature;
	private String title;
	private String description;
	private String link;
	private Date date;
	private String comments;
	private Channel channel;

	public Item(int articleNumber, String signature) {
		this.articleNumber = articleNumber;
		this.signature = signature;
	}
	/**
	 * Returns the articleNumber.
	 * @return int
	 */
	public int getArticleNumber() {
		return articleNumber;
	}

	/**
	 * Returns the signature.
	 * @return String
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Returns the description.
	 * @return String
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the link.
	 * @return String
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the description.
	 * @param description The description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Sets the link.
	 * @param link The link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the date.
	 * @return Date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Sets the date.
	 * @param date The date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Sets the articleNumber.
	 * @param articleNumber The articleNumber to set
	 */
	public void setArticleNumber(int articleNumber) {
		this.articleNumber = articleNumber;
	}

	/**
	 * Sets the signature.
	 * @param signature The signature to set
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * Returns the channel.
	 * @return Channel
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Sets the channel.
	 * @param channel The channel to set
	 */
	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	/**
	 * Returns the comments.
	 * @return String
	 */
	public String getComments() {
		return comments;
	}

	/**
	 * Sets the comments.
	 * @param comments The comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

}
