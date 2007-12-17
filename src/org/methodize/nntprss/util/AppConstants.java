package org.methodize.nntprss.util;

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

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AppConstants.java,v 1.21 2007/12/17 04:16:05 jasonbrome Exp $
 */
public final class AppConstants {

    private static final DocumentBuilderFactory docBuilderFactory;
    private static final String platform;
    private static final String userAgent;

    public static final String NNTPRSS_CONFIGURATION_FILE =
        "nntprss-config.xml";

    public static final String NNTPRSS_PUBLISH_CONFIGURATION_FILE =
        "xml/publish-config.xml";

    public static final String USERS_CONFIG = "users.properties";

    public static final String VERSION = "0.5-beta-3";

    public static final int OPEN_ENDED_RANGE = -1;

    public static final int CONTENT_TYPE_TEXT = 1;
    public static final int CONTENT_TYPE_HTML = 2;
    public static final int CONTENT_TYPE_MIXED = 3;
    
    static {
        // Private constructor... initialize platform string
        StringBuffer pltfmBuf = new StringBuffer();
        String osName = System.getProperty("os.name");
        if (osName != null) {
            pltfmBuf.append(osName);
            pltfmBuf.append(' ');
        }

        String osVersion = System.getProperty("os.version");
        if (osVersion != null) {
            pltfmBuf.append(osVersion);
            pltfmBuf.append(' ');
        }

        String osArch = System.getProperty("os.arch");
        if (osVersion != null) {
            pltfmBuf.append(osArch);
        }

        platform = pltfmBuf.toString();
        userAgent =
            "nntprss/"
                + VERSION
                + " ("
                + platform
                + "; http://www.methodize.org/nntprss/)";

        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
    }

    private AppConstants() {
    }

    public static DocumentBuilder newDocumentBuilder()
        throws ParserConfigurationException {
        return docBuilderFactory.newDocumentBuilder();
    }

    public static String getPlatform() {
        return platform;
    }

    public static String getUserAgent() {
        return userAgent;
    }

    public static String getCurrentHostName() {
        InetAddress localAddr;
        String hostName = null;
        try {
            localAddr = InetAddress.getLocalHost();
            localAddr =
                InetAddress.getByName(
                    InetAddress.getLocalHost().getHostAddress());
            hostName = localAddr.getHostName();
        } catch (UnknownHostException e) {
        }

        // Drop down to localhost (127.0.0.1) if we cannot discover host name
        if (hostName == null) {
            hostName = "127.0.0.1";
        }

        return hostName;
    }

}
