package org.methodize.nntprss.feed.publish;

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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.util.AppConstants;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: LiveJournalPublisher.java,v 1.5 2005/08/24 23:12:11 jasonbrome Exp $
 */

public class LiveJournalPublisher implements Publisher {

    private static final String METHOD_POSTEVENT = "LJ.XMLRPC.postevent";

    private static final String METHOD_LOGIN = "LJ.XMLRPC.login";

    private static final String STRUCT_USERNAME = "username";

    private static final String STRUCT_PASSWORD = "password";

    private static final String STRUCT_VER = "ver";

    private static final String STRUCT_EVENT = "event";

    private static final String STRUCT_LINEENDINGS = "lineendings";

    private static final String STRUCT_SUBJECT = "subject";

    private static final String STRUCT_YEAR = "year";

    private static final String STRUCT_MON = "mon";

    private static final String STRUCT_DAY = "day";

    private static final String STRUCT_HOUR = "hour";

    private static final String STRUCT_MIN = "min";

    private static final String STRUCT_CLIENTVERSION = "clientversion";

    /**
     * @see org.methodize.nntprss.feed.publish.Publisher#publish(Map, String)
     */
    public void publish(Map profile, Item content) throws PublisherException {

        // LJ.XMLRPC.postevent
        //
        try {
            XmlRpcClient xmlrpc =
                new XmlRpcClient((String) profile.get(PROP_PUBLISHER_URL));
            Vector params = new Vector();

            Map struct = new Hashtable();

            // username (string): Login for a Blogger user who has permission to post to the blog. 
            struct.put(STRUCT_USERNAME, profile.get(PROP_USERNAME));
            // password (string): Password for said username. 
            struct.put(STRUCT_PASSWORD, profile.get(PROP_PASSWORD));
            // LiveJournal API version
            struct.put(STRUCT_VER, "1");
            // Event - content of the post
            struct.put(STRUCT_EVENT, content.getDescription());
            // Line Endings - set to PC (default \r\n)
            struct.put(STRUCT_LINEENDINGS, "pc");
            // Subject / Title
            struct.put(STRUCT_SUBJECT, content.getTitle());

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());

            struct.put(STRUCT_YEAR, new Integer(cal.get(Calendar.YEAR)));
            struct.put(STRUCT_MON, new Integer(cal.get(Calendar.MONTH) + 1));
            struct.put(STRUCT_DAY, new Integer(cal.get(Calendar.DAY_OF_MONTH)));
            struct.put(STRUCT_HOUR, new Integer(cal.get(Calendar.HOUR_OF_DAY)));
            struct.put(STRUCT_MIN, new Integer(cal.get(Calendar.MINUTE)));

            params.add(struct);

            Map response = (Map) xmlrpc.execute(METHOD_POSTEVENT, params);

            // Discard response
            // anum - The key number used to calculate the public itemid ID number for URLs.
            // itemid - The unique number the server assigned to the post.

        } catch (Exception e) {
            throw new PublisherException(e);
        }
    }

    /**
     * @see org.methodize.nntprss.feed.publish.Publisher#validate(Map)
     */
    public void validate(Map profile) throws PublisherException {
        // LJ.XMLRPC.login takes the following parameters. 
        //
        try {
            XmlRpcClient xmlrpc =
                new XmlRpcClient((String) profile.get(PROP_PUBLISHER_URL));
            Vector params = new Vector();

            Map struct = new Hashtable();

            // username (string): Login for a Blogger user who has permission to post to the blog. 
            struct.put(STRUCT_USERNAME, profile.get(PROP_USERNAME));
            // password (string): Password for said username. 
            struct.put(STRUCT_PASSWORD, profile.get(PROP_PASSWORD));
            // LiveJournal API version
            struct.put(STRUCT_VER, "1");

            struct.put(
                STRUCT_CLIENTVERSION,
                "Java-nntprss/" + AppConstants.VERSION);

            params.add(struct);

            Map userInfo = (Map) xmlrpc.execute(METHOD_LOGIN, params);

            //TODO: think about message returned in userInfo

        } catch (Exception e) {
            throw new PublisherException(e);
        }

    }

    public static void main(String args[]) {
        // Tester...
        LiveJournalPublisher pub = new LiveJournalPublisher();

        Map profile = new HashMap();

        try {
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Username?");
            String userName = reader.readLine();

            System.out.println("Password?");
            String password = reader.readLine();

            System.out.println("Title?");
            String title = reader.readLine();

            System.out.println("Content?");
            String content = reader.readLine();

            profile.put(PROP_PASSWORD, password);
            profile.put(PROP_USERNAME, userName);

            profile.put(
                Publisher.PROP_PUBLISHER_URL,
                "http://www.livejournal.com/interface/xmlrpc");

            pub.validate(profile);

            Item newItem = new Item();
            newItem.setDescription(content);
            newItem.setTitle(title);

            pub.publish(profile, newItem);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
