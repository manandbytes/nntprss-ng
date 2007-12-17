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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.util.AppConstants;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AdminServer.java,v 1.12 2007/12/17 04:06:24 jasonbrome Exp $
 */
public class AdminServer {

    private HttpServer httpServer;
    private ChannelManager channelManager;
    private NNTPServer nntpServer;
    private int port;
    public static final String SERVLET_CTX_ADMIN_SERVER = "admin.server";
    public static final String SERVLET_CTX_RSS_MANAGER = "rss.manager";
    public static final String SERVLET_CTX_NNTP_SERVER = "nntp.server";

    public static final String REALM_NAME = "nntprss-realm";

    private Map subscriptionListeners = new HashMap();

    /**
     * Constructor for AdminServer.
     */

    public AdminServer(ChannelManager channelManager, NNTPServer nntpServer) {
        this.channelManager = channelManager;
        this.nntpServer = nntpServer;
    }

    public void configure(Document config) throws Exception {

        Element rootElm = config.getDocumentElement();
        Element adminConfig =
            (Element) rootElm.getElementsByTagName("admin").item(0);
        port = Integer.parseInt(adminConfig.getAttribute("port"));

        Node addressNode = adminConfig.getAttributeNode("address");
        String address = null;
        if (addressNode != null) {
            address = addressNode.getNodeValue();
        }

        httpServer = new HttpServer();

        // Check for user realm properties file
        // If it exists, use security.
        InputStream userRealmConfig =
            this.getClass().getResourceAsStream(
                "/" + AppConstants.USERS_CONFIG);
        boolean useSecurity = false;
        if (userRealmConfig != null) {
            useSecurity = true;
            HashUserRealm userRealm = new HashUserRealm(REALM_NAME);
            userRealm.load(AppConstants.USERS_CONFIG);
            httpServer.addRealm(userRealm);
        }

        HttpContext context = httpServer.getContext("/");
        WebApplicationHandler handler = new WebApplicationHandler();

        if (useSecurity) {
            context.setRealmName(REALM_NAME);
            context.setAuthenticator(new BasicAuthenticator());
            context.addHandler(new SecurityHandler());
            context.addSecurityConstraint(
                "/",
                new SecurityConstraint("Admin", "*"));
        }

        context.setAttribute(SERVLET_CTX_RSS_MANAGER, channelManager);
        context.setAttribute(SERVLET_CTX_NNTP_SERVER, nntpServer);
        context.setAttribute(SERVLET_CTX_ADMIN_SERVER, this);

        handler.addServlet("/", AdminServlet.class.getName());
        context.addHandler(handler);

        httpServer.addContext(context);

        HttpListener httpListener = new SocketListener();
        httpListener.setPort(port);
        if (address != null) {
            httpListener.setHost(address);
        }

        httpServer.addListener(httpListener);

        // Add subscription listeners...
        NodeList subListeners = rootElm.getElementsByTagName("subscribe");

        for (int i = 0; i < subListeners.getLength(); i++) {
            Element subscribe = (Element) subListeners.item(i);
            SubscriptionListener subListener = new SubscriptionListener();
            subListener.setName(subscribe.getAttribute("name"));
            subListener.setPort(
                Integer.parseInt(subscribe.getAttribute("port")));
            subListener.setPath(subscribe.getAttribute("path"));
            subListener.setParam(subscribe.getAttribute("param"));

            Integer port = new Integer(subListener.getPort());
            if (!subscriptionListeners.containsKey(port)) {
                // Create new HTTP listener	
                httpListener = new SocketListener();
                httpListener.setPort(subListener.getPort());
                if (address != null) {
                    httpListener.setHost(address);
                }
                httpServer.addListener(httpListener);

                List subList = new ArrayList();
                subList.add(subListener);
                subscriptionListeners.put(port, subList);
            } else {
                // Use existing HTTP listener, just add to map
                 ((List) (subscriptionListeners.get(port))).add(subListener);
            }
        }

    }

    public void start() throws Exception {
        httpServer.start();
    }

    public void shutdown() {
        try {
            httpServer.stop();
        } catch (InterruptedException ie) {
            // FIXME log
        }
    }
    /**
     * Returns the port.
     * @return int
     */
    public int getPort() {
        return port;
    }

    /**
     * @return
     */
    public Map getSubscriptionListeners() {
        return subscriptionListeners;
    }

}
