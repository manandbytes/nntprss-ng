package org.methodize.nntprss.admin;

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

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: SubscriptionListener.java,v 1.7 2007/12/17 04:07:07 jasonbrome Exp $
 */

public class SubscriptionListener {

    private String name = null;
    private int port = 0;
    private String path = null;
    private String param = null;

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public String getParam() {
        return param;
    }

    /**
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        name = string;
    }

    /**
     * @param string
     */
    public void setParam(String string) {
        param = string;
    }

    /**
     * @param string
     */
    public void setPath(String string) {
        path = string;
    }

    /**
     * @param i
     */
    public void setPort(int i) {
        port = i;
    }

}
