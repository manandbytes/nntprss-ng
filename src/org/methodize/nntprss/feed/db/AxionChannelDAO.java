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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AxionChannelDAO.java,v 1.1 2003/09/28 19:53:15 jasonbrome Exp $
 */
public class AxionChannelDAO extends ChannelDAO {

	private Logger log = Logger.getLogger(AxionChannelDAO.class);

	public AxionChannelDAO() {
	}

	protected void createTables() {
		Connection conn = null;
		Statement stmt = null;

		if (log.isInfoEnabled()) {
			log.info("Creating application database tables");
		}

		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();

			stmt.executeUpdate(
				"CREATE SEQUENCE seqChannels");

			stmt.executeUpdate(
				"CREATE TABLE channels ("
					+ "id int default seqChannels.nextval, "
					+ "url varchar(255) not null, "
					+ "name varchar(255) not null, "
					+ "author varchar(255), "
					+ "title varchar(255), "
					+ "link varchar(500), "
					+ "description varchar(500), "
					+ "lastArticle int not null, "
					+ "lastPolled timestamp, "
					+ "created timestamp, "
					+ "lastModified long, "
					+ "lastETag varchar(255), "
					+ "rssVersion varchar(8), "
					+ "historical boolean, "
					+ "enabled boolean, "
					+ "postingEnabled boolean, "
					+ "parseAtAllCost boolean, "
					+ "publishAPI varchar(128), "
					+ "publishConfig varchar(2048), "
					+ "managingEditor varchar(128), "
					+ "pollingInterval long not null, "
					+ "status int)");

			stmt.executeUpdate(
				"CREATE INDEX pk_channels ON channels (id)");

			stmt.executeUpdate(
				"CREATE TABLE items ("
					+ "articleNumber int not null, "
					+ "channel int not null, "
					+ "title varchar, "
					+ "link varchar, "
					+ "description varchar, "
					+ "comments varchar(500), "
					+ "dtStamp timestamp, "
					+ "signature varchar(32), "
					+ "creator varchar(255), "
					+ "guid varchar(255), "
					+ "guidIsPermaLink boolean)");

			stmt.executeUpdate(
				"CREATE INDEX fk_items_channels ON items (channel)");
			stmt.executeUpdate(
				"CREATE INDEX idx_items_signature ON items (signature)");

			stmt.executeUpdate(
				"CREATE TABLE config ("
					+ "pollingInterval long not null, "
					+ "proxyServer varchar(255), "
					+ "proxyPort int, "
					+ "proxyUserID varchar(255), "
					+ "proxyPassword varchar(255), "
					+ "contentType int, "
					+ "dbVersion int, "
					+ "nntpSecure boolean, "
					+ "footnoteUrls boolean, "
					+ "useProxy boolean, "
					+ "observeHttp301 boolean, "
					+ "hostName varchar(255))");

			stmt
				.executeUpdate(
					"INSERT INTO config(pollingInterval, contentType, dbVersion, nntpSecure, footnoteUrls, useProxy, observeHttp301, hostName) VALUES(60*60, "
					+ AppConstants.CONTENT_TYPE_MIXED
					+ ", "
					+ DBVERSION
					+ ", false"
					+ ", true"
					+ ", false" // useProxy
			+", true" // observeHttp301
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

	private boolean migrateHsql() {
		boolean hsqlFound = false;

		//		Check for nntp//rss v0.3 hsqldb database - if found, migrate...
		Connection conn = null;
		Connection hsqlConn = null;
		Statement stmt = null;
		Statement stmt2 = null;
		Statement keyStmt = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;

		try {
			Class.forName("org.hsqldb.jdbcDriver");
			hsqlConn =
				DriverManager.getConnection("jdbc:hsqldb:nntprssdb", "sa", "");
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);

			if(log.isInfoEnabled()) {
				log.info("Migrating hsqldb to Mckoi database");
			}

			if(log.isInfoEnabled()) {
				log.info("Migrating system configuration...");
			}

			stmt = hsqlConn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM config");
			if (rs.next()) {

				ps =
					conn.prepareStatement(
						"UPDATE config "
							+ "SET pollingInterval = ?,"
							+ "proxyServer = ?,"
							+ "proxyPort = ?,"
							+ "proxyUserID = ?,"
							+ "proxyPassword = ?,"
							+ "contentType = ?,"
							+ "nntpSecure = ?");

				int paramCount = 1;
				ps.setLong(paramCount++, rs.getLong("pollingInterval"));
				ps.setString(paramCount++, rs.getString("proxyServer"));
				ps.setInt(paramCount++, rs.getInt("proxyPort"));
				ps.setString(paramCount++, rs.getString("proxyUserID"));
				ps.setString(paramCount++, rs.getString("proxyPassword"));
				ps.setInt(paramCount++, rs.getInt("contentType"));
				ps.setBoolean(paramCount++, rs.getBoolean("nntpSecure"));
				ps.executeUpdate();
				ps.close();
			}

			rs.close();
			stmt.close();

			if(log.isInfoEnabled()) {
				log.info("Finished migration system configuration...");
			}

			if(log.isInfoEnabled()) {
				log.info("Migrating channel configuration...");
			}

			rs = stmt.executeQuery("SELECT * FROM channels");
			ps =
				conn.prepareStatement(
					"INSERT INTO channels ("
						+ "id,"
						+ "url,"
						+ "name,"
						+ "author,"
						+ "title,"
						+ "link,"
						+ "description,"
						+ "lastArticle,"
						+ "created,"
						+ "rssVersion,"
						+ "historical,"
						+ "enabled,"
						+ "postingEnabled,"
						+ "parseAtAllCost,"
						+ "publishAPI,"
						+ "publishConfig,"
						+ "managingEditor,"
						+ "pollingInterval) VALUES("
						+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			ps2 =
				conn.prepareStatement(
					"INSERT INTO items ("
						+ "articleNumber,"
						+ "channel, "
						+ "title, "
						+ "link, "
						+ "description, "
						+ "comments, "
						+ "dtStamp, "
						+ "signature) VALUES("
						+ "?, ?, ?, ?, ?, ?, ?, ?)");

			stmt2 = hsqlConn.createStatement();
			keyStmt = conn.createStatement();
			Map channelMap = new HashMap();

			while (rs.next()) {

				int origId = rs.getInt("id");
				int id = 0;

				rs2 = keyStmt.executeQuery("SELECT seqChannels.nextval");
				if (rs2.next()) {
					id = rs2.getInt(1);
				}
				rs2.close();
//				keyStmt.close();

				channelMap.put(new Integer(origId), new Integer(id));

				int paramCount = 1;
				//				+ "id,"
				ps.setInt(paramCount++, id);
				//				+ "url," 
				ps.setString(paramCount++, rs.getString("url"));
				//				+ "name," 
				ps.setString(paramCount++, rs.getString("name"));
				//				+ "author," 
				ps.setString(paramCount++, rs.getString("author"));
				//				+ "title," 
				ps.setString(paramCount++, rs.getString("title"));
				//				+ "link," 
				ps.setString(paramCount++, rs.getString("link"));
				//				+ "description," 
				ps.setString(paramCount++, rs.getString("description"));
				//				+ "lastArticle," 
				ps.setInt(paramCount++, rs.getInt("lastArticle"));
				//				+ "created," 
				ps.setTimestamp(paramCount++, rs.getTimestamp("created"));
				//				+ "rssVersion," 
				ps.setString(paramCount++, rs.getString("rssVersion"));
				//				+ "historical," 
				ps.setBoolean(paramCount++, rs.getBoolean("historical"));
				//				+ "enabled," 
				ps.setBoolean(paramCount++, rs.getBoolean("enabled"));
				//				+ "postingEnabled," 
				ps.setBoolean(paramCount++, rs.getBoolean("postingEnabled"));
				//				+ "parseAtAllCost," 
				ps.setBoolean(paramCount++, rs.getBoolean("parseAtAllCost"));
				//				+ "publishAPI," 
				ps.setString(paramCount++, rs.getString("publishAPI"));
				//				+ "publishConfig," 
				ps.setString(paramCount++, rs.getString("publishConfig"));
				//				+ "managingEditor,"
				ps.setString(paramCount++, rs.getString("managingEditor"));
				//				+ "pollingInterval) VALUES("
				ps.setLong(paramCount++, rs.getLong("pollingInterval"));
				ps.executeUpdate();

			}

			stmt.close();
			rs.close();

			if(log.isInfoEnabled()) {
				log.info("Finished migrating channel configuration...");
			}

			if(log.isInfoEnabled()) {
				log.info("Migrating items...");
			}

			conn.setAutoCommit(false);

			// Copy channel items...
			Iterator channelIter = channelMap.entrySet().iterator();
			int totalCount = 0;
			while(channelIter.hasNext()) {

				Map.Entry entry = (Map.Entry)channelIter.next();

				int count = 0;
				boolean moreResults = true;
				while(moreResults) {
					rs =
						stmt.executeQuery(
							"SELECT LIMIT " + count + " 1000 * FROM items WHERE channel = " + 
								((Integer)entry.getKey()).intValue());
	
						int recCount = 0;
						while (rs.next()) {
							int paramCount = 1;
							//					+ "articleNumber,"
							ps2.setInt(paramCount++, rs.getInt("articleNumber"));
							//					+ "channel, "
							ps2.setInt(
								paramCount++,
									((Integer)entry.getValue()).intValue());
							//					+ "title, "
							ps2.setString(paramCount++, rs.getString("title"));
							//					+ "link, "
							ps2.setString(paramCount++, rs.getString("link"));
							//					+ "description, "
							ps2.setString(paramCount++, rs.getString("description"));
							//					+ "comments, "
							ps2.setString(paramCount++, rs.getString("comments"));
							//					+ "dtStamp, "
							ps2.setTimestamp(paramCount++, rs.getTimestamp("dtStamp"));
							//					+ "signature) VALUES("
							ps2.setString(paramCount++, rs.getString("signature"));
							ps2.executeUpdate();

							recCount++;
						}
						
						if(recCount < 1000) {
							moreResults = false;
						}

						stmt.close();
						rs.close();
	
					count+= recCount;
					conn.commit();
				}

				totalCount += count;
				
				if(log.isInfoEnabled()) {
					log.info("Migrating items... " + totalCount + " items moved");
				}
				
			}

			conn.setAutoCommit(true);

			if(log.isInfoEnabled()) {
				log.info("Finished migrating items...");
			}

			keyStmt.execute("CHECKPOINT");
			keyStmt.close();

			// Shutdown hsqldb
			stmt.execute("SHUTDOWN");
			hsqlFound = true;

		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Exception thrown when trying to migrate hsqldb", e);
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
				if (ps != null)
					ps.close();
			} catch (Exception e) {
			}
			try {
				if (ps2 != null)
					ps2.close();
			} catch (Exception e) {
			}
			try {
				if (stmt2 != null)
					stmt2.close();
			} catch (Exception e) {
			}
			try {
				if (keyStmt != null)
					keyStmt.close();
			} catch (Exception e) {
			}
			try {
				if (rs2 != null)
					rs2.close();
			} catch (Exception e) {
			}
			try {
				if (conn != null)
					conn.close();
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

	protected void populateInitialChannels(Document config) {
		Connection conn = null;
		PreparedStatement ps = null;

		if (!migrateHsql()) {

			if (log.isInfoEnabled()) {
				log.info("Loading channels");
			}

			try {
				conn =
					DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);

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
								"INSERT INTO channels(url, name, created, lastArticle, historical, enabled, postingEnabled, parseAtAllCost, pollingInterval, status) "
									+ "values(?, ?, ?, ?, ?, true, false, false, 0, "
									+ Channel.STATUS_OK
									+ ")");

						for (int channelCount = 0;
							channelCount < channelList.getLength();
							channelCount++) {
							Element channelElm =
								(Element) channelList.item(channelCount);
							String url = channelElm.getAttribute("url");
							String name = channelElm.getAttribute("name");

							String historicalStr =
								channelElm.getAttribute("historical");
							boolean historical = true;
							if (historicalStr != null) {
								historical =
									historicalStr.equalsIgnoreCase("true");
							}
							int paramCount = 1;
							ps.setString(paramCount++, url);
							ps.setString(paramCount++, name);
							ps.setTimestamp(
								paramCount++,
								new Timestamp(System.currentTimeMillis()));
							// Last Article
							ps.setInt(paramCount++, 0);
							ps.setBoolean(paramCount++, historical);
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();

			switch (dbVersion) {
				case 4 :

				default :
					// Force re-poll of all channels after DB upgrade...
					stmt.executeUpdate(
						"UPDATE config SET dbVersion = " + DBVERSION);
					stmt.executeUpdate(
						"UPDATE channels SET lastPolled = null, lastModified = null, lastETag = null");
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
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			int channelId = 0;

			rs = s.executeQuery("SELECT seqChannels.nextval");
			if(rs.next()) {
				channelId = rs.getInt(1);
			}
			rs.close();
			s.close();

			ps =
				conn.prepareStatement(
					"INSERT INTO channels(id, url, name, lastArticle, created, historical, enabled, postingEnabled, publishAPI, publishConfig, parseAtAllCost, pollingInterval, status) "
						+ "values(?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			int paramCount = 1;
			ps.setInt(paramCount++, channelId);
			ps.setString(paramCount++, channel.getUrl());
			ps.setString(paramCount++, channel.getName());
			ps.setTimestamp(
				paramCount++,
				new Timestamp(channel.getCreated().getTime()));
			ps.setBoolean(paramCount++, channel.isHistorical());
			ps.setBoolean(paramCount++, channel.isEnabled());
			ps.setBoolean(paramCount++, channel.isPostingEnabled());
			ps.setString(paramCount++, channel.getPublishAPI());
			ps.setString(
				paramCount++,
				XMLHelper.stringMapToXML(channel.getPublishConfig()));
			ps.setBoolean(paramCount++, channel.isParseAtAllCost());
			ps.setLong(paramCount++, channel.getPollingIntervalSeconds());
			ps.setInt(paramCount++, channel.getStatus());
			ps.executeUpdate();

			ps.close();
			ps = null;

			channel.setId(channelId);

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
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = DriverManager.getConnection(ChannelDAO.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			stmt.execute("SHUTDOWN");
		} catch (SQLException e) {
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

	public Map loadChannels() {
		Map channels = new TreeMap();
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
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
						"SELECT MIN(articleNumber) FROM items WHERE channel = ?");
//, COUNT(articleNumber)
				ps2 =
					  conn.prepareStatement(
						  "SELECT COUNT(articleNumber) FROM items WHERE channel = ?");
						  
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
							
						}
						rs2.close();
					}

					ps2.setInt(1, channel.getId());
					rs2 = ps2.executeQuery();
					if (rs2 != null) {
						if (rs2.next()) {

							channel.setTotalArticles(
								rs2.getInt(1));
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

		if(log.isInfoEnabled()) {
			log.info("Loaded " + channels.size() + " channels");
		}

		return channels;
	}


}
