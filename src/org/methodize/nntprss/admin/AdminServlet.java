package org.methodize.nntprss.admin;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.methodize.nntprss.nntp.NNTPServer;
import org.methodize.nntprss.rss.Channel;
import org.methodize.nntprss.rss.ChannelManager;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.XMLHelper;
import org.mortbay.servlet.MultiPartRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: AdminServlet.java,v 1.3 2003/01/27 22:39:32 jasonbrome Exp $
 */
public class AdminServlet extends HttpServlet {

	private void writeHeader(Writer writer) throws IOException {
		writer.write("<html><head><title>nntp//rss admin</title></head>");
		writer.write("<body topmargin='0' leftmargin='0' marginheight='0' marginwidth='0' bgcolor='#ffffff' link='#0000FF' alink='#0000FF' vlink='#0000FF'>\n");

		writer.write("<table border='0' cellspacing='0' cellpadding='0' height='100%' width='100%'>");
		writer.write("<tr><td colspan='3' width='100%' bgcolor='#dddddd'><font size='+2'><b>&nbsp;nntp//rss Admin</b></font><hr width='100%'></td></tr>");
		writer.write("<tr height='100%'><td valign='top' bgcolor='#dddddd'><br><a href='/'>View Channels</a><p><a href='?action=addform'>Add Channel</a><p><a href='?action=showconfig'>System Configuration</a></td><td>&nbsp;&nbsp;&nbsp;</td><td width='90%' height='100%' valign='top'><br>");
	}

	private void writeFooter(Writer writer) throws IOException {
		writer.write("<br>&nbsp;<br><hr>");
		writer.write("<table width='100%'><tr><td>nntp//rss v" 
			+ AppConstants.VERSION
			+ "</td><td align='right'><a href='http://www.methodize.org/nntprss'>nntp//rss home page</a>&nbsp;&nbsp;</td></tr></table>");
		writer.write("</td></tr></table></body></html>");
	}
	
	private void writeConfig(Writer writer, ChannelManager channelManager,
		NNTPServer nntpServer) throws IOException {
		writer.write("<b>System Configuration</b><p>");
		writer.write("<form action='?action=updateconfig' method='POST'>");
		writer.write("<table border='1'>");

		writer.write("<tr><th>Channel Polling interval</th>");
		writer.write("<td>Every <select name='pollingInterval'>");
		writer.write("<option selected value='" + channelManager.getPollingIntervalSeconds() + "'>" + channelManager.getPollingIntervalSeconds() / 60 + "\n");
		for(int interval = 10; interval <= 120; interval+=10) {
			writer.write("<option value='" + (interval * 60) + "'>" + interval + "\n");
		}
		writer.write("</select> minutes </td></tr>");
		writer.write("<tr><th>Proxy Server Hostname</th><td><input type='text' name='proxyServer' value='"
			+ (channelManager.getProxyServer() == null? "" : channelManager.getProxyServer())
			+ "'><br><i>Host name of your proxy server, leave blank if no proxy</i></td></tr>");
		writer.write("<tr><th>Proxy Server Port</th><td><input type='text' name='proxyPort' value='"
			+ (channelManager.getProxyPort() == 0 ? "" : Integer.toString(channelManager.getProxyPort()))
			+ "'><br><i>Proxy server listener port, leave blank if no proxy</i></td></tr>");

		writer.write("<tr><th>Content Type</th>");
		writer.write("<td><select name='contentType'>");
		int contentType = nntpServer.getContentType();
		writer.write("<option value='" + AppConstants.CONTENT_TYPE_MIXED + "'"
			+ (contentType == AppConstants.CONTENT_TYPE_MIXED ? " selected" : "")
			+ ">Text & HTML (multipart/alternative)");		
		writer.write("<option value='" + AppConstants.CONTENT_TYPE_TEXT + "'"
			+ (contentType == AppConstants.CONTENT_TYPE_TEXT ? " selected" : "")
			+ ">Text (text/plain)");		
		writer.write("<option value='" + AppConstants.CONTENT_TYPE_HTML + "'"
			+ (contentType == AppConstants.CONTENT_TYPE_HTML ? " selected" : "")
			+ ">HTML (text/html)");		
		
		writer.write("</select></td></tr>");
				
		writer.write("<tr><td align='center' colspan='2'><input type='submit' name='update' value='Update'></td></tr>");
		writer.write("</table>");
		writer.write("</form>");
		writer.write("<p>");
		writer.write("<a href='/?action=export'>Export Channel List</a><p>");
		writer.write("<a href='/?action=importform'>Import Channel List</a>");
	}
	
	private void cmdShowConfig(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		Writer writer = response.getWriter();
		writeHeader(writer);

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
		writeHeader(writer);

		ChannelManager channelManager =
			(ChannelManager) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_RSS_MANAGER);
		NNTPServer nntpServer =
			(NNTPServer) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_NNTP_SERVER);

		nntpServer.setContentType(Integer.parseInt(request.getParameter("contentType")));
		nntpServer.saveConfiguration();


		channelManager.setPollingIntervalSeconds(Long.parseLong(request.getParameter("pollingInterval")));
		channelManager.setProxyServer(request.getParameter("proxyServer").trim());

		String proxyPortStr = request.getParameter("proxyPort");
		boolean validPort = true;
		
		if(proxyPortStr.length() == 0) {
			channelManager.setProxyPort(0);
		} else {
			try {
				channelManager.setProxyPort(Integer.parseInt(proxyPortStr));
			} catch(NumberFormatException nfe) {
				validPort = false;
			}
		}

		if(validPort == true) {
			channelManager.saveConfiguration();
			writer.write("System configuration successfully updated.<p>");
		} else {
			writer.write("<b>Proxy port must either be blank or a numeric value!</b><p>");
		}

		writeConfig(writer, channelManager, nntpServer);
		writeFooter(writer);
	}
	

	private void writeChannel(Writer writer, Channel channel) throws IOException {
		if(channel == null) {
			writer.write("<b>Channel " + channel.getName() + " not found!</b>");
		} else {
			writer.write("<b>Channel View</b><p>");
			writer.write("<form action='?action=update' method='POST'>");
			writer.write("<input type='hidden' name='name' value='" + channel.getName() + "'>");
			writer.write("<table border='1'>");
			writer.write("<tr><th>Name</th><td>" + channel.getName() + "</td></tr>");
			writer.write("<tr><th>URL</th><td><input type='text' name='URL' value='" + channel.getUrl() + "' size='64'></td></tr>");

			writer.write("<tr><th>Status</th>");

			switch(channel.getStatus()) {
				case Channel.STATUS_NOT_FOUND:
					writer.write("<td bgcolor='#FF0000'><font color='#FFFFFF'>RSS web server is returning File Not Found.</font>");
					break;
				case Channel.STATUS_INVALID_CONTENT:
					writer.write("<td bgcolor='#FF0000'><font color='#FFFFFF'>Last RSS document retrieved could not be parsed, check URL.</font>");
					break;
				case Channel.STATUS_CONNECTION_TIMEOUT:
					writer.write("<td bgcolor='#FFFF00'><font color='#000000'>Currently unable to contact RSS web server (Connection timeout).</font>");
					break;
				default:
					writer.write("<td>OK");
			}

			writer.write("</td></tr>");

			writer.write("<tr><th>Last Polled</th><td>" + channel.getLastPolled() + "</td></tr>");
			writer.write("<tr><th>Last Modified</th><td>");
			if(channel.getLastModified() == 0) {
				writer.write("Last modified not supplied by RSS Web Server");
			} else {
				writer.write( new Date(channel.getLastModified()).toString() );
			} 
			writer.write("</td></tr>");
			writer.write("<tr><th>Last ETag</th><td>");
			if(channel.getLastETag() == null) {
				writer.write("ETag not supplied by RSS Web Server");
			} else {
				writer.write(channel.getLastETag());
			}
			writer.write("</td></tr>");
			writer.write("<tr><th>RSS Version</th><td>");
			if(channel.getRssVersion() == null) { 
				writer.write("Unknown");
			} else {
				writer.write(channel.getRssVersion());
			}
			writer.write("</td></tr>");
			writer.write("<tr><th>Historical</th><td><select name='historical'>"
				+ "<option " + (channel.isHistorical() ? "selected" : "") + ">true"
				+ "<option " + (!channel.isHistorical() ? "selected" : "") + ">false"
				+ "</select></td></tr>");
			writer.write("<tr><td align='center' colspan='2'><input type='submit' name='update' value='Update'>&nbsp;&nbsp;&nbsp;<input type='submit' name='delete' onClick='return confirm(\"Are you sure you want to delete this channel?\");' value='Delete'></td></tr>");
			writer.write("</table>");
			writer.write("</form>");
		}
	}
	
	private void cmdShowChannel(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		Writer writer = response.getWriter();
		writeHeader(writer);

		String channelName = request.getParameter("name");

		ChannelManager channelManager =
			(ChannelManager) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_RSS_MANAGER);
		Channel channel = channelManager.channelByName(channelName);
		writeChannel(writer, channel);		

		writeFooter(writer);
	}
	
	private void cmdUpdateChannel(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		Writer writer = response.getWriter();
		writeHeader(writer);

		String channelName = request.getParameter("name");
		ChannelManager channelManager =
			(ChannelManager) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_RSS_MANAGER);
		Channel channel = channelManager.channelByName(channelName);

		if(request.getParameter("update") != null) {
// Update channel.
			String urlString = request.getParameter("URL");
			List errors = new ArrayList();
			if(urlString.length() == 0) {
				errors.add("URL cannot be empty");
			} else if(urlString.equals("http://")) {
				errors.add("You must specify a URL");
			} else if(!urlString.startsWith("http://")) {
				errors.add("Only URLs starting http:// are supported");
			} 
			
			if(errors.size() == 0) {
				try {
					URL url = new URL(urlString);
					if(!Channel.isValid(url)) {
						errors.add("URL does not point to valid RSS document");
						errors.add("<a target='validate' href='http://feeds.archive.org/validator/check?url=" + urlString + "'>Check the URL with the RSS Validator @ archive.org</a><br>");
					} else {
						channel.setUrl(url);
						channel.setHistorical(request.getParameter("historical").equalsIgnoreCase("true"));
// Reset status and last polled date - channel should
// get repolled on next iteration
						channel.setStatus(Channel.STATUS_OK);
						channel.setLastPolled(null);
					}
				}catch(MalformedURLException me) {
					errors.add("URL is malformed");
				}
			}

			if(errors.size() == 0) {
				writer.write("Channel <b>" + channel.getName() + "</b> successfully updated.<p>");
			} else {
				writer.write("<b>There were errors updating the channel:</b><p>");
				writeErrors(writer, errors);
			}


			writeChannel(writer, channel);		
		} else if(request.getParameter("delete") != null) {
			channelManager.deleteChannel(channel);
			writer.write("Channel <b>" + channel.getName() + "</b> successfully deleted.");
		}

		writeFooter(writer);
	}
	
	private void cmdChannelAction(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

// Delete channels currently the only multi-channel action supported
		Enumeration paramEnum = request.getParameterNames();
		ChannelManager channelManager =
			(ChannelManager) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_RSS_MANAGER);

		while(paramEnum.hasMoreElements()) {
			String channelName = (String)paramEnum.nextElement();
			if(channelName.startsWith("chl")) {
// Channel to delete...
				channelName = channelName.substring(3);
				Channel channel = channelManager.channelByName(channelName);
				if(channel != null) {
					channelManager.deleteChannel(channel);
				}
			}
		}
		
		cmdShowCurrentChannels(request, response);
	}	

	private void cmdShowCurrentChannels(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		Writer writer = response.getWriter();
		writeHeader(writer);
		
		writer.write("\n<SCRIPT language='JavaScript'><!--\n");

		writer.write("function checkAllChannels(checkBox)\n");
		writer.write("{\n");
		writer.write("  var form = document.channels;\n");
		writer.write("  for(var itemCount = 0; itemCount<form.elements.length; itemCount++) {\n");
		writer.write("    var item = form.elements[itemCount];\n");
		writer.write("    if(item.type == 'checkbox' && item.name.indexOf(\"chl\") == 0) {\n");
		writer.write("      if(checkBox.checked) {\n");
        writer.write(" 		  item.checked = true;\n");
		writer.write("      } else {\n");
		writer.write("        item.checked = false;\n");
		writer.write("      }\n");
		writer.write("    }\n");
		writer.write("  }\n");
		writer.write("}\n");
		writer.write("--></SCRIPT>\n");
		
		writer.write("<b>Channels</b><p>\n");
		writer.write("<form name='channels' action='/?action=channelaction' method='POST'>");
		writer.write("<table border='1'><tr><th><input type='checkbox' name='change' onClick='checkAllChannels(this);'></th><th>Newsgroup Name</th><th>RSS URL</th><th>Last Polled</th></tr>");

		ChannelManager channelManager =
			(ChannelManager) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_RSS_MANAGER);

		Iterator channelIter = channelManager.channels();
		while (channelIter.hasNext()) {
			Channel channel = (Channel) channelIter.next();
			writer.write("<tr><td><input type='checkbox' name='chl"
				+ channel.getName() + "'></td>");

// Truncate displayed URL...
			String url = channel.getUrl();
			if(url.length() > 32) {
				url = url.substring(0, 32) + "...";
			}

			String lastPolled;
			if(channel.getLastPolled() != null) {
// TODO use DateFormat
				lastPolled = channel.getLastPolled().toString();
			} else {
				lastPolled = "Yet to be polled";
			}

			switch(channel.getStatus()) {
				case Channel.STATUS_INVALID_CONTENT:
				case Channel.STATUS_NOT_FOUND:
					writer.write("<td bgcolor='#FF0000'><a href='/?action=show&name=" + channel.getName() + "'><font color='#FFFFFF'>" + channel.getName() + "</font></a></td>");
					writer.write("<td bgcolor='#FF0000'><font color='#FFFFFF'>" + url + "</font></td>");
					writer.write("<td bgcolor='#FF0000'><font color='#FFFFFF'>" + lastPolled + "</font></td></tr>");				
					break;
				case Channel.STATUS_CONNECTION_TIMEOUT:
					writer.write("<td bgcolor='#FFFF00'><a href='/?action=show&name=" + channel.getName() + "'><font color='#000000'>" + channel.getName() + "</font></a></td>");
					writer.write("<td bgcolor='#FFFF00'><font color='#000000'>" + url + "</font></td>");
					writer.write("<td bgcolor='#FFFF00'><font color='#000000'>" + lastPolled + "</font></td></tr>");				
					break;
				default:
					writer.write("<td><a href='/?action=show&name=" + channel.getName() + "'>" + channel.getName() + "</a></td>");
					writer.write("<td>" + url + "</td>");				
					writer.write("<td>" + lastPolled + "</td></tr>");				
			}
		}


		writer.write("</table><p>");
		writer.write("<input type='submit' onClick='return confirm(\"Are you sure you want to delete these channels?\");' name='delete' value='Delete Selected Channels'>");
		writer.write("</form><p>");
		writeFooter(writer);
		writer.flush();

	}

	private void cmdAddChannelForm(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		// If add has been called from an external page, passing in the URL
		// Check for it
		String urlString = request.getParameter("URL");
		String name = request.getParameter("name");
		String historicalStr = request.getParameter("historical");
		boolean historical = true;
		if(historicalStr != null) {
			historical = historicalStr.equalsIgnoreCase("true");
		}

		String validateStr = request.getParameter("validate");
		boolean validate = true;
		if(validateStr != null) {
			validate = validateStr.equalsIgnoreCase("true");
		}


		Writer writer = response.getWriter();
		writeHeader(writer);
		writer.write("<b>Add New RSS Channel</b><p>");
		writer.write("<form action='/?action=add' method='post'>");
		writer.write("<table>");

		if(name != null) {
			writer.write("<tr><td align='right'>Newsgroup Name:</td><td><input type='text' name='name' size='64' value='" + name + "'></td></tr>");
		} else {
			writer.write("<tr><td align='right'>Newsgroup Name:</td><td><input type='text' name='name' size='64'></td></tr>");
		}			

		if(urlString != null && urlString.length() > 0) {
			writer.write(
				"<tr><td align='right'>RSS URL:</td><td><input type='text' name='url' size='64' value='" + urlString + "'></td></tr>");
		} else {
			writer.write(
				"<tr><td align='right'>RSS URL:</td><td><input type='text' name='url' size='64' value='http://'></td></tr>");
		}

		writer.write("<tr><td align='right' valign='top'>Historical</td><td><select name='historical'>"
			+ "<option " + (historical ? "selected" : "") + ">true"
			+ "<option " + (!historical ? "selected" : "") + ">false"
			+ "</select><br><i>(True = Keep items removed from the original RSS document)</i></td></tr>");

		writer.write("<tr><td align='right' valign='top'>Validate</td><td><input type='checkbox' name='validate' "
			+ (validate ? "checked" : "")
			+ ">"
			+ "<br><i>(Checked = Ensure URL points to a valid RSS document)</i></td></tr>");

		writer.write(
			"<tr><td align='center' colspan='2'><input type='submit' value='Add'> <input type='reset'></td></tr></table>");
		writer.write("</form>");

		writeFooter(writer);
		writer.flush();

	}

	private void writeErrors(Writer writer, List errors) throws IOException {
		for(int errorCount = 0; errorCount < errors.size(); errorCount++) {
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

		String name = request.getParameter("name").trim();
		String urlString = request.getParameter("url").trim();
		boolean historical = request.getParameter("historical").equalsIgnoreCase("true");
		String validateStr = request.getParameter("validate");
		boolean validate = false;
		if(validateStr == null) {
			validate = false;
		} else if(validateStr.equalsIgnoreCase("on")) {
			validate = true;
		} 

		List errors = new ArrayList();
		if(name.length() == 0) {
			errors.add("Name cannot be empty");
		} else if(name.indexOf(' ') > -1) {
			errors.add("Name cannot contain spaces");
		} else if(channelManager.channelByName(name) != null) {
			errors.add("Name is already is use");
		}
		

		if(urlString.length() == 0) {
			errors.add("URL cannot be empty");
		} else if(urlString.equals("http://")) {
			errors.add("You must specify a URL");
		} else if(!urlString.startsWith("http://")) {
			errors.add("Only URLs starting http:// are supported");
		} 
		
		Channel newChannel = null;
		if(errors.size() == 0) {
			try {
				newChannel = new Channel(name, urlString);
				newChannel.setHistorical(historical);
				if(validate && !newChannel.isValid()) {
					errors.add("URL does not point to valid RSS document");
					errors.add("<a target='validate' href='http://feeds.archive.org/validator/check?url=" + urlString + "'>Check the URL with the RSS Validator @ archive.org</a>");
					newChannel = null;
				}
			}catch(MalformedURLException me) {
				errors.add("URL is malformed");
			}
		}

		Writer writer = response.getWriter();
		writeHeader(writer);

		if(errors.size() > 0) {
			writer.write("<b>There were errors adding your channel:</b><p>");
			writeErrors(writer, errors);
			writer.write("<p>");
			writer.write("<form action='/?action=add' method='post'>");
			writer.write("<table>");
			writer.write("<tr><td align='right'>Newsgroup Name:</td><td><input type='text' name='name' size='64' value='" + name + "'></td></tr>");
			writer.write(
				"<tr><td align='right'>RSS URL:</td><td><input type='text' name='url' size='64' value='" + urlString + "'></td></tr>");
			writer.write("<tr><td align='right'>Historical</td><td><select name='historical'>"
				+ "<option " + (historical ? "selected" : "") + ">true"
				+ "<option " + (historical ? "selected" : "") + ">false"
				+ "</select></td></tr>");

			writer.write("<tr><td align='right' valign='top'>Validate</td><td><input type='checkbox' name='validate' "
				+ (validate ? "checked" : "")
				+ ">"
				+ "<br><i>(Checked = Ensure URL points to a valid RSS document)</i></td></tr>");

			writer.write(
				"<tr><td align='center' colspan='2'><input type='submit' value='Add'> <input type='reset'></td></tr></table>");
			writer.write("</form>");
		} else {
			channelManager.addChannel(newChannel);
			
			writer.write("Channel " + newChannel.getName() + " successfully added.");
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
		response.setHeader("Content-Disposition",
			"attachment; filename=\"nntprss-channels.xml\"");
		PrintWriter writer = new PrintWriter(response.getWriter());
		writer.println("<?xml version='1.0' encoding='UTF-8'?>");
		writer.println();
		writer.println("<!-- Generated on "
			+ new Date().toString() + " -->");
		writer.println("<nntprss-channels nntprss-version='"
			+ XMLHelper.escapeString(AppConstants.VERSION) + "'>");

		ChannelManager channelManager =
			(ChannelManager) getServletContext().getAttribute(
				AdminServer.SERVLET_CTX_RSS_MANAGER);

		Iterator channelIter = channelManager.channels();				
		while(channelIter.hasNext()) {
			Channel channel = (Channel)channelIter.next();
			writer.print("  <channel name='");
			writer.print(XMLHelper.escapeString(channel.getName()));
			writer.print("' url='");
			writer.print(XMLHelper.escapeString(channel.getUrl()));
			writer.print("' historical='");
			writer.print(channel.isHistorical() ? "true" : "false");
			writer.println("'/>");
		}

		writer.println("</nntprss-channels>");		
	}

	private void writeImportForm(Writer writer) throws IOException {
		writer.write("<b>Import Channel List</b><p>");
		writer.write("<form action='?action=import' method='POST' enctype='multipart/form-data'>");
		writer.write("<table border='1'>");

		writer.write("<tr><th>Channel List File</th>");
		writer.write("<td><input type='file' name='file'></td></tr>");
		writer.write("<tr><td align='center' colspan='2'><input type='submit' value='Import'></td></tr>");
		writer.write("</table>");
		writer.write("</form>");
	}

	private void cmdImportChannelConfigForm(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		Writer writer = response.getWriter();
		writeHeader(writer);

		writeImportForm(writer);

		writeFooter(writer);
		writer.flush();

	}


	private void cmdImportChannelConfig(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {
			
		MultiPartRequest mpRequest = 
			new MultiPartRequest(request);
			
		Writer writer = response.getWriter();
		writeHeader(writer);

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


			for(int channelCount = 0; channelCount < channels.getLength(); channelCount++) {
				Element chanElm = (Element)channels.item(channelCount);

				String name = chanElm.getAttribute("name");
				String urlString = chanElm.getAttribute("url");
				boolean historical = false;
				String historicalStr = chanElm.getAttribute("historical");
				if(historicalStr != null) {
					historical = historicalStr.equalsIgnoreCase("true");
				}

// Check name...
				List currentErrors = new ArrayList();
				Channel existingChannel = channelManager.channelByName(name);

				if(name.length() == 0) {
					currentErrors.add("Channel with empty name - URL=" + urlString);
				} else if(name.indexOf(' ') > -1) {
					currentErrors.add("Channel name cannot contain spaces - name=" + name);
				} else if(existingChannel != null) {
					currentErrors.add("Channel name " + name + " is already is use");
				}


				if(urlString.length() == 0) {
					currentErrors.add("URL cannot be empty, channel name=" + name);
				} else if(urlString.equals("http://")) {
					currentErrors.add("You must specify a URL, channel name=" + name);
				} else if(!urlString.startsWith("http://")) {
					currentErrors.add("Only URLs starting http:// are supported, channel name=" + name
						+ ", url=" + urlString);
				} 

				if(existingChannel == null) {

					Channel newChannel = null;
					if(currentErrors.size() == 0) {
						try {
							newChannel = new Channel(name, urlString);
							newChannel.setHistorical(historical);
							channelManager.addChannel(newChannel);
							channelsAdded++;
						} catch(MalformedURLException me) {
							errors.add("Channel " + name + " - URL (" 
								+ urlString + ") is malformed");
						}
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
		} catch(SAXException se) {
			errors.add("There was an error parsing your channel file:<br>"
				+ se.getMessage());
		} catch(ParserConfigurationException pce) {
			errors.add("There was a problem reading your channelf file:<br>"
				+ pce.getMessage());
		}
		
// Display any errors encountered during parsing...
		if(errors.size() > 0) {
			writer.write("Problems were encountered while adding channels.<p>");
			writeErrors(writer, errors);
			
			if(channelsAdded > 0) {
				writer.write("<p>" + channelsAdded + " channel(s) were successfully imported.");
			}
		} else {
			if(channelsAdded > 0) {
				writer.write("<p>" + channelsAdded + " channel(s) were successfully imported.");
			} else {
				writer.write("The configuration file did not contain any channels!");
			}
		}

		writeFooter(writer);
		writer.flush();


	}


	private void processRequest(
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {

		response.setContentType("text/html");

		String action = request.getParameter("action");
		if (action == null || action.length() == 0) {
			cmdShowCurrentChannels(request, response);
		} else if (action.equals("add")) {
			cmdAddChannel(request, response);
		} else if (action.equals("addform")) {
			cmdAddChannelForm(request, response);
		} else if (action.equals("showconfig")) {
			cmdShowConfig(request, response);
		} else if (action.equals("updateconfig")) {
			cmdUpdateConfig(request, response);
		} else if (action.equals("show")) {
			cmdShowChannel(request, response);
		} else if (action.equals("update")) {
			cmdUpdateChannel(request, response);
		} else if (action.equals("export")) {
			cmdExportChannelConfig(request, response);
		} else if (action.equals("import")) {
			cmdImportChannelConfig(request, response);
		} else if (action.equals("importform")) {
			cmdImportChannelConfigForm(request, response);
		} else if (action.equals("channelaction")) {
			cmdChannelAction(request, response);
		} else {
			cmdShowCurrentChannels(request, response);
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
