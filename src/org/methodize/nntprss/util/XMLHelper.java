package org.methodize.nntprss.util;

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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: XMLHelper.java,v 1.4 2003/02/08 06:21:35 jasonbrome Exp $
 */
public class XMLHelper {

	public static String getChildElementValue(
		Element parentElm,
		String elementName) {

		String elementValue = null;
		NodeList elemList = parentElm.getElementsByTagName(elementName);
		if (elemList.getLength() > 0) {
			// Use the first matching child element
			Element elm = (Element) elemList.item(0);
			NodeList childNodes = elm.getChildNodes();
			StringBuffer value = new StringBuffer();
			for (int elemCount = 0;
				elemCount < childNodes.getLength();
				elemCount++) {
				
				if (childNodes.item(elemCount) instanceof org.w3c.dom.Text) {
					value.append(childNodes.item(elemCount).getNodeValue());
				} 
			}
			elementValue = value.toString();
		}
		return elementValue;

	}

	public static String getChildElementValue(
		Element parentElm,
		String elementName,
		String defaultValue) {
		String value = getChildElementValue(parentElm, elementName);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	public static String stripTags(String value) {
		StringTokenizer strTok = new StringTokenizer(value, "<>", true);
		StringBuffer strippedString = new StringBuffer();
		boolean inTag = false;
		while (strTok.hasMoreTokens()) {
			String token = strTok.nextToken();
			if (token.equals("<")) {
				inTag = true;
			} else if (token.equals(">")) {
				inTag = false;
			} else if (!inTag) {
				strippedString.append(token);
			}
		}
		return strippedString.toString();

	}

	private static String preprocessMarkup(String value) {
		StringBuffer trimmedString = new StringBuffer();
		boolean lastCharSpace = false;
		for(int c = 0; c < value.length(); c++) {
			char currentChar = value.charAt(c);
			if(currentChar == '\n') {
				trimmedString.append(currentChar);
			} else if(currentChar < 32) {
				continue;
			} else if(currentChar == ' ') {
				if(!lastCharSpace) {
					trimmedString.append(currentChar);
					lastCharSpace = true;
				} 
			} else {
				trimmedString.append(currentChar);
				lastCharSpace = false;
			}
		}
		return trimmedString.toString();
	}

	public static String stripHtmlTags(String value) {
// Trim white space... Use html markup (p, br) as line breaks
		value = preprocessMarkup(value);

		StringTokenizer strTok = new StringTokenizer(value, "<>\n", true);
		StringBuffer strippedString = new StringBuffer();
		boolean inTag = false;
		boolean startOfLine = true;
		String lastURL = null;
		while (strTok.hasMoreTokens()) {
			String token = strTok.nextToken();
			if (token.equals("<")) {
				inTag = true;

// Read entire tag... Tag contents might be split over multiple lines
				StringBuffer concatToken = new StringBuffer();
				while(strTok.hasMoreTokens()) {
					token = strTok.nextToken();
					if(token.equals(">")) {
						inTag = false;
						break;
					} else {
						if(!token.equals("\n")) {
							concatToken.append(token);
						}
					}
				}

				token = concatToken.toString();

				String upperToken = token.toUpperCase();
				if(upperToken.startsWith("A ")) {
					int hrefPos = upperToken.indexOf("HREF=");
					if(hrefPos > -1) {
						int quotePos = hrefPos + 5;
														
						while(quotePos < token.length() &&
						    Character.isWhitespace(token.charAt(quotePos))) {
							quotePos ++;
						}

						char quote = upperToken.charAt(quotePos);
						
						int endPos;
						if(quote == '"' || quote == '\'') {
// URL wrapped in quotes / apostrophes
							endPos = token.indexOf(quote, quotePos+1);
						} else {
// URL not enclosed								
							endPos = quotePos + 1;
							while(endPos < token.length() &&
							    !Character.isWhitespace(token.charAt(endPos))) {
								endPos++;
							}
						}
						
						if(endPos != -1) {
							lastURL = token.substring(quotePos + 1, endPos);
							if(upperToken.endsWith("/")) {
								strippedString.append(" (");
								strippedString.append(lastURL);
								strippedString.append(')');
								lastURL = null;
								startOfLine = false;
							} 
						}
					}		
				} else if(upperToken.startsWith("/A")) {
					if(lastURL != null) {
							strippedString.append(" (");
							strippedString.append(lastURL);
							strippedString.append(')');
							lastURL = null;
							startOfLine = false;
					}
				} else if(upperToken.equals("P") ||
					upperToken.equals("P/") ||
					upperToken.equals("P /") ||
					upperToken.equals("UL") ||
					upperToken.equals("/UL")) {
					strippedString.append("\r\n\r\n");
					startOfLine = true;
				} else if(upperToken.equals("BR") ||
					upperToken.equals("BR/") ||
					upperToken.equals("BR /") ||
					upperToken.equals("LI")) {
					strippedString.append("\r\n");
					startOfLine = true;
				}

			} else if (token.equals(">")) {
				inTag = false;
			} else if (token.equals("\n")) {
				if(!inTag && !startOfLine) {
					strippedString.append(' ');
				}
			} else if (!inTag) {
				strippedString.append(token);
				startOfLine = false;
			}
		}
		return strippedString.toString();

	}
	
	public static String escapeString(String value) {
		StringBuffer escapedString = new StringBuffer();
		for(int charCount = 0; charCount < value.length(); charCount++) {
			char c = value.charAt(charCount);
			switch(c) {
				case '&':
					escapedString.append("&amp;");
					break;
				case '<':
					escapedString.append("&lt;");
					break;
				case '>':
					escapedString.append("&gt;");
					break;
				case '\"':
					escapedString.append("&quot;");
					break;
				case '\'':
					escapedString.append("&apos;");
					break;
				default:
					escapedString.append(c);
			}
		}
		return escapedString.toString();
	}
	
}
