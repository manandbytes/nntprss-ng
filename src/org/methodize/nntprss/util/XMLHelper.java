package org.methodize.nntprss.util;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002 Jason Brome.  All Rights Reserved.
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

import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
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
			for (int elemCount = 0;
				elemCount < childNodes.getLength();
				elemCount++) {
				if (childNodes.item(elemCount) instanceof org.w3c.dom.Text) {
					elementValue = childNodes.item(elemCount).getNodeValue();
				}
			}
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
