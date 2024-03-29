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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AtomParser.java,v 1.13 2007/12/17 04:12:42 jasonbrome Exp $
 */

public class AtomParser extends GenericParser {

    public static final String XMLNS_OLD_ATOM = "http://purl.org/atom/ns#";
    public static final String XMLNS_ATOM = "http://www.w3.org/2005/Atom";

    private static ThreadLocal dateParsers = new ThreadLocal() {
        @Override
        public Object initialValue() {
            SimpleDateFormat[] dcDateArray =
                new SimpleDateFormat[] {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ssz"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")};

            TimeZone gmt = TimeZone.getTimeZone("GMT");
            for (int tz = 0; tz < dcDateArray.length; tz++) {
                dcDateArray[tz].setTimeZone(gmt);
            }

            return dcDateArray;
        }
    };

    private static final AtomParser atomParser = new AtomParser();
    private static final Logger log = Logger.getLogger(AtomParser.class);

    private AtomParser() {
    }

    public static GenericParser getParser() {
        return atomParser;
    }

    /**
     * @param docRootElement Root element of feed document
     * @return
     */
    @Override
    public boolean isParsable(Element docRootElement) {
        if (docRootElement.getNodeName().equals("feed")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getFormatVersion(Element docRootElement) {
        String atomVersion;

        if (docRootElement.getNodeName().equals("feed")) {
            atomVersion = "Atom " + docRootElement.getAttribute("version");
        } else {
            atomVersion = "Unknown Atom";
        }
        return atomVersion;
    }

    @Override
    public void extractFeedInfo(Element docRootElement, Channel channel) {
        // Read header...
        channel.setTitle(
            XMLHelper.getChildElementValue(docRootElement, "title"));
        // XXX Currently assign channelTitle to author
        channel.setAuthor(channel.getTitle());

    	URI xmlBase = extractXmlBase(docRootElement, channel);
        channel.setLink(extractLink(channel, docRootElement, xmlBase));

        // @TODO: Summary or Subtitle?
        String description;
        description = XMLHelper.getChildElementValue(docRootElement, "summary");
        if (description == null || description.length() == 0) {
            description =
                XMLHelper.getChildElementValue(docRootElement, "subtitle");
        }
        channel.setDescription(description);

        // Build author...
        NodeList authorList = docRootElement.getElementsByTagName("author");
        if (authorList.getLength() > 0) {
            Element authorElm = (Element) authorList.item(0);
            channel.setManagingEditor(extractAuthor(authorElm));
        } else {
            channel.setManagingEditor(null);
        }
    }

    private String extractAuthor(Element authorElm) {
        StringBuffer author = new StringBuffer();
        String name = XMLHelper.getChildElementValue(authorElm, "name");
        String email = XMLHelper.getChildElementValue(authorElm, "email");
        if (name != null && name.length() > 0) {
            author.append(name);
        }

        if (email != null && email.length() > 0) {
            if (author.length() > 0) {
                author.append(" (").append(email).append(")");
            } else {
                author.append(email);
            }
        }
        return author.toString();
    }

    @Override
    public void processFeedItems(
        Element rootElm,
        Channel channel,
        ChannelDAO channelDAO,
        boolean keepHistory)
        throws NoSuchAlgorithmException, IOException {

    	URI xmlBase = extractXmlBase(rootElm, channel);
    	
        NodeList entryList = rootElm.getElementsByTagName("entry");

        Calendar retrievalDate = Calendar.getInstance();
        retrievalDate.add(Calendar.SECOND, -entryList.getLength());

        Set currentSignatures = null;
        if (!keepHistory) {
            currentSignatures = new HashSet();
        }

        Map newItems = new HashMap();
        Map newItemKeys = new HashMap();
        // orderedItems maintains original document first-to-last order
        // Assumption: Items in the Atom document go from most recent
        // to earliest.  This is used to assign date/times in a reasonably
        // sensible order to those feeds that do not provide a date

        List orderedItems = new ArrayList();

        // Calculate signature
        MessageDigest md = MessageDigest.getInstance("MD5");

        for (int itemCount = entryList.getLength() - 1;
            itemCount >= 0;
            itemCount--) {
            Element itemElm = (Element) entryList.item(itemCount);
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
                    Element entryElm = (Element) orderedItems.get(i);
                    String signatureStr = (String) newItemKeys.get(entryElm);

                    // If signature is not in new items set, skip...
                    if (!newItemSignatures.contains(signatureStr))
                        continue;

                    String title =
                        XMLHelper.getChildElementValue(entryElm, "title", "");
                    //					String link =
                    //						XMLHelper.getChildElementValue(entryElm, "link", "");
                    String link = extractLink(channel, entryElm, xmlBase);

                    String guid =
                        XMLHelper.getChildElementValue(entryElm, "id");
                    boolean guidIsPermaLink = false;
                    //					if (guid != null) {
                    //						String guidIsPermaLinkStr =
                    //							XMLHelper.getChildElementAttributeValue(
                    //								itemElm,
                    //								"guid",
                    //								"guidIsPermaLink");
                    //						if (guidIsPermaLinkStr != null) {
                    //							guidIsPermaLink =
                    //								guidIsPermaLinkStr.equalsIgnoreCase("true");
                    //						}
                    //					}

                    // Handle xhtml:body / content:encoded / description
                    String description = processContent(entryElm);

                    // @TODO: Comments in Atom?
                    String comments = null;

                    // Date - use modified, if not present use created
                    String pubDateStr =
                        XMLHelper.getChildElementValue(entryElm, "modified");
                    if (pubDateStr == null || pubDateStr.length() == 0) {
                        pubDateStr =
                            XMLHelper.getChildElementValue(entryElm, "created");
                    }

                    Date pubDate = null;

                    if (pubDateStr != null && pubDateStr.length() > 0) {
                        log.debug("create/modified == " + pubDateStr);
                        SimpleDateFormat[] dateParserArray =
                            (SimpleDateFormat[]) dateParsers.get();
                        for (int parseCount = 0;
                            parseCount < dateParserArray.length;
                            parseCount++) {
                            try {
                                pubDate =
                                    dateParserArray[parseCount].parse(
                                        pubDateStr);
                            } catch (ParseException pe) {
                            }
                            if (pubDate != null)
                                break;
                        }
                        if (pubDate != null) {
                            log.debug("processed Atom feed date == " + pubDate);
                        } else {
                            log.debug(
                                "Invalid Atom feed date format - "
                                    + pubDateStr);
                        }
                    }

                    String creator = null;
                    NodeList authorList =
                        entryElm.getElementsByTagName("author");
                    if (authorList.getLength() > 0) {
                        Element authorElm = (Element) authorList.item(0);
                        creator = extractAuthor(authorElm);
                    }

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
                    item.setCreator(creator);

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
					channel.setTotalArticles(currentSignatures.size());
                } else if (
                    channel.getExpiration() > 0
						&& (channel.getLastCleaned() == null || channel.getLastCleaned().before(
					new Date(
						System.currentTimeMillis()
							- Channel.CLEANING_INTERVAL)))) {
                    channelDAO.deleteExpiredItems(channel, currentSignatures);
                    channel.setLastCleaned(new Date());
                }
            }
        }
    }

    private URI extractXmlBase(Element rootElm, Channel channel) {
    	return extractXmlBase(rootElm, channel, null);
    }
    
	private URI extractXmlBase(Element rootElm, Channel channel, URI existingBase)
			 {
		URI xmlBase;
		// Handle xml:base
    	Attr xmlBaseAttr = rootElm.getAttributeNodeNS(XMLHelper.XML_NS_URI, "base");

    	// XXX Malformed URLs?
    	try {
	    	if(xmlBaseAttr != null) {
	    		if(existingBase == null) {
	    			xmlBase = new URI(xmlBaseAttr.getValue());
	    		} else {
	    			xmlBase = existingBase.resolve(xmlBaseAttr.getValue());
	    		}
	    	} else {
	    		if(existingBase != null) {
	    			xmlBase = existingBase;
	    		} else {
	    			xmlBase = new URI(channel.getUrl());
	    		}
	    	}
    	}
    	catch(URISyntaxException use) {
    		// XXX Use channel URL - this should be ok, otherwise
    		// we would have not got here in the first place
    		try {
    			if(existingBase != null) {
    				xmlBase = existingBase;
    			} else {
    				xmlBase = new URI(channel.getUrl());
    			}
    		}
    		catch(URISyntaxException use2) { 
    			xmlBase = null;
    		}
    	}
		return xmlBase;
	}

	@Override
    public String processContent(Element itemElm) {
        // Check for xhtml:body
        String description = null;

        NodeList contentList = itemElm.getElementsByTagName("content");

        if (contentList.getLength() > 0) {
            Element contentElm = (Element) contentList.item(0);

            String type = contentElm.getAttribute("type");
            String mode = contentElm.getAttribute("mode");
            if ((type.startsWith("application/xhtml")
                && !mode.equalsIgnoreCase("escaped")) || 
                type.equals("xhtml")) {
                // xhtml body 				
                NodeList children = contentElm.getChildNodes();
                StringBuffer content = new StringBuffer();
                for (int childCount = 0;
                    childCount < children.getLength();
                    childCount++) {
                    content.append(children.item(childCount).toString());
                }
                description = content.toString();
            } else {
                // escaped text...				
                description = XMLHelper.getElementValue(contentElm);
            }
        }

        // If all else fails, drop back and use the contents of the summary...
        if ((description == null) || (description.length() == 0)) {
            description =
                XMLHelper.getChildElementValue(itemElm, "summary", "");
        }
        return description;
    }

    private String extractLink(Channel channel, Element itemElm, URI xmlBase) {
        String link = null;

        // Get entry's XML base
        URI entryXmlBase = extractXmlBase(itemElm, channel, xmlBase);
        NodeList elemList = itemElm.getChildNodes();
        for (int i = 0; i < elemList.getLength(); i++) {
            if (elemList.item(i).getNodeName().equals("link")
                &&
                ( elemList.item(i).getNamespaceURI().equals(XMLNS_ATOM) ||
                elemList.item(i).getNamespaceURI().equals(XMLNS_OLD_ATOM)) ) {
                Element linkElm = (Element) elemList.item(i);
                Attr linkRelNode = linkElm.getAttributeNode("rel");
                if(linkRelNode == null || linkRelNode.getValue().equals("alternate")) {
                    String entryLink = linkElm.getAttribute("href");
                    link = entryXmlBase.resolve(entryLink).toString();
                    break;
                }
            }
        }
        return link;
    }

}
