package org.methodize.nntprss.feed.parser;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2005 Jason Brome.  All Rights Reserved.
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
import java.security.NoSuchAlgorithmException;

import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.plugin.ItemProcessor;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: GenericParser.java,v 1.6 2005/02/13 22:00:53 jasonbrome Exp $
 */
public abstract class GenericParser {

	protected ItemProcessor[] itemProcessors;

    public abstract boolean isParsable(Element docRootElement);
    public abstract String getFormatVersion(Element docRootElement);
    public abstract void extractFeedInfo(
        Element docRootElement,
        Channel channel);
    public abstract void processFeedItems(
        Element rootElm,
        Channel channel,
        ChannelDAO channelDAO,
        boolean keepHistory)
        throws NoSuchAlgorithmException, IOException;

	protected GenericParser() {
		itemProcessors = ChannelManager.getChannelManager().getItemProcessors();
	}

    String stripControlChars(String string) {
        StringBuffer strippedString = new StringBuffer();
        for (int charCount = 0; charCount < string.length(); charCount++) {
            char c = string.charAt(charCount);
            if (c >= 32) {
                strippedString.append(c);
            }
        }
        return strippedString.toString();
    }
    
    protected void invokeProcessors(Item item)
    {
		if(this.itemProcessors != null) {
			for(int i = 0; i < itemProcessors.length; i++) {
				itemProcessors[i].onItem(item);
			}
		}
    }
}