package org.methodize.nntprss.util;

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
 * @version $Id: RSSHelper.java,v 1.2 2003/03/24 03:15:06 jasonbrome Exp $
 */

public class RSSHelper {

	/**
	 * Parses RSS email string to return email
	 * 
	 * Typically:
	 * 
	 * someone@someone.com (My name) 
	 */
	public static String parseEmail(String email) {
		String parsedEmail = null;
		
		int spacePos = email.indexOf(' ');
		
		if(spacePos == -1) {
			parsedEmail = email;
		} else {
			parsedEmail = email.substring(0, spacePos);
		}
		
		return parsedEmail;
		
	}
}
