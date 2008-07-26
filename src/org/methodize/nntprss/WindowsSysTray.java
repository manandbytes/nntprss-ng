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

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.util.AppConstants;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: WindowsSysTray.java,v 1.9 2007/12/17 04:06:09 jasonbrome Exp $
 */

public class WindowsSysTray implements ActionListener, MouseListener {

    private static final String TOOL_TIP = "nntp//rss";

    private static final long serialVersionUID = -3602617081322972702L;

    private static final String MENU_ABOUT_TEXT = "About nntp//rss...";
    private static final String MENU_ABOUT_CMD = "about";
    private static final String MENU_EXIT_TEXT = "Exit";
    private static final String MENU_EXIT_CMD = "exit";
    private static final String MENU_PROPERTIES_TEXT = "Properties";
    private static final String MENU_PROPERTIES_CMD = "properties";
    private static final String MENU_REPOLL_TEXT = "Repoll All Channels";
    private static final String MENU_REPOLL_CMD = "repoll";

    private static final String ICON_FILE = "nntprss.ico";
	
	private final PopupMenu menu = new PopupMenu();
    private final TrayIcon nntpIcon;

    private String adminURL = "http://127.0.0.1:7810/";

    private ChannelManager channelManager;

    private MenuItem itemCfg;

    private MenuItem itemRepoll;

    private MenuItem itemAbout;

    private MenuItem itemExit;

    public WindowsSysTray() {

        // TODO icon in the .ico format not visible under Linux
        Image iconImage = Toolkit.getDefaultToolkit().getImage(ICON_FILE);
        nntpIcon = new TrayIcon(iconImage, TOOL_TIP, menu);
        nntpIcon.addActionListener(this);
        nntpIcon.addMouseListener(this);

        itemRepoll = new MenuItem(MENU_REPOLL_TEXT);
        itemRepoll.setActionCommand(MENU_REPOLL_CMD);
        itemRepoll.setEnabled(false);
        itemRepoll.addActionListener(this);

        itemCfg = new MenuItem(MENU_PROPERTIES_TEXT);
        itemCfg.setActionCommand(MENU_PROPERTIES_CMD);
        itemCfg.setEnabled(false);
        itemCfg.addActionListener(this);

        itemExit = new MenuItem(MENU_EXIT_TEXT);
        itemExit.setActionCommand(MENU_EXIT_CMD);
        itemExit.addActionListener(this);

        itemAbout = new MenuItem(MENU_ABOUT_TEXT);
        itemAbout.setActionCommand(MENU_ABOUT_CMD);
        itemAbout.addActionListener(this);

        menu.add(itemExit);
        menu.addSeparator();
        menu.add(itemAbout);
        menu.addSeparator();
        menu.add(itemCfg);
        menu.add(itemRepoll);

        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(nntpIcon);
            nntpIcon.displayMessage("", "nntp//rss v" + AppConstants.VERSION
                    + " starting...", MessageType.INFO);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showStarted() {
        nntpIcon.setToolTip("nntp//rss v" + AppConstants.VERSION);
        itemCfg.setEnabled(true);
        itemRepoll.setEnabled(true);
    }

    public void shutdown() {
        nntpIcon.setToolTip(
            "nntp//rss v" + AppConstants.VERSION + " shutting down...");
        itemRepoll.setEnabled(false);
        itemCfg.setEnabled(false);
        itemAbout.setEnabled(false);
        itemExit.setEnabled(false);
    }

    private void startBrowser(String url) {
        try {
            URI uriToBrowse = new URI(url);
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(uriToBrowse);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e == null)
            return;
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals(MENU_ABOUT_CMD)) {
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
                null,
                message,
                MENU_ABOUT_TEXT,
                JOptionPane.INFORMATION_MESSAGE);
        } else if (actionCommand.equals(MENU_EXIT_CMD)) {
            Object[] options = { "Shutdown", "Cancel" };
            int option =
                JOptionPane.showOptionDialog(
                    null,
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
        } else if (actionCommand.equals(MENU_PROPERTIES_CMD)) {
            startBrowser(adminURL);
        } else if (actionCommand.equals(MENU_REPOLL_CMD)) {
            channelManager.repollAllChannels();
        } else {
            // Invalid command, should not happen
            JOptionPane.showMessageDialog(null, actionCommand);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            switch (e.getClickCount()) {
            case 1:
                // just skip single mouse clicks
                break;

            default:
                startBrowser(adminURL);
                break;
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

}
