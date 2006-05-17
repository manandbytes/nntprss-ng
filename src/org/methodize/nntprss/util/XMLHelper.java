package org.methodize.nntprss.util;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2006 Jason Brome.  All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hsqldb.lib.StringInputStream;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: XMLHelper.java,v 1.14 2006/05/17 04:13:38 jasonbrome Exp $
 */
public class XMLHelper {

    public static String getChildElementValue(
        Element parentElm,
        String elementName) {

        String elementValue = null;
        NodeList elemList = parentElm.getChildNodes();
        Element elm = null;
        for (int i = 0; i < elemList.getLength(); i++) {
            if (elemList.item(i).getNodeName().equals(elementName)) {
                elm = (Element) elemList.item(i);
                elementValue = getElementValue(elm);
                break;
            }
        }

        return elementValue;
    }

    public static String getElementValue(Element elm) {
        String elementValue;
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
        return elementValue;
    }

    public static String getChildElementAttributeValue(
        Element parentElm,
        String elementName,
        String attributeName) {

        String attributeValue = null;
        NodeList elemList = parentElm.getElementsByTagName(elementName);
        if (elemList != null && elemList.getLength() > 0) {
            // Use the first matching child element
            Element elm = (Element) elemList.item(0);
            Attr attribute = elm.getAttributeNode(attributeName);
            if (attribute != null) {
                attributeValue = attribute.getValue();
            }
        }
        return attributeValue;
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

    public static String getChildElementValueNS(
        Element parentElm,
        String elementNamespaceURI,
        String elementLocalName) {

        String elementValue = null;
        NodeList elemList =
            parentElm.getElementsByTagNameNS(
                elementNamespaceURI,
                elementLocalName);
        if (elemList != null && elemList.getLength() > 0) {
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

    public static String getChildElementValueNS(
        Element parentElm,
        String elementNamespaceURI,
        String elementLocalName,
        String defaultValue) {
        String value =
            getChildElementValueNS(
                parentElm,
                elementNamespaceURI,
                elementLocalName);
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
        for (int c = 0; c < value.length(); c++) {
            char currentChar = value.charAt(c);
            if (currentChar == '\n') {
                trimmedString.append(currentChar);
            } else if (currentChar < 32) {
                continue;
            } else if (currentChar == ' ') {
                if (!lastCharSpace) {
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

    public static String stripHtmlTags(String value, boolean footnoteLinks) {
        // Trim white space... Use html markup (p, br) as line breaks
        value = preprocessMarkup(value);

        StringTokenizer strTok = new StringTokenizer(value, "<>\n", true);
        StringBuffer strippedString = new StringBuffer();
        boolean inTag = false;
        boolean inPre = false;
        boolean startOfLine = true;
        String lastURL = null;
        List footnotes = null;
        if (footnoteLinks) {
            footnotes = new ArrayList();
        }

        while (strTok.hasMoreTokens()) {
            String token = strTok.nextToken();
            if (token.equals("<")) {
                inTag = true;

                // Read entire tag... Tag contents might be split over multiple lines
                StringBuffer concatToken = new StringBuffer();
                while (strTok.hasMoreTokens()) {
                    token = strTok.nextToken();
                    if (token.equals(">")) {
                        inTag = false;
                        break;
                    } else {
                        if (!token.equals("\n")) {
                            concatToken.append(token);
                        }
                    }
                }

                token = concatToken.toString();

                String upperToken = token.toUpperCase();
                if (upperToken.startsWith("A ")) {
                    int hrefPos = upperToken.indexOf("HREF=");
                    if (hrefPos > -1) {
                        int quotePos = hrefPos + 5;

                        while (quotePos < token.length()
                            && Character.isWhitespace(token.charAt(quotePos))) {
                            quotePos++;
                        }

                        char quote = upperToken.charAt(quotePos);

                        int endPos;
                        if (quote == '"' || quote == '\'') {
                            // URL wrapped in quotes / apostrophes
                            endPos = token.indexOf(quote, quotePos + 1);
                        } else {
                            // URL not enclosed								
                            endPos = quotePos + 1;
                            while (endPos < token.length()
                                && !Character.isWhitespace(
                                    token.charAt(endPos))) {
                                endPos++;
                            }
                        }

                        if (endPos != -1) {
                            lastURL = token.substring(quotePos + 1, endPos);
                            if (upperToken.endsWith("/")) {
                                if (!footnoteLinks) {
                                    // Changed URL wrap characters from parenthesis to lt / gt to avoid
                                    // issue with certain newsreaders
                                    strippedString.append(" <URL:");
                                    strippedString.append(lastURL);
                                    strippedString.append('>');
                                } else {
                                    footnotes.add(lastURL);
                                    strippedString.append('[').append(
                                        footnotes.size()).append(
                                        ']');
                                }
                                lastURL = null;
                                startOfLine = false;
                            }
                        }
                    }
                } else if (upperToken.startsWith("/A")) {
                    if (lastURL != null) {
                        if (!footnoteLinks) {
                            strippedString.append(" <URL:");
                            strippedString.append(lastURL);
                            strippedString.append('>');
                        } else {
                            footnotes.add(lastURL);
                            strippedString.append('[').append(
                                footnotes.size()).append(
                                ']');
                        }
                        lastURL = null;
                        startOfLine = false;
                    }
                } else if (
                    upperToken.equals("P")
                        || upperToken.equals("P/")
                        || upperToken.equals("P /")
                        || upperToken.equals("UL")
                        || upperToken.equals("/UL")) {
                    strippedString.append("\r\n\r\n");
                    startOfLine = true;
                } else if (
                    upperToken.equals("BR")
                        || upperToken.equals("BR/")
                        || upperToken.equals("BR /")
                        || upperToken.equals("LI")) {
                    strippedString.append("\r\n");
                    startOfLine = true;
                } else if (upperToken.equals("PRE")) {
                    inPre = true;
                } else if (upperToken.equals("/PRE")) {
                    inPre = false;
                }

            } else if (token.equals(">")) {
                inTag = false;
            } else if (token.equals("\n")) {
                if (!inTag && !startOfLine) {
                    if (!inPre) {
                        strippedString.append(' ');
                    } else {
                        strippedString.append('\n');
                    }
                }
            } else if (!inTag) {
                strippedString.append(token);
                startOfLine = false;
            }
        }

        if (footnoteLinks && footnotes.size() > 0) {
            strippedString.append("\r\n\r\n");
            for (int count = 0; count < footnotes.size(); count++) {
                strippedString.append(count + 1).append(". ");
                strippedString.append(footnotes.get(count));
                strippedString.append("\r\n");
            }
        }
        return strippedString.toString();

    }

    public static String escapeString(String value) {
        StringBuffer escapedString = new StringBuffer();
        for (int charCount = 0; charCount < value.length(); charCount++) {
            char c = value.charAt(charCount);
            switch (c) {
                case '&' :
                    escapedString.append("&amp;");
                    break;
                case '<' :
                    escapedString.append("&lt;");
                    break;
                case '>' :
                    escapedString.append("&gt;");
                    break;
                case '\"' :
                    escapedString.append("&quot;");
                    break;
                case '\'' :
                    escapedString.append("&apos;");
                    break;
                default :
                    escapedString.append(c);
            }
        }
        return escapedString.toString();
    }

    /**
     * Some helper functions used to serialize String-based
     * maps (i.e. where both key and value are strings) to
     * an XML document.
     */

    public static String stringMapToXML(Map stringMap) {
        String mapXMLResult = null;
        if (stringMap != null && stringMap.size() > 0) {
            StringBuffer mapXML = new StringBuffer();
            mapXML.append("<?xml version='1.0' encoding='UTF-8'?>\n<map>\n");

            Iterator mapIter = stringMap.entrySet().iterator();

            while (mapIter.hasNext()) {
                Map.Entry entry = (Map.Entry) mapIter.next();
                mapXML.append("<entry key='");
                mapXML.append(escapeString((String) entry.getKey()));
                mapXML.append("' value='");
                mapXML.append(escapeString((String) entry.getValue()));
                mapXML.append("'/>\n");
            }

            mapXML.append("</map>");

            mapXMLResult = mapXML.toString();
        } else {
            mapXMLResult = "";
        }
        return mapXMLResult;
    }

    public static Map xmlToStringHashMap(String xml) {
        Map map = new HashMap();

        if (xml != null && xml.length() > 0) {
            try {
                Document doc =
                    AppConstants.newDocumentBuilder().parse(
                        new StringInputStream(xml));
                Element rootElm = doc.getDocumentElement();
                NodeList entryList = rootElm.getElementsByTagName("entry");
                for (int elmCount = 0;
                    elmCount < entryList.getLength();
                    elmCount++) {
                    Element entry = (Element) entryList.item(elmCount);
                    map.put(
                        entry.getAttribute("key"),
                        entry.getAttribute("value"));
                }
            } catch (Exception e) {
                // XXX do we need to handle this scenario?			
            }
        }

        return map;

    }

}
