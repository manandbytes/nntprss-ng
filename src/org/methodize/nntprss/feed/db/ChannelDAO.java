package org.methodize.nntprss.feed.db;

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

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DbcpException;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
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
 * @version $Id: ChannelDAO.java,v 1.3 2003/09/28 20:41:41 jasonbrome Exp $
 */
public abstract class ChannelDAO {

	public static final int LIMIT_NONE = -1;

	static final int DBVERSION = 5;

	Logger log = Logger.getLogger(ChannelDAO.class);

	public static final String POOL_CONNECT_STRING =
		"jdbc:apache:commons:dbcp:nntprss";

	public abstract void shutdown();

	protected abstract void upgradeDatabase(int dbVersion);

	protected abstract void createTables();
	protected abstract void populateInitialChannels(Document config);

	private void initializeDatabasePool(Document config) throws Exception {
		Element rootElm = config.getDocumentElement();
		Element dbConfig = (Element)rootElm.getElementsByTagName("db").item(0);
		String connectString = dbConfig.getAttribute("connect");

		ObjectPool connectionPool = new GenericObjectPool(null);

		String dbDriver = dbConfig.getAttribute("driverClass");
		if(dbDriver != null && dbDriver.length() > 0) {
			Class.forName(dbDriver);
		} else {
// Default to HSSQLDB
			Class.forName("org.hsqldb.jdbcDriver");
		}

		String user = dbConfig.getAttribute("user");
		String password = dbConfig.getAttribute("password");
		if(user == null) {
			user = "sa";
		}
		if(password == null) {
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			try {
				rs = stmt.executeQuery("SELECT * FROM config");
				if(rs != null) {
					if(rs.next()) {
						int dbVersion = rs.getInt("dbVersion");
						if(dbVersion < DBVERSION) {
							upgradeDatabase(dbVersion);
						}
					}
				}
			} catch (SQLException e) {
				if(e.getErrorCode() == -org.hsqldb.Trace.COLUMN_NOT_FOUND) {
// Pre-version db, upgrade database
					upgradeDatabase(0);
				} else {
			// Our tables don't exist, so let's create them...
					createTables = true;
				}
			}
		} catch (SQLException se) {

			throw new RuntimeException("Problem initializing application database "
				+ se);

		} catch (DbcpException de) {
			if(de.getCause() != null && de.getCause() instanceof SQLException) {
				SQLException se = (SQLException)de.getCause();
// McKoi DB
				if(se.getMessage().startsWith("Can not find a database to start.")) {
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

	
	public void loadConfiguration(ChannelManager channelManager) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM config");
			if(rs != null) {
				if(rs.next()) {
					channelManager.setPollingIntervalSeconds(rs.getLong("pollingInterval"));
					channelManager.setProxyServer(rs.getString("proxyServer"));
					channelManager.setProxyPort(rs.getInt("proxyPort"));
					channelManager.setProxyUserID(rs.getString("proxyUserID"));
					channelManager.setProxyPassword(rs.getString("proxyPassword"));
					channelManager.setUseProxy(rs.getBoolean("useProxy"));
					channelManager.setObserveHttp301(rs.getBoolean("observeHttp301"));
				}
			}
		} catch (SQLException se) {
			throw new RuntimeException("Problem loading Channel manager configuration"
				+ se);
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

	public void loadConfiguration(NNTPServer nntpServer) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT contentType, nntpSecure, footnoteUrls, hostName FROM config");
			if(rs != null) {
				if(rs.next()) {
					nntpServer.setContentType(rs.getInt("contentType"));
					nntpServer.setSecure(rs.getBoolean("nntpSecure"));
					nntpServer.setFootnoteUrls(rs.getBoolean("footnoteUrls"));
					nntpServer.setHostName(rs.getString("hostName"));
				}
			}
		} catch (SQLException se) {
			throw new RuntimeException("Problem loading NNTP Server configuration"
				+ se);
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

	
	public Map loadChannels() {
		Map channels = new TreeMap();
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSet rs2 = null;

		if(log.isInfoEnabled()) {
			log.info("Loading channel configuration");
		}

		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM channels");
			if (rs != null) {
				ps =
					conn.prepareStatement(
						"SELECT MIN(articleNumber), COUNT(articleNumber) FROM items WHERE channel = ?");
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
							int firstArticleNumber = 
								rs2.getInt(1);
							if(firstArticleNumber != 0) {
								channel.setFirstArticleNumber(firstArticleNumber);
							} else {
								channel.setFirstArticleNumber(1);
							}
							
							channel.setTotalArticles(
								rs2.getInt(2));
						}
						rs2.close();
					}

					channel.setLastPolled(rs.getTimestamp("lastPolled"));
					channel.setLastModified(rs.getLong("lastModified"));
					channel.setLastETag(rs.getString("lastETag"));
					channel.setRssVersion(rs.getString("rssVersion"));
					channel.setHistorical(rs.getBoolean("historical"));
					channel.setEnabled(rs.getBoolean("enabled"));
					channel.setPostingEnabled(rs.getBoolean("postingEnabled"));
					channel.setPublishAPI(rs.getString("publishAPI"));
					channel.setPublishConfig(XMLHelper.xmlToStringHashMap(rs.getString("publishConfig")));

					channel.setParseAtAllCost(rs.getBoolean("parseAtAllCost"));
					channel.setManagingEditor(rs.getString("managingEditor"));

					channel.setPollingIntervalSeconds(rs.getLong("pollingInterval"));
					
					channel.setStatus(rs.getInt("status"));

					channels.put(channel.getName(), channel);
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

		if(log.isInfoEnabled()) {
			log.info("Loaded " + channels.size() + " channels");
		}

		return channels;
	}

	
	public abstract void addChannel(Channel channel);
	
	public void updateChannel(Channel channel) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"UPDATE channels "
						+ "SET author = ?, name = ?, url = ?, "
						+ "title = ?, link = ?, description = ?, "
						+ "lastArticle = ?, "
						+ "lastPolled = ?, lastModified = ?, lastETag = ?, rssVersion = ?, historical = ?, "
						+ "enabled = ?, "
						+ "postingEnabled = ?, "
						+ "publishAPI = ?, "
						+ "publishConfig = ?, "
						+ "parseAtAllCost = ?, "
						+ "managingEditor = ?, "
						+ "pollingInterval = ?, "
						+ "status = ? "
						+ "WHERE id = ?");

			int paramCount = 1;
			ps.setString(paramCount++, channel.getAuthor());
			ps.setString(paramCount++, channel.getName());
			ps.setString(paramCount++, channel.getUrl());
			ps.setString(paramCount++, channel.getTitle());
			ps.setString(paramCount++, channel.getLink());
			ps.setString(paramCount++, channel.getDescription());
			ps.setInt(paramCount++, channel.getLastArticleNumber());

			if(channel.getLastPolled() != null) {
				ps.setTimestamp(
					paramCount++,
					new Timestamp(channel.getLastPolled().getTime()));
			} else {
				ps.setNull(
					paramCount++,
					java.sql.Types.TIMESTAMP);
			}
			
			ps.setLong(paramCount++, channel.getLastModified());
			ps.setString(paramCount++, channel.getLastETag());
			ps.setString(paramCount++, channel.getRssVersion());
			ps.setBoolean(paramCount++, channel.isHistorical());
			ps.setBoolean(paramCount++, channel.isEnabled());
			ps.setBoolean(paramCount++, channel.isPostingEnabled());
			ps.setString(paramCount++, channel.getPublishAPI());
			ps.setString(paramCount++, XMLHelper.stringMapToXML(channel.getPublishConfig()));
			ps.setBoolean(paramCount++, channel.isParseAtAllCost());

			ps.setString(paramCount++, channel.getManagingEditor());

			ps.setLong(paramCount++, channel.getPollingIntervalSeconds());
			ps.setInt(paramCount++, channel.getStatus());

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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);

			ps = 
				conn.prepareStatement(
					"DELETE FROM items WHERE channel = ?");
					
			int paramCount = 1;
			ps.setInt(paramCount++, channel.getId());
			ps.executeUpdate();
			ps.close();
						
			ps =
				conn.prepareStatement(
					"DELETE FROM channels WHERE id = ?");

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

	private Item readItemFromRS(ResultSet rs, Channel channel) throws SQLException {
		Item item =
			new Item(
				rs.getInt("articleNumber"),
				rs.getString("signature"));
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT * FROM items WHERE articleNumber = ? AND channel = ?");
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
		return loadRelativeItem(channel, relativeArticleNumber,
			"SELECT TOP 1 * FROM items WHERE articleNumber > ? AND channel = ? ORDER BY articleNumber");
	}
	
	public Item loadPreviousItem(Channel channel, int relativeArticleNumber) {
		return loadRelativeItem(channel, relativeArticleNumber,
			"SELECT TOP 1 * FROM items WHERE articleNumber < ? AND channel = ? ORDER BY articleNumber DESC");
	}

	private Item loadRelativeItem(Channel channel, int previousArticleNumber,
		String sql) {
		Item item = null;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT * FROM items WHERE signature = ? AND channel = ?");
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

	
	/**
	 * Method loadItems.
	 * @param channel
	 * @param articleRange
	 * @param onlyHeaders
	 * @param limit Maximum number of items to return
	 * @return List
	 * 
	 * articleRange
	 * -1 = open ended search (all items from article number,
	 *    all items to article number)
	 */

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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			if(articleRange[0] != AppConstants.OPEN_ENDED_RANGE
				 && articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
				ps =
					conn.prepareStatement(
						"SELECT * FROM items WHERE articleNumber >= ? and articleNumber <= ? AND channel = ? ORDER BY articleNumber");
			} else if(articleRange[0] == AppConstants.OPEN_ENDED_RANGE &&
				articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
				ps =
					conn.prepareStatement(
						"SELECT * FROM items WHERE articleNumber <= ? AND channel = ? ORDER BY articleNumber");
			} else if(articleRange[1] == AppConstants.OPEN_ENDED_RANGE &&
				articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
				ps =
					conn.prepareStatement(
						"SELECT * FROM items WHERE articleNumber >= ? AND channel = ? ORDER BY articleNumber");
			} else {
				ps =
					conn.prepareStatement(
						"SELECT * FROM items WHERE channel = ? ORDER BY articleNumber");
			}

			int paramCount = 1;

			if(articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
				ps.setInt(paramCount++, articleRange[0]);
			}
			
			if(articleRange[1] != AppConstants.OPEN_ENDED_RANGE) {
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
						item.setGuidIsPermaLink(rs.getBoolean("guidIsPermaLink"));
					}
					items.add(item);
					if(limit != LIMIT_NONE && items.size() == limit) 
// Break if maximum items returned...
						break;
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

		
	/**
	 * Method loadArticleNumbers
	 * @param channel
	 * @return List
	 * 
	 * Supports NNTP listgroup command
	 */

	public List loadArticleNumbers(
		Channel channel) {

		List articleNumbers = new ArrayList();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT articleNumber FROM items WHERE channel = ? ORDER BY articleNumber");

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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"INSERT INTO items "
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"UPDATE config "
						+ "SET pollingInterval = ?, "
						+ "proxyServer = ?, "
						+ "proxyPort = ?, "
						+ "proxyUserID = ?, "
						+ "proxyPassword = ?, "
						+ "useProxy = ?, "
						+ "observeHttp301 = ?");

			int paramCount = 1;
			ps.setLong(paramCount++, channelManager.getPollingIntervalSeconds());
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"UPDATE config "
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
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteItemsNotInSet(org.methodize.nntprss.feed.Channel, java.util.Set)
	 */
	public void deleteItemsNotInSet(Channel channel, Set itemSignatures) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);

			StringBuffer stBuf = new StringBuffer("DELETE FROM items WHERE channel = ? AND signature NOT IN (");

// Create question marks for signature parameters
			for(int i = 0; i < itemSignatures.size(); i++) {
				if(i > 0) {
					stBuf.append(',');
				}
				stBuf.append('?');
			}

			stBuf.append(")");
			ps = conn.prepareStatement(stBuf.toString());
					
			int paramCount = 1;
			ps.setInt(paramCount++, channel.getId());

			Iterator sigIter = itemSignatures.iterator();
			while(sigIter.hasNext()) {
				ps.setString(paramCount++, (String)sigIter.next());
			}

			ps.executeUpdate();

// Need to reset first article number...
// TODO: only really need to do this if first article number is not in set...
			ps = conn.prepareStatement(
				"SELECT MIN(articleNumber) as firstArticleNumber FROM items WHERE channel = ?");
			paramCount = 1;
			
			ps.setInt(paramCount++, channel.getId());

			rs = ps.executeQuery();
			if(rs != null) {
				if(rs.next()) {
					int firstArticle = rs.getInt("firstArticleNumber");
					if(firstArticle == 0) {
// Have yet to sync any articles, so first article number 
// will be 1
						if(channel.getLastArticleNumber() == 0) {
							channel.setFirstArticleNumber(1);
						} else {
							channel.setFirstArticleNumber(channel.getLastArticleNumber());
						}
					} else {
						channel.setFirstArticleNumber(firstArticle);
					}
				}
				
			}			
			

		} catch (SQLException se) {
			throw new RuntimeException(se);
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException se) {	}
			try { if (ps != null) ps.close(); } catch (SQLException se) {	}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#findNewItemSignatures(int, java.util.Set)
	 */
	public Set findNewItemSignatures(Channel channel, Set itemSignatures) {
		Set newSignatures = new HashSet();

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int channelId = channel.getId();

		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			StringBuffer stBuf = new StringBuffer("SELECT signature FROM items WHERE channel = ? AND signature IN (");

// Create question marks for signature parameters
			for(int i = 0; i < itemSignatures.size(); i++) {
				if(i > 0) {
					stBuf.append(',');
				}
				stBuf.append('?');
			}

			stBuf.append(")");
			ps = conn.prepareStatement(stBuf.toString());

			int paramCount = 1;
			ps.setInt(paramCount++, channelId);

			Iterator sigIter = itemSignatures.iterator();
			while(sigIter.hasNext()) {
				ps.setString(paramCount++, (String)sigIter.next());
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

}