package org.methodize.nntprss.plugin.examples;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2007 Jason Brome.  All Rights Reserved.
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
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.plugin.ItemProcessor;
import org.methodize.nntprss.plugin.PluginException;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ItemLogPlugin.java,v 1.4 2007/12/17 04:15:38 jasonbrome Exp $
 */
public class ItemLogPlugin implements ItemProcessor {

	private static final Logger log = Logger.getLogger(ItemLogPlugin.class);

    public void initialize(Element config) throws PluginException {
    	log.info("Initializing ItemLogPlugin");
    }

    public void onItem(Item item) {
    	if(log.isInfoEnabled()) {
	    	log.info("New item - Channel=" + item.getChannel().getName() + 
	    		", Title=" + item.getTitle());
		}
    }

    public void shutdown() {
		log.info("Terminating ItemLogPlugin");
    }

}
