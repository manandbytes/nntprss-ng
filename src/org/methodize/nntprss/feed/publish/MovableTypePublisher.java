package org.methodize.nntprss.feed.publish;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.methodize.nntprss.feed.Item;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2004 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * web:   http://www.methodize.org/nntprss
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

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: MovableTypePublisher.java,v 1.2 2004/01/04 21:21:58 jasonbrome Exp $
 */

public class MovableTypePublisher implements Publisher {

	private static final String METHOD_NEWPOST = "metaWeblog.newPost";

	private static final String BLOGGER_APP_KEY =
		"0B18094ACF9546D113015FD8376930FA62A827";

	private static final String METHOD_GETCATEGORYLIST = "mt.getCategoryList";

	private static final String STRUCT_TITLE = "title";
	private static final String STRUCT_DESCRIPTION = "description";
	private static final String STRUCT_LINK = "link";

	/**
	 * @see org.methodize.nntprss.feed.publish.Publisher#publish(Map, String)
	 */
	public void publish(Map profile, Item content) throws PublisherException {

		// blogger.newPost takes the following parameters. All are required: 
		//
		try {
			XmlRpcClient xmlrpc =
				new XmlRpcClient((String) profile.get(PROP_PUBLISHER_URL));
			Vector params = new Vector();

			// blogid (string): Unique identifier of the blog the post will be added to. 
			params.addElement(profile.get(PROP_BLOG_ID));

			// username (string): Login for a Blogger user who has permission to post to the blog. 
			params.addElement(profile.get(PROP_USERNAME));

			// password (string): Password for said username. 
			params.addElement(profile.get(PROP_PASSWORD));

			// struct - title, link, description
			Map struct = new Hashtable();

			if (content.getTitle() != null) {
				struct.put(STRUCT_TITLE, content.getTitle());
			}

			if (content.getDescription() != null) {
				struct.put(STRUCT_DESCRIPTION, content.getDescription());
			}

			if (content.getLink() != null) {
				struct.put(STRUCT_LINK, content.getLink());
			}

			// content (string): Contents of the post. 
			params.addElement(struct);

			// publish (boolean): If true, the blog will be published immediately after the post is made. 
			params.addElement(
				Boolean.valueOf((String) profile.get(PROP_PUBLISH)));

			String postId = (String) xmlrpc.execute(METHOD_NEWPOST, params);

		} catch (Exception e) {
			throw new PublisherException(e);
		}
	}

	/**
	 * @see org.methodize.nntprss.feed.publish.Publisher#validate(Map)
	 */
	public void validate(Map profile) throws PublisherException {
		// MetaWebLog API does not currently expose an endpoint
		// that can be used for URL / username / password validation
		// Can't use blogger.getUserInfo, as not supported by Radio
		// Use metaWeblog.getCategories

		try {
			XmlRpcClient xmlrpc =
				new XmlRpcClient((String) profile.get(PROP_PUBLISHER_URL));
			Vector params = new Vector();
			// blogid (string): Unique identifier of the blog the post will be added to. 
			params.addElement(profile.get(PROP_BLOG_ID));

			// username (string): Login for a Blogger user who has permission to post to the blog. 
			params.addElement(profile.get(PROP_USERNAME));

			// password (string): Password for said username. 
			params.addElement(profile.get(PROP_PASSWORD));

			List categories = (List) xmlrpc.execute(METHOD_GETCATEGORYLIST, params);

		} catch (Exception e) {
			throw new PublisherException(e);
		}

	}

	public static void main(String args[]) {
		// Tester...
		MovableTypePublisher pub = new MovableTypePublisher();

		Map profile = new HashMap();

		try {
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Blog id?");
			String blogId = reader.readLine();

			System.out.println("Username?");
			String userName = reader.readLine();

			System.out.println("Password?");
			String password = reader.readLine();

			System.out.println("Description?");
			String description = reader.readLine();

			System.out.println("Publish (true/false)?");
			String publishStr = reader.readLine();
			boolean publish = publishStr.equalsIgnoreCase("true");

			profile.put(PROP_BLOG_ID, blogId);
			profile.put(PROP_PASSWORD, password);
			profile.put(PROP_PUBLISH, new Boolean(publish));
			profile.put(PROP_USERNAME, userName);

			profile.put(
				Publisher.PROP_PUBLISHER_URL,
				"http://localhost/mt/mt-xmlrpc.cgi");

			pub.validate(profile);

			Item content = new Item();
			content.setDescription(description);

			pub.publish(profile, content);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
