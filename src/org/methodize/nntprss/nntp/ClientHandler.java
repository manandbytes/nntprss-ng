package org.methodize.nntprss.nntp;

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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.ChannelManager;
import org.methodize.nntprss.feed.Item;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.feed.publish.BloggerPublisher;
import org.methodize.nntprss.feed.publish.LiveJournalPublisher;
import org.methodize.nntprss.feed.publish.MetaWeblogPublisher;
import org.methodize.nntprss.feed.publish.Publisher;
import org.methodize.nntprss.feed.publish.PublisherException;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.CRLFPrintWriter;
import org.methodize.nntprss.util.HTMLHelper;
import org.methodize.nntprss.util.RSSHelper;
import org.methodize.nntprss.util.XMLHelper;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ClientHandler.java,v 1.9 2003/09/28 20:22:50 jasonbrome Exp $
 */
public class ClientHandler implements Runnable {

	private static final int RETRIEVE_LIMIT = 1000;
	private Logger log = Logger.getLogger(ClientHandler.class);

	private Socket client = null;
	private ChannelManager channelManager = ChannelManager.getChannelManager();
	private DateFormat df;
	private DateFormat nntpDateFormat;
	private NNTPServer nntpServer;

	// Required
	private static final int NNTP_HEADER_UNKNOWN = -1;
	private static final int NNTP_HEADER_FROM = 1;
	private static final int NNTP_HEADER_DATE = 2;
	private static final int NNTP_HEADER_NEWSGROUP = 3;
	private static final int NNTP_HEADER_SUBJECT = 4;
	private static final int NNTP_HEADER_MESSAGE_ID = 5;
	private static final int NNTP_HEADER_PATH = 6;

	// Optional	
	private static final int NNTP_HEADER_FOLLOWUP_TO = 7;
	private static final int NNTP_HEADER_EXPIRES = 8;
	private static final int NNTP_HEADER_REPLY_TO = 9;
	private static final int NNTP_HEADER_SENDER = 10;
	private static final int NNTP_HEADER_REFERENCES = 11;
	private static final int NNTP_HEADER_CONTROL = 12;
	private static final int NNTP_HEADER_DISTRIBUTION = 13;
	private static final int NNTP_HEADER_KEYWORDS = 14;
	private static final int NNTP_HEADER_SUMMARY = 15;
	private static final int NNTP_HEADER_APPROVED = 16;
	private static final int NNTP_HEADER_LINES = 17;
	private static final int NNTP_HEADER_XREF = 18;
	private static final int NNTP_HEADER_ORGANIZATION = 19;

	private static final int NO_CURRENT_ARTICLE = -1;

	public ClientHandler(NNTPServer nntpServer, Socket client) {
		this.nntpServer = nntpServer;
		this.client = client;

		df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		nntpDateFormat = new SimpleDateFormat("yyMMdd HHmmss");
	}

	private int parseHeaderName(String headerName) {
		int header = NNTP_HEADER_UNKNOWN;
		if (headerName.equalsIgnoreCase("from")) {
			header = NNTP_HEADER_FROM;
		} else if (headerName.equalsIgnoreCase("date")) {
			header = NNTP_HEADER_DATE;
		} else if (headerName.equalsIgnoreCase("newsgroup")) {
			header = NNTP_HEADER_NEWSGROUP;
		} else if (headerName.equalsIgnoreCase("subject")) {
			header = NNTP_HEADER_SUBJECT;
		} else if (headerName.equalsIgnoreCase("message-id")) {
			header = NNTP_HEADER_MESSAGE_ID;
		} else if (headerName.equalsIgnoreCase("path")) {
			header = NNTP_HEADER_PATH;
		} else if (headerName.equalsIgnoreCase("followup-to")) {
			header = NNTP_HEADER_FOLLOWUP_TO;
		} else if (headerName.equalsIgnoreCase("expires")) {
			header = NNTP_HEADER_EXPIRES;
		} else if (headerName.equalsIgnoreCase("reply-to")) {
			header = NNTP_HEADER_REPLY_TO;
		} else if (headerName.equalsIgnoreCase("sender")) {
			header = NNTP_HEADER_SENDER;
		} else if (headerName.equalsIgnoreCase("references")) {
			header = NNTP_HEADER_REFERENCES;
		} else if (headerName.equalsIgnoreCase("control")) {
			header = NNTP_HEADER_CONTROL;
		} else if (headerName.equalsIgnoreCase("distribution")) {
			header = NNTP_HEADER_DISTRIBUTION;
		} else if (headerName.equalsIgnoreCase("keywords")) {
			header = NNTP_HEADER_KEYWORDS;
		} else if (headerName.equalsIgnoreCase("summary")) {
			header = NNTP_HEADER_SUMMARY;
		} else if (headerName.equalsIgnoreCase("approved")) {
			header = NNTP_HEADER_APPROVED;
		} else if (headerName.equalsIgnoreCase("lines")) {
			header = NNTP_HEADER_LINES;
		} else if (headerName.equalsIgnoreCase("xref")) {
			header = NNTP_HEADER_XREF;
		} else if (headerName.equalsIgnoreCase("organization")) {
			header = NNTP_HEADER_ORGANIZATION;
		}

		return header;
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
				&& rangePos < rangeStr.length() - 1) {
				String startStr = rangeStr.substring(0, rangePos);
				String endStr = rangeStr.substring(rangePos + 1);
				range[0] = Integer.parseInt(startStr);
				range[1] = Integer.parseInt(endStr);
			} else if (rangePos > 0 && rangeStr.length() > 0) {
				range[0] = Integer.parseInt(rangeStr.substring(0, rangePos));
				range[1] = AppConstants.OPEN_ENDED_RANGE;
			} else if (rangePos == 0) {
				range[0] = AppConstants.OPEN_ENDED_RANGE;
				range[1] = Integer.parseInt(rangeStr.substring(1));
			} else {
				range[0] = Integer.parseInt(rangeStr);
				range[1] = range[0];
			}
		}
		return range;
	}

	private String createMessageId(Item item) {
		StringBuffer messageId = new StringBuffer();
		messageId.append('<');
		messageId.append(item.getSignature());
		messageId.append('@');
		messageId.append(item.getChannel().getName());
		messageId.append('>');
		return messageId.toString();
	}

	/**
	 * 
	 * Very basic article writer
	 * 
	 * @todo Consider char encoding issues
	 * @todo Support additional links from article to nntp//rss services
	 * 
	 */

	private void writeArticle(
		PrintWriter pw,
		PrintWriter bodyPw,
		Channel channel,
		Item item)
		throws IOException {

		writeHead(pw, channel, item);
		pw.println();
		pw.flush();

		writeBody(bodyPw, channel, item);

	}

	private void writeHead(PrintWriter pw, Channel channel, Item item)
		throws IOException {

		String boundary = "----=_Part_" + item.getDate().getTime();

		pw.print("From: ");
		pw.println(processAuthor(channel, item));
		//		if (channel.getManagingEditor() == null) {
		//			pw.println(
		//				stripTabsLineBreaks(processAuthor(channel.getAuthor(), "")));
		//		} else {
		//			pw.println(
		//				processAuthor(channel.getAuthor(), RSSHelper.parseEmail(channel.getManagingEditor())));
		//		}

		pw.println("Newsgroups: " + channel.getName());
		pw.println("Date: " + df.format(item.getDate()));
		pw.println(
			"Subject: "
				+ MimeUtility.encodeText(
					processSubject(item.getTitle()),
					"UTF-8",
					"Q"));

		pw.println(
			"Message-ID: "
				+ "<"
				+ item.getSignature()
				+ "@"
				+ channel.getName()
				+ ">");
		pw.println("Path: nntprss");
		pw.println("MIME-Version: 1.0");

		switch (nntpServer.getContentType()) {
			case AppConstants.CONTENT_TYPE_TEXT :
				pw.println(
					"Content-Type: text/plain; charset=utf-8; format=flowed");
				break;
			case AppConstants.CONTENT_TYPE_HTML :
				pw.println("Content-Type: text/html; charset=utf-8");
				break;
			default :
				// Mixed		
				pw.println("Content-Type: multipart/alternative;");
				pw.println("      boundary=\"" + boundary + "\"");
		}

		pw.flush();
	}

	private void writeBody(PrintWriter pw, Channel channel, Item item)
		throws IOException {

		String boundary = null;

		if (nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED) {
			boundary = "----=_Part_" + item.getDate().getTime();
			pw.println("--" + boundary);
			pw.println(
				"Content-Type: text/plain; charset=utf-8; format=flowed");
			pw.println();
		}

		// Plain text content
		if (nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED
			|| nntpServer.getContentType() == AppConstants.CONTENT_TYPE_TEXT) {

			String description =
				HTMLHelper.unescapeString(
					XMLHelper.stripHtmlTags(
						item.getDescription(),
						nntpServer.isFootnoteUrls()));

			if (description.length() > 0) {
				pw.println(description);
				pw.println();
			}

			if (item.getComments() != null
				&& item.getComments().length() > 0) {
				pw.print("Comments: ");
				pw.println(item.getLink());
			}

			if (item.getGuid() != null
				&& item.isGuidIsPermaLink()
				&& item.getGuid().length() > 0) {
				pw.print("PermaLink: ");
				pw.println(item.getGuid());
			}

			if (item.getLink() != null && item.getLink().length() > 0) {
				// Do not display link if same as PermaLink
				if (!item.isGuidIsPermaLink()
					|| (item.getGuid() == null
						|| !item.getGuid().equals(item.getLink()))) {
					pw.print("Link: ");
					pw.println(item.getLink());
				}
			}

			pw.println();

			if (channel.getTitle() != null
				|| channel.getDescription() != null
				|| channel.getLink() != null) {

				// RFC compliant sig delimiter
				pw.println("-- ");

				StringBuffer header = new StringBuffer();

				if (channel.getTitle() != null) {
					header.append(channel.getTitle());
				}

				if (channel.getLink() != null) {
					if (header.length() > 0) {
						header.append(' ');
					}
					header.append(channel.getLink());
				}

				if (channel.getDescription() != null) {
					if (header.length() > 0) {
						header.append("\r\n");
					}
					header.append(channel.getDescription());
				}

				pw.println(header.toString());
			}

			pw.println();
			pw.println(
				"Served by nntp//rss v"
					+ AppConstants.VERSION
					+ " ( http://www.methodize.org/nntprss )");
			pw.println();
		}

		if (nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED) {
			pw.println("--" + boundary);
			pw.println("Content-Type: text/html; charset=utf-8");
			pw.println();
		}

		// HTML Content
		if (nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED
			|| nntpServer.getContentType() == AppConstants.CONTENT_TYPE_HTML) {
			pw.println("<html>");

			// Handle relative links by inserting base
			if (channel.getLink() != null && channel.getLink().length() > 0) {
				pw.println("<head><base href='" + channel.getLink() + "'>");
			}
			pw.println("<body>");

			pw.println(item.getDescription());

			pw.println("<p>");

			boolean hasLinks = false;
			if (item.getComments() != null
				&& item.getComments().length() > 0) {
				pw.print("<a href=\"");
				pw.print(item.getComments());
				pw.print("\">Comments</a>&nbsp;&nbsp;");
				hasLinks = true;
			}

			boolean hasPermaLink = false;
			// Output link
			if (item.getGuid() != null
				&& item.isGuidIsPermaLink()
				&& item.getGuid().length() > 0) {
				if (hasLinks) {
					pw.println("|&nbsp;&nbsp;");
				}
				pw.print("PermaLink: <a href=\"");
				pw.print(item.getGuid());
				pw.print("\">");
				pw.print(item.getGuid());
				pw.println("</a>");
				hasPermaLink = true;
			}

			if (item.getLink() != null && item.getLink().length() > 0) {
				// Do not display link if same as PermaLink
				if (!item.isGuidIsPermaLink()
					|| item.getGuid() == null
					|| !item.getGuid().equals(item.getLink())) {
					if (hasPermaLink) {
						pw.println("<br>");
					} else if (hasLinks) {
						pw.println("|&nbsp;&nbsp;");
					}
					pw.print("Link: <a href=\"");
					pw.print(item.getLink());
					pw.print("\">");
					pw.print(item.getLink());
					pw.println("</a>");
					hasLinks = true;
				}
			}

			if (hasLinks || hasPermaLink) {
				pw.println("<br>");
			}

			pw.println("<hr>");

			if (channel.getTitle() != null
				|| channel.getDescription() != null
				|| channel.getLink() != null) {

				pw.println(
					"<table width='100%' border='0'><tr><td align='left' valign='top'>");

				StringBuffer header = new StringBuffer();

				pw.println("<font size='-1'>");

				if (channel.getLink() == null) {
					if (channel.getTitle() != null) {
						header.append(channel.getTitle());
					}
				} else {
					header.append("<a href='");
					header.append(channel.getLink());
					header.append("'>");
					if (channel.getTitle() != null) {
						header.append(channel.getTitle());
					} else {
						header.append(channel.getLink());
					}
					header.append("</a>");
				}

				if (channel.getDescription() != null) {
					if (header.length() > 0) {
						header.append("<br>");
					}
					header.append(channel.getDescription());
				}

				pw.println(header.toString());

				pw.println("</font>");
				pw.println("</td><td align='right' valign='top'>");
				pw.println(
					"<font size='-1'>Served by <a href=\"http://www.methodize.org/nntprss\">nntp//rss</a> v"
						+ AppConstants.VERSION
						+ "</font></td></tr></table>");
			} else {
				pw.println(
					"<div align='right'><font size='-1'>Served by <a href=\"http://www.methodize.org/nntprss\">nntp//rss</a> v"
						+ AppConstants.VERSION
						+ "</font></div>");
			}

			pw.println("</body></html>");
		}

		if (nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED) {
			pw.println("--" + boundary + "--");
		}

		pw.flush();

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

	private void processRequestLoop(
		BufferedReader br,
		PrintWriter pw,
		PrintWriter bodyPw)
		throws IOException {

		boolean quitRequested = false;
		String currentGroupName = null;
		int currentArticle = NO_CURRENT_ARTICLE;
		String user = null;
		boolean userAuthenticated = false;

		while (quitRequested == false) {
			String requestString = br.readLine();

			String command = null;

			String[] parameters = null;
			if (requestString != null) {
				parameters = parseParameters(requestString);
				if (parameters.length > 0) {
					command = parameters[0];
				}
			} else {
				// If requestString == null - end of stream, indicate that client
				// wants to quit/has quit (result of OE testing)
				command = "QUIT";
			}

			if (command != null) {
				if (nntpServer.isSecure()
					&& !userAuthenticated
					&& (!command.equalsIgnoreCase("AUTHINFO")
						&& !command.equalsIgnoreCase("QUIT")
						&& !command.equalsIgnoreCase("MODE"))) {
					pw.println("480 Authentication Required");
				} else if (
					command.equalsIgnoreCase("ARTICLE")
						|| command.equalsIgnoreCase("HEAD")
						|| command.equalsIgnoreCase("BODY")
						|| command.equalsIgnoreCase("STAT")) {
					//					pw.println("430 no such article found");
					Item item = null;
					Channel channel = null;

					// ***************************
					//TODO: resolve no current group scenario

					if (parameters.length == 1
						&& currentArticle != NO_CURRENT_ARTICLE) {
						// Get current article
						channel =
							channelManager.channelByName(currentGroupName);

						item =
							channelManager.getChannelDAO().loadItem(
								channel,
								currentArticle);

					} else {
						String artNumOrMsgId = parameters[1];

						if (artNumOrMsgId.indexOf('<') == -1) {
							// Article number
							//						item = channel.getItemByArticleNumber(Long.parseLong(artNumOrMsgId));
							channel =
								channelManager.channelByName(currentGroupName);

							item =
								channelManager.getChannelDAO().loadItem(
									channel,
									Integer.parseInt(artNumOrMsgId));
						} else {
							// Message IDs are in the form
							// <itemsignature@channelname>
							int sepPos = artNumOrMsgId.indexOf('@');
							if (sepPos > -1) {
								String itemSignature =
									artNumOrMsgId.substring(1, sepPos);
								String artChannelName =
									artNumOrMsgId.substring(
										sepPos + 1,
										artNumOrMsgId.length() - 1);

								channel =
									channelManager.channelByName(
										artChannelName);
								if (channel != null) {
									item =
										channelManager
											.getChannelDAO()
											.loadItem(
											channel,
											itemSignature);
								}

							}
						}
					}

					if (item == null) {
						if (parameters.length == 1
							&& currentArticle == NO_CURRENT_ARTICLE) {
							pw.println(
								"420 no current article has been selected");
						} else {
							pw.println("430 no such article found");
						}
					} else {
						if (command.equalsIgnoreCase("ARTICLE")) {
							pw.println(
								"220 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - head and body follow");

							writeArticle(pw, bodyPw, channel, item);
							pw.println(".");
						} else if (command.equalsIgnoreCase("HEAD")) {
							pw.println(
								"221 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - head follows");

							writeHead(pw, channel, item);
							pw.println(".");
						} else if (command.equalsIgnoreCase("BODY")) {
							pw.println(
								"222 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - body follows");

							pw.flush();
							writeBody(bodyPw, channel, item);
							pw.println(".");
						} else if (command.equalsIgnoreCase("STAT")) {
							pw.println(
								"223 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - request text separately");
						}

						currentArticle = item.getArticleNumber();
					}
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
						currentArticle = channel.getFirstArticleNumber();
					} else {
						pw.println("411 no such news group");
					}
				} else if (command.equalsIgnoreCase("HELP")) {
					pw.println("100 help text follows");
					pw.println(".");
				} else if (command.equalsIgnoreCase("IHAVE")) {
					pw.println("435 article not wanted - do not send it");
				} else if (command.equalsIgnoreCase("LAST")) {
					if (currentGroupName == null) {
						pw.println("412 No news group currently selected");
					} else {
						Channel channel =
							channelManager.channelByName(currentGroupName);
						if (currentArticle == NO_CURRENT_ARTICLE) {
							pw.println(
								"420 no current article has been selected");
						} else if (
							currentArticle > channel.getFirstArticleNumber()) {
							Item item =
								channelManager
									.getChannelDAO()
									.loadPreviousItem(
									channel,
									currentArticle);
							currentArticle = item.getArticleNumber();
							pw.println(
								"223 "
									+ item.getArticleNumber()
									+ " "
									+ createMessageId(item)
									+ " article retrieved - request text separately");
						} else {
							pw.println("422 no previous article in thie group");
						}
					}
				} else if (command.equalsIgnoreCase("LIST")) {
					if (parameters.length > 1
						&& !parameters[1].equalsIgnoreCase("ACTIVE")) {
						if (parameters[1].equalsIgnoreCase("ACTIVE.TIMES")) {
							//							pw.println("503 program error, function not performed");
							// Added for nn
							pw.println("215 information follows");
							pw.println(".");
						} else if (
							parameters[1].equalsIgnoreCase("DISTRIBUTIONS")) {
							pw.println(
								"503 program error, function not performed");
						} else if (
							parameters[1].equalsIgnoreCase("DISTRIB.PATS")) {
							pw.println(
								"503 program error, function not performed");
						} else if (
							parameters[1].equalsIgnoreCase("NEWSGROUPS")) {
							//							pw.println("503 program error, function not performed");
							pw.println("215 list of newsgroups follows");
							Iterator channelIter = channelManager.channels();
							while (channelIter.hasNext()) {
								Channel channel = (Channel) channelIter.next();
								pw.println(channel.getName() + " ");
								// @TODO think about description
								//									+ channel.getDescription());
							}
							pw.println(".");
						} else if (
							parameters[1].equalsIgnoreCase("OVERVIEW.FMT")) {
							pw.println("215 information follows");
							pw.println("Subject:");
							pw.println("From:");
							pw.println("Date:");
							pw.println("Message-ID:");
							pw.println("References:");
							pw.println("Bytes:");
							pw.println("Lines:");
							//							pw.println("Xref:full");							
							pw.println(".");
						} else if (
							parameters[1].equalsIgnoreCase("SUBSCRIPTIONS")) {
							// Empty default subscription list
							pw.println("215 information follows");
							pw.println(".");
							//							pw.println("503 program error, function not performed");
						} else {
							pw.println(
								"503 program error, function not performed");
						}

					} else {
						pw.println("215 list of newsgroups follows");
						Iterator channelIter = channelManager.channels();
						while (channelIter.hasNext()) {
							Channel channel = (Channel) channelIter.next();
							// group list first p
							pw.println(
								channel.getName()
									+ " "
									+ (channel.getLastArticleNumber() - 1)
									+ " "
									+ channel.getFirstArticleNumber()
									+ " "
									+ (channel.isPostingEnabled() ? "y" : "n"));
						}
						pw.println(".");
					}
				} else if (command.equalsIgnoreCase("LISTGROUP")) {
					Channel channel = null;
					if (parameters.length > 1) {
						channel = channelManager.channelByName(parameters[1]);
					} else {
						if (currentGroupName != null) {
							channel =
								channelManager.channelByName(currentGroupName);
						}
					}

					if (channel != null) {
						pw.println("211 list of article numbers follow");

						List items =
							channelManager.getChannelDAO().loadArticleNumbers(
								channel);

						Iterator itemIter = items.iterator();
						while (itemIter.hasNext()) {
							pw.println(itemIter.next());
						}
						pw.println(".");

					} else {
						pw.println("412 Not currently in newsgroup");
					}
				} else if (command.equalsIgnoreCase("MODE")) {
					//					pw.println("201 Hello, you can't post");
					pw.println("200 Hello, you can post");
				} else if (command.equalsIgnoreCase("NEWGROUPS")) {
					if (parameters.length < 3) {
						pw.println("500 command not recognized");
					} else {
						if (parameters.length > 3
							&& parameters[3].equalsIgnoreCase("GMT")) {
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
					if (currentGroupName == null) {
						pw.println("412 No news group currently selected");
					} else {
						Channel channel =
							channelManager.channelByName(currentGroupName);
						if (currentArticle == NO_CURRENT_ARTICLE) {
							pw.println(
								"420 no current article has been selected");
						} else if (
							currentArticle < channel.getLastArticleNumber()) {
							Item item =
								channelManager.getChannelDAO().loadNextItem(
									channel,
									currentArticle);
							currentArticle = item.getArticleNumber();
							pw.println(
								"223 "
									+ item.getArticleNumber()
									+ " "
									+ createMessageId(item)
									+ " article retrieved - request text separately");
						} else {
							pw.println("421 no next article in this group");
						}
					}
				} else if (command.equalsIgnoreCase("POST")) {
					//					pw.println("440 posting not allowed");
					cmdPost(br, pw);
				} else if (command.equalsIgnoreCase("QUIT")) {
					pw.println("205 closing connection - goodbye!");
					quitRequested = true;
				} else if (command.equalsIgnoreCase("SLAVE")) {
					pw.println("202 slave status noted");
				} else if (command.equalsIgnoreCase("XHDR")) {
					List items = null;
					int header = NNTP_HEADER_UNKNOWN;
					boolean useMessageId = false;
					boolean groupSelected = true;
					Channel channel = null;
					if (parameters.length == 2) {
						if (currentGroupName == null) {
							groupSelected = false;
						} else {
							header = parseHeaderName(parameters[1]);
							channel =
								channelManager.channelByName(currentGroupName);
							Item item =
								channelManager.getChannelDAO().loadItem(
									channel,
									currentArticle);
							if (item != null) {
								items = new ArrayList();
								items.add(item);
							}
						}
					} else if (parameters.length == 3) {
						header = parseHeaderName(parameters[1]);
						// Check parameter for message id...
						if (parameters[2].charAt(0) == '<') {
							useMessageId = true;

							int sepPos = parameters[2].indexOf('@');
							if (sepPos > -1) {
								String itemSignature =
									parameters[2].substring(1, sepPos);
								String artChannelName =
									parameters[2].substring(
										sepPos + 1,
										parameters[2].length() - 1);

								channel =
									channelManager.channelByName(
										artChannelName);
								if (channel != null) {
									Item item =
										channelManager
											.getChannelDAO()
											.loadItem(
											channel,
											itemSignature);

									if (item != null) {
										items = new ArrayList();
										items.add(item);
									}

								}

							}

						} else {
							if (currentGroupName == null) {
								groupSelected = false;
							} else {
								int[] range = getIntRange(parameters[2]);
								channel =
									channelManager.channelByName(
										currentGroupName);
								items =
									channelManager.getChannelDAO().loadItems(
										channel,
										range,
										false,
										ChannelDAO.LIMIT_NONE);
							}
						}
					} else {
						// Invalid request...
					}

					if (groupSelected == false) {
						pw.println("412 No news group currently selected");
					} else if (header == NNTP_HEADER_UNKNOWN) {
						pw.println(
							"500 command not recognized (unknown header name="
								+ parameters[1]
								+ ")");
					} else if (items == null || items.size() == 0) {
						pw.println("430 no such article");
					} else {
						pw.println("221 Header follows");

						// Only support XHDR on the following 6 required headers...
						if (header == NNTP_HEADER_FROM
							|| header == NNTP_HEADER_DATE
							|| header == NNTP_HEADER_NEWSGROUP
							|| header == NNTP_HEADER_SUBJECT
							|| header == NNTP_HEADER_MESSAGE_ID
							|| header == NNTP_HEADER_PATH
							|| header == NNTP_HEADER_REFERENCES
							|| header == NNTP_HEADER_LINES) {

							Iterator itemIter = items.iterator();
							while (itemIter.hasNext()) {
								Item item = (Item) itemIter.next();

								if (!useMessageId) {
									pw.print(item.getArticleNumber());
								} else {
									pw.print(
										"<"
											+ item.getSignature()
											+ "@"
											+ item.getChannel().getName()
											+ ">");
								}

								pw.print(' ');

								switch (header) {
									case NNTP_HEADER_FROM :

										pw.println(
											processAuthor(channel, item));
										//
										//										String email;
										//										if(channel.getManagingEditor() != null) {
										//											email = RSSHelper.parseEmail(channel.getManagingEditor());	
										//										} else {
										//											email = "";
										//										}
										//										String author = processAuthor(channel.getAuthor(), email);

										//										pw.println(
										//											stripTabsLineBreaks(MimeUtility.encodeText(author, "UTF-8", "Q"))  );
										//										pw.println(stripTabsLineBreaks(author));

										break;
									case NNTP_HEADER_DATE :
										pw.println(df.format(item.getDate()));
										break;
									case NNTP_HEADER_NEWSGROUP :
										pw.println(item.getChannel().getName());
										break;
									case NNTP_HEADER_SUBJECT :
										//										pw.println(
										//											processSubject(item.getTitle()));
										pw.println(
											processSubject(
												MimeUtility.encodeText(
													item.getTitle(),
													"UTF-8",
													"Q")));

										break;
									case NNTP_HEADER_MESSAGE_ID :
										pw.println(
											"<"
												+ item.getSignature()
												+ "@"
												+ item.getChannel().getName()
												+ ">");
										break;
									case NNTP_HEADER_PATH :
										pw.println("nntprss");
										break;
									case NNTP_HEADER_REFERENCES :
										pw.println();
										break;
									case NNTP_HEADER_LINES :
										// TODO Calculate actual lines
										pw.println(10);
										break;
									default :
										pw.println();
										break;
								}
							}
						}
						pw.println(".");
					}
				} else if (command.equalsIgnoreCase("XOVER")) {
					cmdXOVER(pw, currentGroupName, parameters);
				} else if (command.equalsIgnoreCase("AUTHINFO")) {
					if (parameters.length > 1) {
						if (!nntpServer.isSecure()) {
							if (parameters[1].equalsIgnoreCase("USER")
								|| parameters[1].equalsIgnoreCase("PASS")) {
								pw.println("281 Authentication accepted");
							} else if (
								parameters[1].equalsIgnoreCase("GENERIC")) {
								pw.println("501 command not supported");
							} else {
								pw.println("500 command not recognized");
							}
						} else {
							if (parameters[1].equalsIgnoreCase("USER")) {
								user = parameters[2];
								pw.println(
									"381 More authentication information required");
							} else if (
								parameters[1].equalsIgnoreCase("PASS")) {
								if (nntpServer
									.isValidUser(user, parameters[2])) {
									pw.println("281 Authentication accepted");
									userAuthenticated = true;
								} else {
									pw.println("502 No permission");
								}
							} else if (
								parameters[1].equalsIgnoreCase("GENERIC")) {
								pw.println("501 command not supported");
							} else {
								pw.println("500 command not recognized");
							}
						}
					} else {
						pw.println("500 command not recognized");
					}
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

	private void cmdXOVER(
		PrintWriter pw,
		String currentGroupName,
		String[] parameters) {
		if (currentGroupName == null) {
			pw.println("412 No news group currently selected");
		} else if (parameters.length < 2) {
			pw.println("500 command not recognized");
		} else {
			pw.println("224 Overview information follows");
			// Interpret parameters and restrict return
			Channel channel = channelManager.channelByName(currentGroupName);
			int[] range = getIntRange(parameters[1]);
			boolean noItemsFound = true;
			boolean retrieving = true;
			
			while(retrieving) {
				List items =
					channelManager.getChannelDAO().loadItems(channel, range, false, RETRIEVE_LIMIT);

				if(noItemsFound && items.size() == 0) {
					pw.println("420 No article(s) selected");
					break;
				} else if(items.size() > 0) {

					Iterator itemIter = items.iterator();

					while (itemIter.hasNext()) {
						Item item = (Item) itemIter.next();
						try {
							pw
								.println(
									item.getArticleNumber()
									+ "\t"
									+ processSubject(
										MimeUtility.encodeText(
											item.getTitle(),
											"UTF-8",
											"Q"))
									+ "\t"
									+ processAuthor(channel, item)
							//											+ author
							+"\t"
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
							//										+ "\tXref: nntprss "
							//										+ item.getChannel().getName()
							//										+ ":"
							//										+ item.getArticleNumber()
							);
						} catch (Exception e) {
							if (log.isEnabledFor(Priority.WARN)) {
								log.warn("Exception thrown in XOVER", e);
							}
						}

					}
				
					noItemsFound = false;

					range[0] = ((Item)items.get(items.size() - 1)).getArticleNumber() + 1;
					if(range[1] != AppConstants.OPEN_ENDED_RANGE && range[0] > range[1])
//	   Reached end of items...					
						retrieving = false;
				} else {
// end of articles
					retrieving = false;
				}
			}

			if(!noItemsFound)			
				pw.println(".");
		}
	}

	private void cmdPost(BufferedReader br, PrintWriter pw)
		throws IOException {
		pw.println("340 send article to be posted. End with <CR-LF>.<CR-LF>");
		pw.flush();

		Properties props = new Properties();
		Session s = Session.getInstance(props, null);

		StringWriter sw = new StringWriter();
		String line = br.readLine();
		while (line != null && (!line.equals("."))) {
			sw.write(line);
			sw.write("\r\n");
			line = br.readLine();
		}

		sw.flush();

		try {
			String content = null;
			MimeMessage test =
				new MimeMessage(
					s,
					new ByteArrayInputStream(
						sw.getBuffer().toString().getBytes()));

			String newsgroup = test.getHeader("Newsgroups")[0];

			Channel channel = channelManager.channelByName(newsgroup);

			if (channel != null && channel.isPostingEnabled()) {

				boolean htmlContent = false;
				if (test.isMimeType("text/plain")) {
					content = (String) test.getContent();
				} else if (test.isMimeType("text/html")) {
					htmlContent = true;
					// If it is HTML, let's trim the whitespace...
					content = HTMLHelper.stripCRLF((String) test.getContent());
				} else if (test.isMimeType("multipart/alternative")) {
					Multipart mp = (Multipart) test.getContent();
					MimeBodyPart mbp = (MimeBodyPart) mp.getBodyPart(1);
					// HTML content...				
					if (mbp.isMimeType("text/html")) {
						htmlContent = true;
						// If it is HTML, let's trim the whitespace...
						content =
							HTMLHelper.stripCRLF((String) mbp.getContent());
					}
				}

				if (htmlContent) {
					// Parse HTML content and extract pertinent content...
					String upperContent = content.toUpperCase();
					int bodyPos = upperContent.indexOf("<BODY");
					if (bodyPos != -1) {
						int endBodyOpenPos = upperContent.indexOf(">", bodyPos);
						if (endBodyOpenPos != -1) {
							int bodyClosePos = upperContent.indexOf("</BODY");
							if (bodyClosePos != -1) {
								content =
									content.substring(
										endBodyOpenPos + 1,
										bodyClosePos);
							}
						}
					}
				}

				Publisher pub = null;
				if (channel.getPublishAPI().equals("blogger")) {
					pub = new BloggerPublisher();
				} else if (channel.getPublishAPI().equals("metaweblog")) {
					pub = new MetaWeblogPublisher();
				} else if (channel.getPublishAPI().equals("livejournal")) {
					pub = new LiveJournalPublisher();
				}

				Item item = new Item();
				item.setDescription(content);
				item.setTitle(test.getSubject());
				pub.publish(channel.getPublishConfig(), item);

				pw.println("240 article posted ok");

				// request repoll
				channel.setLastPolled(null);

			} else if (!channel.isPostingEnabled()) {
				pw.println("440 posting not allowed to " + newsgroup);
			} else {
				pw.println("441 posting failed");
			}
		} catch (MessagingException me) {
			pw.println("441 posting failed");
		} catch (PublisherException pe) {
			// XXX give more thorough information
			pw.println("441 posting failed " + pe.getMessage().trim());
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

			OutputStream os = client.getOutputStream();

			// Use CRLF PrintWriter wrapper class for output...
			// Changed to enforce utf-8 encoding on output...
			PrintWriter pw =
				new CRLFPrintWriter(
					new OutputStreamWriter(client.getOutputStream()));

			PrintWriter bodyPw =
				new CRLFPrintWriter(
					new OutputStreamWriter(client.getOutputStream(), "utf-8"));

			// Send 201 connection header
			pw.println(
				"200 nntp//rss v"
					+ AppConstants.VERSION
					+ " news server ready");
			pw.flush();

			processRequestLoop(br, pw, bodyPw);

			br.close();
			pw.close();
			bodyPw.close();
			//			client.close();

		} catch (Exception e) {
			if (!(e instanceof IOException)) {
				if (log.isEnabledFor(Priority.WARN)) {
					log.warn("Unexpected exception thrown", e);
				}
			} else if (e instanceof SocketTimeoutException) {
				if (log.isDebugEnabled()) {
					log.debug("NNTP Client socket timed out");
				}
			}
		} finally {
			if (log.isInfoEnabled()) {
				log.info("NNTP Client connection closed");
			}
			try {
				client.close();
			} catch (Exception e) {
			}
		}
	}

	private String processAuthor(Channel channel, Item item) {
		String authorEmail = null;

		if (item.getCreator() != null && item.getCreator().length() > 0) {
			String creator = item.getCreator().trim();
			String author;
			String email = null;

			int atPos = creator.indexOf('@');
			if (atPos > 0) {
				// We have found an @ sign after the first character - indicative 
				// of the creator containing an email address...
				int spacePos = creator.substring(0, atPos).lastIndexOf(' ');
				if (spacePos == -1) {
					// Let's assume that it is just an email...
					author = creator;
					email = creator;
				} else {
					author = creator.substring(0, spacePos);
					email = creator.substring(spacePos + 1);
					if (email.charAt(0) == '('
						&& email.charAt(email.length() - 1) == ')') {
						email = email.substring(1, email.length() - 1);
					}
					if (email.startsWith("mailto:")
						&& email.length() > "mailto:".length()) {
						email = email.substring("mailto:".length());
					}
				}
			} else {
				// No email address provided
				author = creator;
			}

			authorEmail = stripTabsLineBreaks(processAuthor(author, email));
		}

		if ((authorEmail == null || authorEmail.length() == 0)) {
			if (channel.getManagingEditor() == null) {
				authorEmail =
					stripTabsLineBreaks(processAuthor(channel.getAuthor(), ""));
			} else {
				authorEmail =
					stripTabsLineBreaks(
						processAuthor(
							channel.getAuthor(),
							RSSHelper.parseEmail(channel.getManagingEditor())));
			}
		}

		return authorEmail;
	}

	private String processAuthor(String author, String email) {
		String authorEmail;
		if (email == null || email.length() == 0) {
			email = "unknown@email";
		}

		try {
			InternetAddress inetAddress =
				new InternetAddress(email, author, "UTF-8");
			authorEmail = inetAddress.toString();
		} catch (UnsupportedEncodingException uee) {
			authorEmail = "";
		}
		return authorEmail;
	}

	private String stripTabsLineBreaks(String value) {
		StringBuffer strippedString = new StringBuffer();
		boolean lastCharBreak = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\n' || c == '\t') {
				if (!lastCharBreak) {
					strippedString.append(' ');
				}
				lastCharBreak = true;
			} else if (c != '\r') {
				strippedString.append(c);
				lastCharBreak = false;
			}
		}
		return strippedString.toString();
	}

	private String processSubject(String subject) {
		String strippedString = stripTabsLineBreaks(subject);
		return HTMLHelper.unescapeString(strippedString);
	}

}
