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

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.DTDConstants;
import javax.swing.text.html.parser.DocumentParser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: LooseParser.java,v 1.1 2003/07/18 23:58:41 jasonbrome Exp $
 * 
 * 'Loose' Parser - when enabled, will parse those
 * not well-formed RSS/RDF xml documents on which the 
 * standard XML parser chokes.
 * 
 * This first version is an interesting kludge based 
 * upon the HTML parsing capabilities available within
 * Swing.
 * 
 * TODO: More tests on internationalization
 * 
 */
public class LooseParser {

	private static DTD dtd = null;

	static {
		try {
			dtd = DTD.getDTD("html32");
		} catch (IOException ie) {
		}

		dtd.getElement("description");
		javax.swing.text.html.parser.Element element = dtd.getElement("rss");
		element.getAttribute("version");

		dtd.getElement("rdf");
		dtd.getElement("channel");
		dtd.getElement("category");
		dtd.getElement("link");
		dtd.getElement("language");
		dtd.getElement("title");
		dtd.getElement("admin");
		dtd.getElement("item");

		dtd.defEntity("lt", DTDConstants.GENERAL, '<');
		dtd.defEntity("gt", DTDConstants.GENERAL, '>');
		dtd.defEntity("nbsp", DTDConstants.GENERAL, ' ');
		dtd.defEntity("amp", DTDConstants.GENERAL, '&');
		dtd.defEntity("quot", DTDConstants.GENERAL, '"');
		dtd.defEntity("apos", DTDConstants.GENERAL, '\'');
	}

	public static Document parse(InputStream is)
		throws IOException, ParserConfigurationException {

		DocumentBuilder db = AppConstants.newDocumentBuilder();

		Document doc = db.newDocument();
		Element rootElm = doc.createElement("rss");
		doc.appendChild(rootElm);
		Element channelElm = doc.createElement("channel");
		rootElm.appendChild(channelElm);

		Reader reader = new InputStreamReader(is);

		CharArrayWriter caw = new CharArrayWriter();

		// A little hack to use Swing's parser
		// This informs the parser that the content is within
		// the body, and therefore all text should be passed
		// to the callback.

		// TODO: Extract XML PI 
		caw.write("<html><body>");
		char[] buf = new char[1024];
		int charsRead = reader.read(buf);
		while (charsRead > -1) {
			if (charsRead > 0) {
				caw.write(buf, 0, charsRead);
			}
			charsRead = reader.read(buf);
		}
		caw.write("</body></html>");
		caw.flush();
		caw.close();
		reader.close();

		reader = new CharArrayReader(caw.toCharArray());

		DocumentParser docParser = new DocumentParser(dtd);
		docParser.parse(reader, new ParserCallback(doc), false);

		reader.close();

		return doc;

	}

	public static void main(String args[]) {
		try {
			Channel tstChannel = new Channel("test", "http://localhost/");
			List items = new ArrayList();

			InputStream is = new FileInputStream("c:\\test.xml");
			Document doc = parse(is);

			System.out.println(
				"Channel: "
					+ XMLHelper.getChildElementValue(
						(Element) doc
							.getDocumentElement()
							.getElementsByTagName(
							"channel").item(
							0),
						"title"));
			System.out.println(
				"Items: "
					+ doc
						.getDocumentElement()
						.getElementsByTagName("item")
						.getLength());
			System.out.println(
				"Version: "
					+ (
						(Element) doc
							.getDocumentElement()
							.getElementsByTagName(
							"channel").item(
							0)).getAttribute(
						"version"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("finished...");
	}

}
