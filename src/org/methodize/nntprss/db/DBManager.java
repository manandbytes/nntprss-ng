package org.methodize.nntprss.db;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.methodize.nntprss.feed.db.ChannelManagerDAO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: DBManager.java,v 1.4 2003/07/19 00:03:43 jasonbrome Exp $
 */
public class DBManager {

	private String connectString;

	public static final String POOL_CONNECT_STRING =
		"jdbc:apache:commons:dbcp:nntprss";

	public DBManager() {
	}

	public void startup() {
	}

	public void shutdown() {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = DriverManager.getConnection(POOL_CONNECT_STRING);
			stmt = conn.createStatement();
			stmt.executeQuery("CHECKPOINT");
			stmt.executeQuery("SHUTDOWN");
		} catch (SQLException e) {
		} finally {
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
	}

	public void configure(Document config) throws Exception {

		Element rootElm = config.getDocumentElement();
		Element dbConfig = (Element)rootElm.getElementsByTagName("db").item(0);
		connectString = dbConfig.getAttribute("connect");

		ObjectPool connectionPool = new GenericObjectPool(null);

		String dbDriver = dbConfig.getAttribute("driverClass");
		if(dbDriver != null && dbDriver.length() > 0) {
			Class.forName(dbDriver);
		} else {
// Default to HSSQLDB
			Class.forName("org.hsqldb.jdbcDriver");
		}

		ConnectionFactory connectionFactory =
			new DriverManagerConnectionFactory(connectString, "sa", "");

		//
		// Now we'll create the PoolableConnectionFactory, which wraps
		// the "real" Connections created by the ConnectionFactory with
		// the classes that implement the pooling functionality.
		//
		PoolableConnectionFactory poolableConnectionFactory =
			new PoolableConnectionFactory(
				connectionFactory,
				connectionPool,
				null,
				null,
				false,
				true);

		//
		// Finally, we create the PoolingDriver itself...
		//
		PoolingDriver driver = new PoolingDriver();

		//
		// ...and register our pool with it.
		//
		driver.registerPool("nntprss", connectionPool);

		ChannelManagerDAO.getChannelManagerDAO().initialize(config);

	}

}