package org.methodize.nntprss.feed;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2006 Jason Brome.  All Rights Reserved.
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

import java.util.Date;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ItemContainer.java,v 1.8 2006/05/17 04:13:17 jasonbrome Exp $
 */

public class ItemContainer {

    protected String name;

    /**
    	 * @return
    	 */
    public String getName() {
        return name;
    }

    /**
    	 * @param string
    	 */
    public void setName(String string) {
        name = string;
    }

    protected int firstArticleNumber = 1;

    protected int lastArticleNumber = 0;

    protected int totalArticles = 0;

    /**
    	 * Returns the firstArticleNumber.
    	 * @return long
    	 */
    public int getFirstArticleNumber() {
        return firstArticleNumber;
    }

    /**
    	 * Returns the lastArticleNumber.
    	 * @return long
    	 */
    public int getLastArticleNumber() {
        return lastArticleNumber;
    }

    /**
    	 * Sets the firstArticleNumber.
    	 * @param firstArticleNumber The firstArticleNumber to set
    	 */
    public void setFirstArticleNumber(int firstArticleNumber) {
        this.firstArticleNumber = firstArticleNumber;
    }

    /**
    	 * Sets the lastArticleNumber.
    	 * @param lastArticleNumber The lastArticleNumber to set
    	 */
    public void setLastArticleNumber(int lastArticleNumber) {
        if(this.lastArticleNumber < lastArticleNumber) { //make sure that article number NEVER decreases
            this.lastArticleNumber = lastArticleNumber;
        }
    }

    /**
    	 * Returns the totalArticles.
    	 * @return int
    	 */
    public int getTotalArticles() {
        return totalArticles;
    }

    /**
    	 * Sets the totalArticles.
    	 * @param totalArticles The totalArticles to set
    	 */
    public void setTotalArticles(int totalArticles) {
        this.totalArticles = totalArticles;
    }

    protected Date created;

    /**
    	 * @return
    	 */
    public Date getCreated() {
        return created;
    }

    /**
    	 * @param date
    	 */
    public void setCreated(Date date) {
        created = date;
    }

}
