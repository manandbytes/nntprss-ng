package org.methodize.nntprss.admin.search;

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

import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Syndic8Search.java,v 1.2 2004/01/04 21:12:42 jasonbrome Exp $
 */

public class Syndic8Search {

	private static final String METHOD_FINDFEEDS = "syndic8.FindFeeds";
	private static final String METHOD_GETFEEDINFO = "syndic8.GetFeedInfo";

	private static final String SYNDIC8_ENDPOINT = "http://www.syndic8.com/xmlrpc.php";

	public static final String FIELD_DESCRIPTION = "description";
	public static final String FIELD_FEED_URL = "dataurl";
	public static final String FIELD_HOMEPAGE_URL = "siteurl";
	public static final String FIELD_NAME = "sitename";

	private static final int MAX_RESULTS = 100;

	/**
	 * @see org.methodize.nntprss.feed.publish.Publisher#publish(Map, String)
	 */
	public Vector search(String searchTerm) throws Exception {

		// blogger.newPost takes the following parameters. All are required: 
		//
		XmlRpcClient xmlrpc =
			new XmlRpcClient(SYNDIC8_ENDPOINT);
			
		Vector params = new Vector();
		params.add(searchTerm);
		params.add(FIELD_NAME);

		Vector matchedFeedIds = (Vector)xmlrpc.execute(METHOD_FINDFEEDS, params);

		Vector feedId = new Vector();
		for(int i = 0; i < MAX_RESULTS && i < matchedFeedIds.size(); i++) {
			feedId.add(matchedFeedIds.get(i));
		}

		Vector fields = new Vector();
		fields.add(FIELD_HOMEPAGE_URL);
		fields.add(FIELD_FEED_URL);
		fields.add(FIELD_DESCRIPTION);
		fields.add(FIELD_NAME);

		params.clear();
		params.add(feedId);
		params.add(fields);

		Vector feedInfo = (Vector)xmlrpc.execute(METHOD_GETFEEDINFO, params);

		return feedInfo;
	}
	
	public static void main(String[] args) {
			Syndic8Search s = new Syndic8Search();
			try {
				s.search("nntp");
			} catch(Exception e) {
				e.printStackTrace();	
			}			
	}
}
