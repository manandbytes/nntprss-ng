package org.methodize.nntprss.feed.db;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Category;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: MySQLChannelDAO.java,v 1.11 2004/12/15 04:13:30 jasonbrome Exp $
 */

public class MySQLChannelDAO extends JdbcChannelDAO {

    private static final int DBVERSION = 5;
    private static final int MYSQL_FALSE = 0;
    private static final int MYSQL_TRUE = 1;

    private Logger log = Logger.getLogger(MySQLChannelDAO.class);

    public MySQLChannelDAO() {
    }

    protected void createTables() {
        Connection conn = null;
        Statement stmt = null;

        if (log.isInfoEnabled()) {
            log.info("Creating application database tables");
        }

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();

            stmt.executeUpdate(
                "CREATE TABLE "
                    + TABLE_CHANNELS
                    + "("
                    + "id int not null primary key auto_increment, "
                    + "url varchar(255) not null, "
                    + "name varchar(255) not null, "
                    + "author varchar(255), "
                    + "title varchar(255), "
                    + "link text, "
                    + "description text, "
                    + "lastArticle int not null, "
                    + "lastPolled timestamp, "
                    + "created timestamp, "
                    + "lastModified bigint, "
                    + "lastETag varchar(255), "
                    + "rssVersion varchar(8), "
                    + "enabled bit, "
                    + "postingEnabled bit, "
                    + "parseAtAllCost bit, "
                    + "publishAPI varchar(128), "
                    + "publishConfig text, "
                    + "managingEditor varchar(128), "
                    + "pollingInterval bigint not null, "
                    + "status int, "
                    + "expiration bigint, "
                    + "category int)");

            stmt.executeUpdate(
                "CREATE TABLE "
                    + TABLE_ITEMS
                    + "("
                    + "articleNumber int not null, "
                    + "channel int not null, "
                    + "title varchar(255), "
                    + "link text, "
                    + "description mediumtext, "
                    + "comments text, "
                    + "dtStamp timestamp, "
                    + "signature varchar(32), "
                    + "creator varchar(255), "
                    + "guid varchar(255), "
                    + "guidIsPermaLink bit)");

            stmt.executeUpdate(
                "CREATE INDEX fk_channel ON " + TABLE_ITEMS + " (channel)");
            stmt.executeUpdate(
            	"CREATE INDEX fk_signature ON items (signature)");

            stmt.executeUpdate(
                "CREATE TABLE "
                    + TABLE_CATEGORIES
                    + "("
                    + "id int not null primary key auto_increment, "
                    + "name varchar(255) not null, "
                    + "lastArticle int not null, "
                    + "created timestamp)");

            stmt.executeUpdate(
                "CREATE TABLE "
                    + TABLE_CATEGORYITEM
                    + "("
                    + "category int not null, "
                    + "articleNumber int not null, "
                    + "channel int not null, "
                    + "channelArticleNumber int not null)");

            stmt.executeUpdate(
                "CREATE TABLE "
                    + TABLE_CONFIG
                    + "("
                    + "pollingInterval bigint not null, "
                    + "proxyServer varchar(255), "
                    + "proxyPort int, "
                    + "proxyUserID varchar(255), "
                    + "proxyPassword varchar(255), "
                    + "contentType int, "
                    + "dbVersion int, "
                    + "nntpSecure bit, "
                    + "footnoteUrls bit, "
                    + "useProxy bit, "
                    + "observeHttp301 bit, "
                    + "hostName varchar(255))");

            stmt
                .executeUpdate(
                    "INSERT INTO "
                    + TABLE_CONFIG
                    + "(pollingInterval, contentType, dbVersion, nntpSecure, footnoteUrls, useProxy, observeHttp301, hostName) VALUES(60*60, "
                    + AppConstants.CONTENT_TYPE_MIXED
                    + ", "
                    + DBVERSION
                    + ", "
                    + MYSQL_FALSE
                    + ", "
                    + MYSQL_TRUE
                    + ", "
                    + MYSQL_FALSE // useProxy
            +", " + MYSQL_TRUE // observeHttp301
            +", '" + AppConstants.getCurrentHostName() + "' " + ")");

        } catch (SQLException se) {

            if (log.isEnabledFor(Priority.ERROR)) {
                log.error("Error creating application database tables", se);
            }
            throw new RuntimeException(
                "Error creating application tables - " + se.getMessage());

        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Finished creating application database tables");
        }

    }

    protected void populateInitialChannels(Document config) {
        Connection conn = null;
        PreparedStatement ps = null;

		if (!migrateJdbm() && !migrateHsql()) {

            if (log.isInfoEnabled()) {
                log.info("Loading channels");
            }

            try {
                conn =
                    DriverManager.getConnection(
                        JdbcChannelDAO.POOL_CONNECT_STRING);
                NodeList channelsList =
                    config.getDocumentElement().getElementsByTagName(
                        "channels");

                if (channelsList.getLength() > 0) {
                    Element channelsElm = (Element) channelsList.item(0);
                    NodeList channelList =
                        channelsElm.getElementsByTagName("channel");
                    if (channelList.getLength() > 0) {

                        ps =
                            conn.prepareStatement(
                                "INSERT INTO "
                                    + TABLE_CHANNELS
                                    + "(url, name, created, lastPolled, lastArticle, enabled, postingEnabled, parseAtAllCost, pollingInterval, status, expiration) "
                                    + "values(?, ?, ?, 0, ?, "
                                    + MYSQL_TRUE
                                    + ", "
                                    + MYSQL_FALSE
                                    + ", "
                                    + MYSQL_FALSE
                                    + ", 0, "
                                    + Channel.STATUS_OK
                                    + ", "
                                    + 
                                    + Channel.EXPIRATION_KEEP
                                    + ")");

                        for (int channelCount = 0;
                            channelCount < channelList.getLength();
                            channelCount++) {
                            Element channelElm =
                                (Element) channelList.item(channelCount);
                            String url = channelElm.getAttribute("url");
                            String name = channelElm.getAttribute("name");

                            int paramCount = 1;
                            ps.setString(paramCount++, url);
                            ps.setString(paramCount++, name);
                            ps.setTimestamp(
                                paramCount++,
                                new Timestamp(System.currentTimeMillis()));
                            // Last Article
                            ps.setInt(paramCount++, 0);
                            ps.executeUpdate();
                        }
                    }
                }

            } catch (SQLException se) {

                if (log.isEnabledFor(Priority.ERROR)) {
                    log.error("Error loading initial channels", se);
                }
                throw new RuntimeException(
                    "Error loading initial channels - " + se.getMessage());

            } finally {
                try {
                    if (ps != null)
                        ps.close();
                } catch (Exception e) {
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (Exception e) {
                }
            }

            if (log.isInfoEnabled()) {
                log.info("Finished loading initial channels");
            }
        }

    }

    protected void upgradeDatabase(int dbVersion) {
        Connection conn = null;
        Statement stmt = null;

        if (log.isInfoEnabled()) {
            log.info(
                "Upgrading database from db v"
                    + dbVersion
                    + " to db v"
                    + DBVERSION);
        }

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();

            switch (dbVersion) {
                // v0.1 updates
                case 0 :
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN contentType int");
                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CONFIG
                            + " SET contentType = "
                            + AppConstants.CONTENT_TYPE_MIXED);
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN dbVersion int");

                    // Channel
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN title varchar(255)");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN link blob");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN description blob");

                    // Items
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_ITEMS
                            + " ADD COLUMN comments blob");

                case 2 :
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN nntpSecure bit");
                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CONFIG
                            + " SET nntpSecure = "
                            + MYSQL_FALSE);

                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN proxyUserID varchar(255)");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN proxyPassword varchar(255)");

                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN enabled bit");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN postingEnabled bit");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN parseAtAllCost bit");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN publishAPI varchar(128)");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN publishConfig blob");

                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CHANNELS
                            + " SET enabled = "
                            + MYSQL_TRUE
                            + ", postingEnabled = "
                            + MYSQL_FALSE
                            + ", parseAtAllCost = '"
                            + MYSQL_FALSE);

                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN managingEditor varchar(128)");

                case 3 :
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN pollingInterval bigint");
                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CHANNELS
                            + " SET pollingInterval = 0");

                case 4 :
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN footnoteUrls bit");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN useProxy bit");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN observeHttp301 bit");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CONFIG
                            + " ADD COLUMN hostName varchar(255)");

                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_ITEMS
                            + " ADD COLUMN creator varchar(255)");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_ITEMS
                            + " ADD COLUMN guid varchar(255)");
                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_ITEMS
                            + " ADD COLUMN guidIsPermaLink bit");

                    stmt.executeUpdate(
                        "ALTER TABLE "
                            + TABLE_CHANNELS
                            + " ADD COLUMN status int");

                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CONFIG
                            + " SET footnoteUrls = "
                            + MYSQL_TRUE
                            + ", useProxy = "
                            + MYSQL_FALSE
                            + ", observeHttp301 = "
                            + MYSQL_TRUE
                            + ", hostName = '"
                            + AppConstants.getCurrentHostName()
                            + "'");

                    stmt.executeUpdate(
                        "CREATE INDEX fk_channel ON "
                            + TABLE_ITEMS
                            + " (channel)");

                default :
                    // Force re-poll of all channels after DB upgrade...
                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CONFIG
                            + " SET dbVersion = "
                            + DBVERSION);
                    stmt.executeUpdate(
                        "UPDATE "
                            + TABLE_CHANNELS
                            + " SET lastPolled = null, lastModified = null, lastETag = null");
            }

            if (log.isInfoEnabled()) {
                log.info("Successfully upgraded database.");
            }

        } catch (SQLException se) {
            throw new RuntimeException("Problem upgrading database" + se);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    public void addChannel(Channel channel) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Statement s = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "INSERT INTO "
                        + TABLE_CHANNELS
                        + "(url, name, lastArticle, created, enabled, postingEnabled, publishAPI, publishConfig, parseAtAllCost, pollingInterval, status, expiration) "
                        + "values(?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            int paramCount = 1;
            ps.setString(paramCount++, channel.getUrl());
            ps.setString(paramCount++, trim(channel.getName(), FIELD_CHANNEL_NAME_LENGTH));
            ps.setTimestamp(
                paramCount++,
                new Timestamp(channel.getCreated().getTime()));
            ps.setBoolean(paramCount++, channel.isEnabled());
            ps.setBoolean(paramCount++, channel.isPostingEnabled());
            ps.setString(paramCount++, channel.getPublishAPI());
            ps.setString(
                paramCount++,
                XMLHelper.stringMapToXML(channel.getPublishConfig()));
            ps.setBoolean(paramCount++, channel.isParseAtAllCost());
            ps.setLong(paramCount++, channel.getPollingIntervalSeconds());
            ps.setInt(paramCount++, channel.getStatus());
            ps.setLong(paramCount++, channel.getExpiration());
            ps.executeUpdate();

            ps.close();
            ps = null;

            s = conn.createStatement();
            rs = s.executeQuery("SELECT LAST_INSERT_ID()");

            if (rs != null) {
                if (rs.next()) {
                    channel.setId(rs.getInt(1));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (s != null)
                    s.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

    }

    public void addCategory(Category category) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Statement s = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "INSERT INTO "
                        + TABLE_CATEGORIES
                        + "(name, lastArticle, created) "
                        + "values(?, 0, ?)");

            int paramCount = 1;
            ps.setString(paramCount++, category.getName());
            ps.setTimestamp(
                paramCount++,
                new Timestamp(category.getCreated().getTime()));
            ps.executeUpdate();

            ps.close();
            ps = null;

            s = conn.createStatement();
            rs = s.executeQuery("SELECT LAST_INSERT_ID()");

            if (rs != null) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (s != null)
                    s.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#shutdown()
     */
    public void shutdown() {
        // TODO Auto-generated method stub
    }

}
