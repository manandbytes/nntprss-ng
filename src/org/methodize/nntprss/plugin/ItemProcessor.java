package org.methodize.nntprss.plugin;

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

import org.methodize.nntprss.feed.Item;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ItemProcessor.java,v 1.1 2004/12/15 04:11:15 jasonbrome Exp $
 */
public interface ItemProcessor {

	/**
	 * Initializer for Plugin - called once during nntp//rss startup
	 *
	 * @param config Processor Element from nntprss-config.xml 
     * @throws PluginException
     */
	public void initialize(Element config) throws PluginException;

	/**
	 * Invoked during nntp//rss shutdown
	 *
	 */
	public void shutdown();

	/**
	 * Invoked for every new or updated Item
	 * 
	 * Note that this method must perform thread synchronization to 
	 * any of its own resources.  The onItem method may be called 
	 * concurrently from different Channel Poller threads.
	 *
	 * Important: item is not a clone of the original item - any 
	 * updates made to the item will be reflected as the item 
	 * is placed into the database.  Updating Item attributes
	 * may result in undesirable side effects!  
	 *  
	 * @param item
	 */
	public void onItem(Item item);
}
