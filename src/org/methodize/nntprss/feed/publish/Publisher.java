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

import java.util.Map;

import org.methodize.nntprss.feed.Item;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Publisher.java,v 1.4 2005/02/13 22:02:16 jasonbrome Exp $
 */
public interface Publisher {

    public static final String PROP_PUBLISHER_URL = "publisher.url";
    public static final String PROP_BLOG_ID = "publisher.blogid";
    public static final String PROP_USERNAME = "publisher.username";
    public static final String PROP_PASSWORD = "publisher.password";
    public static final String PROP_PUBLISH = "publisher.publish";

    public void publish(Map profile, Item content) throws PublisherException;

    public void validate(Map profile) throws PublisherException;

}
