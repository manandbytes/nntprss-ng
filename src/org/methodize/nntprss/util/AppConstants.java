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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AppConstants.java,v 1.4 2003/01/22 05:10:38 jasonbrome Exp $
 */
public class AppConstants {

	public static final String NNTPRSS_CONFIGURATION_FILE =
		"nntprss-config.xml";
	public static final String VERSION = "0.2-alpha";
	
	public static final int OPEN_ENDED_RANGE = -1;

	public static final int CONTENT_TYPE_TEXT = 1;
	public static final int CONTENT_TYPE_HTML = 2;
	public static final int CONTENT_TYPE_MIXED = 3;

	private static DocumentBuilderFactory docBuilderFactory =
		DocumentBuilderFactory.newInstance();
		
	public static DocumentBuilder newDocumentBuilder()
		throws ParserConfigurationException {
		return docBuilderFactory.newDocumentBuilder();
	}
}
