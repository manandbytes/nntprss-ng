package org.methodize.nntprss.db;

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

import org.methodize.nntprss.feed.db.ChannelManagerDAO;
import org.w3c.dom.Document;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: DBManager.java,v 1.6 2004/01/04 21:12:54 jasonbrome Exp $
 */
public class DBManager {

	public DBManager() {
	}

	public void startup() {
	}

	public void shutdown() {
		ChannelManagerDAO.getChannelManagerDAO().getChannelDAO().shutdown();
	}

	public void configure(Document config) throws Exception {
		ChannelManagerDAO.getChannelManagerDAO(config).getChannelDAO().initialize(config);
	}

}