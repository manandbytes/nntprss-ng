package org.methodize.nntprss.feed.parser;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.mail.internet.MailDateFormat;

import org.apache.log4j.Logger;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.util.Base64;
import org.methodize.nntprss.util.RSSHelper;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: RSSParser.java,v 1.8 2004/12/15 04:12:08 jasonbrome Exp $
 */

public class RSSParser extends GenericParser {

    private static ThreadLocal dcDates = new ThreadLocal() {
        public Object initialValue() {
            SimpleDateFormat[] dcDateArray =
                new SimpleDateFormat[] {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ssz"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'"),
                    new SimpleDateFormat("yyyy-MM-dd")};

            TimeZone gmt = TimeZone.getTimeZone("GMT");
            for (int tz = 0; tz < dcDateArray.length; tz++) {
                dcDateArray[tz].setTimeZone(gmt);
            }

            return dcDateArray;
        }
    };

    private static ThreadLocal date822Parser = new ThreadLocal() {
        public Object initialValue() {
            return new MailDateFormat();
        }
    };

    private static RSSParser rssParser = new RSSParser();
    private Logger log = Logger.getLogger(RSSParser.class);

    private RSSParser() {
    }

    public static GenericParser getParser() {
        return rssParser;
    }

    /**
     * @param docRootElement Root element of feed document
     * @return
     */
    public boolean isParsable(Element docRootElement) {
        if (docRootElement.getNodeName().equals("rss")
            || docRootElement.getNodeName().equals("rdf:RDF")) {
            return true;
        } else {
            return false;
        }
    }

    public String getFormatVersion(Element docRootElement) {
        String rssVersion;

        if (docRootElement.getNodeName().equals("rss")) {
            rssVersion = "RSS " + docRootElement.getAttribute("version");
        } else if (docRootElement.getNodeName().equals("rdf:RDF")) {
            rssVersion = "RDF";
        } else {
            rssVersion = "Unknown RSS";
        }
        return rssVersion;
    }

    public void extractFeedInfo(Element docRootElement, Channel channel) {
        Element channelElm =
            (Element) docRootElement.getElementsByTagName("channel").item(0);

        // Read header...
        channel.setTitle(XMLHelper.getChildElementValue(channelElm, "title"));
        // XXX Currently assign channelTitle to author
        channel.setAuthor(channel.getTitle());
        channel.setLink(XMLHelper.getChildElementValue(channelElm, "link"));
        channel.setDescription(
            XMLHelper.getChildElementValue(channelElm, "description"));
        channel.setManagingEditor(
            XMLHelper.getChildElementValue(channelElm, "managingEditor"));
    }

    public void processFeedItems(
        Element rootElm,
        Channel channel,
        ChannelDAO channelDAO,
        boolean keepHistory)
        throws NoSuchAlgorithmException, IOException {
        Element rssDocElm =
            (Element) rootElm.getElementsByTagName("channel").item(0);
        // Check for items within channel element and outside 
        // channel element
        NodeList itemList = rssDocElm.getElementsByTagName("item");

        if (itemList.getLength() == 0) {
            itemList = rootElm.getElementsByTagName("item");
        }

        Calendar retrievalDate = Calendar.getInstance();
        retrievalDate.add(Calendar.SECOND, -itemList.getLength());

        Set currentSignatures = null;
        if (!keepHistory) {
            currentSignatures = new HashSet();
        }

        Map newItems = new HashMap();
        Map newItemKeys = new HashMap();
        // orderedItems maintains original document first-to-last order
        // Assumption: Items in the RSS document go from most recent
        // to earliest.  This is used to assign date/times in a reasonably
        // sensible order to those feeds that do not provide either pubDate
        // or dc:date

        List orderedItems = new ArrayList();

        // Calculate signature
        MessageDigest md = MessageDigest.getInstance("MD5");

        for (int itemCount = itemList.getLength() - 1;
            itemCount >= 0;
            itemCount--) {
            Element itemElm = (Element) itemList.item(itemCount);
            String title;
            String link;
            String description;
            ByteArrayOutputStream bos;
            byte[] signatureSource;
            byte[] signature;
            String signatureStr = generateItemSignature(md, itemElm);

            if (!keepHistory) {
                currentSignatures.add(signatureStr);
            }
            newItems.put(signatureStr, itemElm);
            newItemKeys.put(itemElm, signatureStr);
            orderedItems.add(itemElm);
        }

        if (newItems.size() > 0) {
            // Discover new items...
            Set newItemSignatures =
                channelDAO.findNewItemSignatures(channel, newItems.keySet());

            if (newItemSignatures.size() > 0) {
                for (int i = 0; i < orderedItems.size(); i++) {
                    Element itemElm = (Element) orderedItems.get(i);
                    String signatureStr = (String) newItemKeys.get(itemElm);

                    // If signature is not in new items set, skip...
                    if (!newItemSignatures.contains(signatureStr))
                        continue;

                    String title =
                        XMLHelper.getChildElementValue(itemElm, "title", "");
                    String link =
                        XMLHelper.getChildElementValue(itemElm, "link", "");

                    String guid =
                        XMLHelper.getChildElementValue(itemElm, "guid");
                    boolean guidIsPermaLink = true;
                    if (guid != null) {
                        String guidIsPermaLinkStr =
                            XMLHelper.getChildElementAttributeValue(
                                itemElm,
                                "guid",
                                "isPermaLink");
                        if (guidIsPermaLinkStr != null) {
                            guidIsPermaLink =
                                guidIsPermaLinkStr.equalsIgnoreCase("true");
                        }
                    }

                    // Handle xhtml:body / content:encoded / description
                    String description = processContent(itemElm);

                    String comments =
                        XMLHelper.getChildElementValue(itemElm, "comments", "");

                    Date pubDate = null;
                    String pubDateStr =
                        XMLHelper.getChildElementValue(itemElm, "pubDate");
                    if (pubDateStr != null && pubDateStr.length() > 0) {
                        // Parse Date...
                        log.info("pubDate == " + pubDateStr);
                        try {
                            pubDate =
                                ((MailDateFormat) date822Parser.get()).parse(
                                    pubDateStr);
                            log.debug("processed pubDate == " + pubDate);
                        } catch (ParseException pe) {
                            log.debug("Invalid pubDate format - " + pubDateStr);
                        }
                    }

                    if (pubDate == null) {
                        // Try for Dublin Core Date
                        String dcDateStr =
                            XMLHelper.getChildElementValueNS(
                                itemElm,
                                RSSHelper.XMLNS_DC,
                                "date");
                        if (dcDateStr != null && dcDateStr.length() > 0) {
                            log.debug("dc:date == " + dcDateStr);

                            if (dcDateStr.indexOf("GMT") == -1) {
                                // Check for : in RFC822 time zone...
                                int hourColon = dcDateStr.indexOf(":");
                                if (hourColon != -1) {
                                    int minuteColon =
                                        dcDateStr.indexOf(":", hourColon + 1);
                                    if (minuteColon != -1) {
                                        int timeZoneColon =
                                            dcDateStr.indexOf(
                                                ":",
                                                minuteColon + 1);
                                        if (timeZoneColon != -1) {
                                            if (dcDateStr.length()
                                                > timeZoneColon) {
                                                dcDateStr =
                                                    dcDateStr.substring(
                                                        0,
                                                        timeZoneColon)
                                                        + dcDateStr.substring(
                                                            timeZoneColon + 1);
                                            }
                                        }
                                    }
                                }

                            }

                            SimpleDateFormat[] dcDateArray =
                                (SimpleDateFormat[]) dcDates.get();
                            for (int parseCount = 0;
                                parseCount < dcDateArray.length;
                                parseCount++) {
                                try {
                                    pubDate =
                                        dcDateArray[parseCount].parse(
                                            dcDateStr);
                                } catch (ParseException pe) {
                                }
                                if (pubDate != null)
                                    break;
                            }
                            if (pubDate != null) {
                                log.debug("processed dc:date == " + pubDate);
                            } else {
                                log.debug(
                                    "Invalid dc:date format - " + dcDateStr);
                            }
                        }
                    }

                    String dcCreator =
                        XMLHelper.getChildElementValueNS(
                            itemElm,
                            RSSHelper.XMLNS_DC,
                            "creator");

                    int lastArticleNumber = channel.getLastArticleNumber();
                    Item item = new Item(++lastArticleNumber, signatureStr);
                    channel.setLastArticleNumber(lastArticleNumber);

                    item.setChannel(channel);

                    if (title.length() > 0) {
                        item.setTitle(title);
                    } else {
                        // We need to create a initial title from the description, because
                        // we do have a description, don't we???
                        String strippedDesc =
                            stripControlChars(XMLHelper.stripTags(description));
                        int length =
                            strippedDesc.length() > 64
                                ? 64
                                : strippedDesc.length();
                        item.setTitle(strippedDesc.substring(0, length));
                    }
                    item.setDescription(description);
                    item.setLink(link);
                    item.setGuid(guid);
                    item.setGuidIsPermaLink(guidIsPermaLink);
                    item.setComments(comments);
                    item.setCreator(dcCreator);

                    if (pubDate == null) {
                        item.setDate(retrievalDate.getTime());
                        // Add 1 second - to introduce some distinction date-wise
                        // between items
                        retrievalDate.add(Calendar.SECOND, 1);
                    } else {
                        item.setDate(pubDate);
                    }

					// Invoke ItemProcessor
					invokeProcessors(item);

                    // persist to database...
                    channelDAO.saveItem(item);
                    channel.setTotalArticles(channel.getTotalArticles() + 1);
                }
            }

        }

        if (!keepHistory) {
            if (currentSignatures.size() > 0) {
                if (channel.getExpiration() == 0) {
                    channelDAO.deleteItemsNotInSet(channel, currentSignatures);
                } else if (
                    channel.getExpiration() > 0
                        && channel.getLastCleaned().before(
                            new Date(
                                System.currentTimeMillis()
                                    - Channel.CLEANING_INTERVAL))) {
                    channelDAO.deleteExpiredItems(channel, currentSignatures);
                    channel.setLastCleaned(new Date());
                }
            }
            channel.setTotalArticles(currentSignatures.size());
        }
    }

    public String processContent(Element itemElm) {
        // Check for xhtml:body
        String description = null;

        NodeList bodyList =
            itemElm.getElementsByTagNameNS(RSSHelper.XMLNS_XHTML, "body");
        if (bodyList.getLength() > 0) {
            Node bodyElm = bodyList.item(0);
            NodeList children = bodyElm.getChildNodes();
            StringBuffer content = new StringBuffer();
            for (int childCount = 0;
                childCount < children.getLength();
                childCount++) {
                content.append(children.item(childCount).toString());
            }
            description = content.toString();
        }

        // Fix for content:encoded section of RSS 1.0/2.0
        if ((description == null) || (description.length() == 0)) {
            description =
                XMLHelper.getChildElementValue(itemElm, "content:encoded");
        }

        if ((description == null) || (description.length() == 0)) {
            description =
                XMLHelper.getChildElementValue(itemElm, "description", "");
        }
        return description;
    }

    private String generateItemSignature(MessageDigest md, Element itemElm)
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
