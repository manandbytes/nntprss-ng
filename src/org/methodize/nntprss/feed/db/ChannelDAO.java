package org.methodize.nntprss.feed.db;

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

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Category;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ChannelDAO.java,v 1.10 2005/07/30 01:47:42 jasonbrome Exp $
 */
public abstract class ChannelDAO {

    public static final int LIMIT_NONE = -1;

    static final int DBVERSION = 6;

    Logger log = Logger.getLogger(ChannelDAO.class);

    public abstract void shutdown();

    protected abstract void upgradeDatabase(int dbVersion);

    protected abstract void createTables();
    protected abstract void populateInitialChannels(Document config);

    public abstract void initialize(Document config) throws Exception;

    public abstract void loadConfiguration(ChannelManager channelManager);

    public abstract void loadConfiguration(NNTPServer nntpServer);

    public abstract Map loadChannels(ChannelManager channelManager);

    public abstract Map loadCategories();

    public abstract void addChannel(Channel channel);
    public abstract void addCategory(Category category);

    public abstract void addChannelToCategory(
        Channel channel,
        Category category);
    public abstract void removeChannelFromCategory(
        Channel channel,
        Category category);

    public abstract void updateChannel(Channel channel);

    public abstract void updateCategory(Category category);

    public abstract void deleteChannel(Channel channel);

    public abstract void deleteCategory(Category category);

    public abstract Item loadItem(Category category, int articleNumber);

    public abstract Item loadItem(Channel channel, int articleNumber);

    public abstract Item loadNextItem(
        Category category,
        int relativeArticleNumber);

    public abstract Item loadNextItem(
        Channel channel,
        int relativeArticleNumber);

    public abstract Item loadPreviousItem(
        Category category,
        int relativeArticleNumber);

    public abstract Item loadPreviousItem(
        Channel channel,
        int relativeArticleNumber);

    public abstract Item loadItem(Channel channel, String signature);

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

    public abstract List loadItems(
        Category category,
        int[] articleRange,
        boolean onlyHeaders,
        int limit);

    public abstract List loadItems(
        Channel channel,
        int[] articleRange,
        boolean onlyHeaders,
        int limit);

    public abstract List loadArticleNumbers(Category category);

    /**
     * Method loadArticleNumbers
     * @param channel
     * @return List
     * 
     * Supports NNTP listgroup command
     */
    public abstract List loadArticleNumbers(Channel channel);

    public abstract void saveItem(Item item);

    public abstract void saveConfiguration(ChannelManager channelManager);

    public abstract void saveConfiguration(NNTPServer nntpServer);

    public abstract void deleteExpiredItems(
        Channel channel,
        Set currentItemSignatures);

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteItemsNotInSet(org.methodize.nntprss.feed.Channel, java.util.Set)
     */
    public abstract void deleteItemsNotInSet(
        Channel channel,
        Set itemSignatures);

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#findNewItemSignatures(int, java.util.Set)
     */
    public abstract Set findNewItemSignatures(
        Channel channel,
        Set itemSignatures);

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

			// Initialize database specific values
			migrateInitializeDatabase();

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
			Map channelMap = new TreeMap();

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

				if (log.isInfoEnabled()) {
					log.info(
						"Added Channel "
							+ channel
							+ " (origId="
							+ origId
							+ ")");
				}
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

				int channelOrigId = ((Integer) entry.getKey()).intValue();
				int count = 0;
				boolean moreResults = true;
				Channel channel = (Channel) entry.getValue();

				if (log.isInfoEnabled()) {
					log.info(
						"Migrating items from channel "
							+ channel.getName()
							+ " (origId="
							+ channelOrigId
							+ ")");
				}

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

						try {
							saveItem(item);
						} catch (Exception e) {
							String msg =
								"Migration failed: Exception thrown while trying to save item "
									+ item
									+ " in channel "
									+ channel;
							log.fatal(msg, e);
							throw new RuntimeException(msg);
						}

						recCount++;
					}

					if (recCount < 1000) {
						moreResults = false;
					}

					stmt.close();
					rs.close();

					count += recCount;

					if (log.isInfoEnabled()) {
						if (moreResults) {
							log.info(
								"Migrating items... "
									+ (totalCount + count)
									+ " items moved");
						}
					}
				}

				channel.setTotalArticles(count);
				updateChannel(channel);

				totalCount += count;

				if (log.isInfoEnabled()) {
					log.info(
						"Migrated "
							+ count
							+ " items (total "
							+ totalCount
							+ ") for channel "
							+ channel.getName());
				}
			}

			if (log.isInfoEnabled()) {
				log.info("Finished migrating items. " + totalCount + " items migrated.");
			}

			// Shutdown hsqldb
			stmt.execute("SHUTDOWN");
			hsqlFound = true;
		} catch (Exception e) {
			if (log.isEnabledFor(Priority.ERROR)) {
				log.error("Exception thrown when trying to migrate hsqldb", e);
			}
			throw new RuntimeException("Exception throws whent rying to migrate hsqldb " + e.getMessage());
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

	boolean migrateJdbm() {
		boolean jdbmFound = false;

		long recID = 0;
		RecordManager recMan = null;
		
		// Check for nntp//rss v0.4 jdbm database - if found, migrate...
		try {
			recMan = RecordManagerFactory.createRecordManager(JdbmChannelDAO.DATABASE);
			recID = recMan.getNamedObject(JdbmChannelDAO.RECORD_CHANNELS_BTREE);
		} catch(IOException ie) {
		}
		
		if (recID != 0) {
		
			try {
				if (log.isInfoEnabled()) {
					log.info("Migrating jdbm to JDBC Database");
				}
	
				// Initialize database specific values
				migrateInitializeDatabase();
	
				// TODO Get config...
				if (log.isInfoEnabled()) {
					log.info("Migrating system configuration...");
				}

				ChannelManager channelManager =
					ChannelManager.getChannelManager();
				ChannelManager channelManagerInstance =
					(ChannelManager) recMan.fetch(
						recMan.getNamedObject(JdbmChannelDAO.RECORD_CHANNEL_CONFIG),
						new InstanceJdbmSerializer(channelManager));
		
				channelManager.setPollingIntervalSeconds(
					channelManagerInstance.getPollingIntervalSeconds());
				channelManager.setProxyServer(
					channelManagerInstance.getProxyServer());
				channelManager.setProxyPort(
					channelManagerInstance.getProxyPort());
				channelManager.setProxyUserID(
					channelManagerInstance.getProxyUserID());
				channelManager.setProxyPassword(
					channelManagerInstance.getProxyPassword());
				saveConfiguration(channelManager);
	
				NNTPServer nntpServer = new NNTPServer();
				loadConfiguration(nntpServer);
				NNTPServer nntpServerInstance =
					(NNTPServer) recMan.fetch(
						recMan.getNamedObject(JdbmChannelDAO.RECORD_NNTP_CONFIG),
						new InstanceJdbmSerializer(nntpServer));
				nntpServer.setContentType(
					nntpServer.getContentType());
				nntpServer.setSecure(
					nntpServer.isSecure());
				saveConfiguration(nntpServer);
	
				if (log.isInfoEnabled()) {
					log.info("Finished migration system configuration...");
				}
	
				if (log.isInfoEnabled()) {
					log.info("Migrating channel configuration...");
				}
	
				Map channelMap = new TreeMap();
				BTree btChannels = BTree.load(recMan, recID);
				TupleBrowser browser = btChannels.browse();
				Tuple tuple = new Tuple();
				Map btItemsByIdMap = new HashMap();
				while (browser.getNext(tuple)) {
					long recId = ((Long) tuple.getValue()).longValue();
					Channel origChannel =
						(Channel) recMan.fetch(
							recId,
							GenericJdbmSerializer.getSerializer(Channel.class));

					BTree bt =
						BTree.load(
							recMan,
							recMan.getNamedObject(
								JdbmChannelDAO.RECORD_ITEMS_BY_ID_BTREE + origChannel.getId()));
					btItemsByIdMap.put(new Integer(origChannel.getId()), bt);

					TupleBrowser articleBrowser = bt.browse(null);
					Tuple articleTuple = new Tuple();
					if (articleBrowser.getPrevious(articleTuple)) {
						origChannel.setLastArticleNumber(
							((Integer) articleTuple.getKey()).intValue());
					}

					int origId = origChannel.getId();
	
					Channel channel =
						new Channel(origChannel.getName(), origChannel.getUrl());
					channel.setAuthor(origChannel.getAuthor());
					channel.setTitle(origChannel.getTitle());
					channel.setLink(origChannel.getLink());
					channel.setDescription(origChannel.getDescription());
					channel.setLastArticleNumber(origChannel.getLastArticleNumber());
					channel.setCreated(origChannel.getCreated());
					channel.setRssVersion(origChannel.getRssVersion());
					channel.setExpiration(origChannel.getExpiration());
					channel.setEnabled(origChannel.isEnabled());
					channel.setPostingEnabled(origChannel.isPostingEnabled());
					channel.setParseAtAllCost(origChannel.isParseAtAllCost());
					channel.setPublishAPI(origChannel.getPublishAPI());
					channel.setPublishConfig(origChannel.getPublishConfig());
					channel.setManagingEditor(origChannel.getManagingEditor());
					channel.setPollingIntervalSeconds(origChannel.getPollingIntervalSeconds());
					addChannel(channel);
	
					channelMap.put(new Integer(origId), channel);
	
					if (log.isInfoEnabled()) {
						log.info(
							"Added Channel "
								+ channel
								+ " (origId="
								+ origId
								+ ")");
					}					
				}
					
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
	
					int channelOrigId = ((Integer) entry.getKey()).intValue();
					int count = 0;
					boolean moreResults = true;
					Channel channel = (Channel) entry.getValue();
	
					if (log.isInfoEnabled()) {
						log.info(
							"Migrating items from channel "
								+ channel.getName()
								+ " (origId="
								+ channelOrigId
								+ ")");
					}

					BTree bt = (BTree) btItemsByIdMap.get(new Integer(channelOrigId));
					browser = bt.browse();
					tuple = new Tuple();
					while (browser.getNext(tuple)) {
						boolean match = true;
						int id = ((Integer) tuple.getKey()).intValue();
						Item origItem =
							(Item) recMan.fetch(
								((Long) tuple.getValue()).longValue(),
								GenericJdbmSerializer.getSerializer(Item.class));

						Item item = new Item();
						item.setArticleNumber(origItem.getArticleNumber());
						item.setChannel(channel);
						item.setTitle(origItem.getTitle());
						item.setLink(origItem.getLink());
						item.setDescription(origItem.getDescription());
						item.setComments(origItem.getComments());
						item.setDate(origItem.getDate());
						item.setSignature(origItem.getSignature());
	
						try {
							saveItem(item);
						} catch (Exception e) {
							String msg =
								"Migration failed: Exception thrown while trying to save item "
									+ item
									+ " in channel "
									+ channel;
							log.fatal(msg, e);
								throw new RuntimeException(msg);
						}
	
						count++;

						if (log.isInfoEnabled()) {
							if (count % 1000 == 0) {
								log.info(
									"Migrating items... "
										+ (totalCount + count)
										+ " items moved");
							}
						}
					}
	
					channel.setTotalArticles(count);
					updateChannel(channel);
	
					totalCount += count;
	
					if (log.isInfoEnabled()) {
						log.info(
							"Migrated "
								+ count
								+ " items (total "
								+ totalCount
								+ ") for channel "
								+ channel.getName());
					}
				}
	
				if (log.isInfoEnabled()) {
					log.info("Finished migrating items. " + totalCount + " items migrated.");
				}
	
				// TODO Shutdown jdbm
				recMan.commit();
				recMan.close();

				jdbmFound = true;
			} catch (Exception e) {
				if (log.isEnabledFor(Priority.ERROR)) {
					log.error("Exception thrown when trying to migrate jdbm", e);
				}
				throw new RuntimeException("Exception throws whent rying to migrate jdbm " + e.getMessage());
			} finally {
			}

		}
		return jdbmFound;
	}


	abstract void migrateInitializeDatabase() throws Exception;
}