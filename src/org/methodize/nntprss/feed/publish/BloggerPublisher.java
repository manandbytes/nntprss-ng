package org.methodize.nntprss.feed.publish;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2005 Jason Brome.  All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.methodize.nntprss.feed.Item;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: BloggerPublisher.java,v 1.4 2005/02/13 22:02:16 jasonbrome Exp $
 */

public class BloggerPublisher implements Publisher {

    private static final String BLOGGER_APP_KEY =
        "0B18094ACF9546D113015FD8376930FA62A827";

    private static final String METHOD_NEWPOST = "blogger.newPost";

    private static final String METHOD_GETUSERINFO = "blogger.getUserInfo";

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
            // appkey (string): Unique identifier/passcode of the application sending the post. (See access info.) 
            params.addElement(BLOGGER_APP_KEY);

            // blogid (string): Unique identifier of the blog the post will be added to. 
            params.addElement(profile.get(PROP_BLOG_ID));

            // username (string): Login for a Blogger user who has permission to post to the blog. 
            params.addElement(profile.get(PROP_USERNAME));

            // password (string): Password for said username. 
            params.addElement(profile.get(PROP_PASSWORD));

            // content (string): Contents of the post. 
            params.addElement(content.getDescription());

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
        // blogger.newPost takes the following parameters. All are required: 
        //
        try {
            XmlRpcClient xmlrpc =
                new XmlRpcClient((String) profile.get(PROP_PUBLISHER_URL));
            Vector params = new Vector();
            // appkey (string): Unique identifier/passcode of the application sending the post. (See access info.) 
            params.addElement(BLOGGER_APP_KEY);

            // username (string): Login for a Blogger user who has permission to post to the blog. 
            params.addElement(profile.get(PROP_USERNAME));

            // password (string): Password for said username. 
            params.addElement(profile.get(PROP_PASSWORD));

            Map userInfo = (Map) xmlrpc.execute(METHOD_GETUSERINFO, params);

        } catch (Exception e) {
            throw new PublisherException(e);
        }

    }

    public static void main(String args[]) {
        // Tester...
        BloggerPublisher pub = new BloggerPublisher();

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

            System.out.println("Content?");
            String content = reader.readLine();

            System.out.println("Publish (true/false)?");
            String publishStr = reader.readLine();
            boolean publish = publishStr.equalsIgnoreCase("true");

            profile.put(PROP_BLOG_ID, blogId);
            profile.put(PROP_PASSWORD, password);
            profile.put(PROP_PUBLISH, new Boolean(publish));
            profile.put(PROP_USERNAME, userName);

            profile.put(
                Publisher.PROP_PUBLISHER_URL,
                "http://plant.blogger.com/api/RPC2");

            pub.validate(profile);

            Item newItem = new Item();
            newItem.setDescription(content);

            pub.publish(profile, newItem);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
