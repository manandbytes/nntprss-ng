package org.methodize.nntprss.nntp;

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.methodize.nntprss.rss.Channel;
import org.methodize.nntprss.rss.ChannelManager;
import org.methodize.nntprss.rss.Item;
import org.methodize.nntprss.util.CRLFPrintWriter;
import org.methodize.nntprss.util.XMLHelper;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 0.1
 */
public class ClientHandler implements Runnable {

	private Logger log = Logger.getLogger(ClientHandler.class);

	private Socket client = null;
	private ChannelManager channelManager = ChannelManager.getChannelManager();
	private DateFormat df;
	private DateFormat nntpDateFormat;

	public ClientHandler(Socket client) {
		df = new SimpleDateFormat("EEE, dd MMM yy HH:mm:ss 'GMT'", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		nntpDateFormat = new SimpleDateFormat("yyMMdd HHmmss");
		this.client = client;
	}

	private String[] parseParameters(String commandString) {

		StringTokenizer strTok = new StringTokenizer(commandString);
		int tokens = strTok.countTokens();
		String[] parameters = new String[tokens];
		int paramPos = 0;
		while (strTok.hasMoreTokens()) {
			parameters[paramPos++] = strTok.nextToken();
		}
		return parameters;
	}

	private int[] getIntRange(String rangeStr) {
		int[] range = new int[2];
		if (rangeStr != null) {
			int rangePos = rangeStr.indexOf('-');
			if (rangePos > -1
				&& rangePos > 0
				&& rangePos < rangeStr.length()) {
				String startStr = rangeStr.substring(0, rangePos);
				String endStr = rangeStr.substring(rangePos + 1);
				range[0] = Integer.parseInt(startStr);
				range[1] = Integer.parseInt(endStr);
			}
		}
		return range;
	}

	/**
	 * 
	 * Very basic article writer
	 * 
	 * @todo Consider char encoding issues
	 * @todo Support additional links from article to nntp//rss services
	 * 
	 */

	private void writeArticle(PrintWriter pw,
		Channel channel, Item item) throws IOException {

		String boundary = "----=_Part_" + System.currentTimeMillis();

		pw.println("From: " + channel.getAuthor());
		pw.println("Date: " + df.format(item.getDate()));
		pw.println("Newsgroups: " + channel.getName());
		pw.println("Content-Type: multipart/alternative;");
		pw.println("      boundary=\"" + boundary + "\"");
		pw.println("Subject: " + item.getTitle());
		pw.println(
			"Message-ID: "
				+ "<"
				+ item.getSignature()
				+ "@"
				+ channel.getName()
				+ ">");
		pw.println("Path: nntprss");
		pw.println();

		pw.println("--" + boundary);	
		pw.println("Content-Type: text/plain");
		pw.println();
		pw.println();


		pw.println(XMLHelper.stripTags(item.getDescription()));
		pw.println();
	
		if(item.getLink() != null && item.getLink().length() > 0) {
			pw.print("Link: ");
			pw.println(item.getLink());
			pw.println();
		}

		pw.println();
		pw.println();
		pw.println("Served by nntp//rss (www.methodize.org/nntprss)");
		pw.println();

		pw.println("--" + boundary);	
		pw.println("Content-Type: text/html");
		pw.println();

		pw.println("<html>");
		pw.println("<body>");

		pw.println(item.getDescription());

		// Output link
		if (item.getLink() != null && item.getLink().length() > 0) {
			pw.print("<p>Link: <a href=\"");
			pw.print(item.getLink());
			pw.print("\">");
			pw.print(item.getLink());
			pw.println("</a>");
		}
		
		pw.println("<p>&nbsp;<p><hr><div align='right'><font size='-1'>Served by <a href=\"http://www.methodize.org/nntprss\">nntp//rss</a></font></div>");
		pw.println("</body></html>");

		pw.println("--" + boundary + "--");	

	}

	/**
	 * Main client request processing loop
	 * 
	 * Note - for those reviewing this code - this is 
	 * not meant to be a full NNTP-server implementation.
	 * It has been implemented to support the bare-minimum
	 * required to support interaction from the popular
	 * NNTP clients.  Functionality is restricted to group
	 * listing, and direct article retrieval (by article number
	 * or message id)
	 * 
	 */

	private void processRequestLoop(BufferedReader br, PrintWriter pw)
		throws IOException {

		boolean quitRequested = false;
		String currentGroupName = null;

		while (quitRequested == false) {
			String requestString = br.readLine();
			String command = null;
			String[] parameters = parseParameters(requestString);
			if (parameters.length > 0) {
				command = parameters[0];
			}

			if (command != null) {
				if (command.equalsIgnoreCase("ARTICLE")) {
					//					pw.println("430 no such article found");
					String artNumOrMsgId = parameters[1];
					Item item = null;
					Channel channel = null;
					
					if (artNumOrMsgId.indexOf('<') == -1) {
						// Article number
						//						item = channel.getItemByArticleNumber(Long.parseLong(artNumOrMsgId));
						channel =
							channelManager.channelByName(currentGroupName);

						item =
							channelManager.getChannelManagerDAO().loadItem(
								channel,
								Integer.parseInt(artNumOrMsgId));
					} else {
// Message IDs are in the form
// <itemsignature@channelname>
						int sepPos = artNumOrMsgId.indexOf('@');
						if(sepPos > -1) {
							String itemSignature = artNumOrMsgId.substring(1, sepPos);
							String artChannelName = artNumOrMsgId.substring(sepPos + 1, artNumOrMsgId.length()-1);

							channel =
								channelManager.channelByName(artChannelName);
							if(channel != null) {
								item =
									channelManager.getChannelManagerDAO().loadItem(
										channel, itemSignature);
							}

						} 
					}
					if (item == null) {
						pw.println("430 no such article found");
					} else {
						pw.println(
							"220 "
								+ item.getArticleNumber()
								+ " <"
								+ item.getSignature()
								+ "@"
								+ channel.getName()
								+ "> article retrieved - head and body follow");

						writeArticle(pw, channel, item);

						pw.println(".");
					}
				} else if (command.equalsIgnoreCase("BODY")) {
					pw.println("430 no such article found");
				} else if (command.equalsIgnoreCase("GROUP")) {
					currentGroupName = parameters[1];
					Channel channel =
						channelManager.channelByName(currentGroupName);
					if (channel != null) {
						pw.println(
							"211 "
								+ channel.getTotalArticles()
								+ " "
								+ channel.getFirstArticleNumber()
								+ " "
								+ channel.getLastArticleNumber()
								+ " "
								+ currentGroupName);
					} else {
						pw.println("411 no such news group");
					}
				} else if (command.equalsIgnoreCase("HEAD")) {
					pw.println("430 no such article found");
				} else if (command.equalsIgnoreCase("HELP")) {
					pw.println("100 help text follows");
					pw.println(".");
				} else if (command.equalsIgnoreCase("IHAVE")) {
					pw.println("435 article not wanted - do not send it");
				} else if (command.equalsIgnoreCase("LAST")) {
					pw.println("422 no previous article in this group");
				} else if (command.equalsIgnoreCase("LIST")) {
					pw.println("215 list of newsgroups follows");
					Iterator channelIter = channelManager.channels();
					while (channelIter.hasNext()) {
						Channel channel = (Channel) channelIter.next();
						pw.println(
							channel.getName()
								+ " "
								+ channel.getFirstArticleNumber()
								+ " "
								+ (channel.getLastArticleNumber() - 1)
								+ " n");
					}
					pw.println(".");
				} else if (command.equalsIgnoreCase("NEWGROUPS")) {
					if (parameters.length < 3) {
						pw.println("500 command not recognized");
					} else {
						if (parameters.length > 3
							&& parameters[4].equalsIgnoreCase("GMT")) {
							nntpDateFormat.setTimeZone(
								TimeZone.getTimeZone("GMT"));
						} else {
							nntpDateFormat.setTimeZone(TimeZone.getDefault());
						}

						Date startDate = null;
						String startDateStr =
							parameters[1] + " " + parameters[2];
						try {
							startDate = nntpDateFormat.parse(startDateStr);
						} catch (ParseException pe) {
							if (log.isDebugEnabled()) {
								log.debug(
									"Invalid date received in NEWGROUPS request - "
										+ startDateStr);
							}
						}

						if (startDate != null) {
							pw.println("231 list of new newsgroups follows");
							Iterator channelIter = channelManager.channels();
							while (channelIter.hasNext()) {
								Channel channel = (Channel) channelIter.next();
								// Only list channels created after the
								// start date provided by the nntp client
								if (channel.getCreated().after(startDate)) {
									pw.println(
										channel.getName()
											+ " "
											+ channel.getFirstArticleNumber()
											+ " "
											+ (channel.getLastArticleNumber()
												- 1)
											+ " n");
								}
							}
							pw.println(".");
						} else {
							pw.println("500 command not recognized");
						}
					}
				} else if (command.equalsIgnoreCase("NEWNEWS")) {
					pw.println(
						"230 list of new articles by message-id follows");
					pw.println(".");
				} else if (command.equalsIgnoreCase("NEXT")) {
					pw.println("421 no next article in this group");
				} else if (command.equalsIgnoreCase("POST")) {
					pw.println("440 posting not allowed");
				} else if (command.equalsIgnoreCase("QUIT")) {
					pw.println("205 closing connection - goodbye!");
					quitRequested = true;
				} else if (command.equalsIgnoreCase("SLAVE")) {
					pw.println("202 slave status noted");
				} else if (command.equalsIgnoreCase("STAT")) {
					pw.println("430 no such article found");
				} else if (command.equalsIgnoreCase("XHDR")) {
					pw.println("502 no permission");
				} else if (command.equalsIgnoreCase("XOVER")) {
					pw.println("224 Overview information follows");
					// Interpret parameters and restrict return
					Channel channel =
						channelManager.channelByName(currentGroupName);
					int[] range = getIntRange(parameters[1]);
					List items =
						channelManager.getChannelManagerDAO().loadItems(
							channel,
							range,
							false);

					Iterator itemIter = items.iterator();
					while (itemIter.hasNext()) {
						Item item = (Item) itemIter.next();
						try {
							pw
								.println(
									item.getArticleNumber()
									+ "\t"
									+ item.getTitle()
									+ "\t"
									+ channel.getAuthor()
									+ "\t"
									+ df.format(item.getDate())
									+ "\t"
									+ "<"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ ">"
									+ "\t" // no references
// FIXME calculate content size and line count
// This is currently a 'hack' - return an arbitrary line length
// of 10 lines.
							+"\t" + item.getDescription().length() + "\t10"
							);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
					pw.println(".");
				} else if (command.length() > 0) {
					// Unknown command
					pw.println("500 command not recognized");
				}
			} else {
				// Bad request received!
				//				pw.println("500 command not recognized");
			}
			pw.flush();
		}
	}

	/**
	 * Handles NNTP Client Conversation
	 */
	public void run() {
		try {
			BufferedReader br =
				new BufferedReader(
					new InputStreamReader(client.getInputStream()));

// Use CRLF PrintWriter wrapper class for output...
			PrintWriter pw =
				new CRLFPrintWriter(
					new OutputStreamWriter(client.getOutputStream()));

			// Send 201 connection header
			pw.println("201 nntp//rss news server ready - no posting allowed");
			pw.flush();

			processRequestLoop(br, pw);

			br.close();
			pw.close();
			client.close();

		} catch (IOException ie) {
		}
	}

}
