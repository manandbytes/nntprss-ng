package org.methodize.nntprss.rss.db;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002 Jason Brome.  All Rights Reserved.
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

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.db.DBManager;
import org.methodize.nntprss.rss.Channel;
import org.methodize.nntprss.rss.ChannelManager;
import org.methodize.nntprss.rss.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */
public class ChannelManagerDAO {

	private Logger log = Logger.getLogger(ChannelManagerDAO.class);

	private static final ChannelManagerDAO rssManagerDAO = new ChannelManagerDAO();

	private static final Channel[] EMTPY_CHANNEL_ARRAY = new Channel[0];

	private ChannelManagerDAO() {
	}

	public static ChannelManagerDAO getRSSManagerDAO() {
		return rssManagerDAO;
	}

	private void createTables(Document config) {
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		if(log.isInfoEnabled()) {
			log.info("Creating application database tables");
		}

		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			stmt = conn.createStatement();

			stmt.executeUpdate(
				"CREATE CACHED TABLE channels ("
					+ "id int not null identity, "
					+ "url varchar(256) not null, "
					+ "name varchar(256) not null, "
					+ "author varchar(256), "
					+ "lastArticle int not null, "
					+ "lastPolled timestamp, "
					+ "created timestamp, "
					+ "lastModified bigint, "
					+ "lastETag varchar(256), "
					+ "rssVersion varchar(8), "
					+ "historical bit )");
			stmt.executeUpdate(
				"CREATE CACHED TABLE items ("
					+ "articleNumber int not null, "
					+ "channel int not null, "
					+ "title varchar, "
					+ "link varchar, "
					+ "description varchar, "
					+ "dtStamp timestamp, "
					+ "signature varchar(32))");
			stmt.executeUpdate(
				"CREATE CACHED TABLE config ("
					+ "pollingInterval bigint not null, "
					+ "proxyServer varchar(256), "
					+ "proxyPort int )");
			stmt.executeUpdate(
				"INSERT INTO config(pollingInterval) VALUES(60*60)");

			NodeList channelsList =
				config.getDocumentElement().getElementsByTagName("channels");

			if (channelsList.getLength() > 0) {
				Element channelsElm = (Element) channelsList.item(0);
				NodeList channelList = channelsElm.getElementsByTagName("channel");
				if (channelList.getLength() > 0) {

					ps =
						conn.prepareStatement(
							"INSERT INTO channels(url, name, created, lastArticle, historical) "
								+ "values(?, ?, ?, ?, ?)");

					for (int channelCount = 0;
						channelCount < channelList.getLength();
						channelCount++) {
						Element channelElm =
							(Element) channelList.item(channelCount);
						String url = channelElm.getAttribute("url");
						String name = channelElm.getAttribute("name");

						String historicalStr = channelElm.getAttribute("historical");
						boolean historical = true;
						if(historicalStr != null) {
							historical = historicalStr.equalsIgnoreCase("true");
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

			if(log.isEnabledFor(Priority.ERROR)) {
				log.error("Error creating application database tables", se);
			}
			throw new RuntimeException("Error creating application tables - "
				+ se.getMessage());

		} finally {
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

		if(log.isInfoEnabled()) {
			log.info("Finished creating application database tables");
		}

	}

	public void initialize(ChannelManager rssManager, Document config) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		boolean createTables = false;
		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			try {
				rs = stmt.executeQuery("SELECT * FROM CONFIG");
				if(rs != null) {
					if(rs.next()) {
						rssManager.setPollingIntervalSeconds(rs.getLong("pollingInterval"));
						rssManager.setProxyServer(rs.getString("proxyServer"));
						rssManager.setProxyPort(rs.getInt("proxyPort"));
					}
				}
			} catch (SQLException e) {
			// Our tables don't exist, so let's create them...
				createTables = true;
			}
		} catch (SQLException se) {

			throw new RuntimeException("Problem initializing application database "
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

		if (createTables) {
			createTables(config);
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
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM channels");
			if (rs != null) {
				ps =
					conn.prepareStatement(
						"SELECT MIN(articleNumber) as firstArticleNumber, COUNT(articleNumber) as totalArticles FROM items WHERE channel = ?");
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

					ps.setInt(1, channel.getId());
					rs2 = ps.executeQuery();
					if (rs2 != null) {
						if (rs2.next()) {
							int firstArticleNumber = 
								rs2.getInt("firstArticleNumber");
							if(firstArticleNumber != 0) {
								channel.setFirstArticleNumber(firstArticleNumber);
							} else {
								channel.setFirstArticleNumber(1);
							}
							
							channel.setTotalArticles(
								rs2.getInt("totalArticles"));
						}
						rs2.close();
					}

					channel.setLastPolled(rs.getTimestamp("lastPolled"));
					channel.setLastModified(rs.getLong("lastModified"));
					channel.setLastETag(rs.getString("lastETag"));
					channel.setRssVersion(rs.getString("rssVersion"));
					channel.setHistorical(rs.getBoolean("historical"));

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

	public void addChannel(Channel channel) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"INSERT INTO channels(url, name, lastArticle, created, historical) "
						+ "values(?, ?, 0, ?, ?); CALL IDENTITY()");


			int paramCount = 1;
			ps.setString(paramCount++, channel.getUrl());
			ps.setString(paramCount++, channel.getName());
			ps.setTimestamp(
				paramCount++,
				new Timestamp(channel.getCreated().getTime()));
			ps.setBoolean(paramCount++, channel.isHistorical());
			rs = ps.executeQuery();
			
			if(rs != null) {
				if(rs.next()) {
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
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"UPDATE channels "
						+ "SET author = ?, name = ?, url = ?, lastArticle = ?, "
						+ "lastPolled = ?, lastModified = ?, lastETag = ?, rssVersion = ?, historical = ? "
						+ "WHERE id = ?");

			int paramCount = 1;
			ps.setString(paramCount++, channel.getAuthor());
			ps.setString(paramCount++, channel.getName());
			ps.setString(paramCount++, channel.getUrl());
			ps.setInt(paramCount++, channel.getLastArticleNumber());
			ps.setTimestamp(
				paramCount++,
				new Timestamp(channel.getLastPolled().getTime()));
			ps.setLong(paramCount++, channel.getLastModified());
			ps.setString(paramCount++, channel.getLastETag());
			ps.setString(paramCount++, channel.getRssVersion());
			ps.setBoolean(paramCount++, channel.isHistorical());

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

	public void deleteItemsBySignature(Channel channel, Set itemSignatures) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);

			ps = 
				conn.prepareStatement(
					"DELETE FROM items WHERE channel = ? AND signature = ?");
					
			int paramCount = 1;
			ps.setInt(paramCount++, channel.getId());

			Iterator sigIter = itemSignatures.iterator();
			while(sigIter.hasNext()) {
				ps.setString(paramCount, (String)sigIter.next());
				ps.executeUpdate();
			}

// Need to reset first article number...
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


	public void deleteChannel(Channel channel) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);

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

	public Item loadItem(Channel channel, int articleNumber) {
		Item item = null;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT * FROM items WHERE articleNumber = ? AND channel = ?");
			int paramCount = 1;
			ps.setInt(paramCount++, articleNumber);
			ps.setInt(paramCount++, channel.getId());
			rs = ps.executeQuery();

			if (rs != null) {
				if (rs.next()) {
					item =
						new Item(
							rs.getInt("articleNumber"),
							rs.getString("signature"));
					item.setChannel(channel);
					item.setDate(rs.getTimestamp("dtStamp"));
					item.setTitle(rs.getString("title"));
					item.setDescription(rs.getString("description"));
					item.setLink(rs.getString("link"));
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
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT * FROM items WHERE signature = ? AND channel = ?");
			int paramCount = 1;
			ps.setString(paramCount++, signature);
			ps.setInt(paramCount++, channel.getId());
			rs = ps.executeQuery();

			if (rs != null) {
				if (rs.next()) {
					item =
						new Item(
							rs.getInt("articleNumber"),
							rs.getString("signature"));
					item.setChannel(channel);
					item.setDate(rs.getTimestamp("dtStamp"));
					item.setTitle(rs.getString("title"));
					item.setDescription(rs.getString("description"));
					item.setLink(rs.getString("link"));
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
		boolean onlyHeaders) {
		List items = new ArrayList();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT * FROM items WHERE articleNumber >= ? and articleNumber <= ? AND channel = ? ORDER BY articleNumber");
			int paramCount = 1;
			ps.setInt(paramCount++, articleRange[0]);
			ps.setInt(paramCount++, articleRange[1]);
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

					if (!onlyHeaders) {
						item.setDescription(rs.getString("description"));
						item.setLink(rs.getString("link"));
					}
					items.add(item);
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

	public void saveItem(Item item) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"INSERT INTO items "
						+ "(articlenumber, channel, title, link, description, dtstamp, signature) "
						+ "VALUES(?,?,?,?,?,?,?)");

			int paramCount = 1;
			ps.setInt(paramCount++, item.getArticleNumber());
			ps.setInt(paramCount++, item.getChannel().getId());
			ps.setString(paramCount++, item.getTitle());
			ps.setString(paramCount++, item.getLink());
			ps.setString(paramCount++, item.getDescription());
			ps.setTimestamp(
				paramCount++,
				new Timestamp(item.getDate().getTime()));
			ps.setString(paramCount++, item.getSignature());
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

	public Set getItemSignatures(int channelId) {
		Set signatures = new HashSet();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"SELECT signature FROM items WHERE channel = ?");

			int paramCount = 1;
			ps.setInt(paramCount++, channelId);

			rs = ps.executeQuery();

			if (rs != null) {
				while (rs.next()) {
					signatures.add(rs.getString("signature"));
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

		return signatures;
	}

	public void saveConfiguration(ChannelManager rssManager) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(DBManager.POOL_CONNECT_STRING);
			ps =
				conn.prepareStatement(
					"UPDATE config "
						+ "SET pollingInterval = ?, "
						+ "proxyServer = ?, "
						+ "proxyPort = ?");

			int paramCount = 1;
			ps.setLong(paramCount++, rssManager.getPollingIntervalSeconds());
			ps.setString(paramCount++, rssManager.getProxyServer());
			ps.setInt(paramCount++, rssManager.getProxyPort());
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



}
