package org.methodize.nntprss;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002 Jason Brome.  All Rights Reserved.
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

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.methodize.nntprss.admin.AdminServer;
import org.methodize.nntprss.db.DBManager;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.rss.ChannelManager;
import org.methodize.nntprss.util.AppConstants;
import org.w3c.dom.Document;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */
public class Main {

	private Logger log = Logger.getLogger(Main.class);

	private DBManager dbManager = null;
	private NNTPServer nntpServer = null;
	private ChannelManager channelManager = null;
	private AdminServer adminServer = null;

	private class ShutdownHook extends Thread {

		private Logger log = Logger.getLogger(Main.ShutdownHook.class);
		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			if(log.isInfoEnabled()) {
				log.info("Shutting down nntp//rss...");
			}

			adminServer.shutdown();

			nntpServer.shutdown();

			channelManager.shutdown();

			dbManager.shutdown();

			if(log.isInfoEnabled()) {
				log.info("nntp//rss shutdown successfully...");
			}
		}

	}

	public void startNntpRss() {

		if (log.isInfoEnabled()) {
			log.info("Starting nntp//rss v" + AppConstants.VERSION);
		}

		try {
			// Load configuration
			Document config = loadConfiguration();

			// Start DB server
			dbManager = new DBManager();
			dbManager.configure(config);
			dbManager.startup();

			// Start NNTP server
			nntpServer = new NNTPServer();
			nntpServer.configure(config);

			channelManager = ChannelManager.getRSSManager();
			channelManager.configure(config);

			adminServer = new AdminServer(channelManager);
			adminServer.configure(config);

			Runtime.getRuntime().addShutdownHook(this.new ShutdownHook());

			adminServer.start();
			nntpServer.start();


		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Document loadConfiguration() {
		Document configDoc = null;

		InputStream configFile =
			getClass().getClassLoader().getResourceAsStream(
				AppConstants.NNTPRSS_CONFIGURATION_FILE);
		if (configFile == null) {
			throw new RuntimeException(
				"Cannot load "
					+ AppConstants.NNTPRSS_CONFIGURATION_FILE
					+ " configuration file");
		}

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			configDoc = db.parse(configFile);
		} catch (Exception e) {
			// FIXME more granular exception?
			throw new RuntimeException(
				"Error parsing "
					+ AppConstants.NNTPRSS_CONFIGURATION_FILE
					+ " configuration file");
		}

		return configDoc;

	}

	public static void main(String[] args) {

		Main startup = new Main();
		startup.startNntpRss();

	}
	
	// Shutdown hook for Windows Java Service Wrappers
	// e.g. JNT
	public static void stopApplication() {
		System.exit(0);
	}
}
