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

import java.io.*;
import java.net.URL;
import java.util.*;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.FastIterator;
import jdbm.helper.IntegerComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.htree.HTree;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Category;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.util.AppConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: JdbmChannelDAO.java,v 1.10 2004/03/28 22:27:13 jasonbrome Exp $
 */
public class JdbmChannelDAO extends ChannelDAO {

    private static final String DATABASE = "nntprss";
    private static final String RECORD_CHANNEL_CONFIG = "ChannelConfig";
    private static final String RECORD_NNTP_CONFIG = "NNTPConfig";
    private static final String RECORD_CATEGORIES_BTREE = "Categories";
    private static final String RECORD_CHANNELS_BTREE = "Channels";
    private static final String RECORD_ITEMS_BY_ID_BTREE = "ItemsById.";
    private static final String RECORD_ITEMS_BY_SIGNATURE_HTREE =
        "ItemsBySignature.";

    private static final String RECORD_CATEGORY_ITEMS_BY_ID_BTREE =
        "CategoryItemsById.";

    private static final String RECORD_LAST_CHANNEL_ID = "LastChannelID";
    private static final String RECORD_LAST_CATEGORY_ID = "LastCategoryID";

    private BTree btCategories = null;
    private BTree btChannels = null;
    private Map btItemsByIdMap = new HashMap();
    private Map htItemsBySigMap = new HashMap();

    private Map btCategoryItemsByIdMap = new HashMap();

    private RecordManager recMan = null;
    private int lastChannelId = 0;
    private int lastCategoryId = 0;

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
            btItemsByIdMap.put(new Integer(channel.getId()), btItemsById);
            HTree htItemsBySig = HTree.createInstance(recMan);
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

            btCategories =
                BTree.createInstance(recMan, new IntegerComparator());
            recMan.setNamedObject(
                RECORD_CATEGORIES_BTREE,
                btCategories.getRecid());

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

            // Delete channel
            btChannels.remove(chlId);
            recMan.delete(recId);

            HTree treeSig = (HTree) htItemsBySigMap.get(channel);
            BTree treeId =
                (BTree) btItemsByIdMap.get(new Integer(channel.getId()));

            // Remove items
            TupleBrowser browser = treeId.browse();
            Tuple tuple = new Tuple();
            while (browser.getNext(tuple)) {
                recMan.delete(((Long) tuple.getValue()).longValue());
            }

            htItemsBySigMap.remove(channel);
            btItemsByIdMap.remove(new Integer(channel.getId()));

            // Delete btrees
            recId = treeSig.getRecid();
            recMan.delete(recId);

            recId = treeId.getRecid();
            recMan.delete(recId);

            recMan.commit();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }

    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteItemsNotInSet(org.methodize.nntprss.feed.Channel, java.util.Set)
     */
    public void deleteItemsNotInSet(Channel channel, Set itemSignatures) {
        int firstArticle = channel.getLastArticleNumber();
        try {
            HTree treeSig = (HTree) htItemsBySigMap.get(channel);
            BTree treeId =
                (BTree) btItemsByIdMap.get(new Integer(channel.getId()));
            Set articlesToKeep = new HashSet();
            Set articlesToRemove = new HashSet();

            FastIterator iter = treeSig.keys();
            String sig = null;

            while ((sig = (String) iter.next()) != null) {
                if (itemSignatures.contains(sig)) {
                    // Keep...
                    long recId = ((Long) treeSig.get(sig)).longValue();
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
                    long recId = ((Long) treeSig.get(sig)).longValue();
                    articlesToRemove.add(sig);
                    recMan.delete(recId);
                }
            }

            Iterator removeIter = articlesToRemove.iterator();
            while (removeIter.hasNext()) {
                sig = (String) removeIter.next();
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
            while (removeIter.hasNext()) {
                Integer articleNumber = (Integer) removeIter.next();
                treeId.remove(articleNumber);
            }

            if (channel.getCategory() != null) {
                // Remove articles from category...
                BTree treeCatId =
                    (BTree) btCategoryItemsByIdMap.get(channel.getCategory());
                browser = treeCatId.browse();
                articlesToRemove.clear();
                while (browser.getNext(tuple)) {
                    long channelItemId = ((Long) tuple.getValue()).longValue();
                    int channelId = (int) (channelItemId >> 32);
                    int articleNumber = (int) (channelItemId & 0xFFFFFFFF);

                    if (channelId == channel.getId()
                        && !articlesToKeep.contains(new Integer(articleNumber))) {
                        articlesToRemove.add(tuple.getKey());
                    }
                }

                removeIter = articlesToRemove.iterator();
                while (removeIter.hasNext()) {
                    Integer articleNumber = (Integer) removeIter.next();
                    treeCatId.remove(articleNumber);
                }
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
            //			FastIterator browser = tree.keys();
            //			String sig = null;
            //			while ((sig = (String) browser.next()) != null) {
            //				if (newSignatures.contains(sig))
            //					newSignatures.remove(sig);
            //			}
            Iterator sigIter = newSignatures.iterator();
            while (sigIter.hasNext()) {
                String sig = (String) sigIter.next();
                if (tree.get(sig) != null) {
                    sigIter.remove();
                }
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

        recID = recMan.getNamedObject(RECORD_CATEGORIES_BTREE);
        btCategories = BTree.load(recMan, recID);

        lastChannelId =
            ((Integer) recMan
                .fetch(recMan.getNamedObject(RECORD_LAST_CHANNEL_ID)))
                .intValue();

        lastCategoryId =
            ((Integer) recMan
                .fetch(recMan.getNamedObject(RECORD_LAST_CATEGORY_ID)))
                .intValue();
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadArticleNumbers(org.methodize.nntprss.feed.Channel)
     */
    public List loadArticleNumbers(Channel channel) {
        Set articleNumbers = new TreeSet(new IntegerComparator());
        try {
            BTree bt = (BTree) btItemsByIdMap.get(new Integer(channel.getId()));
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

    public Map loadCategories() {
        Map categories = new TreeMap();
        try {
            TupleBrowser browser = btCategories.browse();
            Tuple tuple = new Tuple();
            while (browser.getNext(tuple)) {
                long recId = ((Long) tuple.getValue()).longValue();
                Category category =
                    (Category) recMan.fetch(
                        recId,
                        GenericJdbmSerializer.getSerializer(Category.class));
                categories.put(category.getName(), category);

                BTree bt =
                    BTree.load(
                        recMan,
                        recMan.getNamedObject(
                            RECORD_CATEGORY_ITEMS_BY_ID_BTREE
                                + category.getId()));
                btCategoryItemsByIdMap.put(category, bt);

                TupleBrowser articleBrowser = bt.browse(null);
                Tuple articleTuple = new Tuple();
                if (articleBrowser.getPrevious(articleTuple)) {
                    category.setLastArticleNumber(
                        ((Integer) articleTuple.getKey()).intValue());
                }
                category.setTotalArticles(bt.size());
            }
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }

        return categories;
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadChannels()
     */
    public Map loadChannels(ChannelManager channelManager) {
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

                BTree bt =
                    BTree.load(
                        recMan,
                        recMan.getNamedObject(
                            RECORD_ITEMS_BY_ID_BTREE + channel.getId()));
                btItemsByIdMap.put(new Integer(channel.getId()), bt);

                TupleBrowser articleBrowser = bt.browse(null);
                Tuple articleTuple = new Tuple();
                if (articleBrowser.getPrevious(articleTuple)) {
                    channel.setLastArticleNumber(
                        ((Integer) articleTuple.getKey()).intValue());
                }

                HTree ht =
                    HTree.load(
                        recMan,
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
                (ChannelManager) recMan.fetch(
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
            NNTPServer nntpServerInstance =
                (NNTPServer) recMan.fetch(
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
        if (source != destination) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            source.writeExternal(oos);
            oos.flush();
            oos.close();
            ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
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
                ((Long) ((BTree) btItemsByIdMap
                    .get(new Integer(channel.getId())))
                    .find(new Integer(articleNumber)))
                    .longValue();
            item =
                (Item) recMan.fetch(
                    recID,
                    GenericJdbmSerializer.getSerializer(Item.class));
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
            item =
                (Item) recMan.fetch(
                    recID,
                    GenericJdbmSerializer.getSerializer(Item.class));
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
            BTree bt = (BTree) btItemsByIdMap.get(new Integer(channel.getId()));

            TupleBrowser browser = null;
            if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
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

                    if (limit != LIMIT_NONE && items.size() == limit)
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
            BTree tree =
                (BTree) btItemsByIdMap.get(new Integer(channel.getId()));
            TupleBrowser browser =
                tree.browse(new Integer(relativeArticleNumber));
            Tuple tuple = new Tuple();
            if (browser.getNext(tuple)) {
                // Skip the current and move on to the next record
                if (browser.getNext(tuple)) {
                    long recID = ((Long) tuple.getValue()).longValue();
                    item =
                        (Item) recMan.fetch(
                            recID,
                            GenericJdbmSerializer.getSerializer(Item.class));
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
            BTree tree =
                (BTree) btItemsByIdMap.get(new Integer(channel.getId()));
            TupleBrowser browser =
                tree.browse(new Integer(relativeArticleNumber));
            Tuple tuple = new Tuple();
            if (browser.getPrevious(tuple)) {
                // Skip the current and move on to the next record
                long recID = ((Long) tuple.getValue()).longValue();
                item =
                    (Item) recMan.fetch(
                        recID,
                        GenericJdbmSerializer.getSerializer(Item.class));
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

        if (!migrateHsql()) {
            if (log.isInfoEnabled()) {
                log.info("Loading channels");
            }

            try {
                NodeList channelsList =
                    config.getDocumentElement().getElementsByTagName(
                        "channels");

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
                            HTree htItemsBySig = HTree.createInstance(recMan);
                            recMan.setNamedObject(
                                RECORD_ITEMS_BY_SIGNATURE_HTREE
                                    + channel.getId(),
                                htItemsBySig.getRecid());
                        }
                        recMan.commit();
                    }

                    long recID = recMan.insert(new Integer(channelCount));
                    recMan.setNamedObject(RECORD_LAST_CHANNEL_ID, recID);
                    recMan.commit();

                    recID = recMan.insert(new Integer(0));
                    recMan.setNamedObject(RECORD_LAST_CATEGORY_ID, recID);
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
            (
                (BTree) btItemsByIdMap.get(
                    new Integer(item.getChannel().getId()))).insert(
                new Integer(item.getArticleNumber()),
                new Long(recID),
                false);
            ((HTree) htItemsBySigMap.get(item.getChannel())).put(
                item.getSignature(),
                new Long(recID));

            // Update associated category...
            if (item.getChannel().getCategory() != null) {
                long channelItemId =
                    (((long) item.getChannel().getId() << 32)
                        | item.getArticleNumber());
                (
                    (BTree) btCategoryItemsByIdMap.get(
                        item.getChannel().getCategory())).insert(
                    new Integer(
                        item.getChannel().getCategory().nextArticleNumber()),
                    new Long(channelItemId),
                    false);
            }

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
            recMan.update(
                recId,
                channel,
                GenericJdbmSerializer.getSerializer(Channel.class));
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

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#addCategory(org.apache.log4j.Category)
     */
    public void addCategory(Category category) {
        try {
            lastCategoryId++;
            category.setId(lastCategoryId);
            long recId =
                recMan.insert(
                    category,
                    GenericJdbmSerializer.getSerializer(Category.class));
            btCategories.insert(
                new Integer(category.getId()),
                new Long(recId),
                false);

            BTree btCategoryItemsById =
                BTree.createInstance(recMan, new IntegerComparator());
            recMan.setNamedObject(
                RECORD_CATEGORY_ITEMS_BY_ID_BTREE + category.getId(),
                btCategoryItemsById.getRecid());
            btCategoryItemsByIdMap.put(category, btCategoryItemsById);

            recMan.update(
                recMan.getNamedObject(RECORD_LAST_CATEGORY_ID),
                new Integer(lastCategoryId));
            recMan.commit();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteCategory(org.apache.log4j.Category)
     */
    public void deleteCategory(Category category) {
        try {
            Integer catId = new Integer(category.getId());
            long recId = ((Long) btChannels.find(catId)).longValue();

            // Delete category
            btCategories.remove(catId);
            recMan.delete(recId);

            BTree treeId = (BTree) btCategoryItemsByIdMap.get(category);
            btCategoryItemsByIdMap.remove(category);

            // Delete btree
            recId = treeId.getRecid();
            recMan.delete(recId);

            recMan.commit();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#updateCategory(org.apache.log4j.Category)
     */
    public void updateCategory(Category category) {
        try {
            long recId =
                ((Long) btCategories.find(new Integer(category.getId())))
                    .longValue();
            recMan.update(
                recId,
                category,
                GenericJdbmSerializer.getSerializer(Category.class));
            recMan.commit();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadArticleNumbers(org.methodize.nntprss.feed.Category)
     */
    public List loadArticleNumbers(Category category) {
        Set articleNumbers = new TreeSet(new IntegerComparator());
        try {
            BTree bt = (BTree) btCategoryItemsByIdMap.get(category);
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
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadItem(org.methodize.nntprss.feed.Category, int)
     */
    public Item loadItem(Category category, int articleNumber) {
        Item item = null;
        try {
            Long channelItemLong =
                (Long) ((BTree) btCategoryItemsByIdMap.get(category)).find(
                    new Integer(articleNumber));

            if (channelItemLong != null) {
                long channelItemId = channelItemLong.longValue();

                int channelId = (int) (channelItemId >> 32);
                int origArticleNumber = (int) (channelItemId & 0xFFFFFFFF);

                item =
                    loadItem(
                        (Channel) category.getChannels().get(
                            new Integer(channelId)),
                        origArticleNumber);
                // Override channel article number with category article number
                try {
                    item = (Item) item.clone();
                } catch (CloneNotSupportedException cnse) {
                }
                item.setArticleNumber(articleNumber);
            }
        } catch (IOException ie) {
            throw new RuntimeException(ie);
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
        try {
            BTree bt = (BTree) btCategoryItemsByIdMap.get(category);

            TupleBrowser browser = null;
            if (articleRange[0] != AppConstants.OPEN_ENDED_RANGE) {
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
                    //	   End of article range reached.
                    break;
                //						match = false;

                if (match) {
                    long channelItemId = ((Long) tuple.getValue()).longValue();
                    int channelId = (int) (channelItemId >> 32);
                    int articleNumber = (int) (channelItemId & 0xFFFFFFFF);
                    Item item =
                        loadItem(
                            (Channel) category.getChannels().get(
                                new Integer(channelId)),
                            articleNumber);
                    //						Override channel article number with category article number
                    try {
                        item = (Item) item.clone();
                    } catch (CloneNotSupportedException cnse) {
                    }
                    item.setArticleNumber(id);
                    items.add(item);

                    if (limit != LIMIT_NONE && items.size() == limit)
                        //	   Break if maximum items returned...
                        break;
                }
            }
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
        return items;
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadNextItem(org.methodize.nntprss.feed.Category, int)
     */
    public Item loadNextItem(Category category, int relativeArticleNumber) {
        Item item = null;
        try {
            BTree tree = (BTree) btCategoryItemsByIdMap.get(category);
            TupleBrowser browser =
                tree.browse(new Integer(relativeArticleNumber));
            Tuple tuple = new Tuple();
            if (browser.getNext(tuple)) {
                // Skip the current and move on to the next record
                if (browser.getNext(tuple)) {
                    long channelItemId = ((Long) tuple.getValue()).longValue();
                    int channelId = (int) (channelItemId >> 32);
                    int articleNumber = (int) (channelItemId & 0xFFFFFFFF);
                    item =
                        loadItem(
                            (Channel) category.getChannels().get(
                                new Integer(channelId)),
                            articleNumber);
                    try {
                        item = (Item) item.clone();
                    } catch (CloneNotSupportedException cnse) {
                    }
                    item.setArticleNumber(articleNumber);
                }
            }
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
        return item;
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#loadPreviousItem(org.methodize.nntprss.feed.Category, int)
     */
    public Item loadPreviousItem(
        Category category,
        int relativeArticleNumber) {
        Item item = null;
        try {
            BTree tree = (BTree) btCategoryItemsByIdMap.get(category);
            TupleBrowser browser =
                tree.browse(new Integer(relativeArticleNumber));
            Tuple tuple = new Tuple();
            if (browser.getPrevious(tuple)) {
                long channelItemId = ((Long) tuple.getValue()).longValue();
                int channelId = (int) (channelItemId >> 32);
                int articleNumber = (int) (channelItemId & 0xFFFFFFFF);
                item =
                    loadItem(
                        (Channel) category.getChannels().get(
                            new Integer(channelId)),
                        articleNumber);
                try {
                    item = (Item) item.clone();
                } catch (CloneNotSupportedException cnse) {
                }
                item.setArticleNumber(articleNumber);
            }
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
        return item;
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#addChannelToCategory(org.methodize.nntprss.feed.Channel, org.methodize.nntprss.feed.Category)
     */
    public void addChannelToCategory(Channel channel, Category category) {
        // Iterate through channel items and add to category...
        try {
            BTree bt = (BTree) btItemsByIdMap.get(new Integer(channel.getId()));
            BTree btCat = (BTree) btCategoryItemsByIdMap.get(category);

            TupleBrowser browser = bt.browse();
            Tuple tuple = new Tuple();
            int channelId = channel.getId();
            while (browser.getNext(tuple)) {
                int articleNumber = ((Integer) tuple.getKey()).intValue();
                long channelItemId =
                    (((long) channelId << 32) | articleNumber & 0xFFFFFFFF);
                btCat.insert(
                    new Integer(category.nextArticleNumber()),
                    new Long(channelItemId),
                    false);
            }

            recMan.commit();

            category.setTotalArticles(btCat.size());
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }

    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#removeChannelFromCategory(org.methodize.nntprss.feed.Channel, org.methodize.nntprss.feed.Category)
     */
    public void removeChannelFromCategory(Channel channel, Category category) {
        try {
            BTree btCat = (BTree) btCategoryItemsByIdMap.get(category);

            TupleBrowser browser = btCat.browse();
            Tuple tuple = new Tuple();
            int channelId = channel.getId();
            List articlesToRemove = new ArrayList();
            while (browser.getNext(tuple)) {
                int articleChannelId =
                    (int) (((Long) tuple.getValue()).longValue() >> 32);
                if (articleChannelId == channelId) {
                    articlesToRemove.add(tuple.getKey());
                }
            }

            Iterator iter = articlesToRemove.iterator();
            while (iter.hasNext()) {
                btCat.remove(iter.next());
            }

            // @TODO What to do about total article count???
            recMan.commit();
            category.setTotalArticles(btCat.size());

        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

    /* (non-Javadoc)
     * @see org.methodize.nntprss.feed.db.ChannelDAO#deleteExpiredItems(org.methodize.nntprss.feed.Channel, java.util.Set)
     */
    public void deleteExpiredItems(
        Channel channel,
        Set currentItemSignatures) {

        int firstArticle = channel.getLastArticleNumber();
        try {
            HTree treeSig = (HTree) htItemsBySigMap.get(channel);
            BTree treeId =
                (BTree) btItemsByIdMap.get(new Integer(channel.getId()));
            Set articlesToKeep = new HashSet();
            Set articlesToRemove = new HashSet();

            FastIterator iter = treeSig.keys();
            String sig = null;

            Date expirationDate =
                new Date(System.currentTimeMillis() - channel.getExpiration());

            while ((sig = (String) iter.next()) != null) {
                if (currentItemSignatures.contains(sig)) {
                    // Keep...
                    long recId = ((Long) treeSig.get(sig)).longValue();
                    Item item =
                        (Item) recMan.fetch(
                            recId,
                            GenericJdbmSerializer.getSerializer(Item.class));
                    if (item.getArticleNumber() < firstArticle) {
                        firstArticle = item.getArticleNumber();
                    }

                    articlesToKeep.add(new Integer(item.getArticleNumber()));
                } else {
                    // Check item age...
                    // Delete record...
                    long recId = ((Long) treeSig.get(sig)).longValue();
                    Item item =
                        (Item) recMan.fetch(
                            recId,
                            GenericJdbmSerializer.getSerializer(Item.class));
                    if (!item.getDate().before(expirationDate)) {
                        if (item.getArticleNumber() < firstArticle) {
                            firstArticle = item.getArticleNumber();
                        }

                        articlesToKeep.add(
                            new Integer(item.getArticleNumber()));
                    } else {
                        articlesToRemove.add(sig);
                        recMan.delete(recId);
                    }

                }
            }

            Iterator removeIter = articlesToRemove.iterator();
            while (removeIter.hasNext()) {
                sig = (String) removeIter.next();
                treeSig.remove(sig);
            }

            TupleBrowser browser = treeId.browse();
            Tuple tuple = new Tuple();
            articlesToRemove.clear();
            while (browser.getNext(tuple)) {
                if (!articlesToKeep.contains(tuple.getKey())) {
                    articlesToRemove.add(tuple.getKey());
                    //						treeId.remove(tuple.getKey());
                }
            }

            removeIter = articlesToRemove.iterator();
            while (removeIter.hasNext()) {
                Integer articleNumber = (Integer) removeIter.next();
                treeId.remove(articleNumber);
            }

            if (channel.getCategory() != null) {
                //	   Remove articles from category...
                BTree treeCatId =
                    (BTree) btCategoryItemsByIdMap.get(channel.getCategory());
                browser = treeCatId.browse();
                articlesToRemove.clear();
                while (browser.getNext(tuple)) {
                    long channelItemId = ((Long) tuple.getValue()).longValue();
                    int channelId = (int) (channelItemId >> 32);
                    int articleNumber = (int) (channelItemId & 0xFFFFFFFF);

                    if (channelId == channel.getId()
                        && !articlesToKeep.contains(new Integer(articleNumber))) {
                        articlesToRemove.add(tuple.getKey());
                    }
                }

                removeIter = articlesToRemove.iterator();
                while (removeIter.hasNext()) {
                    Integer articleNumber = (Integer) removeIter.next();
                    treeCatId.remove(articleNumber);
                }
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
     * @see org.methodize.nntprss.feed.db.ChannelDAO#migrateInitializeDatabase()
     */
    void migrateInitializeDatabase() throws IOException {
		// Establish LAST_CHANNEL_ID entry within persistent store
		int channelCount = 0;
		long recID = recMan.insert(new Integer(channelCount));
		recMan.setNamedObject(RECORD_LAST_CHANNEL_ID, recID);

		// Establish LAST_CATEGORY_ID entry within persistent store
		recMan.setNamedObject(
			RECORD_LAST_CATEGORY_ID,
			recMan.insert(new Integer(0)));

		recMan.commit();
    }

}
