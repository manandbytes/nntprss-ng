package org.methodize.nntprss;

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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Startup.java,v 1.8 2004/10/22 03:33:13 jasonbrome Exp $
 */
public class Startup {

    private static String[] resources =
        new String[] {
            "./nntprss.jar",
            "./ext/lib/log4j-1.2.7.jar",
            "./ext/lib/commons-collections.jar",
            "./ext/lib/commons-dbcp.jar",
            "./ext/lib/commons-httpclient.jar",
            "./ext/lib/commons-logging.jar",
            "./ext/lib/commons-pool.jar",
            "./ext/lib/javax.servlet.jar",
            "./ext/lib/org.mortbay.jetty-jdk1.2.jar",
            "./ext/lib/crimson.jar",
            "./ext/lib/xmlrpc-1.1.jar",
            "./ext/lib/mailapi.jar",
            "./ext/lib/activation.jar",
            "./ext/lib/systray4j.jar",
        // hsqldb Support
        "./ext/lib/hsqldb.jar",
        // MySQL Support
        "./ext/lib/mysql.jar",
        // JDBM Support
        "./ext/lib/jdbm-0.20.jar",
        // Derby Support
        "./ext/lib/derby.jar",
		"./ext/lib/derbytools.jar",
         "." };

    public static void main(String[] args) {
        if (args.length == 0 || !args[0].equals("stop")) {
            try {
                new Startup().run(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.exit(0);
        }
    }

    public void run(String[] args) throws Exception {
        URL resourceURLs[] = new URL[resources.length];

        for (int resCount = 0; resCount < resources.length; resCount++) {
            try {
                File file = new File(resources[resCount]);
                if (file.exists()) {
                    resourceURLs[resCount] = file.getCanonicalFile().toURL();
                } else {
                    throw new RuntimeException(
                        "Missing file: " + file.toString());
                }
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        }

        ClassLoader existingClassLoader =
            Thread.currentThread().getContextClassLoader();
        URLClassLoader urlClassLoader =
            URLClassLoader.newInstance(resourceURLs, existingClassLoader);
        Thread.currentThread().setContextClassLoader(urlClassLoader);

        Class clazz = urlClassLoader.loadClass("org.methodize.nntprss.Main");
        Method methods[] = clazz.getMethods();
        Method mainMethod = null;
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals("main")) {
                mainMethod = methods[i];
                break;
            }
        }

        mainMethod.invoke(null, new Object[] { args });
    }

    // Shutdown hook for Windows Java Service Wrappers
    // e.g. JNT
    public static void stopApplication() {
        System.exit(0);
    }
}
