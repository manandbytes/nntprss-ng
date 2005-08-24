package org.methodize.nntprss.feed.parser;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2005 Jason Brome.  All Rights Reserved.
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

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML.Tag;

import org.methodize.nntprss.feed.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ParserCallback.java,v 1.5 2005/08/24 23:12:10 jasonbrome Exp $
 */
public class ParserCallback extends HTMLEditorKit.ParserCallback {

    private boolean inChannel = false;
    private boolean inItem = false;
    private String currentText = null;
    private Item currentItem = null;
    private Element currentItemElm = null;

    private Element rootElm = null;
    private Element channelElm = null;
    private Document doc = null;

    private ParserCallback() {
    }

    public ParserCallback(Document doc) {
        this.doc = doc;
        this.rootElm = doc.getDocumentElement();
        this.channelElm =
            (Element) rootElm.getElementsByTagName("channel").item(0);
    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleComment(char[], int)
     */
    public void handleComment(char[] data, int pos) {
        //		System.out.println("Comment: " 
        //			+ new String(data));
    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleEndOfLineString(String)
     */
    public void handleEndOfLineString(String eol) {
        //		System.out.println("EOL: " + eol.toString());
    }

    private void createTextChild(
        Element parent,
        String element,
        String value) {

        Element elm = doc.createElement(element);
        Text elmTextElm = doc.createTextNode(value);
        elm.appendChild(elmTextElm);
        parent.appendChild(elm);
    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleEndTag(Tag, int)
     */
    public void handleEndTag(Tag t, int pos) {
        //		System.out.println("EndTag: " + t.toString());

        String tagName = t.toString();
        if (tagName.equals("item")) {
            inItem = false;
            if (currentItemElm != null) {
                rootElm.appendChild(currentItemElm);
                currentItemElm = null;
            }
        } else if (tagName.equals("channel")) {
            inChannel = false;
        } else {
            if (inItem) {
                if (currentItemElm == null) {
                    currentItemElm = doc.createElement("item");
                }

                if (tagName.equals("title")) {
                    createTextChild(currentItemElm, "title", currentText);
                } else if (tagName.equals("link")) {
                    createTextChild(currentItemElm, "link", currentText);
                } else if (tagName.equals("description")) {
                    createTextChild(currentItemElm, "description", currentText);
                } else if (tagName.equals("comments")) {
                    createTextChild(currentItemElm, "comments", currentText);
                }

                currentText = null;
            } else if (inChannel) {
                if (tagName.equals("title")) {
                    createTextChild(channelElm, "title", currentText);
                } else if (tagName.equals("link")) {
                    createTextChild(channelElm, "link", currentText);
                } else if (tagName.equals("description")) {
                    createTextChild(channelElm, "description", currentText);
                } else if (tagName.equals("managingEditor")) {
                    createTextChild(channelElm, "managingEditor", currentText);
                }

            }
        }

    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleError(String, int)
     */
    public void handleError(String errorMsg, int pos) {
        //		System.out.println("Error: " + errorMsg.toString());
    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleSimpleTag(Tag, MutableAttributeSet, int)
     */
    public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos) {
        //		System.out.println("SimpleTag: " + t.toString());
    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleStartTag(Tag, MutableAttributeSet, int)
     */
    public void handleStartTag(Tag t, MutableAttributeSet a, int pos) {
        //		System.out.println("Tag: " + t.toString());
        //		System.out.println("Atts List: " + a.toString());

        String tagName = t.toString();
        if (tagName.equals("item")) {
            inItem = true;
        } else if (tagName.equals("channel")) {
            inChannel = true;
        } else if (tagName.equals("rss")) {
            String version = (String) a.getAttribute(HTML.Attribute.VERSION);
            rootElm.setAttribute("version", version);
        } else if (tagName.equals("rdf")) {
            rootElm.setAttribute("version", "RDF");
        }
    }

    /**
     * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleText(char[], int)
     */
    public void handleText(char[] data, int pos) {
        currentText = new String(data);
        //		System.out.println("Text: " 
        //			+ currentText);
    }

}
