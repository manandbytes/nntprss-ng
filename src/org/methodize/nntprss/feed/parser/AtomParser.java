package org.methodize.nntprss.feed.parser;

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

import org.apache.log4j.Logger;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.feed.db.ChannelManagerDAO;
import org.methodize.nntprss.util.Base64;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AtomParser.java,v 1.2 2003/07/20 02:46:25 jasonbrome Exp $
 */

public class AtomParser extends GenericParser {

	private static ThreadLocal dateParsers = new ThreadLocal() {
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
	
	private static AtomParser atomParser = new AtomParser();
	private Logger log = Logger.getLogger(AtomParser.class);

	private AtomParser() {
	} 

	public static GenericParser getParser() {
		return atomParser;
	}

	/**
	 * @param docRootElement Root element of feed document
	 * @return
	 */
	public boolean isParsable(Element docRootElement) {
		if (docRootElement.getNodeName().equals("feed")) {
			return true;
		} else {
			return false;
		}
	}

	public String getFormatVersion(Element docRootElement) {
		String atomVersion;

		if (docRootElement.getNodeName().equals("feed")) {
			atomVersion = "Atom " + docRootElement.getAttribute("version");
		} else {
			atomVersion = "Unknown Atom";
		}
		return atomVersion;
	}

	public void extractFeedInfo(Element docRootElement, Channel channel) {
		// Read header...
		channel.setTitle(XMLHelper.getChildElementValue(docRootElement, "title"));
		// XXX Currently assign channelTitle to author
		channel.setAuthor(channel.getTitle());
		channel.setLink(XMLHelper.getChildElementValue(docRootElement, "link"));

// @TODO: Summary or Subtitle?
		String description;
		description =
			XMLHelper.getChildElementValue(docRootElement, "summary");
		if(description == null || description.length() == 0) { 
			description =
				XMLHelper.getChildElementValue(docRootElement, "subtitle");
		}
		channel.setDescription(description);
		
// Build author...
		NodeList authorList = docRootElement.getElementsByTagName("author");
		if(authorList.getLength() > 0) {
			Element authorElm = (Element)authorList.item(0);
			channel.setManagingEditor(extractAuthor(authorElm));
		} else {
			channel.setManagingEditor(null);
		}
	}

	private String extractAuthor(Element authorElm) {
		StringBuffer author = new StringBuffer();
		String name = XMLHelper.getChildElementValue(authorElm, "name");
		String email = XMLHelper.getChildElementValue(authorElm, "email");
		if(name != null && name.length() > 0) {
			author.append(name);
		}
		
		if(email != null && email.length() > 0) {
			if(author.length() > 0) {
				author.append(" (").append(email).append(")");
			} else {
				author.append(email);
			}
		}
		return author.toString();
	}

	public void processFeedItems(
		Element rootElm,
		Channel channel,
		ChannelManagerDAO channelManagerDAO,
		boolean keepHistory)
		throws NoSuchAlgorithmException, IOException {

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
			String title;
			String link;
			String description;
			ByteArrayOutputStream bos;
			byte[] signatureSource;
			byte[] signature;
			String signatureStr = generateEntrySignature(md, itemElm);

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
				channelManagerDAO.findNewItemSignatures(
					channel.getId(),
					newItems.keySet());

			if (newItemSignatures.size() > 0) {
				for (int i = 0; i < orderedItems.size(); i++) {
					Element entryElm = (Element) orderedItems.get(i);
					String signatureStr = (String) newItemKeys.get(entryElm);

					// If signature is not in new items set, skip...
					if (!newItemSignatures.contains(signatureStr))
						continue;

					String title =
						XMLHelper.getChildElementValue(entryElm, "title", "");
					String link =
						XMLHelper.getChildElementValue(entryElm, "link", "");

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
					String pubDateStr = XMLHelper.getChildElementValue(entryElm,
						"modified");
					if(pubDateStr == null || pubDateStr.length() == 0) {
						pubDateStr = XMLHelper.getChildElementValue(entryElm,
							"created");
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
								"Invalid Atom feed date format - " + pubDateStr);
						}
					}

					String creator = null;
					NodeList authorList = entryElm.getElementsByTagName("author");
					if(authorList.getLength() > 0) {
						Element authorElm = (Element)authorList.item(0);
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

					// persist to database...
					channelManagerDAO.saveItem(item);
					channel.setTotalArticles(channel.getTotalArticles() + 1);
				}
			}

		}

		if (!keepHistory) {
			channelManagerDAO.deleteItemsNotInSet(channel, currentSignatures);
			channel.setTotalArticles(currentSignatures.size());
		}
	}

	public String processContent(Element itemElm) {
		// Check for xhtml:body
		String description = null;

		NodeList contentList =
			itemElm.getElementsByTagName("content");
			
		if (contentList.getLength() > 0) {
			Element contentElm = (Element)contentList.item(0);

			String type = contentElm.getAttribute("type");
			String mode = contentElm.getAttribute("mode");
			if(type.startsWith("application/xhtml") && !mode.equalsIgnoreCase("escaped")) {
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
			   description =
	   			XMLHelper.getElementValue(contentElm);
			}
		}

// If all else fails, drop back and use the contents of the summary...
		if ((description == null) || (description.length() == 0)) {
			description =
				XMLHelper.getChildElementValue(itemElm, "summary", "");
		}
		return description;
	}

	private String generateEntrySignature(MessageDigest md, Element itemElm)
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
