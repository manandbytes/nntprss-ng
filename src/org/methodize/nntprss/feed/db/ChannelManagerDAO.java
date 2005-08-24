package org.methodize.nntprss.feed.db;

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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelManagerDAO.java,v 1.7 2005/08/24 23:12:11 jasonbrome Exp $
 */
public class ChannelManagerDAO {

    private Logger log = Logger.getLogger(ChannelManagerDAO.class);

    private static final ChannelManagerDAO channelManagerDAO =
        new ChannelManagerDAO();

    // The actual DB specific channel DAO logic instance
    private boolean initialized = false;

    private ChannelDAO channelDAO = null;

    private ChannelManagerDAO() {
    }

    public static ChannelManagerDAO getChannelManagerDAO(Document config) {
        channelManagerDAO.initialize(config);
        return channelManagerDAO;
    }

    public static ChannelManagerDAO getChannelManagerDAO() {
        if (!channelManagerDAO.initialized) {
            throw new RuntimeException("Channel Manager was not initialized");
        }
        return channelManagerDAO;
    }

    public ChannelDAO getChannelDAO() {
        return channelDAO;
    }

    protected void upgradeDatabase(int dbVersion) {
    }

    protected void createTables(Document config) {
    }

    public void initialize(Document config) {
        Element rootElm = config.getDocumentElement();
        Element dbConfig = (Element) rootElm.getElementsByTagName("db").item(0);
        String daoClass = dbConfig.getAttribute("daoClass");

        if (daoClass != null && daoClass.length() > 0) {
            try {
                channelDAO = (ChannelDAO) Class.forName(daoClass).newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                    "Problem initializing database class "
                        + daoClass
                        + ", exception="
                        + e);
            }
        } else {
            // Default to JDBM
            channelDAO = new JdbmChannelDAO();
        }
        initialized = true;
    }

}
