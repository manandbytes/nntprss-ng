package org.methodize.nntprss.feed.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.FastIterator;
import jdbm.helper.IntegerComparator;
import jdbm.helper.Serializer;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.htree.HTree;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: JdbmChannelDAO.java,v 1.3 2003/09/28 21:14:25 jasonbrome Exp $
 */
public class JdbmChannelDAO extends ChannelDAO {

	private static final String DATABASE = "nntprss";
	private static final String RECORD_CHANNEL_CONFIG = "ChannelConfig";
	private static final String RECORD_NNTP_CONFIG = "NNTPConfig";
	private static final String RECORD_CHANNELS_BTREE = "Channels";
	private static final String RECORD_ITEMS_BY_ID_BTREE = "ItemsById.";
	private static final String RECORD_ITEMS_BY_SIGNATURE_HTREE =
		"ItemsBySignature.";
	private static final String RECORD_LAST_CHANNEL_ID = "LastChannelID";

	private BTree btChannels = null;
	private Map btItemsByIdMap = new HashMap();
	private Map htItemsBySigMap = new HashMap();
	private RecordManager recMan = null;
	private int lastChannelId = 0;

	private Logger log = Logger.getLogger(JdbmChannelDAO.class);

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#addChannel(org.methodize.nntprss.feed.Channel)
	 */
	public void addChannel(Channel channel) {
		try {
			lastChannelId++;
			channel.setId(lastChannelId);
			long recId =
				recMan.insert(
					channel,
					GenericJdbmSerializer.getSerializer(Channel.class));
			btChannels.insert(
				new Integer(channel.getId()),
				new Long(recId),
				false);

			BTree btItemsById =
				BTree.createInstance(recMan, new IntegerComparator());
			recMan.setNamedObject(
				RECORD_ITEMS_BY_ID_BTREE + channel.getId(),
				btItemsById.getRecid());
			btItemsByIdMap.put(channel, btItemsById);
			HTree htItemsBySig =
				HTree.createInstance(recMan);
			recMan.setNamedObject(
				RECORD_ITEMS_BY_SIGNATURE_HTREE + channel.getId(),
				htItemsBySig.getRecid());
			htItemsBySigMap.put(channel, htItemsBySig);

			recMan.update(
				recMan.getNamedObject(RECORD_LAST_CHANNEL_ID),
				new Integer(lastChannelId));
			recMan.commit();

		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#createTables()
	 */
	protected void createTables() {
		if (log.isInfoEnabled()) {
			log.info("Creating application database tables");
		}

		try {
			btChannels = BTree.createInstance(recMan, new IntegerComparator());
			recMan.setNamedObject(RECORD_CHANNELS_BTREE, btChannels.getRecid());
			//			btItemsById = BTree.createInstance(recMan, new IntegerComparator());
			//			btItemsBySig = BTree.createInstance(recMan, new StringComparator());

			ChannelManager chlMgr = ChannelManager.getChannelManager();
			chlMgr.setObserveHttp301(true);
			chlMgr.setPollingIntervalSeconds(60 * 60);

			long recID =
				recMan.insert(
					chlMgr,
					GenericJdbmSerializer.getSerializer(ChannelManager.class));
			recMan.setNamedObject(RECORD_CHANNEL_CONFIG, recID);

			NNTPServer nntpServer = new NNTPServer();
			nntpServer.setContentType(AppConstants.CONTENT_TYPE_MIXED);
			nntpServer.setSecure(false);
			nntpServer.setFootnoteUrls(true);
			nntpServer.setHostName(AppConstants.getCurrentHostName());

			recID =
				recMan.insert(
					nntpServer,
					GenericJdbmSerializer.getSerializer(NNTPServer.class));
			recMan.setNamedObject(RECORD_NNTP_CONFIG, recID);

			recMan.commit();

		} catch (IOException ie) {
			if (log.isEnabledFor(Priority.ERROR)) {
				log.error("Error creating application database tables", ie);
			}
			throw new RuntimeException(
				"Error creating application tables - " + ie.getMessage());
		}

		if (log.isInfoEnabled()) {
			log.info("Finished creating application database tables");
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteChannel(org.methodize.nntprss.feed.Channel)
	 */
	public void deleteChannel(Channel channel) {
		try {
			Integer chlId = new Integer(channel.getId());
			long recId = ((Long) btChannels.find(chlId)).longValue();
			btChannels.remove(chlId);
			recMan.delete(recId);

			HTree treeSig = (HTree) htItemsBySigMap.get(channel);
			BTree treeId = (BTree) btItemsByIdMap.get(channel);

			TupleBrowser browser = treeId.browse();
			Tuple tuple = new Tuple();
			while (browser.getNext(tuple)) {
				recMan.delete(((Long) tuple.getValue()).longValue());
			}

			htItemsBySigMap.remove(channel);
			btItemsByIdMap.remove(channel);

			recId = treeSig.getRecid();
			recMan.delete(recId);

			recId = treeId.getRecid();
			recMan.delete(recId);

			recMan.commit();
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		// Remove items

		// Delete btrees

		// Delete channel

	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteItemsNotInSet(org.methodize.nntprss.feed.Channel, java.util.Set)
	 */
	public void deleteItemsNotInSet(Channel channel, Set itemSignatures) {
		int firstArticle = channel.getLastArticleNumber();
		try {
			HTree treeSig = (HTree) htItemsBySigMap.get(channel);
			BTree treeId = (BTree) btItemsByIdMap.get(channel);
			Set articlesToKeep = new HashSet();
			Set articlesToRemove = new HashSet(); 

			FastIterator iter = treeSig.keys();
			String sig = null; 

			while ((sig = (String)iter.next()) != null) {
				if (itemSignatures.contains(sig)) {
					// Keep...
					long recId = ((Long)treeSig.get(sig)).longValue();
					Item item =
						(Item) recMan.fetch(
							recId,
							GenericJdbmSerializer.getSerializer(Item.class));
					if (item.getArticleNumber() < firstArticle) {
						firstArticle = item.getArticleNumber();
					}

					articlesToKeep.add(new Integer(item.getArticleNumber()));
				} else {
					// Delete record...
					long recId = ((Long)treeSig.get(sig)).longValue();
					articlesToRemove.add(sig);
					recMan.delete(recId);
				}
			}

			Iterator removeIter = articlesToRemove.iterator();
			while(removeIter.hasNext()) {
				sig = (String)removeIter.next();
				treeSig.remove(sig);
			}

			TupleBrowser browser = treeId.browse();
			Tuple tuple = new Tuple();
			articlesToRemove.clear();
			while (browser.getNext(tuple)) {
				if (!articlesToKeep.contains(tuple.getKey())) {
					articlesToRemove.add(tuple.getKey());
//					treeId.remove(tuple.getKey());
				}
			}

			removeIter = articlesToRemove.iterator();
			while(removeIter.hasNext()) {
				Integer articleNumber = (Integer)removeIter.next();
				treeId.remove(articleNumber);
			}

			recMan.commit();
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}

		if (firstArticle == 0) {
			if (channel.getLastArticleNumber() == 0) {
				channel.setFirstArticleNumber(1);
			} else {
				channel.setFirstArticleNumber(channel.getLastArticleNumber());
			}
		} else {
			channel.setFirstArticleNumber(firstArticle);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#findNewItemSignatures(int, java.util.Set)
	 */
	public Set findNewItemSignatures(Channel channel, Set itemSignatures) {
		Set newSignatures = new HashSet();
		newSignatures.addAll(itemSignatures);

		try {
			HTree tree = (HTree) htItemsBySigMap.get(channel);
			FastIterator browser = tree.keys();
			String sig = null;
			while ((sig = (String)browser.next()) != null) {
				if (newSignatures.contains(sig))
					newSignatures.remove(sig);
			}
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}

		return newSignatures;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#initialize(org.w3c.dom.Document)
	 */
	public void initialize(Document config) throws Exception {
		//		Properties props = new Properties();
		boolean createTables = false;

		recMan = RecordManagerFactory.createRecordManager(DATABASE);
		long recID = recMan.getNamedObject(RECORD_CHANNELS_BTREE);
		if (recID == 0) {
			createTables();
			populateInitialChannels(config);
			recID = recMan.getNamedObject(RECORD_CHANNELS_BTREE);
		}

		btChannels = BTree.load(recMan, recID);
		lastChannelId =
			((Integer) recMan
				.fetch(recMan.getNamedObject(RECORD_LAST_CHANNEL_ID)))
				.intValue();
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadArticleNumbers(org.methodize.nntprss.feed.Channel)
	 */
	public List loadArticleNumbers(Channel channel) {
		Set articleNumbers = new TreeSet(new IntegerComparator());
		try {
			BTree bt = (BTree) btItemsByIdMap.get(channel);
			TupleBrowser browser = bt.browse();
			Tuple tuple = new Tuple();
			while (browser.getNext(tuple)) {
				articleNumbers.add(tuple.getKey());
			}
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		return new ArrayList(articleNumbers);
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadChannels()
	 */
	public Map loadChannels() {
		Map channels = new TreeMap();
		try {
			TupleBrowser browser = btChannels.browse();
			Tuple tuple = new Tuple();
			while (browser.getNext(tuple)) {
				long recId = ((Long) tuple.getValue()).longValue();
				Channel channel =
					(Channel) recMan.fetch(
						recId,
						GenericJdbmSerializer.getSerializer(Channel.class));
				channels.put(channel.getName(), channel);

				BTree bt =	BTree.load(recMan, 
						recMan.getNamedObject(
							RECORD_ITEMS_BY_ID_BTREE + channel.getId()));
				btItemsByIdMap.put(channel, bt);

				TupleBrowser articleBrowser = bt.browse(null);
				Tuple articleTuple = new Tuple();
				if(articleBrowser.getPrevious(articleTuple)) {
					channel.setLastArticleNumber(((Integer)articleTuple.getKey()).intValue());
				}

				HTree ht = HTree.load(recMan, 
						recMan.getNamedObject(
							RECORD_ITEMS_BY_SIGNATURE_HTREE + channel.getId()));
				htItemsBySigMap.put(channel, ht);
			}
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}

		return channels;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadConfiguration(org.methodize.nntprss.feed.ChannelManager)
	 */
	public void loadConfiguration(ChannelManager channelManager) {
		try {
			ChannelManager channelManagerInstance = 
				(ChannelManager)recMan.fetch(
				recMan.getNamedObject(RECORD_CHANNEL_CONFIG),
				new InstanceJdbmSerializer(channelManager));
//			If item retrieved from cache, ensure that instance is appropriately 
//			updated
			 transferExternalState(channelManagerInstance, channelManager);

		} catch (Exception ie) {
			throw new RuntimeException(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadConfiguration(org.methodize.nntprss.nntp.NNTPServer)
	 */
	public void loadConfiguration(NNTPServer nntpServer) {
		try {
			NNTPServer nntpServerInstance = (NNTPServer)recMan.fetch(
				recMan.getNamedObject(RECORD_NNTP_CONFIG),
				new InstanceJdbmSerializer(nntpServer));
// If item retrieved from cache, ensure that instance is appropriately 
// updated
			transferExternalState(nntpServerInstance, nntpServer);
		} catch (Exception ie) {
			throw new RuntimeException(ie);
		}
	}
	private void transferExternalState(
		Externalizable source,
		Externalizable destination)
		throws IOException, ClassNotFoundException {
		if(source != destination) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			source.writeExternal(oos);
			oos.flush();
			oos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			destination.readExternal(ois);		
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadItem(org.methodize.nntprss.feed.Channel, int)
	 */
	public Item loadItem(Channel channel, int articleNumber) {
		Item item = null;
		try {
			long recID =
				((Long) ((BTree) btItemsByIdMap.get(channel))
					.find(new Integer(articleNumber)))
					.longValue();
			item = (Item) recMan.fetch(recID, GenericJdbmSerializer.getSerializer(Item.class));
			item.setChannel(channel);
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		return item;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadItem(org.methodize.nntprss.feed.Channel, java.lang.String)
	 */
	public Item loadItem(Channel channel, String signature) {
		Item item = null;
		try {
			long recID =
				((Long) ((HTree) htItemsBySigMap.get(channel)).get(signature))
					.longValue();
			item = (Item) recMan.fetch(recID, GenericJdbmSerializer.getSerializer(Item.class));
			item.setChannel(channel);
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		return item;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadItems(org.methodize.nntprss.feed.Channel, int[], boolean)
	 */
	public List loadItems(
		Channel channel,
		int[] articleRange,
		boolean onlyHeaders,
		int limit) {

		List items = new ArrayList();
		try {
			BTree bt = (BTree) btItemsByIdMap.get(channel);
			
			TupleBrowser browser = null;
			if(articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
				browser = bt.browse(new Integer(articleRange[0]));
			} else {
				browser = bt.browse();
			}
			Tuple tuple = new Tuple();
			while (browser.getNext(tuple)) {
				boolean match = true;
				int id = ((Integer) tuple.getKey()).intValue();
				if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE
					&& id < articleRange[0])
					match = false;

				if (articleRange[1] != AppConstants.OPEN_ENDED_RANGE
					&& id > articleRange[1])
// End of article range reached.
					break;
//					match = false;

				if (match) {
					Item item =
						(Item) recMan.fetch(
							((Long) tuple.getValue()).longValue(),
							GenericJdbmSerializer.getSerializer(Item.class));
					item.setChannel(channel);
					items.add(item);

					if(limit != LIMIT_NONE && items.size() == limit) 
// Break if maximum items returned...
						break;
				}
			}
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		return items;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadNextItem(org.methodize.nntprss.feed.Channel, int)
	 */
	public Item loadNextItem(Channel channel, int relativeArticleNumber) {
		Item item = null;
		try {
			BTree tree = (BTree) btItemsByIdMap.get(channel);
			TupleBrowser browser =
				tree.browse(new Integer(relativeArticleNumber));
			Tuple tuple = new Tuple();
			if (browser.getNext(tuple)) {
				// Skip the current and move on to the next record
				if (browser.getNext(tuple)) {
					long recID = ((Long) tuple.getValue()).longValue();
					item = (Item) recMan.fetch(recID, GenericJdbmSerializer.getSerializer(Item.class));
					item.setChannel(channel);
				}
			}
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		return item;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#loadPreviousItem(org.methodize.nntprss.feed.Channel, int)
	 */
	public Item loadPreviousItem(Channel channel, int relativeArticleNumber) {
		Item item = null;
		try {
			BTree tree = (BTree) btItemsByIdMap.get(channel);
			TupleBrowser browser =
				tree.browse(new Integer(relativeArticleNumber));
			Tuple tuple = new Tuple();
			if (browser.getPrevious(tuple)) {
				// Skip the current and move on to the next record
				long recID = ((Long) tuple.getValue()).longValue();
				item = (Item) recMan.fetch(recID, GenericJdbmSerializer.getSerializer(Item.class));
				item.setChannel(channel);
			}
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
		return item;
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#populateInitialChannels(org.w3c.dom.Document)
	 */
	protected void populateInitialChannels(Document config) {

		if(!migrateHsql()) {
			if (log.isInfoEnabled()) {
				log.info("Loading channels");
			}
	
			try {
				NodeList channelsList =
					config.getDocumentElement().getElementsByTagName("channels");
	
				int channelCount = 0;
	
				if (channelsList.getLength() > 0) {
					Element channelsElm = (Element) channelsList.item(0);
					NodeList channelList =
						channelsElm.getElementsByTagName("channel");
					if (channelList.getLength() > 0) {
						for (channelCount = 0;
							channelCount < channelList.getLength();
							channelCount++) {
							Element channelElm =
								(Element) channelList.item(channelCount);
	
							Channel channel =
								new Channel(
									channelElm.getAttribute("name"),
									channelElm.getAttribute("url"));
	
							String historicalStr =
								channelElm.getAttribute("historical");
							boolean historical = true;
							if (historicalStr != null) {
								channel.setHistorical(
									historicalStr.equalsIgnoreCase("true"));
							}
	
							channel.setCreated(
								new Date(System.currentTimeMillis()));
							// Last Article
							channel.setLastArticleNumber(0);
							channel.setEnabled(true);
							channel.setPostingEnabled(false);
							channel.setParseAtAllCost(false);
							channel.setPollingIntervalSeconds(0);
							channel.setStatus(Channel.STATUS_OK);
							channel.setId(lastChannelId++);
							long recId =
								recMan.insert(
									channel,
									GenericJdbmSerializer.getSerializer(
										Channel.class));
							btChannels.insert(
								new Integer(channel.getId()),
								new Long(recId),
								false);
	
							BTree btItemsById =
								BTree.createInstance(
									recMan,
									new IntegerComparator());
							recMan.setNamedObject(
								RECORD_ITEMS_BY_ID_BTREE + channel.getId(),
								btItemsById.getRecid());
							HTree htItemsBySig =
								HTree.createInstance(
									recMan);
							recMan.setNamedObject(
								RECORD_ITEMS_BY_SIGNATURE_HTREE + channel.getId(),
								htItemsBySig.getRecid());
						}
						recMan.commit();
					}
	
					long recID = recMan.insert(new Integer(channelCount));
					recMan.setNamedObject(RECORD_LAST_CHANNEL_ID, recID);
					recMan.commit();
	
				}
	
			} catch (IOException ie) {
	
				if (log.isEnabledFor(Priority.ERROR)) {
					log.error("Error loading initial channels", ie);
				}
				throw new RuntimeException(
					"Error loading initial channels - " + ie.getMessage());
	
			}
	
			if (log.isInfoEnabled()) {
				log.info("Finished loading initial channels");
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#saveConfiguration(org.methodize.nntprss.feed.ChannelManager)
	 */
	public void saveConfiguration(ChannelManager channelManager) {
		try {
			recMan.update(
				recMan.getNamedObject(RECORD_CHANNEL_CONFIG),
				channelManager,
				GenericJdbmSerializer.getSerializer(ChannelManager.class));
			recMan.commit();
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#saveConfiguration(org.methodize.nntprss.nntp.NNTPServer)
	 */
	public void saveConfiguration(NNTPServer nntpServer) {
		try {
			recMan.update(
				recMan.getNamedObject(RECORD_NNTP_CONFIG),
				nntpServer,
				GenericJdbmSerializer.getSerializer(NNTPServer.class));
			recMan.commit();
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#saveItem(org.methodize.nntprss.feed.Item)
	 */
	public void saveItem(Item item) {
		// Insert
		try {
			long recID =
				recMan.insert(
					item,
					GenericJdbmSerializer.getSerializer(Item.class));
			((BTree) btItemsByIdMap.get(item.getChannel())).insert(
				new Integer(item.getArticleNumber()),
				new Long(recID),
				false);
			((HTree) htItemsBySigMap.get(item.getChannel())).put(
				item.getSignature(),
				new Long(recID));
			recMan.commit();
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#shutdown()
	 */
	public void shutdown() {
		try {
			// Do I need to iterate through the BTrees and close them?

			recMan.commit();
			recMan.close();
		} catch (IOException ie) {
		}

	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#updateChannel(org.methodize.nntprss.feed.Channel)
	 */
	public void updateChannel(Channel channel) {
		try {
			long recId =
				((Long) btChannels.find(new Integer(channel.getId())))
					.longValue();
			recMan.update(recId, channel, GenericJdbmSerializer.getSerializer(Channel.class));
			recMan.commit();
		} catch (IOException ie) {
			throw new RuntimeException(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.methodize.nntprss.feed.db.ChannelDAO#upgradeDatabase(int)
	 */
	protected void upgradeDatabase(int dbVersion) {
	}

	public static void main(String[] args) {
		// JDBM test...

		try {
			Channel channel = new Channel();
			channel.setId(123);
			channel.setTitle("Testing");
			channel.setUrl(new URL("http://www.jasonbrome.com/blog/index.rdf"));

			RecordManager recMan =
				RecordManagerFactory.createRecordManager(DATABASE);
			//		long recID = recMan.getNamedObject(RECORD_CHANNELS_BTREE);
			//		if (recID == 0) {
			//			createTables();
			//			populateInitialChannels(config);
			//			recID = recMan.getNamedObject(RECORD_CHANNELS_BTREE);
			//		}

			//		btChannels = BTree.load(recMan, recID);
			//		lastChannelId = ((Integer)recMan.fetch(recMan.getNamedObject(RECORD_LAST_CHANNEL_ID))).intValue();

			long recId =
				recMan.insert(
					channel,
					GenericJdbmSerializer.getSerializer(Channel.class));

			recMan.commit();
			recMan.close();

			recMan = RecordManagerFactory.createRecordManager(DATABASE);
			Channel channel2 =
				(Channel) recMan.fetch(
					recId,
					GenericJdbmSerializer.getSerializer(Channel.class));
			System.out.println(channel2.getTitle());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean migrateHsql() {
		boolean hsqlFound = false;

		//		Check for nntp//rss v0.3 hsqldb database - if found, migrate...
		Connection hsqlConn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			Class.forName("org.hsqldb.jdbcDriver");
			hsqlConn =
				DriverManager.getConnection("jdbc:hsqldb:nntprssdb", "sa", "");

			if(log.isInfoEnabled()) {
				log.info("Migrating hsqldb to JDBM");
			}

			stmt = hsqlConn.createStatement();

			try {
				rs = stmt.executeQuery("SELECT * FROM config");
			} catch(SQLException e) {
// Assume that hsqldb is not found...
				   if (log.isEnabledFor(Priority.WARN)) {
					   log.warn("Exising hsqldb database not found, skipping migration");
				   }
				return hsqlFound;
			}

			if(log.isInfoEnabled()) {
				log.info("Migrating system configuration...");
			}

			if (rs.next()) {
				ChannelManager channelManager = ChannelManager.getChannelManager();
				channelManager.setPollingIntervalSeconds(rs.getLong("pollingInterval"));
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

			if(log.isInfoEnabled()) {
				log.info("Finished migration system configuration...");
			}

			if(log.isInfoEnabled()) {
				log.info("Migrating channel configuration...");
			}

			rs = stmt.executeQuery("SELECT * FROM channels");
			Map channelMap = new HashMap();

// Establish LAST_CHANNEL_ID entry within persistent store
			int channelCount = 0;
			long recID = recMan.insert(new Integer(channelCount));
			recMan.setNamedObject(RECORD_LAST_CHANNEL_ID, recID);

			while (rs.next()) {
				int origId = rs.getInt("id");

				Channel channel = new Channel(rs.getString("name"),
					rs.getString("url"));
				channel.setAuthor(rs.getString("author"));
				channel.setTitle(rs.getString("title"));
				channel.setLink(rs.getString("link"));
				channel.setDescription(rs.getString("description"));
				channel.setLastArticleNumber(rs.getInt("lastArticle"));
				channel.setCreated(rs.getTimestamp("created"));
				channel.setRssVersion(rs.getString("rssVersion"));
				channel.setHistorical(rs.getBoolean("historical"));
				channel.setEnabled(rs.getBoolean("enabled"));
				channel.setPostingEnabled(rs.getBoolean("postingEnabled"));
				channel.setParseAtAllCost(rs.getBoolean("parseAtAllCost"));
				channel.setPublishAPI(rs.getString("publishAPI"));
				channel.setPublishConfig(XMLHelper.xmlToStringHashMap(rs.getString("publishConfig")));
				channel.setManagingEditor(rs.getString("managingEditor"));
				channel.setPollingIntervalSeconds(rs.getLong("pollingInterval"));
				addChannel(channel);

				channelMap.put(new Integer(origId), channel);
			}

			stmt.close();
			rs.close();

			if(log.isInfoEnabled()) {
				log.info("Finished migrating channel configuration...");
			}

			if(log.isInfoEnabled()) {
				log.info("Migrating items...");
			}

			// Copy channel items...
			Iterator channelIter = channelMap.entrySet().iterator();
			int totalCount = 0;
			while(channelIter.hasNext()) {

				Map.Entry entry = (Map.Entry)channelIter.next();

				int count = 0;
				boolean moreResults = true;
				Serializer itemSerializer = GenericJdbmSerializer.getSerializer(Item.class);
				Channel channel = (Channel)entry.getValue();
				while(moreResults) {
					rs =
						stmt.executeQuery(
							"SELECT LIMIT " + count + " 1000 * FROM items WHERE channel = " + 
								((Integer)entry.getKey()).intValue());
	
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
							recID =
								recMan.insert(
									item,
									itemSerializer);
							((BTree) btItemsByIdMap.get(item.getChannel())).insert(
								new Integer(item.getArticleNumber()),
								new Long(recID),
								false);
							((HTree) htItemsBySigMap.get(item.getChannel())).put(
								item.getSignature(),
								new Long(recID));
							recCount++;
						}
						
						if(recCount < 1000) {
							moreResults = false;
						}

						stmt.close();
						rs.close();
	
					count+= recCount;

					recMan.commit();					
				}

				channel.setTotalArticles(count);
				updateChannel(channel);

				totalCount += count;
				
				if(log.isInfoEnabled()) {
					log.info("Migrating items... " + totalCount + " items moved");
				}
				
			}

//			recMan.commit();

			if(log.isInfoEnabled()) {
				log.info("Finished migrating items...");
			}

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
				if (hsqlConn != null)
					hsqlConn.close();
			} catch (Exception e) {
			}
		}

		return hsqlFound;
	}


}
