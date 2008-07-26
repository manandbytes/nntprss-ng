package org.methodize.nntprss.feed.publish;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import org.methodize.nntprss.util.AppConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: PublishManager.java,v 1.7 2007/12/17 04:14:04 jasonbrome Exp $
 */

public class PublishManager {

    private static PublishManager publishManager = new PublishManager();

    private Map interfaceMap;
    private Map systemMap;

    private PublishManager() {
    }

    public static PublishManager getPublishManager() {
        return publishManager;
    }

    public void configure(Document config) {
        Document publishDoc = null;

        CodeSource codeSource = getClass().getProtectionDomain()
                .getCodeSource();
        String publishFileName = new File(codeSource.getLocation().getFile())
                .getParent()
                + "/" + AppConstants.NNTPRSS_PUBLISH_CONFIGURATION_FILE;
        try {
            InputStream publishFile = new FileInputStream(publishFileName);
            DocumentBuilder db = AppConstants.newDocumentBuilder();
            publishDoc = db.parse(publishFile);
        } catch (FileNotFoundException e1) {
            throw new RuntimeException("Cannot load "
                    + AppConstants.NNTPRSS_PUBLISH_CONFIGURATION_FILE
                    + " configuration file as " + publishFileName.toString());
        } catch (Exception e) {
            // FIXME more granular exception?
            throw new RuntimeException("Error parsing "
                    + AppConstants.NNTPRSS_PUBLISH_CONFIGURATION_FILE
                    + " configuration file");
        }

        interfaceMap = new HashMap();
        systemMap = new HashMap();

        // Extract and instantiate interfaces...
        Element rootElm = publishDoc.getDocumentElement();
        NodeList interfaceList = rootElm.getElementsByTagName("interface");

        String className = null;
        try {
            for (int i = 0; i < interfaceList.getLength(); i++) {
                Element interfaceElm = (Element) interfaceList.item(i);
                className = interfaceElm.getAttribute("class");
                Publisher pubClazz =
                    (Publisher) Class.forName(className).newInstance();
                interfaceMap.put(interfaceElm.getAttribute("name"), pubClazz);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Error instantiating publisher class " + className);
        }
        // Extract and configure publishing systems...

        NodeList systemList = rootElm.getElementsByTagName("system");
        for (int i = 0; i < interfaceList.getLength(); i++) {
            Element systemElm = (Element) systemList.item(i);
            Publisher publisher =
                (Publisher) interfaceMap.get(
                    systemElm.getAttribute("interface"));

            // @TODO check for null publisher...
            String name = systemElm.getAttribute("name");

            System system =
                new System(
                    name,
                    systemElm.getAttribute("title"),
                    publisher,
                    systemElm.getAttribute("homePage"),
                    systemElm.getAttribute("url"));

            systemMap.put(name, system);
        }

    }

    public class System {

        private String name;
        private String title;
        private Publisher publisher;
        private String homePage;
        private String url;

        public System(
            String name,
            String title,
            Publisher publisher,
            String homePage,
            String url) {

            this.name = name;
            this.title = title;
            this.publisher = publisher;
            this.homePage = homePage;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public Publisher getPublisher() {
            return publisher;
        }

        public String getHomePage() {
            return homePage;
        }

        public String getUrl() {
            return url;
        }
    }
}
