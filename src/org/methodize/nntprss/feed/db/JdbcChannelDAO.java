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

import java.net.MalformedURLException;
import java.sql.*;
import java.util.*;
import java.util.Date;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Category;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: JdbcChannelDAO.java,v 1.6 2004/03/27 02:12:48 jasonbrome Exp $
 */

public abstract class JdbcChannelDAO extends ChannelDAO {

    public static final String POOL_CONNECT_STRING =
        "jdbc:apache:commons:dbcp:nntprss";

    static final String TABLE_CATEGORIES = "categories";
    static final String TABLE_CATEGORYITEM = "categoryitem";
    static final String TABLE_CHANNELS = "channels";
    static final String TABLE_CONFIG = "config";
    static final String TABLE_ITEMS = "items";

    public Map loadCategories() {
        Map categories = new TreeMap();
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        if (log.isInfoEnabled()) {
            log.info("Loading categories");
        }

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM " + TABLE_CATEGORIES);
            if (rs != null) {
                ps =
                    conn.prepareStatement(
                        "SELECT MIN(articleNumber), COUNT(articleNumber) FROM "
                            + TABLE_CATEGORYITEM
                            + " WHERE category = ?");
                while (rs.next()) {
                    Category category = new Category();
                    category.setName(rs.getString("name"));
                    category.setId(rs.getInt("id"));
                    category.setLastArticleNumber(rs.getInt("lastArticle"));
                    category.setCreated(rs.getTimestamp("created"));

                    ps.setInt(1, category.getId());
                    rs2 = ps.executeQuery();
                    if (rs2 != null) {
                        if (rs2.next()) {
                            int firstArticleNumber = rs2.getInt(1);
                            if (firstArticleNumber != 0) {
                                category.setFirstArticleNumber(
                                    firstArticleNumber);
                            } else {
                                category.setFirstArticleNumber(1);
                            }

                            category.setTotalArticles(rs2.getInt(2));
                        }
                        rs2.close();
                    }

                    categories.put(category.getName(), category);
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (rs2 != null)
                    rs2.close();
            } catch (SQLException se) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Loaded " + categories.size() + " categories");
        }

        return categories;
    }

    public Map loadChannels(ChannelManager channelManager) {
        Map channels = new TreeMap();
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        if (log.isInfoEnabled()) {
            log.info("Loading channel configuration");
        }

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM " + TABLE_CHANNELS);
            if (rs != null) {
                ps =
                    conn.prepareStatement(
                        "SELECT MIN(articleNumber), COUNT(articleNumber) FROM "
                            + TABLE_ITEMS
                            + " WHERE channel = ?");
                while (rs.next()) {
                    String name = rs.getString("name");
                    String url = rs.getString("url");
                    Channel channel = null;
                    try {
                        channel = new Channel(name, url);
                    } catch (MalformedURLException me) {
                        System.out.println(name + " - Bad url: " + url);
                        // Skip this entry
                        continue;
                    }
                    channel.setId(rs.getInt("id"));
                    channel.setAuthor(rs.getString("author"));
                    channel.setLastArticleNumber(rs.getInt("lastArticle"));
                    channel.setCreated(rs.getTimestamp("created"));
                    channel.setTitle(rs.getString("title"));
                    channel.setLink(rs.getString("link"));
                    channel.setDescription(rs.getString("description"));

                    ps.setInt(1, channel.getId());
                    rs2 = ps.executeQuery();
                    if (rs2 != null) {
                        if (rs2.next()) {
                            int firstArticleNumber = rs2.getInt(1);
                            if (firstArticleNumber != 0) {
                                channel.setFirstArticleNumber(
                                    firstArticleNumber);
                            } else {
                                channel.setFirstArticleNumber(1);
                            }

                            channel.setTotalArticles(rs2.getInt(2));
                        }
                        rs2.close();
                    }

                    channel.setLastPolled(rs.getTimestamp("lastPolled"));
                    channel.setLastModified(rs.getLong("lastModified"));
                    channel.setLastETag(rs.getString("lastETag"));
                    channel.setRssVersion(rs.getString("rssVersion"));
                    channel.setEnabled(rs.getBoolean("enabled"));
                    channel.setPostingEnabled(rs.getBoolean("postingEnabled"));
                    channel.setPublishAPI(rs.getString("publishAPI"));
                    channel.setPublishConfig(
                        XMLHelper.xmlToStringHashMap(
                            rs.getString("publishConfig")));

                    channel.setParseAtAllCost(rs.getBoolean("parseAtAllCost"));
                    channel.setManagingEditor(rs.getString("managingEditor"));

                    channel.setPollingIntervalSeconds(
                        rs.getLong("pollingInterval"));

                    channel.setStatus(rs.getInt("status"));
                    channel.setExpiration(rs.getLong("expiration"));

                    channels.put(channel.getName(), channel);

                    int categoryId = rs.getInt("category");

                    if (categoryId != 0) {
                        Category category =
                            channelManager.categoryById(categoryId);
                        category.getChannels().put(
                            new Integer(channel.getId()),
                            channel);
                        channel.setCategory(category);
                    }
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (rs2 != null)
                    rs2.close();
            } catch (SQLException se) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Loaded " + channels.size() + " channels");
        }

        return channels;
    }

    public void loadConfiguration(NNTPServer nntpServer) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();
            rs =
                stmt.executeQuery(
                    "SELECT contentType, nntpSecure, footnoteUrls, hostName FROM "
                        + TABLE_CONFIG);
            if (rs != null) {
                if (rs.next()) {
                    nntpServer.setContentType(rs.getInt("contentType"));
                    nntpServer.setSecure(rs.getBoolean("nntpSecure"));
                    nntpServer.setFootnoteUrls(rs.getBoolean("footnoteUrls"));
                    nntpServer.setHostName(rs.getString("hostName"));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(
                "Problem loading NNTP Server configuration" + se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
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

    public void loadConfiguration(ChannelManager channelManager) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM " + TABLE_CONFIG);
            if (rs != null) {
                if (rs.next()) {
                    channelManager.setPollingIntervalSeconds(
                        rs.getLong("pollingInterval"));
                    channelManager.setProxyServer(rs.getString("proxyServer"));
                    channelManager.setProxyPort(rs.getInt("proxyPort"));
                    channelManager.setProxyUserID(rs.getString("proxyUserID"));
                    channelManager.setProxyPassword(
                        rs.getString("proxyPassword"));
                    channelManager.setUseProxy(rs.getBoolean("useProxy"));
                    channelManager.setObserveHttp301(
                        rs.getBoolean("observeHttp301"));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(
                "Problem loading Channel manager configuration" + se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
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

    private void initializeDatabasePool(Document config) throws Exception {
        Element rootElm = config.getDocumentElement();
        Element dbConfig = (Element) rootElm.getElementsByTagName("db").item(0);
        String connectString = dbConfig.getAttribute("connect");

        if (log.isInfoEnabled()) {
            log.info("Initializing JDBC, connection string = " + connectString);
        }

        ObjectPool connectionPool = new GenericObjectPool(null);

        String dbDriver = dbConfig.getAttribute("driverClass");
        if (dbDriver != null && dbDriver.length() > 0) {
            Class.forName(dbDriver);
        } else {
            // Default to HSSQLDB
            Class.forName("org.hsqldb.jdbcDriver");
        }

        String user = dbConfig.getAttribute("user");
        String password = dbConfig.getAttribute("password");
        if (user == null) {
            user = "sa";
        }
        if (password == null) {
            password = "";
        }

        ConnectionFactory connectionFactory =
            new DriverManagerConnectionFactory(connectString, user, password);

        //
        // Now we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        //
        PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(
                connectionFactory,
                connectionPool,
                null,
                null,
                false,
                true);

        //
        // Finally, we create the PoolingDriver itself...
        //
        PoolingDriver driver = new PoolingDriver();

        //
        // ...and register our pool with it.
        //
        driver.registerPool("nntprss", connectionPool);
    }

    public void initialize(Document config) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        boolean createTables = false;

        initializeDatabasePool(config);

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            stmt = conn.createStatement();
            try {
                rs = stmt.executeQuery("SELECT * FROM " + TABLE_CONFIG);
                if (rs != null) {
                    if (rs.next()) {
                        int dbVersion = rs.getInt("dbVersion");
                        if (dbVersion < DBVERSION) {
                            upgradeDatabase(dbVersion);
                        }
                    }
                }
            } catch (SQLException e) {
                if (e.getErrorCode() == -org.hsqldb.Trace.COLUMN_NOT_FOUND) {
                    // Pre-version db, upgrade database
                    upgradeDatabase(0);
                } else {
                    // Our tables don't exist, so let's create them...
                    createTables = true;
                }
            }
        } catch (SQLException se) {

            throw new RuntimeException(
                "Problem initializing application database " + se);

        } catch (DbcpException de) {
            if (de.getCause() != null
                && de.getCause() instanceof SQLException) {
                SQLException se = (SQLException) de.getCause();
                // McKoi DB
                if (se
                    .getMessage()
                    .startsWith("Can not find a database to start.")) {
                    createTables = true;
                }
            }
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
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

        if (createTables) {
            createTables();
            populateInitialChannels(config);
        }
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#addChannelToCategory(org.methodize.nntprss.feed.Channel, org.methodize.nntprss.feed.Category)
     */
    public void addChannelToCategory(Channel channel, Category category) {
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "SELECT articleNumber FROM "
                        + TABLE_ITEMS
                        + " WHERE channel = ?");
            ps2 =
                conn.prepareStatement(
                    "INSERT INTO "
                        + TABLE_CATEGORYITEM
                        + " (category, articleNumber, channel, channelArticleNumber) VALUES(?,?,?,?)");

            int paramCount = 1;
            ps2.setInt(paramCount++, category.getId());
            int articleNumberIdx = paramCount++;
            ps2.setInt(paramCount++, channel.getId());
            int chlArticleNumberIdx = paramCount++;

            paramCount = 1;
            ps.setInt(paramCount++, channel.getId());
            rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    ps2.setInt(articleNumberIdx, category.nextArticleNumber());
                    ps2.setInt(chlArticleNumberIdx, rs.getInt(1));
                    ps2.executeUpdate();
                }
            }
            rs.close();
            rs = null;
            ps.close();

            ps =
                conn.prepareStatement(
                    "SELECT count(articleNumber) FROM "
                        + TABLE_CATEGORYITEM
                        + " where category = ?");
            paramCount = 1;
            ps.setInt(paramCount++, category.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                category.setTotalArticles(rs.getInt(1));
            }

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (ps2 != null)
                    ps2.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }
    }

    public void updateCategory(Category category) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "UPDATE category "
                        + "SET name = ?, "
                        + "lastArticle = ?, "
                        + "WHERE id = ?");

            int paramCount = 1;
            ps.setString(paramCount++, category.getName());
            ps.setInt(paramCount++, category.getLastArticleNumber());
            ps.setInt(paramCount++, category.getId());
            ps.executeUpdate();

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

    }

    public void updateChannel(Channel channel) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "UPDATE "
                        + TABLE_CHANNELS
                        + " "
                        + "SET author = ?, name = ?, url = ?, "
                        + "title = ?, link = ?, description = ?, "
                        + "lastArticle = ?, "
                        + "lastPolled = ?, lastModified = ?, lastETag = ?, rssVersion = ?, "
                        + "enabled = ?, "
                        + "postingEnabled = ?, "
                        + "publishAPI = ?, "
                        + "publishConfig = ?, "
                        + "parseAtAllCost = ?, "
                        + "managingEditor = ?, "
                        + "pollingInterval = ?, "
                        + "status = ?, "
                        + "expiration = ?, "
                        + "category = ? "
                        + "WHERE id = ?");

            int paramCount = 1;
            ps.setString(paramCount++, channel.getAuthor());
            ps.setString(paramCount++, channel.getName());
            ps.setString(paramCount++, channel.getUrl());
            ps.setString(paramCount++, channel.getTitle());
            ps.setString(paramCount++, channel.getLink());
            ps.setString(paramCount++, channel.getDescription());
            ps.setInt(paramCount++, channel.getLastArticleNumber());

            if (channel.getLastPolled() != null) {
                ps.setTimestamp(
                    paramCount++,
                    new Timestamp(channel.getLastPolled().getTime()));
            } else {
                ps.setNull(paramCount++, java.sql.Types.TIMESTAMP);
            }

            ps.setLong(paramCount++, channel.getLastModified());
            ps.setString(paramCount++, channel.getLastETag());
            ps.setString(paramCount++, channel.getRssVersion());
            //			ps.setBoolean(paramCount++, channel.isHistorical());
            ps.setBoolean(paramCount++, channel.isEnabled());
            ps.setBoolean(paramCount++, channel.isPostingEnabled());
            ps.setString(paramCount++, channel.getPublishAPI());
            ps.setString(
                paramCount++,
                XMLHelper.stringMapToXML(channel.getPublishConfig()));
            ps.setBoolean(paramCount++, channel.isParseAtAllCost());

            ps.setString(paramCount++, channel.getManagingEditor());

            ps.setLong(paramCount++, channel.getPollingIntervalSeconds());
            ps.setInt(paramCount++, channel.getStatus());
            ps.setLong(paramCount++, channel.getExpiration());
            int categoryId = 0;
            if (channel.getCategory() != null) {
                categoryId = channel.getCategory().getId();
            }
            ps.setInt(paramCount++, categoryId);

            ps.setInt(paramCount++, channel.getId());
            ps.executeUpdate();

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

    }

    public void deleteChannel(Channel channel) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);

            ps =
                conn.prepareStatement(
                    "DELETE FROM " + TABLE_ITEMS + " WHERE channel = ?");

            int paramCount = 1;
            ps.setInt(paramCount++, channel.getId());
            ps.executeUpdate();
            ps.close();

            ps =
                conn.prepareStatement(
                    "DELETE FROM " + TABLE_CHANNELS + " WHERE id = ?");

            paramCount = 1;
            ps.setInt(paramCount++, channel.getId());
            ps.executeUpdate();

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

    }

    private Item readItemFromRS(ResultSet rs, Channel channel)
        throws SQLException {
        Item item =
            new Item(rs.getInt("articleNumber"), rs.getString("signature"));
        item.setChannel(channel);
        item.setDate(rs.getTimestamp("dtStamp"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setComments(rs.getString("comments"));
        item.setLink(rs.getString("link"));
        item.setCreator(rs.getString("creator"));
        item.setGuid(rs.getString("guid"));
        item.setGuidIsPermaLink(rs.getBoolean("guidIsPermaLink"));

        return item;
    }

    public Item loadItem(Channel channel, int articleNumber) {
        Item item = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "SELECT * FROM "
                        + TABLE_ITEMS
                        + " WHERE articleNumber = ? AND channel = ?");
            int paramCount = 1;
            ps.setInt(paramCount++, articleNumber);
            ps.setInt(paramCount++, channel.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                if (rs.next()) {
                    item = readItemFromRS(rs, channel);
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return item;
    }

    public Item loadNextItem(Channel channel, int relativeArticleNumber) {
        return loadRelativeItem(
            channel,
            relativeArticleNumber,
            "SELECT TOP 1 * FROM "
                + TABLE_ITEMS
                + " WHERE articleNumber > ? AND channel = ? ORDER BY articleNumber");
    }

    public Item loadPreviousItem(Channel channel, int relativeArticleNumber) {
        return loadRelativeItem(
            channel,
            relativeArticleNumber,
            "SELECT TOP 1 * FROM "
                + TABLE_ITEMS
                + " WHERE articleNumber < ? AND channel = ? ORDER BY articleNumber DESC");
    }

    private Item loadRelativeItem(
        Channel channel,
        int previousArticleNumber,
        String sql) {
        Item item = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps = conn.prepareStatement(sql);
            int paramCount = 1;
            ps.setInt(paramCount++, previousArticleNumber);
            ps.setInt(paramCount++, channel.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                if (rs.next()) {
                    item = readItemFromRS(rs, channel);
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return item;
    }

    public Item loadItem(Channel channel, String signature) {
        Item item = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "SELECT * FROM "
                        + TABLE_ITEMS
                        + " WHERE signature = ? AND channel = ?");
            int paramCount = 1;
            ps.setString(paramCount++, signature);
            ps.setInt(paramCount++, channel.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                if (rs.next()) {
                    item = readItemFromRS(rs, channel);
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return item;
    }

    public List loadItems(
        Channel channel,
        int[] articleRange,
        boolean onlyHeaders,
        int limit) {
        List items = new ArrayList();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE
                && articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
                ps =
                    conn.prepareStatement(
                        "SELECT * FROM "
                            + TABLE_ITEMS
                            + " WHERE articleNumber >= ? and articleNumber <= ? AND channel = ? ORDER BY articleNumber");
            } else if (
                articleRange[0] == AppConstants.OPEN_ENDED_RANGE
                    && articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
                ps =
                    conn.prepareStatement(
                        "SELECT * FROM "
                            + TABLE_ITEMS
                            + " WHERE articleNumber <= ? AND channel = ? ORDER BY articleNumber");
            } else if (
                articleRange[1] == AppConstants.OPEN_ENDED_RANGE
                    && articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
                ps =
                    conn.prepareStatement(
                        "SELECT * FROM "
                            + TABLE_ITEMS
                            + " WHERE articleNumber >= ? AND channel = ? ORDER BY articleNumber");
            } else {
                ps =
                    conn.prepareStatement(
                        "SELECT * FROM "
                            + TABLE_ITEMS
                            + " WHERE channel = ? ORDER BY articleNumber");
            }

            int paramCount = 1;

            if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
                ps.setInt(paramCount++, articleRange[0]);
            }

            if (articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
                ps.setInt(paramCount++, articleRange[1]);
            }

            ps.setInt(paramCount++, channel.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Item item =
                        new Item(
                            rs.getInt("articleNumber"),
                            rs.getString("signature"));
                    item.setChannel(channel);
                    item.setDate(rs.getTimestamp("dtStamp"));
                    item.setTitle(rs.getString("title"));
                    item.setCreator(rs.getString("creator"));

                    if (!onlyHeaders) {
                        item.setDescription(rs.getString("description"));
                        item.setLink(rs.getString("link"));
                        item.setComments(rs.getString("comments"));
                        item.setGuid(rs.getString("guid"));
                        item.setGuidIsPermaLink(
                            rs.getBoolean("guidIsPermaLink"));
                    }
                    items.add(item);
                    if (limit != LIMIT_NONE && items.size() == limit) {
                        // Break if maximum items returned...
                        break;
                    }
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return items;
    }

    public List loadArticleNumbers(Channel channel) {

        List articleNumbers = new ArrayList();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "SELECT articleNumber FROM "
                        + TABLE_ITEMS
                        + " WHERE channel = ? ORDER BY articleNumber");

            int paramCount = 1;
            ps.setInt(paramCount++, channel.getId());

            rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    articleNumbers.add(new Integer(rs.getInt("articleNumber")));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return articleNumbers;
    }

    public void saveItem(Item item) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "INSERT INTO "
                        + TABLE_ITEMS
                        + "(articleNumber, channel, title, link, description, comments, dtStamp, signature, creator, guid, guidIsPermaLink) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?)");

            int paramCount = 1;
            ps.setInt(paramCount++, item.getArticleNumber());
            ps.setInt(paramCount++, item.getChannel().getId());
            ps.setString(paramCount++, item.getTitle());
            ps.setString(paramCount++, item.getLink());
            ps.setString(paramCount++, item.getDescription());
            ps.setString(paramCount++, item.getComments());
            ps.setTimestamp(
                paramCount++,
                new Timestamp(item.getDate().getTime()));
            ps.setString(paramCount++, item.getSignature());
            ps.setString(paramCount++, item.getCreator());
            ps.setString(paramCount++, item.getGuid());
            ps.setBoolean(paramCount++, item.isGuidIsPermaLink());
            ps.executeUpdate();

            // Update associated category...
            if (item.getChannel().getCategory() != null) {
                ps.close();
                ps =
                    conn.prepareStatement(
                        "INSERT INTO "
                            + TABLE_CATEGORYITEM
                            + "(category, articleNumber, channel, channelArticleNumber) values(?,?,?,?)");
                paramCount = 1;
                ps.setInt(
                    paramCount++,
                    item.getChannel().getCategory().getId());
                ps.setInt(
                    paramCount++,
                    item.getChannel().getCategory().nextArticleNumber());
                ps.setInt(paramCount++, item.getChannel().getId());
                ps.setInt(paramCount++, item.getArticleNumber());
                ps.executeUpdate();
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
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }
    }

    public void saveConfiguration(ChannelManager channelManager) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "UPDATE "
                        + TABLE_CONFIG
                        + " "
                        + "SET pollingInterval = ?, "
                        + "proxyServer = ?, "
                        + "proxyPort = ?, "
                        + "proxyUserID = ?, "
                        + "proxyPassword = ?, "
                        + "useProxy = ?, "
                        + "observeHttp301 = ?");

            int paramCount = 1;
            ps.setLong(
                paramCount++,
                channelManager.getPollingIntervalSeconds());
            ps.setString(paramCount++, channelManager.getProxyServer());
            ps.setInt(paramCount++, channelManager.getProxyPort());
            ps.setString(paramCount++, channelManager.getProxyUserID());
            ps.setString(paramCount++, channelManager.getProxyPassword());
            ps.setBoolean(paramCount++, channelManager.isUseProxy());
            ps.setBoolean(paramCount++, channelManager.isObserveHttp301());
            ps.executeUpdate();

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

    }

    public void saveConfiguration(NNTPServer nntpServer) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "UPDATE "
                        + TABLE_CONFIG
                        + " "
                        + "SET contentType = ?, nntpSecure = ?, footnoteUrls = ?, hostName = ?");

            int paramCount = 1;
            ps.setInt(paramCount++, nntpServer.getContentType());
            ps.setBoolean(paramCount++, nntpServer.isSecure());
            ps.setBoolean(paramCount++, nntpServer.isFootnoteUrls());
            ps.setString(paramCount++, nntpServer.getHostName());
            ps.executeUpdate();

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
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
     * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteExpiredItems(org.methodize.nntprss.feed.Channel, java.util.Set)
     */
    public void deleteExpiredItems(
        Channel channel,
        Set currentItemSignatures) {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Date expirationDate =
            new Date(System.currentTimeMillis() - channel.getExpiration());

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);

            if (channel.getCategory() != null) {
                ps =
                    conn.prepareStatement(
                        "SELECT articleNumber FROM "
                            + TABLE_ITEMS
                            + " WHERE channel = ? AND dtStamp < ?");
                int paramCount = 1;
                ps.setInt(paramCount++, channel.getId());
                ps.setTimestamp(
                    paramCount++,
                    new java.sql.Timestamp(expirationDate.getTime()));
                rs = ps.executeQuery();
                List expiredIds = new ArrayList();
                while (rs.next()) {
                    expiredIds.add(new Integer(rs.getInt(1)));
                }

                // Delete category items				
                StringBuffer stBuf =
                    new StringBuffer(
                        "DELETE FROM "
                            + TABLE_CATEGORYITEM
                            + " WHERE channel = ? AND category = ? AND articleNumber IN (");

                // Create question marks for signature parameters
                for (int i = 0; i < expiredIds.size(); i++) {
                    if (i > 0) {
                        stBuf.append(',');
                    }
                    stBuf.append('?');
                }

                stBuf.append(")");
                ps = conn.prepareStatement(stBuf.toString());
                paramCount = 1;
                ps.setInt(paramCount++, channel.getId());
                ps.setInt(paramCount++, channel.getCategory().getId());

                for (int i = 0; i < expiredIds.size(); i++) {
                    ps.setInt(
                        paramCount++,
                        ((Integer) expiredIds.get(i)).intValue());
                }

                ps.executeUpdate();

                // Delete items				
                stBuf =
                    new StringBuffer(
                        "DELETE FROM "
                            + TABLE_ITEMS
                            + " WHERE channel = ? AND articleNumber IN (");

                // Create question marks for signature parameters
                for (int i = 0; i < expiredIds.size(); i++) {
                    if (i > 0) {
                        stBuf.append(',');
                    }
                    stBuf.append('?');
                }

                stBuf.append(")");
                ps = conn.prepareStatement(stBuf.toString());
                paramCount = 1;
                ps.setInt(paramCount++, channel.getId());

                for (int i = 0; i < expiredIds.size(); i++) {
                    ps.setInt(
                        paramCount++,
                        ((Integer) expiredIds.get(i)).intValue());
                }

                ps.executeUpdate();

            } else {
                ps =
                    conn.prepareStatement(
                        "DELETE FROM "
                            + TABLE_ITEMS
                            + " WHERE channel = ? AND dtStamp < ?");
                int paramCount = 1;
                ps.setInt(paramCount++, channel.getId());
                ps.setTimestamp(
                    paramCount++,
                    new java.sql.Timestamp(expirationDate.getTime()));
                ps.executeUpdate();

            }

            // Need to reset first article number...
            // TODO: only really need to do this if first article number is not in set...
            ps =
                conn.prepareStatement(
                    "SELECT MIN(articleNumber) as firstArticleNumber FROM "
                        + TABLE_ITEMS
                        + " WHERE channel = ?");
            int paramCount = 1;

            ps.setInt(paramCount++, channel.getId());

            rs = ps.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    int firstArticle = rs.getInt("firstArticleNumber");
                    if (firstArticle == 0) {
                        // Have yet to sync any articles, so first article number 
                        // will be 1
                        if (channel.getLastArticleNumber() == 0) {
                            channel.setFirstArticleNumber(1);
                        } else {
                            channel.setFirstArticleNumber(
                                channel.getLastArticleNumber());
                        }
                    } else {
                        channel.setFirstArticleNumber(firstArticle);
                    }
                }

            }

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }
    }

    public void deleteItemsNotInSet(Channel channel, Set itemSignatures) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);

            StringBuffer stBuf =
                new StringBuffer(
                    "DELETE FROM "
                        + TABLE_ITEMS
                        + " WHERE channel = ? AND signature NOT IN (");

            // Create question marks for signature parameters
            for (int i = 0; i < itemSignatures.size(); i++) {
                if (i > 0) {
                    stBuf.append(',');
                }
                stBuf.append('?');
            }

            stBuf.append(")");
            ps = conn.prepareStatement(stBuf.toString());

            int paramCount = 1;
            ps.setInt(paramCount++, channel.getId());

            Iterator sigIter = itemSignatures.iterator();
            while (sigIter.hasNext()) {
                ps.setString(paramCount++, (String) sigIter.next());
            }

            ps.executeUpdate();

            // Need to reset first article number...
            // TODO: only really need to do this if first article number is not in set...
            ps =
                conn.prepareStatement(
                    "SELECT MIN(articleNumber) as firstArticleNumber FROM "
                        + TABLE_ITEMS
                        + " WHERE channel = ?");
            paramCount = 1;

            ps.setInt(paramCount++, channel.getId());

            rs = ps.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    int firstArticle = rs.getInt("firstArticleNumber");
                    if (firstArticle == 0) {
                        // Have yet to sync any articles, so first article number 
                        // will be 1
                        if (channel.getLastArticleNumber() == 0) {
                            channel.setFirstArticleNumber(1);
                        } else {
                            channel.setFirstArticleNumber(
                                channel.getLastArticleNumber());
                        }
                    } else {
                        channel.setFirstArticleNumber(firstArticle);
                    }
                }

            }

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }
    }

    public Set findNewItemSignatures(Channel channel, Set itemSignatures) {
        Set newSignatures = new HashSet();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int channelId = channel.getId();

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            StringBuffer stBuf =
                new StringBuffer(
                    "SELECT signature FROM "
                        + TABLE_ITEMS
                        + " WHERE channel = ? AND signature IN (");

            // Create question marks for signature parameters
            for (int i = 0; i < itemSignatures.size(); i++) {
                if (i > 0) {
                    stBuf.append(',');
                }
                stBuf.append('?');
            }

            stBuf.append(")");
            ps = conn.prepareStatement(stBuf.toString());

            int paramCount = 1;
            ps.setInt(paramCount++, channelId);

            Iterator sigIter = itemSignatures.iterator();
            while (sigIter.hasNext()) {
                ps.setString(paramCount++, (String) sigIter.next());
            }

            rs = ps.executeQuery();

            // Generate the list of existing signatures...
            Set currentSignatures = new HashSet();
            if (rs != null) {
                while (rs.next()) {
                    currentSignatures.add(rs.getString("signature"));
                }
            }

            // Perform set arithmatic to discover new items
            newSignatures.addAll(itemSignatures);
            newSignatures.removeAll(currentSignatures);

        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return newSignatures;
    }

    public abstract void addCategory(Category category);
    public abstract void addChannel(Channel channel);

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadArticleNumbers(org.methodize.nntprss.feed.Category)
     */
    public List loadArticleNumbers(Category category) {
        List articleNumbers = new ArrayList();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "SELECT articleNumber FROM "
                        + TABLE_CATEGORYITEM
                        + " WHERE category = ? ORDER BY articleNumber");

            int paramCount = 1;
            ps.setInt(paramCount++, category.getId());

            rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    articleNumbers.add(new Integer(rs.getInt("articleNumber")));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return articleNumbers;
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#removeChannelFromCategory(org.methodize.nntprss.feed.Channel, org.methodize.nntprss.feed.Category)
     */
    public void removeChannelFromCategory(Channel channel, Category category) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);

            ps =
                conn.prepareStatement(
                    "DELETE FROM "
                        + TABLE_CATEGORYITEM
                        + " WHERE channel = ? and category = ?");

            int paramCount = 1;
            ps.setInt(paramCount++, channel.getId());
            ps.setInt(paramCount++, category.getId());
            ps.executeUpdate();
            ps.close();

            ps =
                conn.prepareStatement(
                    "SELECT count(articleNumber) FROM "
                        + TABLE_CATEGORYITEM
                        + " WHERE channel = ?");
            paramCount = 1;
            ps.setInt(paramCount++, category.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                category.setTotalArticles(rs.getInt(1));
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
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
     * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteCategory(org.methodize.nntprss.feed.Category)
     */
    public void deleteCategory(Category category) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);

            ps =
                conn.prepareStatement(
                    "DELETE FROM "
                        + TABLE_CATEGORYITEM
                        + " WHERE category = ?");

            int paramCount = 1;
            ps.setInt(paramCount++, category.getId());
            ps.executeUpdate();
            ps.close();

            ps =
                conn.prepareStatement(
                    "DELETE FROM " + TABLE_CATEGORIES + " WHERE id = ?");

            paramCount = 1;
            ps.setInt(paramCount++, category.getId());
            ps.executeUpdate();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (ps != null)
                    ps.close();
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
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadItem(org.methodize.nntprss.feed.Category, int)
     */
    public Item loadItem(Category category, int articleNumber) {
        Item item = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int origArticleNumber = 0;
        int channelId = 0;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps =
                conn.prepareStatement(
                    "SELECT channel, channelArticleNumber FROM "
                        + TABLE_CATEGORYITEM
                        + " WHERE articleNumber = ? AND category = ?");
            int paramCount = 1;
            ps.setInt(paramCount++, articleNumber);
            ps.setInt(paramCount++, category.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                if (rs.next()) {
                    channelId = rs.getInt(1);
                    origArticleNumber = rs.getInt(2);

                    item =
                        loadItem(
                            (Channel) category.getChannels().get(
                                new Integer(channelId)),
                            origArticleNumber);
                    item.setArticleNumber(articleNumber);
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }
        return item;
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadItems(org.methodize.nntprss.feed.Category, int[], boolean, int)
     */
    public List loadItems(
        Category category,
        int[] articleRange,
        boolean onlyHeaders,
        int limit) {

        List items = new ArrayList();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE
                && articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
                ps =
                    conn.prepareStatement(
                        "SELECT categoryitem.articleNumber as categoryArticleNumber,items.* FROM "
                            + TABLE_CATEGORYITEM
                            + ", "
                            + TABLE_ITEMS
                            + " WHERE items.channel = categoryitem.channel AND items.articleNumber = categoryitem.channelArticleNumber AND categoryitem.articleNumber >= ? and categoryitem.articleNumber <= ? AND categoryitem.category = ? ORDER BY categoryitem.articleNumber");
            } else if (
                articleRange[0] == AppConstants.OPEN_ENDED_RANGE
                    && articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
                ps =
                    conn.prepareStatement(
                        "SELECT categoryitem.articleNumber as categoryArticleNumber,items.* FROM "
                            + TABLE_CATEGORYITEM
                            + ", "
                            + TABLE_ITEMS
                            + " WHERE items.channel = categoryitem.channel AND items.articleNumber = categoryitem.channelArticleNumber AND categoryitem.articleNumber <= ? AND categoryitem.category = ? ORDER BY categoryitem.articleNumber");
            } else if (
                articleRange[1] == AppConstants.OPEN_ENDED_RANGE
                    && articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
                ps =
                    conn.prepareStatement(
                        "SELECT categoryitem.articleNumber as categoryArticleNumber,items.* FROM "
                            + TABLE_CATEGORYITEM
                            + ", "
                            + TABLE_ITEMS
                            + " WHERE items.channel = categoryitem.channel AND items.articleNumber = categoryitem.channelArticleNumber AND categoryitem.articleNumber >= ? AND categoryitem.category = ? ORDER BY categoryitem.articleNumber");
            } else {
                ps =
                    conn.prepareStatement(
                        "categoryitem.articleNumber as categoryArticleNumber,items.* FROM "
                            + TABLE_CATEGORYITEM
                            + ", "
                            + TABLE_ITEMS
                            + " WHERE items.channel = categoryitem.channel AND items.articleNumber = categoryitem.channelArticleNumber AND categoryitem.category = ? ORDER BY categoryitem.articleNumber");
            }

            int paramCount = 1;

            if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
                ps.setInt(paramCount++, articleRange[0]);
            }

            if (articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
                ps.setInt(paramCount++, articleRange[1]);
            }

            ps.setInt(paramCount++, category.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Item item =
                        new Item(
                            rs.getInt("categoryArticleNumber"),
                            rs.getString("signature"));
                    item.setChannel(
                        (Channel) category.getChannels().get(
                            new Integer(rs.getInt("channel"))));
                    item.setDate(rs.getTimestamp("dtStamp"));
                    item.setTitle(rs.getString("title"));
                    item.setCreator(rs.getString("creator"));

                    if (!onlyHeaders) {
                        item.setDescription(rs.getString("description"));
                        item.setLink(rs.getString("link"));
                        item.setComments(rs.getString("comments"));
                        item.setGuid(rs.getString("guid"));
                        item.setGuidIsPermaLink(
                            rs.getBoolean("guidIsPermaLink"));
                    }
                    items.add(item);
                    if (limit != LIMIT_NONE && items.size() == limit) {
                        // Break if maximum items returned...
                        break;
                    }
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return items;

    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadNextItem(org.methodize.nntprss.feed.Category, int)
     */
    public Item loadNextItem(Category category, int relativeArticleNumber) {
        return loadRelativeItem(
            category,
            relativeArticleNumber,
            "SELECT TOP 1 categoryitem.articleNumber as categoryArticleNumber,items.* FROM "
                + TABLE_CATEGORYITEM
                + ", "
                + TABLE_ITEMS
                + " WHERE items.channel = categoryitem.channel AND items.articleNumber = categoryitem.channelArticleNumber AND categoryitem.articleNumber > ? AND categoryitem.category = ? ORDER BY categoryitem.articleNumber");
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadPreviousItem(org.methodize.nntprss.feed.Category, int)
     */
    public Item loadPreviousItem(
        Category category,
        int relativeArticleNumber) {
        return loadRelativeItem(
            category,
            relativeArticleNumber,
            "SELECT TOP 1 categoryitem.articleNumber as categoryArticleNumber,items.* FROM "
                + TABLE_CATEGORYITEM
                + ", "
                + TABLE_ITEMS
                + " WHERE items.channel = categoryitem.channel AND items.articleNumber = categoryitem.channelArticleNumber AND categoryitem.articleNumber > ? AND categoryitem.category = ? ORDER BY categoryitem.articleNumber DESC");
    }

    private Item loadRelativeItem(
        Category category,
        int previousArticleNumber,
        String sql) {
        Item item = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn =
                DriverManager.getConnection(JdbcChannelDAO.POOL_CONNECT_STRING);
            ps = conn.prepareStatement(sql);
            int paramCount = 1;
            ps.setInt(paramCount++, previousArticleNumber);
            ps.setInt(paramCount++, category.getId());
            rs = ps.executeQuery();

            if (rs != null) {
                if (rs.next()) {
                    item =
                        readItemFromRS(
                            rs,
                            (Channel) category.getChannels().get(
                                new Integer(rs.getInt("channel"))));
                    item.setArticleNumber(rs.getInt("categoryArticleNumber"));
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException se) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
            }
        }

        return item;
    }

    boolean migrateHsql() {
        boolean hsqlFound = false;

        // Check for nntp//rss v0.3 hsqldb database - if found, migrate...
        Connection hsqlConn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("org.hsqldb.jdbcDriver");
            hsqlConn =
                DriverManager.getConnection("jdbc:hsqldb:nntprssdb", "sa", "");

            if (log.isInfoEnabled()) {
                log.info("Migrating hsqldb to JDBC Database");
            }

            stmt = hsqlConn.createStatement();

            try {
                rs = stmt.executeQuery("SELECT * FROM config");
            } catch (SQLException e) {
                // Assume that hsqldb is not found...
                if (log.isEnabledFor(Priority.WARN)) {
                    log.warn(
                        "Exising hsqldb database not found, skipping migration");
                }
                return hsqlFound;
            }

            if (log.isInfoEnabled()) {
                log.info("Migrating system configuration...");
            }

            if (rs.next()) {
                ChannelManager channelManager =
                    ChannelManager.getChannelManager();
                channelManager.setPollingIntervalSeconds(
                    rs.getLong("pollingInterval"));
                channelManager.setProxyServer(rs.getString("proxyServer"));
                channelManager.setProxyPort(rs.getInt("proxyPort"));
                channelManager.setProxyUserID(rs.getString("proxyUserID"));
                channelManager.setProxyPassword(rs.getString("proxyPassword"));
                saveConfiguration(channelManager);

                NNTPServer nntpServer = new NNTPServer();
                loadConfiguration(nntpServer);
                nntpServer.setContentType(rs.getInt("contentType"));
                nntpServer.setSecure(rs.getBoolean("nntpSecure"));
                saveConfiguration(nntpServer);
            }

            rs.close();
            stmt.close();

            if (log.isInfoEnabled()) {
                log.info("Finished migration system configuration...");
            }

            if (log.isInfoEnabled()) {
                log.info("Migrating channel configuration...");
            }

            rs = stmt.executeQuery("SELECT * FROM channels");
            Map channelMap = new HashMap();

            while (rs.next()) {
                int origId = rs.getInt("id");

                Channel channel =
                    new Channel(rs.getString("name"), rs.getString("url"));
                channel.setAuthor(rs.getString("author"));
                channel.setTitle(rs.getString("title"));
                channel.setLink(rs.getString("link"));
                channel.setDescription(rs.getString("description"));
                channel.setLastArticleNumber(rs.getInt("lastArticle"));
                channel.setCreated(rs.getTimestamp("created"));
                channel.setRssVersion(rs.getString("rssVersion"));
                channel.setExpiration(
                    rs.getBoolean("historical") ? Channel.EXPIRATION_KEEP : 0);
                channel.setEnabled(rs.getBoolean("enabled"));
                channel.setPostingEnabled(rs.getBoolean("postingEnabled"));
                channel.setParseAtAllCost(rs.getBoolean("parseAtAllCost"));
                channel.setPublishAPI(rs.getString("publishAPI"));
                channel.setPublishConfig(
                    XMLHelper.xmlToStringHashMap(
                        rs.getString("publishConfig")));
                channel.setManagingEditor(rs.getString("managingEditor"));
                channel.setPollingIntervalSeconds(
                    rs.getLong("pollingInterval"));
                addChannel(channel);

                channelMap.put(new Integer(origId), channel);
            }

            stmt.close();
            rs.close();

            if (log.isInfoEnabled()) {
                log.info("Finished migrating channel configuration...");
            }

            if (log.isInfoEnabled()) {
                log.info("Migrating items...");
            }

            // Copy channel items...
            Iterator channelIter = channelMap.entrySet().iterator();
            int totalCount = 0;
            while (channelIter.hasNext()) {

                Map.Entry entry = (Map.Entry) channelIter.next();

                int count = 0;
                boolean moreResults = true;
                Channel channel = (Channel) entry.getValue();
                while (moreResults) {
                    rs =
                        stmt.executeQuery(
                            "SELECT LIMIT "
                                + count
                                + " 1000 * FROM items WHERE channel = "
                                + ((Integer) entry.getKey()).intValue());

                    int recCount = 0;

                    while (rs.next()) {
                        Item item = new Item();
                        item.setArticleNumber(rs.getInt("articleNumber"));
                        item.setChannel(channel);
                        item.setTitle(rs.getString("title"));
                        item.setLink(rs.getString("link"));
                        item.setDescription(rs.getString("description"));
                        item.setComments(rs.getString("comments"));
                        item.setDate(rs.getTimestamp("dtStamp"));
                        item.setSignature(rs.getString("signature"));
                        saveItem(item);
                        recCount++;
                    }

                    if (recCount < 1000) {
                        moreResults = false;
                    }

                    stmt.close();
                    rs.close();

                    count += recCount;

                    if (moreResults && log.isInfoEnabled()) {
                        log.info(
                            "Migrating items... "
                                + (totalCount + count)
                                + " items moved");
                    }
                }

                channel.setTotalArticles(count);
                updateChannel(channel);

                totalCount += count;

                if (log.isInfoEnabled()) {
                    log.info(
                        "Migrating items... " + totalCount + " items moved");
                }

            }

            if (log.isInfoEnabled()) {
                log.info("Finished migrating items...");
            }

            // Shutdown hsqldb
            stmt.execute("SHUTDOWN");
            hsqlFound = true;
        } catch (Exception e) {
            if (log.isEnabledFor(Priority.ERROR)) {
                log.error("Exception thrown when trying to migrate hsqldb", e);
            }
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
            try {
                if (hsqlConn != null)
                    hsqlConn.close();
            } catch (Exception e) {
            }
        }

        return hsqlFound;
    }

}
