package org.methodize.nntprss;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2005 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * mail:  Methodize Solutions
 *        PO Box 3865
 *        Grand Central Station
 *        New York NY 10163
 * 
 * This file is part of nntp//rss
 * 
 * This class uses the SysTray for Java libraries,
 * distributed under the GNU LGPL.
 * Further information: http://www.eikon.tum.de/~tamas/
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.util.AppConstants;
import snoozesoft.systray4j.SysTrayMenu;
import snoozesoft.systray4j.SysTrayMenuEvent;
import snoozesoft.systray4j.SysTrayMenuIcon;
import snoozesoft.systray4j.SysTrayMenuItem;
import snoozesoft.systray4j.SysTrayMenuListener;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: WindowsSysTray.java,v 1.6 2005/02/13 21:53:59 jasonbrome Exp $
 */

public class WindowsSysTray extends JFrame implements SysTrayMenuListener {

    private SysTrayMenu menu;
    private SysTrayMenuIcon nntpIcon;

    private String adminURL = "http://127.0.0.1:7810/";

    private static final String MENU_ABOUT_TEXT = "About nntp//rss...";
    private static final String MENU_ABOUT_CMD = "about";
    private static final String MENU_EXIT_TEXT = "Exit";
    private static final String MENU_EXIT_CMD = "exit";
    private static final String MENU_PROPERTIES_TEXT = "Properties";
    private static final String MENU_PROPERTIES_CMD = "properties";
    private static final String MENU_REPOLL_TEXT = "Repoll All Channels";
    private static final String MENU_REPOLL_CMD = "repoll";

    private static final String ICON_FILE = "nntprss.ico";

    private ChannelManager channelManager;

    public WindowsSysTray() {

        super("nntp//rss");

        nntpIcon = new SysTrayMenuIcon(ICON_FILE);
        nntpIcon.addSysTrayMenuListener(this);

        SysTrayMenuItem itemRepoll =
            new SysTrayMenuItem(MENU_REPOLL_TEXT, MENU_REPOLL_CMD);
        itemRepoll.setEnabled(false);
        itemRepoll.addSysTrayMenuListener(this);

        SysTrayMenuItem itemCfg =
            new SysTrayMenuItem(MENU_PROPERTIES_TEXT, MENU_PROPERTIES_CMD);
        itemCfg.setEnabled(false);
        itemCfg.addSysTrayMenuListener(this);

        SysTrayMenuItem itemExit =
            new SysTrayMenuItem(MENU_EXIT_TEXT, MENU_EXIT_CMD);
        itemExit.addSysTrayMenuListener(this);

        SysTrayMenuItem itemAbout =
            new SysTrayMenuItem(MENU_ABOUT_TEXT, MENU_ABOUT_CMD);
        itemAbout.addSysTrayMenuListener(this);

        Vector items = new Vector();
        items.add(itemExit);
        items.add(SysTrayMenu.SEPARATOR);
        items.add(itemAbout);
        items.add(SysTrayMenu.SEPARATOR);
        items.add(itemCfg);
        items.add(itemRepoll);

        menu =
            new SysTrayMenu(
                nntpIcon,
                "nntp//rss v" + AppConstants.VERSION + " starting...",
                items);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
    }

    public void showStarted() {
        menu.setToolTip("nntp//rss v" + AppConstants.VERSION);
        menu.getItem(MENU_PROPERTIES_TEXT).setEnabled(true);
        menu.getItem(MENU_REPOLL_TEXT).setEnabled(true);
    }

    public void shutdown() {
        menu.setToolTip(
            "nntp//rss v" + AppConstants.VERSION + " shutting down...");
        menu.getItem(MENU_REPOLL_TEXT).setEnabled(false);
        menu.getItem(MENU_PROPERTIES_TEXT).setEnabled(false);
        menu.getItem(MENU_ABOUT_TEXT).setEnabled(false);
        menu.getItem(MENU_EXIT_TEXT).setEnabled(false);
    }

    /**
     * @see snoozesoft.systray4j.SysTrayMenuListener#iconLeftClicked(SysTrayMenuEvent)
     */
    public void iconLeftClicked(SysTrayMenuEvent arg0) {
    }

    /**
     * @see snoozesoft.systray4j.SysTrayMenuListener#iconLeftDoubleClicked(SysTrayMenuEvent)
     */
    public void iconLeftDoubleClicked(SysTrayMenuEvent arg0) {
        startBrowser(adminURL);
    }

    private void startBrowser(String url) {
        try {
            Process process =
                Runtime.getRuntime().exec(
                    new String[] {
                        "cmd.exe",
                        "/c",
                        "start",
                        "\"\"",
                        "\"" + url + "\"" });
            process.waitFor();
            process.exitValue();

        } catch (IOException ie) {
        } catch (InterruptedException ie) {
        }
    }

    /**
     * @see snoozesoft.systray4j.SysTrayMenuListener#menuItemSelected(SysTrayMenuEvent)
     */
    public void menuItemSelected(SysTrayMenuEvent e) {
        if (e.getActionCommand().equals(MENU_ABOUT_CMD)) {
            final JLabel url =
                new JLabel("<html><u><font color=blue>http://www.methodize.org/nntprss</font></u></html>");
            url.addMouseListener(new MouseListener() {
                /**
                 * @see java.awt.event.MouseListener#mouseClicked(MouseEvent)
                 */
                public void mouseClicked(MouseEvent arg0) {
                    startBrowser("http://www.methodize.org/nntprss");
                }

                public void mouseEntered(MouseEvent arg0) {
                    url.setText(
                        "<html><u><font color=red>http://www.methodize.org/nntprss</font></u></html>");
                }

                public void mouseExited(MouseEvent arg0) {
                    url.setText(
                        "<html><u><font color=blue>http://www.methodize.org/nntprss</font></u></html>");
                }

                public void mousePressed(MouseEvent arg0) {
                }
                public void mouseReleased(MouseEvent arg0) {
                }

            });

            Object[] message =
                new Object[] {
                    "nntp//rss v" + AppConstants.VERSION,
                    url,
                    "\n",
                    "Copyright (c) 2002-2004 Jason Brome",
                    "Licensed under the GNU Public License\n" };
            JOptionPane.showMessageDialog(
                this,
                message,
                MENU_ABOUT_TEXT,
                JOptionPane.INFORMATION_MESSAGE);
        } else if (e.getActionCommand().equals(MENU_EXIT_CMD)) {
            Object[] options = { "Shutdown", "Cancel" };
            int option =
                JOptionPane.showOptionDialog(
                    this,
                    "Are you sure you want to shutdown nntp//rss?",
                    "nntp//rss - Warning",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);
            if (option == 0) {
                System.exit(0);
            }
        } else if (e.getActionCommand().equals(MENU_PROPERTIES_CMD)) {
            startBrowser(adminURL);
        } else if (e.getActionCommand().equals(MENU_REPOLL_CMD)) {
            channelManager.repollAllChannels();
        } else {
            // Invalid command, should not happen
            JOptionPane.showMessageDialog(this, e.getActionCommand());
        }
    }

    /**
     * Returns the adminURL.
     * @return String
     */
    public String getAdminURL() {
        return adminURL;
    }

    /**
     * Sets the adminURL.
     * @param adminURL The adminURL to set
     */
    public void setAdminURL(String adminURL) {
        this.adminURL = adminURL;
    }

    /**
     * Sets the channelManager.
     * @param channelManager The channelManager to set
     */
    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

}
