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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.*;

import javax.mail.internet.MailDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpStatus;
import org.methodize.nntprss.feed.Category;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.publish.*;
import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.util.*;
import org.mortbay.servlet.MultiPartRequest;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AdminServlet.java,v 1.21 2007/12/17 04:06:54 jasonbrome Exp $
 * 
 * Web Administration interface for nntp//rss
 * 
 * In its current implementation, it's a rather inelegant
 * bundle of admin + presentation logic.  When I find the
 * right lightweight template-driven solution, I'll switch
 * over to that...
 * 
 */
public class AdminServlet extends HttpServlet {

	private static final long serialVersionUID = 8555883793099816127L;
	
	private static final String TAB_HELP = "help";
    private static final String TAB_CONFIG = "config";
    private static final String TAB_QUICKEDIT = "quickedit";
    private static final String TAB_ADD_CHANNEL = "add";
    private static final String TAB_VIEW_CATEGORIES = "categories";
    private static final String TAB_VIEW_CHANNELS = "channels";
    private static final String CSS_HEADER = "<style type='text/css'>" 
    	+ "<!--"
        //		+ "a:link,a:active,a:visited { color : #FFF240; } "
        //		+ "a:hover { text-decoration: underline; color : #FFF240; } "
    	+ "body { background-color: #E5E5E5; } "
        + ".bodyborder { background-color: #FFFFFF; border: 1px #98AAB1 solid; } "
        + ".tableborder { background-color: #FFFFFF; border: 2px #006699 solid; } "
        + ".smalltext { font-family: Verdana, Arial, Helvetica, sans-serif; font-size : 9px; } "
        + "font,th,td,p { font-family: Verdana, Arial, Helvetica, sans-serif; font-size : 12px; } "
        + "td.chlerror	{ background-color: #FF0000; } "
        + "td.chlwarning	{ background-color: #FFFF00; } "
        + "td.chldisabled	{ background-color: #CCCCCC; } "
        + "td.row1	{ background-color: #EFEFEF; vertical-align: top } "
        + "td.row2	{ background-color: #DEE3E7; vertical-align: top } "
        + "a.chlerror { color: #FFFFFF; text-decoration: underline} "
        + "a.head { color: #FFFFFF; text-decoration: none} "
        + "a:hover.head { text-decoration: underline; color : #FFF240; } "
        + "a.head2 { color: #FFFFFF; text-decoration: underline} "
        + "a:hover.head2 { text-decoration: underline; color : #FFF240; } "
        + "a.row { text-decoration: none} "
        + "a:hover.row { text-decoration: underline } "
        + "th	{ color: #FFF240; font-size: 11px; font-weight : bold; background-color: #408BFF; height: 25px; } "
        + "a.tableHead { color: #FFF240; text-decoration: underline} "
        + "th.subHead	{ background-color: #2D62B3; color: #FFFFFF; height: 18px;} "
        + "th.subHeadSelected	{ background-color: #408BFF; color: #FFFFFF; height: 18px;} "
        + "input,textarea, select {	color : #000000; font: normal 11px Verdana, Arial, Helvetica, sans-serif; border-width: 2px; border-color : #000000; } "
        + "-->"
        + "</style>";

    // Magic value to indicate that the current password should not be changed.
    // Currently used within posting/publishing configuration.
    private static final String PASSWORD_MAGIC_KEY = "###__KCV__###";

    private void writeHeader(Writer writer, String tab) throws IOException {

        writer.write("<html><head><title>nntp//rss admin</title>");
        writer.write(CSS_HEADER);
        writer.write("</head>");
        //		writer.write("<body topmargin='0' leftmargin='0' marginheight='0' marginwidth='0' bgcolor='#ffffff' link='#0000FF' alink='#0000FF' vlink='#0000FF'>\n");
        writer.write(
            "<body bgcolor='#ffffff' link='#0000FF' alink='#0000FF' vlink='#0000FF'>\n");

        writer.write(
            "<table width='100%' border='0' cellspacing='0' cellpadding='2'><tr><td class='bodyborder' bgcolor='#FFFFFF'>");

        writer.write(
            "<table width='100%' border='0' cellspacing='3' cellpadding='0'>");
        writer.write("<tr><th colspan='7'>nntp//rss Administration</th></tr>");
        writer.write("<tr>");
        writer.write(
            "<th class='subHead"
                + "' width='50%' align='left'>&nbsp;</th>");
//		writer.write(
//			"<th class='subHead"
//				+ (tab.equals(TAB_FIND_FEEDS) ? "Selected" : "")
//				+ "' width='50%' align='left'>&nbsp;<a class='head' href='?action=findfeedsform'>Find Feeds</a></th>");
        writer.write(
            "<th class='subHead"
                + (tab.equals(TAB_VIEW_CATEGORIES) ? "Selected" : "")
                + "' nowrap='nowrap'>&nbsp;<a class='head' href='?action=categories'>Categories</a>&nbsp;</td>");
        writer.write(
            "<th class='subHead"
                + (tab.equals(TAB_VIEW_CHANNELS) ? "Selected" : "")
                + "' nowrap='nowrap'>&nbsp;<a class='head' href='/'>Channels</a>&nbsp;</td>");
        writer.write(
            "<th class='subHead"
                + (tab.equals(TAB_ADD_CHANNEL) ? "Selected" : "")
                + "' nowrap='nowrap'>&nbsp;<a class='head' href='?action=addform'>Add Channel</a>&nbsp;</td>");
        writer.write(
            "<th class='subHead"
                + (tab.equals(TAB_QUICKEDIT) ? "Selected" : "")
                + "' nowrap='nowrap'>&nbsp;<a class='head' href='?action=quickedit'>Quick Edit</a>&nbsp;</td>");
        writer.write(
            "<th class='subHead"
                + (tab.equals(TAB_CONFIG) ? "Selected" : "")
                + "' nowrap='nowrap'>&nbsp;<a class='head' href='?action=showconfig'>System Configuration</a>&nbsp;</td>");
        writer.write(
            "<th class='subHead"
                + (tab.equals(TAB_HELP) ? "Selected" : "")
                + "' width='50%' align='right'><a class='head' href='?action=help'>Help</a>&nbsp;</th>");
        writer.write("</tr>");
        writer.write("</table>");

        writer.write(
            "<table border='0' cellspacing='0' cellpadding='0' height='100%' width='100%'>");
        //		writer.write("<tr><td colspan='3' width='100%' bgcolor='#dddddd'><font size='+2'><b>&nbsp;nntp//rss Admin</b></font><hr width='100%'></td></tr>");
        //		writer.write("<tr height='100%'><td valign='top' bgcolor='#dddddd'><br><a href='/'>View Channels</a><p><a href='?action=addform'>Add Channel</a><p><a href='?action=showconfig'>System Configuration</a></td><td>&nbsp;&nbsp;&nbsp;</td><td width='90%' height='100%' valign='top'><br>");
        writer.write(
            "<tr height='100%'><td width='100%' height='100%' valign='top' align='center'><br>");
    }

    private void writeFooter(Writer writer) throws IOException {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);

        writer.write("<p>");
        writer.write("</td></tr></table>");

        writer.write(
            "<table cellspacing='0' cellpadding='2' width='100%'><tr><td class='row2'>nntp//rss v"
                + AppConstants.VERSION
                + "</td><td class='row2' align='center'>nntp//rss Time: "
                + df.format(new Date())
                + "</td><td class='row2' align='right'><a href='http://www.methodize.org/nntprss'>nntp//rss home page</a>&nbsp;&nbsp;</td></tr></table>");

        writer.write("</td></tr></table>");

        writer.write("</body></html>");
    }

    private void writeConfig(
        Writer writer,
        ChannelManager channelManager,
        NNTPServer nntpServer)
        throws IOException {
        writer.write("<form action='?action=updateconfig' method='POST'>");
        writer.write("<table class='tableBorder'>");

        writer.write(
            "<tr><th colspan='2' class='titleHead'>System Configuration</th></tr>");

        // Polling
        writer.write("<tr><th colspan='2' class='subHead'>Polling</th></tr>");

        writer.write(
            "<tr><td class='row1' align='right'><nobr>Default Channel Polling Interval<nobr></td>");
        writer.write("<td class='row2'>Every <select name='pollingInterval'>");
        writer.write(
            "<option selected value='"
                + channelManager.getPollingIntervalSeconds()
                + "'>"
                + channelManager.getPollingIntervalSeconds() / 60
                + "\n");
        for (int interval = 10; interval <= 120; interval += 10) {
            writer.write(
                "<option value='" + (interval * 60) + "'>" + interval + "\n");
        }
        writer.write("</select> minutes </td></tr>");

        writer.write(
            "<tr><td class='row1' align='right'>Observe HTTP 301</td>");
        writer.write(
            "<td class='row2'><input type='checkbox' name='observeHttp301' value='true' ");
        if (channelManager.isObserveHttp301()) {
            writer.write("checked");
        }
        writer.write(
            "><br><i>When checked, nntp//rss will update the URL of a feed when a 301 (Permanent Redirection) message is received from the remote web server.</td></tr>");

        // NNTP Server
        writer.write(
            "<tr><th colspan='2' class='subHead'>NNTP Server</th></tr>");

        writer.write(
            "<tr><td class='row1' align='right'>This Machine's Hostname</td><td class='row2'><input type='text' name='hostName' value='"
                + (nntpServer.getHostName() == null
                    ? ""
                    : nntpServer.getHostName())
                + "'><br><i>The host name of the machine running nntp//rss.  This is used when creating news:// links, and enabling access to the nntp//rss web interface from within your newsreader.</i></td></tr>");

        writer.write("<tr><td class='row1' align='right'>Content Type</td>");
        writer.write("<td class='row2'><select name='contentType'>");
        int contentType = nntpServer.getContentType();
        writer.write(
            "<option value='"
                + AppConstants.CONTENT_TYPE_MIXED
                + "'"
                + (contentType == AppConstants.CONTENT_TYPE_MIXED
                    ? " selected"
                    : "")
                + ">Text & HTML (multipart/alternative)");
        writer.write(
            "<option value='"
                + AppConstants.CONTENT_TYPE_TEXT
                + "'"
                + (contentType == AppConstants.CONTENT_TYPE_TEXT
                    ? " selected"
                    : "")
                + ">Text (text/plain)");
        writer.write(
            "<option value='"
                + AppConstants.CONTENT_TYPE_HTML
                + "'"
                + (contentType == AppConstants.CONTENT_TYPE_HTML
                    ? " selected"
                    : "")
                + ">HTML (text/html)");

        writer.write("</select></td></tr>");

        writer.write(
            "<tr><td class='row1' align='right'>Text (text/plain) Footnote URLs</td>");
        writer.write(
            "<td class='row2'><input type='checkbox' name='footnoteUrls' value='true' ");
        if (nntpServer.isFootnoteUrls()) {
            writer.write("checked");
        }
        writer.write("></td></tr>");

        writer.write(
            "<tr><td class='row1' align='right'>Authenticated NNTP Access</td>");
        writer.write(
            "<td class='row2'><input type='checkbox' name='nntpSecure' value='true' ");
        if (nntpServer.isSecure()) {
            writer.write("checked");
        }
        writer.write("></td></tr>");

        // Proxy
        writer.write("<tr><th colspan='2' class='subHead'>Proxy</th></tr>");

        writer.write("<tr><td class='row1' align='right'>Use Proxy</td>");
        writer.write(
            "<td class='row2'><input type='checkbox' name='useProxy' value='true' ");
        if (channelManager.isUseProxy()) {
            writer.write("checked");
        }
        writer.write("></td></tr>");

        writer.write(
            "<tr><td class='row1' align='right'>Proxy Server Hostname</td><td class='row2'><input type='text' name='proxyServer' value='"
                + (channelManager.getProxyServer() == null
                    ? ""
                    : channelManager.getProxyServer())
                + "'><br><i>Host name of your proxy server, leave blank if no proxy</i></td></tr>");
        writer.write(
            "<tr><td class='row1' align='right'>Proxy Server Port</td><td class='row2'><input type='text' name='proxyPort' value='"
                + (channelManager.getProxyPort() == 0
                    ? ""
                    : Integer.toString(channelManager.getProxyPort()))
                + "'><br><i>Proxy server listener port, leave blank if no proxy</i></td></tr>");
        writer.write(
            "<tr><td class='row1' align='right'>Proxy User ID</td><td class='row2'><input type='text' name='proxyUserID' value='"
                + ((channelManager.getProxyUserID() == null)
                    ? ""
                    : channelManager.getProxyUserID())
                + "'><br><i>Proxy userid, leave blank if no userid</i></td></tr>");
        writer.write(
            "<tr><td class='row1' align='right'>Proxy Password</td><td class='row2'><input type='password' name='proxyPassword' value='"
                + ((channelManager.getProxyPassword() == null)
                    ? ""
                    : channelManager.getProxyPassword())
                + "'><br><i>Proxy password, leave blank if no password</i></td></tr>");

        writer.write(
            "<tr><td class='row2' align='center' colspan='2'><input type='submit' name='update' value='Update'></td></tr>");
        writer.write("</table>");
        writer.write("</form>");
        writer.write("<p>");
        writer.write(
            "Export <a class='row' href='/?action=export'>nntp//rss</a> or <a class='row' href='/?action=exportopml'>mySubscriptions.opml</a> Channel List<p>");
        writer.write(
            "<a class='row' href='/?action=importform'>Import nntp//rss or mySubscriptions.opml Channel List</a>");
    }

    private void cmdShowConfig(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_CONFIG);

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        NNTPServer nntpServer =
            (NNTPServer) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_NNTP_SERVER);

        writeConfig(writer, channelManager, nntpServer);
        writeFooter(writer);
    }

    private void cmdUpdateConfig(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_CONFIG);

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        NNTPServer nntpServer =
            (NNTPServer) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_NNTP_SERVER);

        // NNTP Server config
        nntpServer.setContentType(
            Integer.parseInt(request.getParameter("contentType")));

        String secure = request.getParameter("nntpSecure");
        nntpServer.setSecure((secure != null) && secure.equals("true"));

        String footnoteUrls = request.getParameter("footnoteUrls");
        nntpServer.setFootnoteUrls(
            (footnoteUrls != null) && footnoteUrls.equals("true"));

        // We will not allow the hostname to be blank - if the user erases the
        // contents of the field, default to the current host name
        nntpServer.setHostName(request.getParameter("hostName").trim());
        if (nntpServer.getHostName().length() == 0) {
            nntpServer.setHostName(AppConstants.getCurrentHostName());
        }
        nntpServer.saveConfiguration();

        // Channel Manager config
        channelManager.setPollingIntervalSeconds(
            Long.parseLong(request.getParameter("pollingInterval")));
        String observeHttp301 = request.getParameter("observeHttp301");
        channelManager.setObserveHttp301(
            (observeHttp301 != null) && observeHttp301.equals("true"));

        channelManager.setProxyServer(
            request.getParameter("proxyServer").trim());
        String proxyPortStr = request.getParameter("proxyPort");
        boolean validPort = true;

        if (proxyPortStr.length() == 0) {
            channelManager.setProxyPort(0);
        } else {
            try {
                channelManager.setProxyPort(Integer.parseInt(proxyPortStr));
            } catch (NumberFormatException nfe) {
                validPort = false;
            }
        }

        channelManager.setProxyUserID(
            request.getParameter("proxyUserID").trim());
        channelManager.setProxyPassword(
            request.getParameter("proxyPassword").trim());
        String useProxy = request.getParameter("useProxy");
        channelManager.setUseProxy(
            (useProxy != null) && useProxy.equals("true"));

        if (validPort == true) {
            channelManager.saveConfiguration();
            writer.write("System configuration successfully updated.<p>");
        } else {
            writer.write(
                "<b>Proxy port must either be blank or a numeric value!</b><p>");
        }

        writeConfig(writer, channelManager, nntpServer);
        writeFooter(writer);
    }

    private boolean isChecked(HttpServletRequest request, String checkbox) {
        String checkboxValue = request.getParameter(checkbox);
        if (checkboxValue != null && checkboxValue.equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    private void writeChannel(
        Writer writer,
        Channel channel,
        HttpServletRequest request,
        boolean refresh)
        throws IOException {
        if (channel == null) {
            writer.write("<b>Channel " + channel.getName() + " not found!</b>");
        } else {
            ChannelManager channelManager =
                (ChannelManager) getServletContext().getAttribute(
                    AdminServer.SERVLET_CTX_RSS_MANAGER);

            DateFormat df =
                DateFormat.getDateTimeInstance(
                    DateFormat.FULL,
                    DateFormat.FULL);

            String url =
                ((!refresh) ? channel.getUrl() : request.getParameter("URL"));
            boolean enabled =
                ((!refresh)
                    ? channel.isEnabled()
                    : isChecked(request, "enabled"));
            boolean parseAtAllCost =
                ((!refresh)
                    ? channel.isParseAtAllCost()
                    : isChecked(request, "parseAtAllCost"));
            //			boolean historical = ((!refresh) ? channel.isHistorical() : isChecked(request, "historical"));
            boolean postingEnabled =
                ((!refresh)
                    ? channel.isPostingEnabled()
                    : (!request.getParameter("postingEnabled").equals("false")));
            String publishAPI =
                ((!refresh)
                    ? channel.getPublishAPI()
                    : request.getParameter("publishAPI"));
//            long pollingIntervalSeconds =
//                ((!refresh)
//                    ? channel.getPollingIntervalSeconds()
//                    : Long.parseLong(request.getParameter("pollingInterval")));
//            long expiration =
//                ((!refresh)
//                    ? channel.getExpiration()
//                    : Long.parseLong(request.getParameter("expiration")));
            int categoryId = 0;
            if (!refresh) {
                if (channel.getCategory() != null) {
                    categoryId = channel.getCategory().getId();
                }
            } else {
                categoryId =
                    Integer.parseInt(request.getParameter("categoryId"));
            }

            writer.write(
                "<form name='channel' action='?action=update' method='POST'>");
            writer.write(
                "<input type='hidden' name='name' value='"
                    + HTMLHelper.escapeString(channel.getName())
                    + "'>");
            writer.write("<table class='tableborder'>");

            writer.write(
                "<tr><th class='tableHead' colspan='2'>Channel Configuration</th></tr>");

            writer.write(
                "<tr><td class='row1' align='right'>Title</td><td class='row2'>"
                    + HTMLHelper.escapeString(
                        channel.getTitle() == null
                            ? "Unknown"
                            : channel.getTitle())
                    + "</td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Newsgroup Name</td><td class='row2'>"
                    + HTMLHelper.escapeString(channel.getName())
                    + "</td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>URL</td><td class='row2'><input type='text' name='URL' value='"
                    + HTMLHelper.escapeString(url)
                    + "' size='64'></td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Polling</td><td class='row2'>"
                    + "<input name='enabled' type='checkbox' value='true' "
                    + (enabled ? "checked>" : ">")
                    + "</td></tr>");

            writer.write(
                "<tr><td class='row1' align='right'>Polling Interval</td>");
            writer.write("<td class='row2'><select name='pollingInterval'>");

            if (channel.getPollingIntervalSeconds()
                == Channel.DEFAULT_POLLING_INTERVAL) {
                writer.write(
                    "<option selected value='"
                        + Channel.DEFAULT_POLLING_INTERVAL
                        + "'>Use Default Polling Interval\n");
            } else {
                writer.write(
                    "<option selected value='"
                        + channel.getPollingIntervalSeconds()
                        + "'>"
                        + channel.getPollingIntervalSeconds() / 60
                        + " minutes\n");
            }

            writer.write(
                "<option value='"
                    + Channel.DEFAULT_POLLING_INTERVAL
                    + "'>Use Default Polling Interval\n");
            for (int interval = 10; interval <= 120; interval += 10) {
                writer.write(
                    "<option value='"
                        + (interval * 60)
                        + "'>"
                        + interval
                        + " minutes\n");
            }
            writer.write("</select></td></tr>");

            writer.write(
                "<tr><td class='row1' align='right'>Parse-at-all-costs</td><td class='row2'><input name='parseAtAllCost' type='checkbox' value='true' "
                    + (parseAtAllCost ? "checked>" : ">")
                    + "<br><i>This will enable the experimental parse-at-all-costs RSS parser.  This feature supports the parsing of badly-formatted RSS feeds.</i></td></tr>");

            writer.write("<tr><td class='row1' align='right'>Status</td>");

            switch (channel.getStatus()) {
                case Channel.STATUS_NOT_FOUND :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><font color='#FFFFFF'>Feed Web Server is returning File Not Found.</font>");
                    break;
                case Channel.STATUS_INVALID_CONTENT :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><font color='#FFFFFF'>Last feed document retrieved could not be parsed, <a class='chlerror' target='validate' href='http://feedvalidator.org/check?url="
                            + HTMLHelper.escapeString(url)
                            + "'>check URL</a>.</font>");
                    break;
                case Channel.STATUS_UNKNOWN_HOST :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><font color='#FFFFFF'>Unable to contact Feed Web Server (Unknown Host).  Check URL.</font>");
                    break;
                case Channel.STATUS_NO_ROUTE_TO_HOST :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><font color='#FFFFFF'>Unable to contact Feed Web Server (No Route To Host).  Check URL.</font>");
                    break;
                case Channel.STATUS_CONNECTION_TIMEOUT :
                    writer.write(
                        "<td class='chlwarning' bgcolor='#FFFF00'><font color='#000000'>Currently unable to contact Feed Web Server (Connection timeout).</font>");
                    break;
                case Channel.STATUS_SOCKET_EXCEPTION :
                    writer.write(
                        "<td class='chlwarning' bgcolor='#FFFF00'><font color='#000000'>Currently unable to contact Feed Web Server (Socket exception).</font>");
                    break;
                case Channel.STATUS_PROXY_AUTHENTICATION_REQUIRED :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FFFF00'><font color='#FFFFFF'>Proxy authentication required.  Please configure user name and password in <a class='chlerror' href='?action=showconfig'>System Configuration</a>.</font>");
                    break;
                case Channel.STATUS_USER_AUTHENTICATION_REQUIRED :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FFFF00'><font color='#FFFFFF'>User authentication required. Please specific user name and password in the URL, e.g.<br>http://username:password@www.myhost.com/feed.xml</font>");
                    break;
                default :
                    writer.write("<td class='row2'>OK");
            }

            writer.write("</td></tr>");

            writer.write(
                "<tr><td class='row1' align='right'>Last Polled</td><td class='row2'>"
                    + ((channel.getLastPolled() == null)
                        ? "Yet to be polled."
                        : df.format(channel.getLastPolled()))
                    + "</td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Last Modified</td><td class='row2'>");
            if (channel.getLastModified() == 0) {
                writer.write("Last modified not supplied by Feed Web Server");
            } else {
                writer.write(df.format(new Date(channel.getLastModified())));
            }
            writer.write("</td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Last ETag</td><td class='row2'>");
            if (channel.getLastETag() == null) {
                writer.write("ETag not supplied by Feed Web Server");
            } else {
                writer.write(channel.getLastETag());
            }
            writer.write("</td></tr>");

            writer.write(
                "<tr><td class='row1' align='right'>Feed Type</td><td class='row2'>");
            if (channel.getRssVersion() == null) {
                writer.write("Unknown");
            } else {
                writer.write(channel.getRssVersion());
            }
            writer.write("</td></tr>");

            //			writer.write("<tr><td class='row1' align='right'>Historical</td><td class='row2'><input name='historical' type='checkbox' value='true' "
            //				+ (historical ? "checked>" : ">")
            //				+ "</td></tr>");

            writeExpiration(writer, channel);

            writer.write("<tr><td class='row1' align='right'>Category</td>");
            writer.write("<td class='row2'><select name='categoryId'>");

            writeOption(writer, "[No Category]", 0, categoryId);
            Iterator categories = channelManager.categories();
            while (categories.hasNext()) {
                Category category = (Category) categories.next();
                writeOption(
                    writer,
                    category.getName(),
                    category.getId(),
                    categoryId);
            }
            writer.write("</select></td></tr>");

            writer.write(
                "<tr><td class='row1' align='right'>Managing Editor</td><td class='row2'>");
            if (channel.getManagingEditor() != null) {
                writer.write(
                    "<a href='mailto:"
                        + URLEncoder.encode(
                            RSSHelper.parseEmail(channel.getManagingEditor()))
                        + "'>"
                        + HTMLHelper.escapeString(channel.getManagingEditor())
                        + "</a>");
            } else {
                writer.write("Unknown");
            }

            writer.write("</td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Posting</td><td class='row2'><select name='postingEnabled' onChange='this.form.action=\"?action=editchlrefresh\"; this.form.submit();'>"
                    + "<option "
                    + (postingEnabled ? "selected" : "")
                    + ">true"
                    + "<option "
                    + (!postingEnabled ? "selected" : "")
                    + ">false"
                    + "</select></td></tr>");

            if (postingEnabled) {

                writer.write(
                    "<tr><th class='subHead' colspan='2' align='center'>Posting Configuration</td></tr>");

                writer
                    .write(
                        "<tr><td class='row1' align='right'>API</td><td class='row2'><select name='publishAPI' onChange='this.form.action=\"?action=editchlrefresh&publishapichange=true\"; this.form.submit();'>"
                        + "<option value='blogger' "
                        + (publishAPI == null
                            || publishAPI.equals("blogger") ? "selected" : "")
                        + ">Blogger"
                        + "<option value='livejournal' "
                        + (publishAPI != null
                            && publishAPI.equals("livejournal") ? "selected" : "")
                        + ">LiveJournal"
                        + "<option value='metaweblog' "
                        + (publishAPI != null
                            && publishAPI.equals("metaweblog") ? "selected" : "")
                        + ">MetaWeblog"
                //					+ "<option " + (!postingEnabled ? "selected" : "") + ">false"
                +"</select></td></tr>");

                if (publishAPI == null || publishAPI.equals("blogger")) {
                    // Default API
                    String publishUrl = null;
                    String blogId = null;
                    String userName = null;
                    String password = null;
                    boolean autoPublish = true;

                    if (refresh) {
                        // If a refresh, get the parameter values from the parameter collection
                        if (request.getParameter("publishapichange") == null) {
                            publishUrl =
                                request.getParameter(
                                    BloggerPublisher.PROP_PUBLISHER_URL);
                        }
                        blogId =
                            (String) request.getParameter(
                                BloggerPublisher.PROP_BLOG_ID);
                        userName =
                            (String) request.getParameter(
                                BloggerPublisher.PROP_USERNAME);
                        password =
                            (String) request.getParameter(
                                BloggerPublisher.PROP_PASSWORD);
                        String autoPublishStr =
                            request.getParameter(BloggerPublisher.PROP_PUBLISH);
                        autoPublish =
                            (autoPublishStr != null
                                && autoPublishStr.equals("false"))
                                ? false
                                : true;
                    } else {
                        // If a initial channel view, extract parameter from the Channel publish config map
                        Map publishConfig = channel.getPublishConfig();
                        if (publishConfig != null) {
                            publishUrl =
                                (String) publishConfig.get(
                                    BloggerPublisher.PROP_PUBLISHER_URL);
                            blogId =
                                (String) publishConfig.get(
                                    BloggerPublisher.PROP_BLOG_ID);
                            userName =
                                (String) publishConfig.get(
                                    BloggerPublisher.PROP_USERNAME);
                            password =
                                (String) publishConfig.get(
                                    BloggerPublisher.PROP_PASSWORD);
                            if (password != null)
                                password = PASSWORD_MAGIC_KEY;
                            String autoPublishStr =
                                (String) publishConfig.get(
                                    BloggerPublisher.PROP_PUBLISH);
                            autoPublish =
                                (autoPublishStr != null
                                    && autoPublishStr.equals("false"))
                                    ? false
                                    : true;
                        }
                    }

                    // Make sure that everything has a value, especially if publish has just been enabled
                    if (publishUrl == null)
                        publishUrl = "http://plant.blogger.com/api/RPC2";
                    if (blogId == null)
                        blogId = "";
                    if (userName == null)
                        userName = "";
                    if (password == null)
                        password = "";

                    writer.write("<tr><td class='row1' align='right'>URL</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + BloggerPublisher.PROP_PUBLISHER_URL
                            + "' type='text' size='64' value='"
                            + HTMLHelper.escapeString(publishUrl)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Blog Id</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + BloggerPublisher.PROP_BLOG_ID
                            + "' type='text' value='"
                            + HTMLHelper.escapeString(blogId)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Username</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + BloggerPublisher.PROP_USERNAME
                            + "' type='text' value='"
                            + HTMLHelper.escapeString(userName)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Password</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + BloggerPublisher.PROP_PASSWORD
                            + "' type='password' value='"
                            + HTMLHelper.escapeString(password)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Auto Publish</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + BloggerPublisher.PROP_PUBLISH
                            + "' type='checkbox' value='true' "
                            + (autoPublish ? "checked" : "")
                            + "></td></tr>");

                } else if (publishAPI.equals("metaweblog")) {
                    String publishUrl = null;
                    String blogId = null;
                    String userName = null;
                    String password = null;
                    boolean autoPublish = true;

                    if (refresh) {
                        // If a refresh, get the parameter values from the parameter collection
                        if (request.getParameter("publishapichange") == null) {
                            publishUrl =
                                request.getParameter(
                                    MetaWeblogPublisher.PROP_PUBLISHER_URL);
                        }
                        blogId =
                            (String) request.getParameter(
                                BloggerPublisher.PROP_BLOG_ID);
                        if (blogId != null && blogId.length() == 0)
                            blogId = "home";

                        userName =
                            (String) request.getParameter(
                                MetaWeblogPublisher.PROP_USERNAME);
                        password =
                            (String) request.getParameter(
                                MetaWeblogPublisher.PROP_PASSWORD);
                        String autoPublishStr =
                            request.getParameter(
                                MetaWeblogPublisher.PROP_PUBLISH);
                        autoPublish =
                            (autoPublishStr != null
                                && autoPublishStr.equals("false"))
                                ? false
                                : true;
                    } else {
                        // If a initial channel view, extract parameter from the Channel publish config map
                        Map publishConfig = channel.getPublishConfig();
                        if (publishConfig != null) {
                            publishUrl =
                                (String) publishConfig.get(
                                    MetaWeblogPublisher.PROP_PUBLISHER_URL);
                            userName =
                                (String) publishConfig.get(
                                    MetaWeblogPublisher.PROP_USERNAME);
                            password =
                                (String) publishConfig.get(
                                    MetaWeblogPublisher.PROP_PASSWORD);
                            if (password != null)
                                password = PASSWORD_MAGIC_KEY;
                            String autoPublishStr =
                                (String) publishConfig.get(
                                    MetaWeblogPublisher.PROP_PUBLISH);
                            autoPublish =
                                (autoPublishStr != null
                                    && autoPublishStr.equals("false"))
                                    ? false
                                    : true;
                        }
                    }

                    // Make sure that everything has a value, especially if publish has just been enabled
                    if (publishUrl == null)
                        publishUrl = "http://127.0.0.1:5335/RPC2";
                    if (blogId == null)
                        blogId = "home";
                    if (userName == null)
                        userName = "";
                    if (password == null)
                        password = "";

                    writer.write("<tr><td class='row1' align='right'>URL</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + MetaWeblogPublisher.PROP_PUBLISHER_URL
                            + "' type='text' size='64' value='"
                            + HTMLHelper.escapeString(publishUrl)
                            + "'><br><i>Ensure that the URL points to your MetaWeblog (e.g. Radio Userland) host</i></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Blog Id</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + BloggerPublisher.PROP_BLOG_ID
                            + "' type='text' value='"
                            + HTMLHelper.escapeString(blogId)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Username</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + MetaWeblogPublisher.PROP_USERNAME
                            + "' type='text' value='"
                            + HTMLHelper.escapeString(userName)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Password</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + MetaWeblogPublisher.PROP_PASSWORD
                            + "' type='password' value='"
                            + HTMLHelper.escapeString(password)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Auto Publish</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + MetaWeblogPublisher.PROP_PUBLISH
                            + "' type='checkbox' value='true' "
                            + (autoPublish ? "checked" : "")
                            + "></td></tr>");

                } else if (publishAPI.equals("livejournal")) {
                    String publishUrl = null;
                    String userName = null;
                    String password = null;

                    if (refresh) {
                        // If a refresh, get the parameter values from the parameter collection
                        if (request.getParameter("publishapichange") == null) {
                            publishUrl =
                                request.getParameter(
                                    LiveJournalPublisher.PROP_PUBLISHER_URL);
                        }
                        userName =
                            (String) request.getParameter(
                                LiveJournalPublisher.PROP_USERNAME);
                        password =
                            (String) request.getParameter(
                                LiveJournalPublisher.PROP_PASSWORD);
                    } else {
                        // If a initial channel view, extract parameter from the Channel publish config map
                        Map publishConfig = channel.getPublishConfig();
                        if (publishConfig != null) {
                            publishUrl =
                                (String) publishConfig.get(
                                    LiveJournalPublisher.PROP_PUBLISHER_URL);
                            userName =
                                (String) publishConfig.get(
                                    LiveJournalPublisher.PROP_USERNAME);
                            password =
                                (String) publishConfig.get(
                                    LiveJournalPublisher.PROP_PASSWORD);
                            if (password != null)
                                password = PASSWORD_MAGIC_KEY;
                        }
                    }

                    // Make sure that everything has a value, especially if publish has just been enabled
                    if (publishUrl == null)
                        publishUrl =
                            "http://www.livejournal.com/interface/xmlrpc";
                    if (userName == null)
                        userName = "";
                    if (password == null)
                        password = "";

                    writer.write("<tr><td class='row1' align='right'>URL</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + LiveJournalPublisher.PROP_PUBLISHER_URL
                            + "' type='text' size='64' value='"
                            + HTMLHelper.escapeString(publishUrl)
                            + "'><br></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Username</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + LiveJournalPublisher.PROP_USERNAME
                            + "' type='text' value='"
                            + HTMLHelper.escapeString(userName)
                            + "'></td></tr>");

                    writer.write(
                        "<tr><td class='row1' align='right'>Password</td>");
                    writer.write(
                        "<td class='row2'><input name='"
                            + LiveJournalPublisher.PROP_PASSWORD
                            + "' type='password' value='"
                            + HTMLHelper.escapeString(password)
                            + "'></td></tr>");

                }
            }

            writer.write(
                "<tr><td class='row2' align='center' colspan='2'><input type='submit' name='update' value='Update'>&nbsp;&nbsp;&nbsp;<input type='submit' name='delete' onClick='return confirm(\"Are you sure you want to delete this channel?\");' value='Delete'></td></tr>");
            writer.write("</table>");
            writer.write("</form>");
        }
    }

    private void writeCategory(
        Writer writer,
        Category category,
        HttpServletRequest request)
        throws IOException {
        if (category == null) {
            writer.write(
                "<b>Category " + category.getName() + " not found!</b>");
        } else {
            writer.write(
                "<form name='category' action='?action=categoryupdate' method='POST'>");
            writer.write(
                "<input type='hidden' name='name' value='"
                    + HTMLHelper.escapeString(category.getName())
                    + "'>");
            writer.write("<table class='tableborder'>");
            writer.write(
                "<tr><th class='tableHead' colspan='2'>Category Configuration</th></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Newsgroup Name</td><td class='row2'>"
                    + HTMLHelper.escapeString(category.getName())
                    + "</td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Linked Channels</td>"
                    + "<td class='row2'>");

            Iterator channelIter = category.getChannels().values().iterator();
            Set channelNames = new TreeSet();
            while (channelIter.hasNext()) {
                Channel channel = (Channel) channelIter.next();
                channelNames.add(channel.getName());
            }

            channelIter = channelNames.iterator();
            while (channelIter.hasNext()) {
                String channelName = (String) channelIter.next();
                writer.write(
                    "<a class='row' title='Channel configuration' href='/?action=show&name="
                        + URLEncoder.encode(channelName)
                        + "'>"
                        + HTMLHelper.escapeString(channelName)
                        + "</a><br>");
            }

            writer.write("</td></tr>");

            if (category.getChannels().size() == 0) {
                writer.write(
                    "<tr><td class='row2' align='center' colspan='2'><input type='submit' name='delete' onClick='return confirm(\"Are you sure you want to delete this category?\");' value='Delete'></td></tr>");
            } else {
                writer.write(
                    "<tr><td class='row2' align='center' colspan='2'>Please disassociate channels from category to enable category deletion.</td></tr>");
            }
            writer.write("</table>");
            writer.write("</form>");
        }
    }

    private void writeExpiration(Writer writer, Channel channel)
        throws IOException {
        writer.write("<tr><td class='row1' align='right'>Expiration</td>");
        writer.write("<td class='row2'><select name='expiration'>");

        long expiration = channel.getExpiration();

        writeOption(
            writer,
            "Keep all items",
            Channel.EXPIRATION_KEEP,
            expiration);
        writeOption(writer, "Keep only current items", 0, expiration);
        writeOption(
            writer,
            "Keep items for 1 day",
            (1000L * 60 * 60 * 24 * 1),
            expiration);
        writeOption(
            writer,
            "Keep items for 2 days",
            (1000L * 60 * 60 * 24 * 2),
            expiration);
        writeOption(
            writer,
            "Keep items for 4 days",
            (1000L * 60 * 60 * 24 * 4),
            expiration);
        writeOption(
            writer,
            "Keep items for 1 week",
            (1000L * 60 * 60 * 24 * 7),
            expiration);
        writeOption(
            writer,
            "Keep items for 2 weeks",
            (1000L * 60 * 60 * 24 * 14),
            expiration);
        writeOption(
            writer,
            "Keep items for 4 weeks",
            (1000L * 60 * 60 * 24 * 28),
            expiration);

		writeOption(
			writer,
			"Keep items for 6 months",
			((1000L * 60 * 60 * 24) * 180),
			expiration);

		writeOption(
			writer,
			"Keep items for 1 year",
			((1000L * 60 * 60 * 24) * 365),
			expiration);

        writer.write("</select></td></tr>");
    }

    private void writeOption(
        Writer writer,
        String option,
        long value,
        long selectedValue)
        throws IOException {
        if (value == selectedValue) {
            writer.write(
                "<option selected value='" + value + "'>" + option + "\n");
        } else {
            writer.write("<option value='" + value + "'>" + option + "\n");
        }
    }

    private void writeOption(
        StringBuffer buffer,
        String option,
        long value,
        long selectedValue)
        throws IOException {
        if (value == selectedValue) {
            buffer.append(
                "<option selected value='" + value + "'>" + option + "\n");
        } else {
            buffer.append("<option value='" + value + "'>" + option + "\n");
        }
    }

    private void cmdShowChannel(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CHANNELS);

        String channelName = request.getParameter("name");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        Channel channel = channelManager.channelByName(channelName);
        writeChannel(writer, channel, request, false);

        writeFooter(writer);
    }

    private void cmdShowCategory(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CATEGORIES);

        String categoryName = request.getParameter("name");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        Category category = channelManager.categoryByName(categoryName);
        writeCategory(writer, category, request);
        writeFooter(writer);
    }

    private void cmdUpdateChannel(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CHANNELS);

        String channelName = request.getParameter("name");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        Channel channel = channelManager.channelByName(channelName);

        if (request.getParameter("update") != null) {
            // Update channel.
            String urlString = request.getParameter("URL");
            List errors = new ArrayList();
            if (urlString.length() == 0) {
                errors.add("URL cannot be empty");
            } else if (
                urlString.equals("http://") || urlString.equals("https://")) {
                errors.add("You must specify a URL");
            } else if (
                !urlString.startsWith("http://")
                    && !urlString.startsWith("https://")) {
                errors.add(
                    "Only URLs starting http:// or https:// are supported");
            }

            boolean postingEnabled =
                request.getParameter("postingEnabled").equalsIgnoreCase("true");
            String publishAPI = null;
            Map publishConfig = null;
            if (postingEnabled) {
                publishConfig = new HashMap();
                publishAPI = request.getParameter("publishAPI");

                // Validate...
                //TODO: improve componentization / pluggability of publishers
                if (publishAPI.equals("blogger")) {
                    publishConfig.put(
                        BloggerPublisher.PROP_PUBLISHER_URL,
                        request.getParameter(
                            BloggerPublisher.PROP_PUBLISHER_URL));
                    publishConfig.put(
                        BloggerPublisher.PROP_USERNAME,
                        request.getParameter(BloggerPublisher.PROP_USERNAME));

                    String password =
                        request.getParameter(BloggerPublisher.PROP_PASSWORD);
                    if (password.equals(PASSWORD_MAGIC_KEY)
                        && (channel.getPublishConfig() != null)) {
                        password =
                            (String) channel.getPublishConfig().get(
                                BloggerPublisher.PROP_PASSWORD);
                    }
                    publishConfig.put(BloggerPublisher.PROP_PASSWORD, password);

                    publishConfig.put(
                        BloggerPublisher.PROP_BLOG_ID,
                        request.getParameter(BloggerPublisher.PROP_BLOG_ID));

                    String autoPublishStr =
                        (String) request.getParameter(
                            BloggerPublisher.PROP_PUBLISH);
                    if (autoPublishStr.equals("true")) {
                        publishConfig.put(
                            BloggerPublisher.PROP_PUBLISH,
                            "true");
                    } else {
                        publishConfig.put(
                            BloggerPublisher.PROP_PUBLISH,
                            "false");
                    }

                    try {
                        Publisher pub = new BloggerPublisher();
                        pub.validate(publishConfig);
                    } catch (PublisherException pe) {
                        errors.add(
                            "Error validating Blogger posting configuration - "
                                + pe.getMessage());
                        errors.add(
                            "Check Blogger URL, user name, password, and blog id.");
                    }

                } else if (publishAPI.equals("metaweblog")) {
                    publishConfig.put(
                        MetaWeblogPublisher.PROP_PUBLISHER_URL,
                        request.getParameter(
                            MetaWeblogPublisher.PROP_PUBLISHER_URL));
                    publishConfig.put(
                        MetaWeblogPublisher.PROP_USERNAME,
                        request.getParameter(
                            MetaWeblogPublisher.PROP_USERNAME));

                    String password =
                        request.getParameter(MetaWeblogPublisher.PROP_PASSWORD);
                    if (password.equals(PASSWORD_MAGIC_KEY)
                        && (channel.getPublishConfig() != null)) {
                        password =
                            (String) channel.getPublishConfig().get(
                                MetaWeblogPublisher.PROP_PASSWORD);
                    }
                    publishConfig.put(
                        MetaWeblogPublisher.PROP_PASSWORD,
                        password);

                    publishConfig.put(
                        MetaWeblogPublisher.PROP_BLOG_ID,
                        request.getParameter(MetaWeblogPublisher.PROP_BLOG_ID));

                    String autoPublishStr =
                        (String) request.getParameter(
                            MetaWeblogPublisher.PROP_PUBLISH);
                    if (autoPublishStr.equals("true")) {
                        publishConfig.put(
                            MetaWeblogPublisher.PROP_PUBLISH,
                            "true");
                    } else {
                        publishConfig.put(
                            MetaWeblogPublisher.PROP_PUBLISH,
                            "false");
                    }

                    try {
                        Publisher pub = new MetaWeblogPublisher();
                        pub.validate(publishConfig);
                    } catch (PublisherException pe) {
                        errors.add(
                            "Error validating MetaWeblog posting configuration - "
                                + pe.getMessage());
                        errors.add("Check URL, user name, and password.");
                    }

                } else if (publishAPI.equals("livejournal")) {
                    publishConfig.put(
                        LiveJournalPublisher.PROP_PUBLISHER_URL,
                        request.getParameter(
                            LiveJournalPublisher.PROP_PUBLISHER_URL));
                    publishConfig.put(
                        LiveJournalPublisher.PROP_USERNAME,
                        request.getParameter(
                            LiveJournalPublisher.PROP_USERNAME));

                    String password =
                        request.getParameter(
                            LiveJournalPublisher.PROP_PASSWORD);
                    if (password.equals(PASSWORD_MAGIC_KEY)
                        && (channel.getPublishConfig() != null)) {
                        password =
                            (String) channel.getPublishConfig().get(
                                LiveJournalPublisher.PROP_PASSWORD);
                    }
                    publishConfig.put(
                        LiveJournalPublisher.PROP_PASSWORD,
                        password);

                    try {
                        Publisher pub = new LiveJournalPublisher();
                        pub.validate(publishConfig);
                    } catch (PublisherException pe) {
                        errors.add(
                            "Error validating LiveJournal posting configuration - "
                                + pe.getMessage());
                        errors.add(
                            "Check LiveJournal URL, user name, and password.");
                    }
                }

            }

            if (errors.size() == 0) {
                try {
                    boolean parseAtAllCost =
                        isChecked(request, "parseAtAllCost");
                    boolean enabled = isChecked(request, "enabled");
                    boolean valid = true;

                    URL url = new URL(urlString);
                    if (!parseAtAllCost && enabled) {
                        try {
                            valid = Channel.isValid(url);
                            if (!valid) {
                                errors.add(
                                    "URL does not point to valid RSS or ATOM document");
                                errors.add(
                                    "<a target='validate' href='http://feedvalidator.org/check?url="
                                        + urlString
                                        + "'>Check the URL with the RSS and ATOM Validator @ archive.org</a><br>");
                            }
                        } catch (HttpUserException hue) {
                            if (hue.getStatus()
                                == HttpStatus.SC_UNAUTHORIZED) {
                                errors.add(
                                    "This feed requires user name and password authentication.  Please specify User Name and Password in the URL, e.g.");
                                errors.add(
                                    "http://username:password@www.myhost.com/");
                            } else if (
                                hue.getStatus()
                                    == HttpStatus
                                        .SC_PROXY_AUTHENTICATION_REQUIRED) {
                                errors.add(
                                    "You are using a proxy server that requires authentication.");
                                errors.add(
                                    "Please enter your User Name and Password in the <a href='?action=showconfig'>System Configuration.</a>");
                            }
                            valid = false;
                        }
                    }

                    if (valid) {
                        channel.setUrl(url);
                        //						channel.setHistorical(isChecked(request, "historical"));
                        channel.setExpiration(
                            Long.parseLong(request.getParameter("expiration")));

                        int categoryId =
                            Integer.parseInt(
                                request.getParameter("categoryId"));
                        boolean categoryChanged =
                            ((!(categoryId == 0
                                && channel.getCategory() == null))
                                && ((categoryId != 0
                                    && channel.getCategory() == null)
                                    || (categoryId
                                        != channel.getCategory().getId())));
                        Category oldCategory = channel.getCategory();

                        channel.setEnabled(enabled);
                        channel.setParseAtAllCost(parseAtAllCost);

                        channel.setPostingEnabled(postingEnabled);
                        channel.setPublishAPI(publishAPI);
                        channel.setPublishConfig(publishConfig);

                        channel.setPollingIntervalSeconds(
                            Long.parseLong(
                                request.getParameter("pollingInterval")));

                        if (categoryChanged) {
                            Category category = null;
                            if (oldCategory != null) {
                                oldCategory.removeChannel(channel);
                            }
                            if (categoryId != 0) {
                                category =
                                    channelManager.categoryById(categoryId);
                                category.addChannel(channel);
                            }
                            channel.setCategory(category);
                        }

                        channel.save();

                        if (enabled) {
                            // Reset status and last polled date - channel should
                            // get repolled on next iteration
                            channel.setStatus(Channel.STATUS_OK);
                            channel.setLastPolled(null);
                        }
                    }
                } catch (MalformedURLException me) {
                    errors.add("URL is malformed");
                }
            }

            if (errors.size() == 0) {
                writer.write(
                    "Channel <b>"
                        + channel.getName()
                        + "</b> successfully updated.<p>");
                writeChannel(writer, channel, request, false);
            } else {
                writer.write(
                    "<b>There were errors updating the channel:</b><p>");
                writeErrors(writer, errors);
                writeChannel(writer, channel, request, true);
            }

        } else if (request.getParameter("delete") != null) {
            channelManager.deleteChannel(channel);
            writer.write(
                "Channel <b>"
                    + channel.getName()
                    + "</b> successfully deleted.");
        }

        writeFooter(writer);
    }

    private void cmdUpdateCategory(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CATEGORIES);

        String categoryName = request.getParameter("name");
        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        Category category = channelManager.categoryByName(categoryName);

        if (request.getParameter("delete") != null) {
            channelManager.deleteCategory(category);
            writer.write(
                "Category <b>"
                    + category.getName()
                    + "</b> successfully deleted.");
        }

        writeFooter(writer);
    }

    private void cmdEditChannelRefresh(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CHANNELS);

        String channelName = request.getParameter("name");
        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        Channel channel = channelManager.channelByName(channelName);

        writeChannel(writer, channel, request, true);

        writeFooter(writer);
    }

    private void channelDelete(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Enumeration paramEnum = request.getParameterNames();
        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        while (paramEnum.hasMoreElements()) {
            String channelName = (String) paramEnum.nextElement();
            if (channelName.startsWith("chl")) {
                // Channel to delete...
                channelName = channelName.substring(3);
                Channel channel = channelManager.channelByName(channelName);
                if (channel != null) {
                    channelManager.deleteChannel(channel);
                }
            }
        }
    }

    private void channelRepoll(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Enumeration paramEnum = request.getParameterNames();
        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        while (paramEnum.hasMoreElements()) {
            String channelName = (String) paramEnum.nextElement();
            if (channelName.startsWith("chl")) {
                // Channel to repoll...
                channelName = channelName.substring(3);
                Channel channel = channelManager.channelByName(channelName);
                if (channel != null) {
                    // Reset last polled
                    channel.setLastPolled(null);
                    channel.setStatus(Channel.STATUS_OK);
                }
            }
        }
    }

    private void cmdChannelAction(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        if (request.getParameter("delete") != null) {
            channelDelete(request, response);
        } else if (request.getParameter("repoll") != null) {
            channelRepoll(request, response);
        }

        // Redirect to main page after performing channel action - otherwise
        // user could hit refresh and resubmit the channel action
        response.sendRedirect("/");
        //		cmdShowCurrentChannels(request, response);
    }

    private void cmdShowCurrentChannels(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        DateFormat df =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CHANNELS);

        writeCheckboxSelector(writer, "checkAllChannels", "chl", "channels");

        writer.write(
            "<form name='channels' action='/?action=channelaction' method='POST'>");
        writer.write("<table class='tableborder' border='0'>");
        writer.write(
            "<tr><th colspan='5' class='tableHead'>Channels</td></th>");
        //		writer.write("<tr><th class='subHead'><input type='checkbox' name='change' onClick='checkAllChannels(this);'></th><th class='subHead'>Newsgroup Name</th><th class='subHead'>RSS URL</th><th class='subHead'>Last Polled</th></tr>");
        writer.write(
            "<tr><th class='subHead'><input type='checkbox' name='change' onClick='checkAllChannels(this);'></th><th class='subHead'>Newsgroup Name</th><th class='subHead'>&nbsp;</th><th class='subHead'>Feed URL</th><th class='subHead'>Last Polled</th></tr>");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        NNTPServer nntpServer =
            (NNTPServer) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_NNTP_SERVER);

        Iterator channelIter = channelManager.channels();
        String newsPrefix = getNewsURLPrefix(nntpServer);

        while (channelIter.hasNext()) {
            Channel channel = (Channel) channelIter.next();
            writer.write(
                "<tr><td class='row1'><input type='checkbox' name='chl"
                    + HTMLHelper.escapeString(channel.getName())
                    + "'></td>");

            // Truncate displayed URL...
            String url = channel.getUrl();
            if (url.length() > 32) {
                url = url.substring(0, 32) + "...";
            }

            String lastPolled;
            if (channel.getLastPolled() != null) {
                lastPolled = df.format(channel.getLastPolled());
            } else {
                lastPolled = "Awaiting poll";
            }

            String parser = (channel.isParseAtAllCost() ? "*" : "");

            switch (channel.getStatus()) {
                case Channel.STATUS_INVALID_CONTENT :
                case Channel.STATUS_NOT_FOUND :
                case Channel.STATUS_UNKNOWN_HOST :
                case Channel.STATUS_NO_ROUTE_TO_HOST :
                case Channel.STATUS_PROXY_AUTHENTICATION_REQUIRED :
                case Channel.STATUS_USER_AUTHENTICATION_REQUIRED :
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'>"
                            + parser
                            + "<a class='row' title='Channel configuration' href='/?action=show&name="
                            + URLEncoder.encode(channel.getName())
                            + "'><font color='#FFFFFF'>"
                            + channel.getName()
                            + "</font></a></td>");
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><a class='row' title='Read this channel in your default newsreader' href='"
                            + newsPrefix
                            + HTMLHelper.escapeString(channel.getName())
                            + "'><font color='#FFFFFF'>[Read]</font></a></td>");
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><font color='#FFFFFF'>"
                            + url
                            + "</font></td>");
                    writer.write(
                        "<td class='chlerror' bgcolor='#FF0000'><font color='#FFFFFF'>"
                            + lastPolled
                            + "</font></td></tr>");
                    break;
                case Channel.STATUS_SOCKET_EXCEPTION :
                case Channel.STATUS_CONNECTION_TIMEOUT :
                    writer.write(
                        "<td class='chlwarning' bgcolor='#FFFF00'>"
                            + parser
                            + "<a class='row' title='Channel configuration' href='/?action=show&name="
                            + URLEncoder.encode(channel.getName())
                            + "'><font color='#000000'>"
                            + channel.getName()
                            + "</font></a></td>");
                    writer.write(
                        "<td class='chlwarning' bgcolor='#FFFF00'><a class='row' title='Read this channel in your default newsreader' href='"
                            + newsPrefix
                            + HTMLHelper.escapeString(channel.getName())
                            + "'>[Read]</a></td>");
                    writer.write(
                        "<td class='chlwarning' bgcolor='#FFFF00'><font color='#000000'>"
                            + url
                            + "</font></td>");
                    writer.write(
                        "<td class='chlwarning' bgcolor='#FFFF00'><font color='#000000'>"
                            + lastPolled
                            + "</font></td></tr>");
                    break;
                default :
                    if (channel.isEnabled()) {
                        writer.write(
                            "<td class='row1'>"
                                + parser
                                + "<a class='row' title='Channel configuration' href='/?action=show&name="
                                + URLEncoder.encode(channel.getName())
                                + "'>"
                                + channel.getName()
                                + "</a></td>");
                        writer.write(
                            "<td class='row1'><a class='row' title='Read this channel in your default newsreader' href='"
                                + newsPrefix
                                + HTMLHelper.escapeString(channel.getName())
                                + "'>[Read]</a></td>");

                        writer.write("<td class='row1'>" + url + "</td>");
                        writer.write(
                            "<td class='row1'>" + lastPolled + "</td></tr>");
                    } else {
                        writer.write(
                            "<td class='chldisabled' bgcolor='#CCCCCC'>"
                                + parser
                                + "<a class='row' title='Channel configuration' href='/?action=show&name="
                                + URLEncoder.encode(channel.getName())
                                + "'>"
                                + channel.getName()
                                + "</a></td>");
                        writer.write(
                            "<td class='chldisabled'><a class='row' title='Read this channel in your default newsreader' href='"
                                + newsPrefix
                                + HTMLHelper.escapeString(channel.getName())
                                + "'>[Read]</a></td>");
                        writer.write(
                            "<td class='chldisabled' bgcolor='#CCCCCC'>"
                                + url
                                + "</td>");
                        writer.write(
                            "<td class='chldisabled' bgcolor='#CCCCCC'>"
                                + lastPolled
                                + "</td></tr>");
                    }
            }
        }

        writer.write("<tr><td class='row2' colspan='5'>");
        writer.write(
            "<input type='submit' onClick='return confirm(\"Are you sure you want to delete these channels?\");' name='delete' value='Delete Selected Channels'>&nbsp;&nbsp;"
                + "<input type='submit' onClick='return confirm(\"Are you sure you want to repoll these channels?\");' name='repoll' value='Repoll Selected Channels'>"
                + "</td></tr>");

        writer.write(
            "</table><font size='-1'>[* = Channel configured for Parse-At-All-Cost parser]</font><p>");
        writer.write("</form><p>");
        writeFooter(writer);
        writer.flush();

    }

    private void cmdShowCurrentCategories(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CATEGORIES);

        //		writeCheckboxSelector(writer, "checkAllChannels", "chl", "channels");
        writer.write("<table class='tableborder' border='0'>");

        writer.write(
            "<tr><th colspan='2' class='tableHead'>Categories</td></th>");
        writer.write(
            "<tr><th colspan='2' class='subHead'><a class='head2' href=\"?action=addcategoryform\">Add Category</a></th></tr>");
        //		writer.write("<tr><th class='subHead'><input type='checkbox' name='change' onClick='checkAllChannels(this);'></th><th class='subHead'>Newsgroup Name</th><th class='subHead'>RSS URL</th><th class='subHead'>Last Polled</th></tr>");
        writer.write(
            "<tr><th class='subHead'>&nbsp;Category Newsgroup Name&nbsp;</th><th class='subHead'>&nbsp;</th></tr>");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        NNTPServer nntpServer =
            (NNTPServer) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_NNTP_SERVER);

        Iterator categoryIter = channelManager.categories();
        String newsPrefix = getNewsURLPrefix(nntpServer);

        while (categoryIter.hasNext()) {
            Category category = (Category) categoryIter.next();
            writer.write("<tr>");
            writer.write(
                "<td class='row1'>"
                    + "<a class='row' title='Channel configuration' href='/?action=showcategory&name="
                    + URLEncoder.encode(category.getName())
                    + "'>"
                    + category.getName()
                    + "</a></td>");
            writer.write(
                "<td class='row1'><a class='row' title='Read this category in your default newsreader' href='"
                    + newsPrefix
                    + HTMLHelper.escapeString(category.getName())
                    + "'>[Read]</a></td></tr>");
        }

        writer.write("</table>");
        writeFooter(writer);
        writer.flush();

    }

    private String getNewsURLPrefix(NNTPServer nntpServer) {
        String newsPrefix;
        if (nntpServer.getListenerPort() == 119) {
            newsPrefix = "news://" + nntpServer.getHostName() + "/";
        } else {
            newsPrefix =
                "news://"
                    + nntpServer.getHostName()
                    + ":"
                    + nntpServer.getListenerPort()
                    + "/";
        }
        return newsPrefix;
    }

    private void cmdQuickEditChannels(
        HttpServletRequest request,
        HttpServletResponse response,
        boolean updated)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_QUICKEDIT);

        writeCheckboxSelector(writer, "checkAllEnabled", "enabled", "channels");
        writeCheckboxSelector(
            writer,
            "checkAllParse",
            "parseAtAllCost",
            "channels");
        //		writeCheckboxSelector(writer, "checkAllHistorical", "historical", "channels");

        if (updated) {
            writer.write("<b>Channels successfully updated!</b>");
        }

        writer.write(
            "<form name='channels' action='/?action=quickeditupdate' method='POST'>");
        writer.write("<table class='tableborder' border='0'>");
        writer.write(
            "<tr><th colspan='6' class='tableHead'>Channels</td></th>");

        writer.write(
            "<tr><th class='subHead'>Newsgroup Name</th>"
                + "<th class='subHead'><input type='checkbox' name='changeEnabled' onClick='checkAllEnabled(this);'>Enabled</th>"
                + "<th class='subHead'>Category</th>"
                + "<th class='subHead'>Polling Interval</th>"
                + "<th class='subHead'><input type='checkbox' name='changeParse' onClick='checkAllParse(this);'>Parse-at-all-costs</th>"
                + "<th class='subHead'>Expiration</th></tr>");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);
        Iterator channelIter = channelManager.channels();

        int chlCount = 0;
        while (channelIter.hasNext()) {
            Channel channel = (Channel) channelIter.next();
            writer.write("<tr>");

            writer.write(
                "<input type='hidden' name='chl"
                    + chlCount
                    + "' value='"
                    + HTMLHelper.escapeString(channel.getName())
                    + "'>");

            // Channel name
            writer.write("<td class='row1'>" + channel.getName() + "</td>");

            // Enabled
            writer.write(
                "<td class='row1'>"
                    + "<input name='enabled"
                    + chlCount
                    + "' type='checkbox' value='true' "
                    + (channel.isEnabled() ? "checked>" : ">")
                    + "</td>");

            // Category
            int categoryId = 0;
            if (channel.getCategory() != null) {
                categoryId = channel.getCategory().getId();
            }
            writer.write(
                "<td class='row1'><select name='categoryId" + chlCount + "'>");
            writeOption(writer, "[No Category]", 0, categoryId);
            Iterator categories = channelManager.categories();
            while (categories.hasNext()) {
                Category category = (Category) categories.next();
                writeOption(
                    writer,
                    category.getName(),
                    category.getId(),
                    categoryId);
            }
            writer.write("</select></td>");

            // Polling Interval
            writer.write(
                "<td class='row1'>"
                    + "<select name='pollingInterval"
                    + chlCount
                    + "'>");

            if (channel.getPollingIntervalSeconds()
                == Channel.DEFAULT_POLLING_INTERVAL) {
                writer.write(
                    "<option selected value='"
                        + Channel.DEFAULT_POLLING_INTERVAL
                        + "'>Default Interval\n");
            } else {
                writer.write(
                    "<option selected value='"
                        + channel.getPollingIntervalSeconds()
                        + "'>"
                        + channel.getPollingIntervalSeconds() / 60
                        + " minutes\n");
            }

            writer.write(
                "<option value='"
                    + Channel.DEFAULT_POLLING_INTERVAL
                    + "'>Default Polling\n");
            for (int interval = 10; interval <= 120; interval += 10) {
                writer.write(
                    "<option value='"
                        + (interval * 60)
                        + "'>"
                        + interval
                        + " minutes\n");
            }
            writer.write("</select></td>");

            // Parse-at-all-costs
            writer.write(
                "<td class='row1'><input name='parseAtAllCost"
                    + chlCount
                    + "' type='checkbox' value='true' "
                    + (channel.isParseAtAllCost() ? "checked>" : ">")
                    + "</td>");

            // Historical
            //			writer.write("<td class='row1'><input name='historical" + chlCount + "' type='checkbox' value='true' "
            //				+ (channel.isHistorical() ? "checked>" : ">")
            //				+ "</td>");

            // @TODO Expiration
            writer.write(
                "<td class='row1'><select name='expiration" + chlCount + "'>");

            long expiration = channel.getExpiration();

            writeOption(
                writer,
                "Keep all items",
                Channel.EXPIRATION_KEEP,
                expiration);
            writeOption(writer, "Keep only current items", 0, expiration);
            writeOption(
                writer,
                "Keep items for 1 day",
                (1000 * 60 * 60 * 24 * 1),
                expiration);
            writeOption(
                writer,
                "Keep items for 2 days",
                (1000 * 60 * 60 * 24 * 2),
                expiration);
            writeOption(
                writer,
                "Keep items for 4 days",
                (1000 * 60 * 60 * 24 * 4),
                expiration);
            writeOption(
                writer,
                "Keep items for 1 week",
                (1000 * 60 * 60 * 24 * 7),
                expiration);
            writeOption(
                writer,
                "Keep items for 2 weeks",
                (1000 * 60 * 60 * 24 * 14),
                expiration);
            writeOption(
                writer,
                "Keep items for 4 weeks",
                (1000 * 60 * 60 * 24 * 28),
                expiration);

			writeOption(
				writer,
				"Keep items for 6 months",
				((1000 * 60 * 60 * 24) * 180),
				expiration);

			writeOption(
				writer,
				"Keep items for 1 year",
				((1000 * 60 * 60 * 24) * 365),
				expiration);

            writer.write("</select></td>");

            writer.write("</tr>");

            chlCount++;
        }

        writer.write("<tr><td class='row2' colspan='6'>");
        writer.write(
            "<input type='submit' onClick='return confirm(\"Are you sure you want to update the configuration?\");' name='update' value='Update Channels'>"
                + "</td></tr>");

        writer.write("</table><p>");
        writer.write("</form><p>");
        writeFooter(writer);
        writer.flush();
    }

    private void cmdQuickEditChannelsUpdate(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        int chlCount = 0;

        String channelName;
        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        while ((channelName = request.getParameter("chl" + chlCount))
            != null) {
            Channel channel = channelManager.channelByName(channelName);
            if (channel != null) {
                boolean enabled = isChecked(request, "enabled" + chlCount);
                // @TODO Expiration
                //				boolean historical = isChecked(request, "historical" + chlCount);
                long expiration =
                    Long.parseLong(
                        request.getParameter("expiration" + chlCount));
                long pollingInterval =
                    Long.parseLong(
                        request.getParameter("pollingInterval" + chlCount));
                boolean parseAtAllCost =
                    isChecked(request, "parseAtAllCost" + chlCount);
                int categoryId =
                    Integer.parseInt(
                        request.getParameter("categoryId" + chlCount));

                boolean channelUpdated = true;

                if (channel.isEnabled() != enabled
                    || channel.getExpiration() != expiration
                    || channel.getPollingIntervalSeconds() != pollingInterval
                    || channel.isParseAtAllCost() != parseAtAllCost) {
                    // Something changed, so update the channel
                    channel.setEnabled(enabled);
                    //					channel.setHistorical(historical);
                    channel.setExpiration(expiration);
                    channel.setPollingIntervalSeconds(pollingInterval);
                    channel.setParseAtAllCost(parseAtAllCost);

                    channelUpdated = true;
                }

                boolean categoryChanged =
                    ((!(categoryId == 0 && channel.getCategory() == null))
                        && ((categoryId != 0 && channel.getCategory() == null)
                            || (categoryId != channel.getCategory().getId())));

                if (categoryChanged) {
                    Category category = channel.getCategory();
                    if (category != null) {
                        category.removeChannel(channel);
                    }
                    if (categoryId != 0) {
                        category = channelManager.categoryById(categoryId);
                        category.addChannel(channel);
                    } else {
                        category = null;
                    }
                    channel.setCategory(category);
                    channelUpdated = true;
                }

                if (channelUpdated) {
                    channel.save();
                }
            }

            chlCount++;
        }

        cmdQuickEditChannels(request, response, true);
    }

    private void writeCheckboxSelector(
        Writer writer,
        String functionName,
        String checkPrefix,
        String formName)
        throws IOException {
        writer.write("\n<SCRIPT language='JavaScript'><!--\n");

        writer.write("function " + functionName + "(checkBox)\n");
        writer.write("{\n");
        writer.write("  var form = document." + formName + ";\n");
        writer.write(
            "  for(var itemCount = 0; itemCount<form.elements.length; itemCount++) {\n");
        writer.write("    var item = form.elements[itemCount];\n");
        writer.write(
            "    if(item.type == 'checkbox' && item.name.indexOf(\""
                + checkPrefix
                + "\") == 0) {\n");
        writer.write("      if(checkBox.checked) {\n");
        writer.write(" 		  item.checked = true;\n");
        writer.write("      } else {\n");
        writer.write("        item.checked = false;\n");
        writer.write("      }\n");
        writer.write("    }\n");
        writer.write("  }\n");
        writer.write("}\n");
        writer.write("--></SCRIPT>\n");
    }

    private void cmdAddChannelForm(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        // If add has been called from an external page, passing in the URL
        // Check for it
        String urlString = request.getParameter("URL");
        String name = request.getParameter("name");
        if (name != null) {
            name = name.trim();
        }

        if ((name == null || name.length() == 0) && urlString != null) {
            name = createChannelName(urlString);
        }

        //		String historicalStr = request.getParameter("historical");
        //		boolean historical = true;
        //		if(historicalStr != null) {
        //			historical = historicalStr.equalsIgnoreCase("true");
        //		}

        String expirationStr = request.getParameter("expiration");
        long expiration = Channel.EXPIRATION_KEEP;
        if (expirationStr != null) {
            expiration = Long.parseLong(expirationStr);
        }

        String validateStr = request.getParameter("validate");
        boolean validate = true;
        if (validateStr != null) {
            validate = validateStr.equalsIgnoreCase("true");
        }

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_ADD_CHANNEL);
        writer.write("<form action='/?action=add' method='post'>");
        writer.write("<table class='tableborder'>");

        writer.write("<tr><th colspan='2'>Add Channel</th></tr>");

        if (name != null) {
            writer.write(
                "<tr><td class='row1' align='right'>Newsgroup Name:</td><td class='row2'><input type='text' name='name' size='64' value='"
                    + HTMLHelper.escapeString(name)
                    + "'></td></tr>");
        } else {
            writer.write(
                "<tr><td class='row1' align='right'>Newsgroup Name:</td><td class='row2'><input type='text' name='name' size='64'></td></tr>");
        }

        if (urlString != null && urlString.length() > 0) {
            writer.write(
                "<tr><td class='row1' align='right'>Feed URL:</td><td class='row2' ><input type='text' name='url' size='64' value='"
                    + HTMLHelper.escapeString(urlString)
                    + "'><br><i>(nntp//rss supports both RSS and ATOM feeds)</i></td></tr>");
        } else {
            writer.write(
                "<tr><td class='row1' align='right'>Feed URL:</td><td class='row2' ><input type='text' name='url' size='64' value='http://'><br><i>(nntp//rss supports both RSS and ATOM feeds)</i></td></tr>");
        }

        writer.write(
            "<tr><td class='row1' align='right' valign='top'>Item Expiration</td><td class='row2'>");

        writer.write("<select name='expiration'>");
        writeOption(
            writer,
            "Keep all items",
            Channel.EXPIRATION_KEEP,
            expiration);
        writeOption(writer, "Keep only current items", 0, expiration);
        writeOption(
            writer,
            "Keep items for 1 day",
            (1000 * 60 * 60 * 24 * 1),
            expiration);
        writeOption(
            writer,
            "Keep items for 2 days",
            (1000 * 60 * 60 * 24 * 2),
            expiration);
        writeOption(
            writer,
            "Keep items for 4 days",
            (1000 * 60 * 60 * 24 * 4),
            expiration);
        writeOption(
            writer,
            "Keep items for 1 week",
            (1000 * 60 * 60 * 24 * 7),
            expiration);
        writeOption(
            writer,
            "Keep items for 2 weeks",
            (1000 * 60 * 60 * 24 * 14),
            expiration);
        writeOption(
            writer,
            "Keep items for 4 weeks",
            (1000 * 60 * 60 * 24 * 28),
            expiration);
		writeOption(
			writer,
			"Keep items for 6 months",
			((1000 * 60 * 60 * 24) * 180),
			expiration);
		writeOption(
			writer,
			"Keep items for 1 year",
			((1000 * 60 * 60 * 24) * 365),
			expiration);

        writer.write("</select></td></tr>");

        writer.write(
            "<tr><td class='row1' align='right' valign='top'>Validate</td><td class='row2'><input type='checkbox' value='true' name='validate' "
                + (validate ? "checked" : "")
                + ">"
                + "<br><i>(Checked = Ensure URL points to a valid RSS or ATOM document)</i></td></tr>");

        writer.write("<tr><td class='row1' align='right'>Category</td>");
        writer.write("<td class='row2'><select name='categoryId'>");

        writeOption(writer, "[No Category]", 0, 0);
        Iterator categories = channelManager.categories();
        while (categories.hasNext()) {
            Category category = (Category) categories.next();
            writeOption(writer, category.getName(), category.getId(), 0);
        }
        writer.write("</select></td></tr>");

        writer.write(
            "<tr><td class='row2' align='center' colspan='2'><input type='submit' value='Add'> <input type='reset'></td></tr></table>");
        writer.write("</form>");

        writeFooter(writer);
        writer.flush();

    }

    private void cmdAddCategoryForm(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CATEGORIES);
        writer.write("<form action='/?action=addcategory' method='post'>");
        writer.write("<table class='tableborder'>");
        writer.write("<tr><th colspan='2'>Add Category</th></tr>");
        writer.write(
            "<tr><td class='row1' align='right'>Category Newsgroup Name:</td><td class='row2'><input type='text' name='name' size='64'></td></tr>");
        writer.write(
            "<tr><td class='row2' align='center' colspan='2'><input type='submit' value='Add'> <input type='reset'></td></tr></table>");
        writer.write("</form>");

        writeFooter(writer);
        writer.flush();
    }

    private void writeErrors(Writer writer, List errors) throws IOException {
        for (int errorCount = 0; errorCount < errors.size(); errorCount++) {
            writer.write(errors.get(errorCount) + "<br>");
        }
    }

    private void cmdAddChannel(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        NNTPServer nntpServer =
            (NNTPServer) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_NNTP_SERVER);

        String name = request.getParameter("name").trim();
        String urlString = request.getParameter("url").trim();
        //		boolean historical = isChecked(request, "historical");
        boolean validate = isChecked(request, "validate");
        long expiration = Long.parseLong(request.getParameter("expiration"));
        int categoryId = Integer.parseInt(request.getParameter("categoryId"));

        List errors = new ArrayList();
        if (name.length() == 0) {
            errors.add("Name cannot be empty");
        } else if (name.indexOf(' ') > -1) {
            errors.add("Name cannot contain spaces");
        } else if (
            channelManager.channelByName(name) != null
                || channelManager.categoryByName(name) != null) {
            errors.add("Name is already is use");
        }

        if (urlString.length() == 0) {
            errors.add("URL cannot be empty");
        } else if (
            urlString.equals("http://") || urlString.equals("https://")) {
            errors.add("You must specify a URL");
        } else if (
            !urlString.startsWith("http://")
                && !urlString.startsWith("https://")) {
            errors.add("Only URLs starting http:// or https:// are supported");
        }

        Channel newChannel = null;
        if (errors.size() == 0) {
            try {
                newChannel = new Channel(name, urlString);
                //				newChannel.setHistorical(historical);
                newChannel.setExpiration(expiration);
                if (validate && !newChannel.isValid()) {
                    errors.add(
                        "URL does not point to valid RSS or ATOM document");
                    errors.add(
                        "<a target='validate' href='http://feedvalidator.org/check?url="
                            + urlString
                            + "'>Check the URL with the RSS and ATOM Validator @ archive.org</a>");
                    newChannel = null;
                }
            } catch (HttpUserException hue) {
                if (hue.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                    errors.add(
                        "This feed requires user name and password authentication.");
                    errors.add(
                        "Please specify User Name and Password in the URL, e.g.");
                    errors.add("http://username:password@www.myhost.com/");
                } else if (
                    hue.getStatus()
                        == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    errors.add(
                        "You are using a proxy server that requires authentication.");
                    errors.add(
                        "Please enter your User Name and Password in the <a href='?action=showconfig'>System Configuration.</a>");
                }
            } catch (MalformedURLException me) {
                errors.add(
                    "URL is malformed (" + me.getLocalizedMessage() + ")");
            }
        }

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_ADD_CHANNEL);

        if (errors.size() > 0) {
            writer.write("<b>There were errors adding your channel:</b><p>");
            writeErrors(writer, errors);
            writer.write("<p>");
            writer.write("<form action='/?action=add' method='post'>");
            writer.write("<table class='tableborder'>");

            writer.write("<tr><th colspan='2'>Add Channel</th></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Newsgroup Name:</td><td class='row2'><input type='text' name='name' size='64' value='"
                    + HTMLHelper.escapeString(name)
                    + "'></td></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Feed URL:</td><td class='row2'><input type='text' name='url' size='64' value='"
                    + HTMLHelper.escapeString(urlString)
                    + "'><br><i>(nntp//rss supports both RSS and ATOM feeds)</i></td></tr>");

            //			writer.write("<tr><td class='row1' align='right' valign='top'>Historical</td><td class='row2'><input type='checkbox' value='true' name='historical' "
            //				+ (historical ? "checked" : "")
            //				+ ">"
            //				+ "<br><i>(Checked = Keep items removed from the original RSS document)</i></td></tr>");

            writer.write(
                "<tr><td class='row1' align='right' valign='top'>Item Expiration</td><td class='row2'>");

            writer.write("<select name='expiration'>");
            writeOption(
                writer,
                "Keep all items",
                Channel.EXPIRATION_KEEP,
                expiration);
            writeOption(writer, "Keep only current items", 0, expiration);
            writeOption(
                writer,
                "Keep items for 1 day",
                (1000 * 60 * 60 * 24 * 1),
                expiration);
            writeOption(
                writer,
                "Keep items for 2 days",
                (1000 * 60 * 60 * 24 * 2),
                expiration);
            writeOption(
                writer,
                "Keep items for 4 days",
                (1000 * 60 * 60 * 24 * 4),
                expiration);
            writeOption(
                writer,
                "Keep items for 1 week",
                (1000 * 60 * 60 * 24 * 7),
                expiration);
            writeOption(
                writer,
                "Keep items for 2 weeks",
                (1000 * 60 * 60 * 24 * 14),
                expiration);
            writeOption(
                writer,
                "Keep items for 4 weeks",
                (1000 * 60 * 60 * 24 * 28),
                expiration);

            writer.write("</select></td></tr>");

            writer.write(
                "<tr><td class='row1' align='right' valign='top'>Validate</td><td class='row2'><input type='checkbox'  value='true' name='validate' "
                    + (validate ? "checked" : "")
                    + ">"
                    + "<br><i>(Checked = Ensure URL points to a valid RSS document)</i></td></tr>");

            writer.write("<tr><td class='row1' align='right'>Category</td>");
            writer.write("<td class='row2'><select name='categoryId'>");

            writeOption(writer, "[No Category]", 0, categoryId);
            Iterator categories = channelManager.categories();
            while (categories.hasNext()) {
                Category category = (Category) categories.next();
                writeOption(
                    writer,
                    category.getName(),
                    category.getId(),
                    categoryId);
            }
            writer.write("</select></td></tr>");

            writer.write(
                "<tr><td class='row2' align='center' colspan='2'><input type='submit' value='Add'> <input type='reset'></td></tr></table>");
            writer.write("</form>");
        } else {
            channelManager.addChannel(newChannel);
            if (categoryId != 0) {
                Category category = channelManager.categoryById(categoryId);
                category.addChannel(newChannel);
                newChannel.setCategory(category);
                newChannel.save();
            }

            writer.write(
                "Channel " + newChannel.getName() + " successfully added.<p>");

            writer.write(
                "<a href='"
                    + getNewsURLPrefix(nntpServer)
                    + newChannel.getName()
                    + "'>"
                    + "[View the channel in your newsreader]</a>");
        }

        writeFooter(writer);
        writer.flush();

    }

    private void cmdAddCategory(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        String name = request.getParameter("name").trim();

        List errors = new ArrayList();
        if (name.length() == 0) {
            errors.add("Name cannot be empty");
        } else if (name.indexOf(' ') > -1) {
            errors.add("Name cannot contain spaces");
        } else if (
            channelManager.channelByName(name) != null
                || channelManager.categoryByName(name) != null) {
            errors.add("Name is already is use");
        }

        Category newCategory = null;
        if (errors.size() == 0) {
            newCategory = new Category();
            newCategory.setName(name);
        }

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_VIEW_CATEGORIES);

        if (errors.size() > 0) {
            writer.write("<b>There were errors adding your category:</b><p>");
            writeErrors(writer, errors);
            writer.write("<p>");
            writer.write("<form action='/?action=addcategory' method='post'>");
            writer.write("<table class='tableborder'>");
            writer.write("<tr><th colspan='2'>Add Category</th></tr>");
            writer.write(
                "<tr><td class='row1' align='right'>Category Newsgroup Name:</td><td class='row2'><input type='text' name='name' size='64' value='"
                    + HTMLHelper.escapeString(name)
                    + "'></td></tr>");
            writer.write(
                "<tr><td class='row2' align='center' colspan='2'><input type='submit' value='Add'> <input type='reset'></td></tr></table>");
            writer.write("</form>");
        } else {
            channelManager.addCategory(newCategory);
            writer.write(
                "Category "
                    + newCategory.getName()
                    + " successfully added.<p>");
        }

        writeFooter(writer);
        writer.flush();
    }

    private void cmdExportChannelConfig(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        //		response.setContentType("text/xml");
        response.setContentType("application/octet-stream");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"nntprss-channels.xml\"");
        PrintWriter writer = new PrintWriter(response.getWriter());
        writer.println("<?xml version='1.0' encoding='UTF-8'?>");
        writer.println();
        writer.println("<!-- Generated on " + new Date().toString() + " -->");
        writer.println(
            "<nntprss-channels nntprss-version='"
                + XMLHelper.escapeString(AppConstants.VERSION)
                + "'>");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        Iterator channelIter = channelManager.channels();
        while (channelIter.hasNext()) {
            Channel channel = (Channel) channelIter.next();
            writer.print("  <channel name='");
            writer.print(XMLHelper.escapeString(channel.getName()));
            writer.print("' url='");
            writer.print(XMLHelper.escapeString(channel.getUrl()));
            writer.print("' expiration='");
            writer.print(channel.getExpiration());
            writer.print("' ");
            if (channel.getCategory() != null) {
                writer.print(" category='");
                writer.print(channel.getCategory().getName());
                writer.print("' ");
            }
            writer.println("/>");
        }

        writer.println("</nntprss-channels>");
    }

    private void cmdExportOpmlChannelConfig(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        //		response.setContentType("text/xml");
        response.setContentType("application/octet-stream; charset=UTF-8");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"nntprss-channels-opml.xml\"");
        PrintWriter writer = new PrintWriter(response.getWriter());
        writer.println("<?xml version='1.0' encoding='UTF-8'?>");
        writer.println();
        writer.println("<!-- Generated on " + new Date().toString() + " -->");
        writer.println(
            "<!-- nntp//rss v"
                + XMLHelper.escapeString(AppConstants.VERSION)
                + " - http://www.methodize.org/nntprss/ -->");

        writer.println("<opml version='1.1'>");
        writer.println(" <head>");
        writer.println("  <title>My nntp//rss Subscriptions</title>");

        MailDateFormat mailDateFormat = new MailDateFormat();
        String currentDateTime = mailDateFormat.format(new Date());
        writer.println("  <dateCreated>" + currentDateTime + "</dateCreated>");
        writer.println(
            "  <dateModified>" + currentDateTime + "</dateModified>");
        writer.println(" </head>");
        writer.println(" <body>");

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        Iterator channelIter = channelManager.channels();
        while (channelIter.hasNext()) {
            Channel channel = (Channel) channelIter.next();
            writer.print("  <outline text='");
            writer.print(channel.getTitle() != null ? XMLHelper.escapeString(channel.getTitle()) : "");
            writer.print("' description='");
            writer.print(channel.getDescription() != null ? XMLHelper.escapeString(channel.getDescription()) : "");
            writer.print("' htmlUrl='");
            writer.print(channel.getLink() != null ? XMLHelper.escapeString(channel.getLink()) : "");
            writer.print("' title='");
            writer.print(channel.getTitle() != null ? XMLHelper.escapeString(channel.getTitle()) : "");
            writer.print("' ");
            if (channel.getRssVersion() != null) {
                if (channel.getRssVersion().toUpperCase().startsWith("RSS")
                    || channel.getRssVersion().startsWith("RDF")) {
                    writer.print("type='rss' version='RSS' ");
                } else if (
                    channel.getRssVersion().toUpperCase().startsWith("ATOM")) {
                    writer.print("type='atom' version='ATOM' ");
                }
            }
            writer.print("xmlUrl='");
            writer.print(XMLHelper.escapeString(channel.getUrl()));
            writer.println("'/>");
        }

        writer.println(" </body>");
        writer.println("</opml>");
    }

    private void writeImportForm(Writer writer) throws IOException {
        writer.write(
            "<form action='?action=import' method='POST' enctype='multipart/form-data'>");
        writer.write("<table class='tableBorder'>");

        writer.write(
            "<tr><th colspan='2' class='tableHead'>Import Channel List</th></tr>");

        writer.write(
            "<tr><td class='row1' align='right'>Channel List File</td>");
        writer.write(
            "<td class='row2'><input type='file' name='file'></td></tr>");
        writer.write("<tr><td class='row1' align='right'>Format</td>");
        writer.write(
            "<td class='row2'><input type='radio' name='type' value='nntp' checked>nntp//rss Channel List<br>"
                + "<input type='radio' name='type' value='opml'>OPML (mySubscriptions.opml)</td></tr>");

        writer.write(
            "<tr><td class='row2' align='center' colspan='2'><input type='submit' value='Import'></td></tr>");
        writer.write("</table>");
        writer.write("</form>");
    }

    private void cmdImportChannelConfigForm(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_CONFIG);

        writeImportForm(writer);

        writeFooter(writer);
        writer.flush();

    }

    private void cmdImportChannelConfig(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        MultiPartRequest mpRequest = new MultiPartRequest(request);

        if (mpRequest.getString("type").equalsIgnoreCase("opml")) {
            cmdImportOpmlChannelConfigValidate(request, response, mpRequest);
        } else {
            cmdImportNntpRssChannelConfig(request, response, mpRequest);
        }
    }

    private void cmdImportNntpRssChannelConfig(
        HttpServletRequest request,
        HttpServletResponse response,
        MultiPartRequest mpRequest)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_CONFIG);

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        writer.write("<b>Import status</b><p>");

        List errors = new ArrayList();
        int channelsAdded = 0;

        // Parse XML
        try {
            DocumentBuilder db = AppConstants.newDocumentBuilder();
            Document doc = db.parse(mpRequest.getInputStream("file"));
            Element docElm = doc.getDocumentElement();
            NodeList channels = docElm.getElementsByTagName("channel");

            for (int channelCount = 0;
                channelCount < channels.getLength();
                channelCount++) {
                Element chanElm = (Element) channels.item(channelCount);

                String name = chanElm.getAttribute("name");
                String urlString = chanElm.getAttribute("url");
                boolean historical = false;
                Node historicalNode = chanElm.getAttributeNode("historical");
                if (historicalNode != null) {
                    historical =
                        historicalNode.getNodeValue().equalsIgnoreCase("true");
                }

                long expiration = Channel.EXPIRATION_KEEP;
                Node expirationNode = chanElm.getAttributeNode("expiration");
                if (expirationNode != null) {
                    expiration = Long.parseLong(expirationNode.getNodeValue());
                } else {
                    expiration = historical ? Channel.EXPIRATION_KEEP : 0;
                }

                String categoryName = chanElm.getAttribute("category");

                // Check name...
                List currentErrors = new ArrayList();
                Channel existingChannel = channelManager.channelByName(name);

                if (name.length() == 0) {
                    currentErrors.add(
                        "Channel with empty name - URL=" + urlString);
                } else if (name.indexOf(' ') > -1) {
                    currentErrors.add(
                        "Channel name cannot contain spaces - name=" + name);
                } else if (existingChannel != null) {
                    currentErrors.add(
                        "Channel name " + name + " is already is use");
                }

                if (urlString.length() == 0) {
                    currentErrors.add(
                        "URL cannot be empty, channel name=" + name);
                } else if (
                    urlString.equals("http://")
                        || urlString.equals("https://")) {
                    currentErrors.add(
                        "You must specify a URL, channel name=" + name);
                } else if (
                    !urlString.startsWith("http://")
                        && !urlString.startsWith("https://")) {
                    currentErrors.add(
                        "Only URLs starting http:// or https:// are supported, channel name="
                            + name
                            + ", url="
                            + urlString);
                }

                if (existingChannel == null) {

                    Channel newChannel = null;
                    if (currentErrors.size() == 0) {
                        try {
                            newChannel = new Channel(name, urlString);
                            //							newChannel.setHistorical(historical);
                            newChannel.setExpiration(expiration);
                            channelManager.addChannel(newChannel);
                            channelsAdded++;
                        } catch (MalformedURLException me) {
                            errors.add(
                                "Channel "
                                    + name
                                    + " - URL ("
                                    + urlString
                                    + ") is malformed");
                        }
                    }

                    if (categoryName.length() > 0) {
                        //Handle category...
                        Category category =
                            channelManager.categoryByName(categoryName);
                        if (category == null) {
                            // Need to create category...
                            category = new Category();
                            category.setName(categoryName);
                            channelManager.addCategory(category);
                        }
                        category.addChannel(newChannel);
                        newChannel.setCategory(category);
                        newChannel.save();
                    }

                    // Removed channel validation... channels will be validated
                    // on next iteration of channel poller - will be highlighted
                    // in channel list if invalid
                    // Validate channel...
                    //					if(Channel.isValid(new URL(urlString))) {
                    //// Add channel...
                    //						Channel newChannel = null;
                    //						if(currentErrors.size() == 0) {
                    //							try {
                    //								newChannel = new Channel(name, urlString);
                    //								newChannel.setHistorical(historical);
                    //								channelManager.addChannel(newChannel);
                    //								channelsAdded++;
                    //							} catch(MalformedURLException me) {
                    //								errors.add("Channel " + name + " - URL (" 
                    //									+ urlString + ") is malformed");
                    //							}
                    //						}				
                    //						
                    //					} else {
                    //// URL points to invalid document
                    //						errors.add("Channel " + name + "'s URL (" + urlString + ") "
                    //							+ "points to an invalid document");
                    //					}
                }

                errors.addAll(currentErrors);

            }
        } catch (SAXException se) {
            errors.add(
                "There was an error parsing your channel file:<br>"
                    + se.getMessage());
        } catch (ParserConfigurationException pce) {
            errors.add(
                "There was a problem reading your channelf file:<br>"
                    + pce.getMessage());
        }

        // Display any errors encountered during parsing...
        if (errors.size() > 0) {
            writer.write("Problems were encountered while adding channels.<p>");
            writeErrors(writer, errors);

            if (channelsAdded > 0) {
                writer.write(
                    "<p>"
                        + channelsAdded
                        + " channel(s) were successfully imported.");
            }
        } else {
            if (channelsAdded > 0) {
                writer.write(
                    "<p>"
                        + channelsAdded
                        + " channel(s) were successfully imported.");
            } else {
                writer.write(
                    "The configuration file did not contain any channels!");
            }
        }

        writeFooter(writer);
        writer.flush();

    }

    private String fixChannelName(String channelName) {
        StringBuffer fixedName = new StringBuffer();

        for (int i = 0; i < channelName.length(); i++) {
            char c = channelName.charAt(i);
            if (Character.isWhitespace(c)) {
                fixedName.append("_");
            } else {
                fixedName.append(c);
            }
        }
        return fixedName.toString().trim();
    }

    private String createChannelName(String urlString) {
        String name = null;
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            Stack hostStack = new Stack();
            StringTokenizer hostTok = new StringTokenizer(host, ".");
            while (hostTok.hasMoreTokens()) {
                String token = hostTok.nextToken();
                hostStack.push(token);
            }

            StringBuffer channelName = new StringBuffer();
            while (hostStack.size() > 0) {
                channelName.append(hostStack.pop());
                if (hostStack.size() > 0) {
                    channelName.append('.');
                }
            }

            name = channelName.toString();

        } catch (MalformedURLException mue) {
            name = "Unnamed_Channel_" + System.currentTimeMillis();
        }

        return name;
    }

    private void cmdImportOpmlChannelConfigValidate(
        HttpServletRequest request,
        HttpServletResponse response,
        MultiPartRequest mpRequest)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_CONFIG);

        writeCheckboxSelector(writer, "checkAllImport", "import", "channels");

        writer.write("<b>mySubscriptions.opml validation</b><p>");

        List errors = new ArrayList();

        // Parse XML
        try {
            DocumentBuilder db = AppConstants.newDocumentBuilder();
            Document doc = db.parse(mpRequest.getInputStream("file"));
            Element docElm = doc.getDocumentElement();

            NodeList channels = docElm.getElementsByTagName("outline");
            if (channels.getLength() > 0) {

                writer.write(
                    "<form name='channels' action='?action=importopml' method='POST'>");
                writer.write(
                    "<table class='tableBorder'><tr><th>Import<br>"
                        + "<input type='checkbox' name='changeImport' onClick='checkAllImport(this);' checked>"
                        + "</th><th>Channel Name</th><th>Expiration"
                        + "</th><th>URL</th></tr>");

                int channelCount = 0;
                for (int chlLoopCounter = 0;
                    chlLoopCounter < channels.getLength();
                    chlLoopCounter++) {
                    Element chanElm = (Element) channels.item(chlLoopCounter);

                    String name = fixChannelName(chanElm.getAttribute("title"));
                    String urlString = chanElm.getAttribute("xmlUrl");
                    if (urlString == null || urlString.length() == 0) {
                        urlString = chanElm.getAttribute("xmlurl");
                    }

                    if (urlString == null || urlString.length() == 0) {
                        continue;
                    }

                    if (name.length() == 0) {
                        name = createChannelName(urlString);
                    }

                    String rowClass =
                        (((channelCount % 2) == 1) ? "row1" : "row2");

                    writer.write(
                        "<tr>"
                            + "<td class='"
                            + rowClass
                            + "' align='center'><input type='checkbox' name='import"
                            + channelCount
                            + "' checked></td>"
                            + "<td class='"
                            + rowClass
                            + "'><input type='value' size='50' name='name"
                            + channelCount
                            + "' value='"
                            + HTMLHelper.escapeString(name)
                            + "'></td>"
                            + "<td class='"
                            + rowClass
                            + "' align='center'>"
                            + "<select name='expiration"
                            + channelCount
                            + "'>");

                    long expiration = Channel.EXPIRATION_KEEP;

                    writeOption(
                        writer,
                        "Keep all items",
                        Channel.EXPIRATION_KEEP,
                        expiration);
                    writeOption(
                        writer,
                        "Keep only current items",
                        0,
                        expiration);
                    writeOption(
                        writer,
                        "Keep items for 1 day",
                        (1000 * 60 * 60 * 24 * 1),
                        expiration);
                    writeOption(
                        writer,
                        "Keep items for 2 days",
                        (1000 * 60 * 60 * 24 * 2),
                        expiration);
                    writeOption(
                        writer,
                        "Keep items for 4 days",
                        (1000 * 60 * 60 * 24 * 4),
                        expiration);
                    writeOption(
                        writer,
                        "Keep items for 1 week",
                        (1000 * 60 * 60 * 24 * 7),
                        expiration);
                    writeOption(
                        writer,
                        "Keep items for 2 weeks",
                        (1000 * 60 * 60 * 24 * 14),
                        expiration);
                    writeOption(
                        writer,
                        "Keep items for 4 weeks",
                        (1000 * 60 * 60 * 24 * 28),
                        expiration);

                    writer.write(
                        "</select></td>"
                            + "<td class='"
                            + rowClass
                            + "' ><input type='hidden' name='url"
                            + channelCount
                            + "' value='"
                            + HTMLHelper.escapeString(urlString)
                            + "'>"
                            + HTMLHelper.escapeString(urlString)
                            + "</td></tr>\n");

                    channelCount++;
                }

                writer.write(
                    "<tr><td align='center' class='row2' colspan='4'><input type='submit' value='Import Channels'></td></tr>");
                writer.write("</table></form>");
            } else {
                writer.write(
                    "Your mySubscriptions.opml file did not contain any channels, or was in an invalid format.<p>");
            }

        } catch (SAXException se) {
            errors.add(
                "There was an error parsing your channel file:<br>"
                    + se.getMessage());
        } catch (ParserConfigurationException pce) {
            errors.add(
                "There was a problem reading your channelf file:<br>"
                    + pce.getMessage());
        }

        // Display any errors encountered during parsing...
        if (errors.size() > 0) {
            writer.write("Problems were encountered while adding channels.<p>");
            writeErrors(writer, errors);
        }

        writeFooter(writer);
        writer.flush();

    }

    private void cmdImportOpmlChannelConfig(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_CONFIG);

        ChannelManager channelManager =
            (ChannelManager) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_RSS_MANAGER);

        writer.write("<b>Import status</b><p>");

        List errors = new ArrayList();
        int channelsAdded = 0;

        StringBuffer errorContent = new StringBuffer();

        int channelCount = 0;
        int newChannelCount = 0;
        String urlString = request.getParameter("url" + channelCount);

        while (urlString != null) {

            String addStr = request.getParameter("import" + channelCount);
            if (addStr != null) {

                String name = request.getParameter("name" + channelCount);
//                String historicalStr =
//                    request.getParameter("historical" + channelCount);
                long expiration =
                    Long.parseLong(
                        request.getParameter("expiration" + channelCount));
                //				boolean historical = false;
                //	// FIXME check
                //				if(historicalStr != null) {
                //					historical = true;
                //				}

                // Check name...
                List currentErrors = new ArrayList();
                Channel existingChannel = channelManager.channelByName(name);

                if (name.length() == 0) {
                    currentErrors.add(
                        "Channel with empty name - URL=" + urlString);
                } else if (name.indexOf(' ') > -1) {
                    currentErrors.add(
                        "Channel name cannot contain spaces - name=" + name);
                } else if (existingChannel != null) {
                    currentErrors.add(
                        "Channel name " + name + " is already is use");
                }

                if (urlString.length() == 0) {
                    currentErrors.add(
                        "URL cannot be empty, channel name=" + name);
                } else if (
                    urlString.equals("http://")
                        || urlString.equals("https://")) {
                    currentErrors.add(
                        "You must specify a URL, channel name=" + name);
                } else if (
                    !urlString.startsWith("http://")
                        && !urlString.startsWith("https://")) {
                    currentErrors.add(
                        "Only URLs starting http:// or https:// are supported, channel name="
                            + name
                            + ", url="
                            + urlString);
                }

                if (existingChannel == null) {

                    Channel newChannel = null;
                    if (currentErrors.size() == 0) {
                        try {
                            newChannel = new Channel(name, urlString);
                            //							newChannel.setHistorical(historical);
                            newChannel.setExpiration(expiration);
                            channelManager.addChannel(newChannel);
                            channelsAdded++;
                        } catch (MalformedURLException me) {
                            errors.add(
                                "Channel "
                                    + name
                                    + " - URL ("
                                    + urlString
                                    + ") is malformed");
                        }
                    }

                }

                if (currentErrors.size() > 0) {
                    errorContent.append(
                        "<tr>"
                            + "<td rowspan='2' align='center'><input type='checkbox' name='import"
                            + newChannelCount
                            + "' checked></td>"
                            + "<td><input type='value' size='50' name='name"
                            + newChannelCount
                            + "' value='"
                            + HTMLHelper.escapeString(name)
                            + "'></td>"
                            + "<td align='center'>"
                            + "<select name='expiration"
                            + newChannelCount
                            + "'>");

                    writeOption(
                        errorContent,
                        "Keep all items",
                        Channel.EXPIRATION_KEEP,
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep only current items",
                        0,
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep items for 1 day",
                        (1000 * 60 * 60 * 24 * 1),
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep items for 2 days",
                        (1000 * 60 * 60 * 24 * 2),
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep items for 4 days",
                        (1000 * 60 * 60 * 24 * 4),
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep items for 1 week",
                        (1000 * 60 * 60 * 24 * 7),
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep items for 2 weeks",
                        (1000 * 60 * 60 * 24 * 14),
                        expiration);
                    writeOption(
                        errorContent,
                        "Keep items for 4 weeks",
                        (1000 * 60 * 60 * 24 * 28),
                        expiration);

                    errorContent.append(
                        "</select></td>"
                            + "<td><input type='hidden' name='url"
                            + newChannelCount++
                            + "' value='"
                            + HTMLHelper.escapeString(urlString)
                            + "'>"
                            + HTMLHelper.escapeString(urlString)
                            + "</td></tr>\n");
                    errorContent.append(
                        "<tr><td colspan='3'>&nbsp;&nbsp;<font color='red'>");
                    for (Iterator i = currentErrors.iterator(); i.hasNext();) {
                        String error = (String) i.next();
                        errorContent.append(error);
                        errorContent.append("<br>\n");
                    }
                    errorContent.append("</font></td></tr>\n");
                }
            } // end import != null

            channelCount++;
            urlString = request.getParameter("url" + channelCount);

        }

        // Display any errors encountered during processing...
        if (errorContent.length() > 0) {
            if (channelsAdded > 0) {
                writer.write(
                    "<p>"
                        + channelsAdded
                        + " channel(s) were successfully imported.");
            }

            writeCheckboxSelector(
                writer,
                "checkAllImport",
                "import",
                "channels");

            writer.write("Problems were encountered while adding channels.<p>");
            writer.write(
                "<form name='channels' action='?action=importopml' method='POST'>");
            writer.write(
                "<table border='1'><tr><th>Import<br>"
                    + "<input type='checkbox' name='changeImport' onClick='checkAllImport(this);' checked>"
                    + "</th><th>Channel Name</th><th>Expiration"
                    + "</th><th>URL</th></tr>");
            writer.write(errorContent.toString());
            writer.write("</table>");
            writer.write(
                "<br><input type='submit' value='Import Channels'></form>");

        } else {
            if (channelsAdded > 0) {
                writer.write(
                    "<p>"
                        + channelsAdded
                        + " channel(s) were successfully imported.");
            } else {
                writer.write("No channels were selected for import!");
            }
        }

        writeFooter(writer);
        writer.flush();

    }

    private void cmdHelp(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        Writer writer = response.getWriter();
        writeHeader(writer, TAB_HELP);

        writer.write("<table class='tableborder' border='0' width='80%'>");
        writer.write("<tr><th class='tableHead'>Help</td></th>");

        writer.write("<tr><th class='subHead'>Latest News</th></tr>");
        writer.write(
            "<tr><td><br>The latest nntp//rss project news can always be found on the <a href='http://www.methodize.org/nntprss'>nntp//rss project page</a>.<br><br></td></tr>");

        writer.write(
            "<tr><th class='subHead'>Using the Administration Interface</th></tr>");
        writer.write(
            "<tr><td>"
                + "<br><b>Add Channel</b></p>"
                + "Channels can be added to nntp//rss through this screen.<p><ul>"
                + "<li>Newsgroup name - name of the NNTP newsgroup that this feed will appear as in your newsreader"
                + "<li>URL - URL for the feed"
                + "<li>Historical - keep items once they have been removed from the original feed"
                + "<li>Validate - check the URL to a validate the feed when adding to the list of channels.  You may want to disable this validation if the remote feed is temporarily unavailable, but you still wish to add it to the list."
                + "</ul>"
                + "<p><b>View Channels</b><p>"
                + "This screen provides an overview of configured channels, their status, and the date and time of the last poll.<p>"
                + "Channel Status<p>"
                + "<table border='0'>"
                + "<tr><td class='row1'><b>OK</b></td><td class='row2'>Last poll was successful.</td></tr>"
                + "<tr><td class='chldisabled'><b>Disabled</b></td><td class='row2'>Channel has been disabled.</td></tr>"
                + "<tr><td class='chlwarning'><b>Warning</b></td><td class='row2'>nntp//rss is was unable to contact the channel's web server on the last poll.  This usually indicates an temporary problem, generally resolved on the next poll.</td></tr>"
                + "<tr><td class='chlerror'><b>Error</b></td><td class='row2'>A significant error occured when polling.  This may either be that the feed no longer exists, or that it is badly formatted.  In the case of the latter, enabling <i>Parse-at-all-cost</i> within the channel's configuration may provide a resolution</td></tr>"
                + "</table><p>"
                + "Clicking on the name of a channel will display the channel's configuration screen.<p>"
                + "Clicking on the [Read] button will open the channel in your default newsreader.<p>"
                + "<b>Channel Configuration</b><p>"
                + "Within this screen you can:"
                + "<ul><li>Modify the URL for the channel,"
                + "<li>Enable or disable polling of the channel,"
                + "<li>Set a channel-specific polling interval, or use the system wide default (set in <i>System Configuration</i>),"
                + "<li>Enable the Parse-at-all-cost parser.  This experimental parser will parse RSS feeds that are not well-formed XML,"
                + "<li>Change the historical status of the channel.  If a channel is 'historical', items that are removed from the feed will be kept within nntp//rss's database,"
                + "<li>Enabled posting (See below)."
                + "</ul>"
                + "<p><b>Posting</b><p>"
                + "nntp//rss supports the <a href='http://plant.blogger.com/api/index.html'>Blogger</a>, <a href='http://www.xmlrpc.com/metaWeblogApi'>MetaWeblog</a> and <a href='http://www.livejournal.com/doc/server/ljp.csp.xml-rpc.protocol.html'>LiveJournal</a> APIs, allow you to publish directly to your blog from within your NNTP newsreader.  Just configure your blog's nntp//rss channel for posting, then use your newsreader's native posting capability to post to the group.  Your post will be immediately sent to your blog's host for publication.<p>"
                + "Posting is configured within the channel's configuration screen.  Enabling posting on a channel will display an additional set of configuration fields.  With these fields you provide the information required by your blog's publishing API.<p>"
                + "<b>Note: It is recommended that you enable <a href='#SecureNNTP'>Authenticated NNTP Access</a> if you enable posting.  This will ensure that only an authenticated newsreader will be able to post to the blog.</b>"
                + "<p><b>System Configuration</b><p>"
                + "System wide configuration is managed on this screen.<p>"
                + "<i>Channel Polling Interval</i> - this determines how often nntp//rss will check feeds for updates.  This can be set as low as ten minutes, but sixty minutes should be sufficient for most feeds.<p>"
                + "<i>Content Type</i> - By default, nntp//rss serves messages to newsreaders in a combined (multipart/alternative) plain text and HTML MIME format.  This means that each message contains both a plain text and an HTML version of the item.  If you a using an older newsreader that does not support this mode, you may want to change the content type to Text (text/plain).<p>"
                + "<i>Proxy</i> - If you are running nntp//rss behind a proxy, you may need to configure your proxy settings here.  Proxy host name, port and an optional username and password can be specified.  Leave these fields blank if you are not behind a proxy.<p>"
                + "<i><a name='SecureNNTP'>Authenticated NNTP Access</a></i>, when enabled, will require NNTP newsreaders to authenticate themselves before being allowed to read or post.  This uses the same user information as used for securing the web interface.<p>"
                + "Import and Export of Channel Lists<p>"
                + "nntp//rss supports the import and export of channel lists.  For import, both nntp//rss and OPML (mySubscription.opml) format lists are supported.  Click on <i>Import Channel List</i>, select the file containing the subscription list, ensuring the correct type of file is checked, then click on the <i>Import</i> button.  For OPML files, nntp//rss will automatically generate a newsgroup name.  You will be able to modify these names, and also select the subset of channels to import, before the process is finalized.<p>");

        writer.write(
            "<tr><th class='subHead'>Securing access to nntp//rss</th></tr>");

        writer.write(
            "<tr><td><br>Access to the web admin interface can be password protected by creating a file named users.properties in the root directory of your nntp//rss installation.<p>"
                + "This file takes the form:<p><code>username:password</code><p>e.g. sample user.properties<p><code>jason:mypassword</code><p>"
                + "Multiple users can be configured by adding additional lines of usernames and passwords in the above format.<p>"
                + "You will need to stop and restart nntp//rss for these changes to take effect.  The next time you access the web interface, you will be prompted to enter your user name and password.<p>"
                + "To disable secure access, just delete or rename the users.properties file, and stop and restart nntp//rss.<p>"
                + "If you wish to secure access to the NNTP interface, just enable <i>Secure NNTP</i> within the System Configuration screen.<br><p></td></tr>");

        writer.write("<tr><th class='subHead'>Windows Users</th></tr>");

        writer.write(
            "<tr><td><br>Read the <a href='http://www.methodize.org/nntprss/docs/WINDOWS-SERVICE.TXT'>WINDOWS-SERVICE.TXT</a> file, located in the installation directory, for information on how to configure nntp//rss to run as a Windows service.<p>"
                + "nntp//rss has a System Tray icon when run interactively on Windows.  This provides quick access to the administration interface, as well as the ability to easily shut-down nntp//rss.  "
                + "Just right click on the 'N' icon, and choose your option.  You can also double click on the icon to bring up the administrative interface.<p>");

        writer.write("</td></tr>");

        writer.write(
            "<tr><th class='subHead'><span class='smalltext'>nntp//rss - Copyright &copy; 2002-2007 Jason Brome.  All Rights Reserved.</span></th></tr>");

        writer.write("</table>");

        writeFooter(writer);
        writer.flush();
    }

    private void processRequest(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("text/html");

        AdminServer adminServer =
            (AdminServer) getServletContext().getAttribute(
                AdminServer.SERVLET_CTX_ADMIN_SERVER);

        if (request.getServerPort() == adminServer.getPort()) {
            String action = request.getParameter("action");
            if (action == null || action.length() == 0) {
                cmdShowCurrentChannels(request, response);
            } else if (action.equals("add")) {
                cmdAddChannel(request, response);
            } else if (action.equals("addcategory")) {
                cmdAddCategory(request, response);
            } else if (action.equals("addform")) {
                cmdAddChannelForm(request, response);
            } else if (action.equals("addcategoryform")) {
                cmdAddCategoryForm(request, response);
            } else if (action.equals("showconfig")) {
                cmdShowConfig(request, response);
            } else if (action.equals("updateconfig")) {
                cmdUpdateConfig(request, response);
            } else if (action.equals("show")) {
                cmdShowChannel(request, response);
            } else if (action.equals("showcategory")) {
                cmdShowCategory(request, response);
            } else if (action.equals("update")) {
                cmdUpdateChannel(request, response);
            } else if (action.equals("categoryupdate")) {
                cmdUpdateCategory(request, response);
            } else if (action.equals("export")) {
                cmdExportChannelConfig(request, response);
            } else if (action.equals("exportopml")) {
                cmdExportOpmlChannelConfig(request, response);
            } else if (action.equals("import")) {
                cmdImportChannelConfig(request, response);
            } else if (action.equals("importform")) {
                cmdImportChannelConfigForm(request, response);
            } else if (action.equals("importopml")) {
                cmdImportOpmlChannelConfig(request, response);
            } else if (action.equals("channelaction")) {
                cmdChannelAction(request, response);
            } else if (action.equals("editchlrefresh")) {
                cmdEditChannelRefresh(request, response);
            } else if (action.equals("quickedit")) {
                cmdQuickEditChannels(request, response, false);
            } else if (action.equals("quickeditupdate")) {
                cmdQuickEditChannelsUpdate(request, response);
            } else if (action.equals("help")) {
                cmdHelp(request, response);
            } else if (action.equals("categories")) {
                cmdShowCurrentCategories(request, response);
            } else {
                cmdShowCurrentChannels(request, response);
            }
        } else {
            // Must be a subscription listener...
            List subListeners =
                (List) adminServer.getSubscriptionListeners().get(
                    new Integer(request.getServerPort()));
            String url = null;
            if (subListeners != null) {
                for (int i = 0; i < subListeners.size(); i++) {
                    SubscriptionListener subListener =
                        (SubscriptionListener) subListeners.get(i);
                    if (request.getPathInfo().equals(subListener.getPath())) {
                        url = request.getParameter(subListener.getParam());
                        break;
                    }
                }
            }

            if (url != null) {
                // Generate HTML redirect, as sendRedirect does not seem to function
                // @TODO: Investigate behavior...
                Writer out = response.getWriter();
                String destinationURL =
                    new URL(
                        "http",
                        request.getServerName(),
                        adminServer.getPort(),
                        "/?action=addform&URL=" + URLEncoder.encode(url))
                        .toString();
                out.write("<html><head>");
                out.write(
                    "<META HTTP-EQUIV='Refresh' Content='0; URL="
                        + destinationURL
                        + "'>");
                out.write(
                    "</head><body><a href='"
                        + destinationURL
                        + "'>Redirecting to nntp//rss admin...</a></html>");
                //				response.sendRedirect(new URL("http", request.getServerName(), adminServer.getPort(), "/?action=addform&URL=" + URLEncoder.encode(url)).toString()); 
            } else {
                response.sendRedirect(
                    new URL(
                        "http",
                        request.getServerName(),
                        adminServer.getPort(),
                        "/")
                        .toString());
            }
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
     */
    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        processRequest(request, response);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
     */
    protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        processRequest(request, response);
    }

}
