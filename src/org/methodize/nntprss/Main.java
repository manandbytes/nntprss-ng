package org.methodize.nntprss;

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

import java.io.InputStream;
import java.security.Provider;
import java.security.Security;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.methodize.nntprss.admin.AdminServer;
import org.methodize.nntprss.db.DBManager;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.publish.PublishManager;
import org.methodize.nntprss.util.AppConstants;
import org.w3c.dom.Document;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Main.java,v 1.13 2007/12/17 04:03:56 jasonbrome Exp $
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class);

    private DBManager dbManager = null;
    private NNTPServer nntpServer = null;
    private ChannelManager channelManager = null;
    private PublishManager publishManager = null;
    private AdminServer adminServer = null;
    private WindowsSysTray windowsSysTray = null;

    private class ShutdownHook extends Thread {

        private Logger log = Logger.getLogger(Main.ShutdownHook.class);
        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if (log.isInfoEnabled()) {
                log.info("Shutting down nntp//rss...");
            }

            if (windowsSysTray != null) {
                windowsSysTray.shutdown();
            }

            adminServer.shutdown();

            nntpServer.shutdown();

            channelManager.shutdown();

            dbManager.shutdown();

            if (log.isInfoEnabled()) {
                log.info("nntp//rss shutdown successfully...");
            }
        }

    }

    public void startNntpRss() {

        if (log.isInfoEnabled()) {
            log.info("Starting nntp//rss v" + AppConstants.VERSION);
        }

        try {
            // Set DNS cache properties to sensible values...
            // Cache successful DNS for one hour
            System.setProperty(
                "networkaddress.cache.ttl",
                Integer.toString(60 * 60));
            System.setProperty("networkaddress.cache.negative.ttl", "1");

            // Initialize SSL 
            try {
                Class providerClass =
                    Class.forName("com.sun.net.ssl.internal.ssl.Provider");
                Security.addProvider((Provider) providerClass.newInstance());
                System.setProperty(
                    "java.protocol.handler.pkgs",
                    "com.sun.net.ssl.internal.www.protocol");
            } catch (ClassNotFoundException cnfe) {
                log.warn("JSSE not found - HTTPS support not available");
            }

            if (System
                .getProperty("os.name")
                .toLowerCase()
                .startsWith("windows")) {
                windowsSysTray = new WindowsSysTray();
            }

            // Load configuration
            Document config = loadConfiguration();

            // Start DB server
            dbManager = new DBManager();
            dbManager.configure(config);
            dbManager.startup();

            channelManager = ChannelManager.getChannelManager();
            channelManager.configure(config);

            publishManager = PublishManager.getPublishManager();
            publishManager.configure(config);

            // Start NNTP server
            nntpServer = new NNTPServer();
            nntpServer.configure(config);

            adminServer = new AdminServer(channelManager, nntpServer);
            adminServer.configure(config);

            Runtime.getRuntime().addShutdownHook(this.new ShutdownHook());

            adminServer.start();
            nntpServer.start();
            channelManager.start();

            if (windowsSysTray != null) {
                windowsSysTray.setAdminURL(
                    "http://127.0.0.1:" + adminServer.getPort() + "/");
                windowsSysTray.setChannelManager(channelManager);
                windowsSysTray.showStarted();
            }

        } catch (Exception e) {
            log.error("Exception thrown during startup", e);
            e.printStackTrace();
            System.exit(-1);
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
            log.error("Error parsing configuration", e);
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
