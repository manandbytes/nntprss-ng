package org.methodize.nntprss.feed.parser;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2007 Jason Brome.  All Rights Reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.plugin.ItemProcessor;
import org.methodize.nntprss.util.Base64;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: GenericParser.java,v 1.9 2007/12/17 04:12:59 jasonbrome Exp $
 */
public abstract class GenericParser {

	protected final ItemProcessor[] itemProcessors;

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

    static String stripControlChars(String string) {
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
    public abstract String processContent(Element itemElm);
    protected String generateItemSignature(MessageDigest md, Element itemElm)
            throws IOException {
                String title = XMLHelper.getChildElementValue(itemElm, "title", "");
                String link = XMLHelper.getChildElementValue(itemElm, "link", "");
            
                // Handle xhtml:body / content:encoded / description
                String description = processContent(itemElm);
            
                String signatureStr = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
            
                // Used trimmed forms of content, ignore whitespace changes
                bos.write(title.trim().getBytes());
                bos.write(link.trim().getBytes());
                bos.write(description.trim().getBytes());
                bos.flush();
                bos.close();
            
                byte[] signatureSource = bos.toByteArray();
                md.reset();
                byte[] signature = md.digest(signatureSource);
            
                signatureStr = Base64.encodeBytes(signature);
                return signatureStr;
            }
}