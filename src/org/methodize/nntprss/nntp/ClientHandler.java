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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.rss.Channel;
import org.methodize.nntprss.rss.ChannelManager;
import org.methodize.nntprss.rss.Item;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.CRLFPrintWriter;
import org.methodize.nntprss.util.XMLHelper;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: ClientHandler.java,v 1.5 2003/02/01 02:45:12 jasonbrome Exp $
 */
public class ClientHandler implements Runnable {

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
		if(headerName.equalsIgnoreCase("from")) {
			header = NNTP_HEADER_FROM;
		} else if(headerName.equalsIgnoreCase("date")) {
			header = NNTP_HEADER_DATE;
		} else if(headerName.equalsIgnoreCase("newsgroup")) {
			header = NNTP_HEADER_NEWSGROUP;
		} else if(headerName.equalsIgnoreCase("subject")) {
			header = NNTP_HEADER_SUBJECT;
		} else if(headerName.equalsIgnoreCase("message-id")) {
			header = NNTP_HEADER_MESSAGE_ID;
		} else if(headerName.equalsIgnoreCase("path")) {
			header = NNTP_HEADER_PATH;
		} else if(headerName.equalsIgnoreCase("followup-to")) {
			header = NNTP_HEADER_FOLLOWUP_TO;
		} else if(headerName.equalsIgnoreCase("expires")) {
			header = NNTP_HEADER_EXPIRES;
		} else if(headerName.equalsIgnoreCase("reply-to")) {
			header = NNTP_HEADER_REPLY_TO;
		} else if(headerName.equalsIgnoreCase("sender")) {
			header = NNTP_HEADER_SENDER;
		} else if(headerName.equalsIgnoreCase("references")) {
			header = NNTP_HEADER_REFERENCES;
		} else if(headerName.equalsIgnoreCase("control")) {
			header = NNTP_HEADER_CONTROL;
		} else if(headerName.equalsIgnoreCase("distribution")) {
			header = NNTP_HEADER_DISTRIBUTION;
		} else if(headerName.equalsIgnoreCase("keywords")) {
			header = NNTP_HEADER_KEYWORDS;
		} else if(headerName.equalsIgnoreCase("summary")) {
			header = NNTP_HEADER_SUMMARY;
		} else if(headerName.equalsIgnoreCase("approved")) {
			header = NNTP_HEADER_APPROVED;
		} else if(headerName.equalsIgnoreCase("lines")) {
			header = NNTP_HEADER_LINES;
		} else if(headerName.equalsIgnoreCase("xref")) {
			header = NNTP_HEADER_XREF;
		} else if(headerName.equalsIgnoreCase("organization")) {
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
				&& rangePos < rangeStr.length()-1) {
				String startStr = rangeStr.substring(0, rangePos);
				String endStr = rangeStr.substring(rangePos + 1);
				range[0] = Integer.parseInt(startStr);
				range[1] = Integer.parseInt(endStr);
			} else if(rangePos > 0 && rangeStr.length() > 0) {
				range[0] = Integer.parseInt(rangeStr.substring(0, rangePos));
				range[1] = AppConstants.OPEN_ENDED_RANGE;
			} else if(rangePos == 0) {
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

	private void writeArticle(PrintWriter pw,
		Channel channel, Item item) throws IOException {

		writeHead(pw, channel, item);
		pw.println();
		writeBody(pw, channel, item);

	}



	private void writeHead(PrintWriter pw,
		Channel channel, Item item) throws IOException {

		String boundary = "----=_Part_" + item.getDate().getTime();

		pw.println("From: " + channel.getAuthor());
		pw.println("Newsgroups: " + channel.getName());
		pw.println("Date: " + df.format(item.getDate()));
		pw.println("Subject: " + item.getTitle());
		pw.println(
			"Message-ID: "
				+ "<"
				+ item.getSignature()
				+ "@"
				+ channel.getName()
				+ ">");
		pw.println("Path: nntprss");

		switch(nntpServer.getContentType()) {
			case AppConstants.CONTENT_TYPE_TEXT:
				pw.println("Content-Type: text/plain; charset=utf-8");
				break;
			case AppConstants.CONTENT_TYPE_HTML:
				pw.println("Content-Type: text/html; charset=utf-8");
				break;
			default:
// Mixed		
				pw.println("Content-Type: multipart/alternative;");
				pw.println("      boundary=\"" + boundary + "\"");
		}
	}

	private void writeBody(PrintWriter pw,
		Channel channel, Item item) throws IOException {

		String boundary = null;
	
		if(nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED) {
			boundary = "----=_Part_" + item.getDate().getTime();
			pw.println("--" + boundary);	
			pw.println("Content-Type: text/plain; charset=utf-8");
			pw.println();
		}

// Plain text content
		if(nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED ||
			nntpServer.getContentType() == AppConstants.CONTENT_TYPE_TEXT) {

			pw.println(XMLHelper.stripHtmlTags(item.getDescription()));
			pw.println();
	
			if(item.getComments() != null && item.getComments().length() > 0) {
				pw.print("Comments: ");
				pw.println(item.getLink());
			}

			if(item.getLink() != null && item.getLink().length() > 0) {
				pw.print("Link: ");
				pw.println(item.getLink());
			}

			pw.println();

			if(channel.getTitle() != null ||
				channel.getDescription() != null ||
				channel.getLink() != null) {

				pw.println("---");


				StringBuffer header = new StringBuffer();

				if(channel.getTitle() != null) {
					header.append(channel.getTitle());
				}
				
				if(channel.getLink() != null) {
					if(header.length() > 0) {
						header.append(' ');
					}
					header.append(channel.getLink());
				}
				
				if(channel.getDescription() != null) {
					if(header.length() > 0) {
						header.append("\r\n");
					}
					header.append(channel.getDescription());
				}
				
				pw.println(header.toString());
			}

			pw.println();
			pw.println("Served by nntp//rss v"
				+ AppConstants.VERSION
				+" ( http://www.methodize.org/nntprss )");
			pw.println();
		}
		
		
		if(nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED) {
			pw.println("--" + boundary);	
			pw.println("Content-Type: text/html; charset=utf-8");
			pw.println();
		}
		

// HTML Content
		if(nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED ||
			nntpServer.getContentType() == AppConstants.CONTENT_TYPE_HTML) {
			pw.println("<html>");
			pw.println("<body>");

			pw.println(item.getDescription());

			pw.println("<p>");

			boolean hasLinks = false;
			if (item.getComments() != null && item.getComments().length() > 0) {
				pw.print("<a href=\"");
				pw.print(item.getComments());
				pw.print("\">Comments</a>&nbsp;&nbsp;");
				hasLinks = true;
			}

	
			// Output link
			if (item.getLink() != null && item.getLink().length() > 0) {
				if(hasLinks) {
					pw.println("|&nbsp;&nbsp;");
				}
				pw.print("Link: <a href=\"");
				pw.print(item.getLink());
				pw.print("\">");
				pw.print(item.getLink());
				pw.println("</a>");
				hasLinks = true;
			}

			if(hasLinks) {
				pw.println("<br>");
			}
			
			pw.println("<hr>");
		
			if(channel.getTitle() != null ||
				channel.getDescription() != null ||
				channel.getLink() != null) {
				
				pw.println("<table width='100%' border='0'><tr><td align='left' valign='top'>");

				StringBuffer header = new StringBuffer();

				pw.println("<font size='-1'>");

				if(channel.getLink() == null) {
					if(channel.getTitle() != null) {
						header.append(channel.getTitle());
					}
				} else {
					header.append("<a href='");
					header.append(channel.getLink());
					header.append("'>");
					if(channel.getTitle() != null) {
						header.append(channel.getTitle());
					} else {
						header.append(channel.getLink());
					}
					header.append("</a>");
				}
								
				if(channel.getDescription() != null) {
					if(header.length() > 0) {
						header.append("<br>");
					}
					header.append(channel.getDescription());
				}
				
				pw.println(header.toString());

				pw.println("</font>");
				pw.println("</td><td align='right' valign='top'>");
				pw.println("<font size='-1'>Served by <a href=\"http://www.methodize.org/nntprss\">nntp//rss</a> v"
				+ AppConstants.VERSION
				+ "</font></td></tr></table>");
			} else {
				pw.println("<div align='right'><font size='-1'>Served by <a href=\"http://www.methodize.org/nntprss\">nntp//rss</a> v"
				+ AppConstants.VERSION
				+ "</font></div>");
			}



			pw.println("</body></html>");
		}
		
		
		if(nntpServer.getContentType() == AppConstants.CONTENT_TYPE_MIXED) {
			pw.println("--" + boundary + "--");
		}

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
		int currentArticle = NO_CURRENT_ARTICLE;

		while (quitRequested == false) {
			String requestString = br.readLine();
			
			String command = null;

			String[] parameters	= null;
			if(requestString != null) {
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
				if (command.equalsIgnoreCase("ARTICLE") ||
					command.equalsIgnoreCase("HEAD") ||
					command.equalsIgnoreCase("BODY") ||
					command.equalsIgnoreCase("STAT")) {
					//					pw.println("430 no such article found");
					Item item = null;
					Channel channel = null;

// TODO resolve no current group scenario

					if(parameters.length == 1 && currentArticle != NO_CURRENT_ARTICLE) {
// Get current article
						channel =
							channelManager.channelByName(currentGroupName);
	
						item =
							channelManager.getChannelManagerDAO().loadItem(
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
					}
					
					if (item == null) {
						if(parameters.length == 1 && currentArticle == NO_CURRENT_ARTICLE) {
							pw.println("420 no current article has been selected");
						} else {
							pw.println("430 no such article found");
						}
					} else {
						if(command.equalsIgnoreCase("ARTICLE")) {
							pw.println(
								"220 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - head and body follow");
	
							writeArticle(pw, channel, item);
						} else if(command.equalsIgnoreCase("HEAD")) {
							pw.println(
								"221 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - head follows");
	
							writeHead(pw, channel, item);
						} else if(command.equalsIgnoreCase("BODY")) {
							pw.println(
								"222 "
									+ item.getArticleNumber()
									+ " <"
									+ item.getSignature()
									+ "@"
									+ channel.getName()
									+ "> article retrieved - body follows");
							writeBody(pw, channel, item);
						} else if(command.equalsIgnoreCase("STAT")) {
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

						pw.println(".");
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
					if(currentGroupName == null) {
						pw.println("412 No news group currently selected");
					} else {
						Channel channel =
							channelManager.channelByName(currentGroupName);
						if(currentArticle == NO_CURRENT_ARTICLE) {
							pw.println("420 no current article has been selected");
						} else	if(currentArticle > channel.getFirstArticleNumber()) {
							Item item = channelManager.getChannelManagerDAO().loadPreviousItem(channel,
								currentArticle);
							currentArticle = item.getArticleNumber();
							pw.println("223 "
								+ item.getArticleNumber()
								+ " "
								+ createMessageId(item)
								+ " article retrieved - request text separately");
						} else {
							pw.println("422 no previous article in thie group");
						}
					}
				} else if (command.equalsIgnoreCase("LIST")) {
					if(parameters.length > 1 && !parameters[1].equalsIgnoreCase("ACTIVE")) {
						if(parameters[1].equalsIgnoreCase("ACTIVE.TIMES")) {
//							pw.println("503 program error, function not performed");
// Added for nn
							pw.println("215 information follows");
							pw.println(".");
						} else if(parameters[1].equalsIgnoreCase("DISTRIBUTIONS")) {
							pw.println("503 program error, function not performed");
						} else if(parameters[1].equalsIgnoreCase("DISTRIB.PATS")) {
							pw.println("503 program error, function not performed");
						} else if(parameters[1].equalsIgnoreCase("NEWSGROUPS")) {
//							pw.println("503 program error, function not performed");
							pw.println("215 list of newsgroups follows");
							Iterator channelIter = channelManager.channels();
							while (channelIter.hasNext()) {
								Channel channel = (Channel) channelIter.next();
								pw.println(channel.getName() + " ");
							}
							pw.println(".");
						} else if(parameters[1].equalsIgnoreCase("OVERVIEW.FMT")) {
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
						} else if(parameters[1].equalsIgnoreCase("SUBSCRIPTIONS")) {
// Empty default subscription list
							pw.println("215 information follows");
							pw.println(".");
//							pw.println("503 program error, function not performed");
						} else {
							pw.println("503 program error, function not performed");
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
									+ " n");
						}
						pw.println(".");
					}
				} else if (command.equalsIgnoreCase("MODE")) {
					pw.println("201 Hello, you can't post");
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
					if(currentGroupName == null) {
						pw.println("412 No news group currently selected");
					} else {
						Channel channel =
							channelManager.channelByName(currentGroupName);
						if(currentArticle == NO_CURRENT_ARTICLE) {
							pw.println("420 no current article has been selected");
						} else	if(currentArticle < channel.getLastArticleNumber()) {
							Item item = channelManager.getChannelManagerDAO().loadNextItem(channel,
								currentArticle);
							currentArticle = item.getArticleNumber();
							pw.println("223 "
								+ item.getArticleNumber()
								+ " "
								+ createMessageId(item)
								+ " article retrieved - request text separately");
						} else {
							pw.println("421 no next article in this group");
						}
					}
				} else if (command.equalsIgnoreCase("POST")) {
					pw.println("440 posting not allowed");
				} else if (command.equalsIgnoreCase("QUIT")) {
					pw.println("205 closing connection - goodbye!");
					quitRequested = true;
				} else if (command.equalsIgnoreCase("SLAVE")) {
					pw.println("202 slave status noted");
				} else if (command.equalsIgnoreCase("XHDR")) {
					List items = null;
					int header = NNTP_HEADER_UNKNOWN;
					boolean useMessageId = false;
					if(parameters.length == 2) {
						header = parseHeaderName(parameters[1]);
						Channel channel =
							channelManager.channelByName(currentGroupName);
						Item item = 
							channelManager.getChannelManagerDAO().loadItem(
								channel,
								currentArticle);
						if(item != null) {
							items = new ArrayList();
							items.add(item);
						}
					} else if(parameters.length == 3) {
						header = parseHeaderName(parameters[1]);
// Check parameter for message id...
						if(parameters[2].charAt(0) == '<') {
							useMessageId = true;
							
							int sepPos = parameters[2].indexOf('@');
							if(sepPos > -1) {
								String itemSignature = parameters[2].substring(1, sepPos);
								String artChannelName = parameters[2].substring(sepPos + 1, parameters[2].length()-1);
	
								Channel channel =
									channelManager.channelByName(artChannelName);
								if(channel != null) {
									Item item =
										channelManager.getChannelManagerDAO().loadItem(
											channel, itemSignature);

									if(item != null) {
										items = new ArrayList();
										items.add(item);
									}

								}
	
							} 
							
						} else {
							int[] range = getIntRange(parameters[2]);
							Channel channel =
								channelManager.channelByName(currentGroupName);
							items =
								channelManager.getChannelManagerDAO().loadItems(
									channel,
									range,
									false);
						}
					} else {
// Invalid request...
					}
					
					if(header == NNTP_HEADER_UNKNOWN) {
						pw.println("500 command not recognized (unknown header name=" + parameters[1] + ")");
					} else if(items == null || items.size() == 0) {
						pw.println("430 no such article");
					} else {
						pw.println("221 Header follows");

// Only support XHDR on the following 6 required headers...
						if(header == NNTP_HEADER_FROM ||
							header == NNTP_HEADER_DATE ||
							header == NNTP_HEADER_NEWSGROUP ||
							header == NNTP_HEADER_SUBJECT || 
							header == NNTP_HEADER_MESSAGE_ID ||
							header == NNTP_HEADER_PATH ||
							header == NNTP_HEADER_REFERENCES ||
							header == NNTP_HEADER_LINES) {

							Iterator itemIter = items.iterator();
							while (itemIter.hasNext()) {
								Item item = (Item) itemIter.next();
	
								if(!useMessageId) {
									pw.print(item.getArticleNumber());
								} else {
									pw.print("<"
											+ item.getSignature()
											+ "@"
											+ item.getChannel().getName()
											+ ">");
								}
								
								pw.print(' ');
	
								switch(header) {
									case NNTP_HEADER_FROM:
										pw.println(item.getChannel().getAuthor());
										break;
									case NNTP_HEADER_DATE:
										pw.println(df.format(item.getDate()));
										break;
									case NNTP_HEADER_NEWSGROUP:
										pw.println(item.getChannel().getName());
										break;
									case NNTP_HEADER_SUBJECT:
										pw.println(item.getTitle());
										break;
									case NNTP_HEADER_MESSAGE_ID:
										pw.println("<"
												+ item.getSignature()
												+ "@"
												+ item.getChannel().getName()
												+ ">");
										break;
									case NNTP_HEADER_PATH:
										pw.println("nntprss");
										break;
									case NNTP_HEADER_REFERENCES:
										pw.println();
										break;
									case NNTP_HEADER_LINES:
// TODO Calculate actual lines
										pw.println(10);
										break;
									default:
										pw.println();
										break;
								}
							}
						}
						pw.println(".");						
					}
				} else if (command.equalsIgnoreCase("XOVER")) {
					if(currentGroupName == null) {
						pw.println("412 No news group currently selected");
					} else {
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
	
						if(items.size() == 0) {
							pw.println("420 No article(s) selected");
						} else {
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
	//										+ "\tXref: nntprss "
	//										+ item.getChannel().getName()
	//										+ ":"
	//										+ item.getArticleNumber()
											);
								} catch(Exception e) {
									if(log.isEnabledFor(Priority.WARN)) {
										log.warn("Exception thrown in XOVER", 
											e);
									}
	//								e.printStackTrace();
								}
							}
							pw.println(".");
						}
					}
				} else if (command.equalsIgnoreCase("AUTHINFO")) {
					if(parameters.length > 1) {
						if(parameters[1].equalsIgnoreCase("USER") ||
							parameters[1].equalsIgnoreCase("PASS")) {
							pw.println("281 Authentication accepted");
						} else if(parameters[1].equalsIgnoreCase("GENERIC")) {
							pw.println("281 Authentication accepted");
						} else {
							pw.println("500 command not recognized");
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
			pw.println("201 nntp//rss v"  
				+ AppConstants.VERSION
				+ " news server ready - no posting allowed");
			pw.flush();

			processRequestLoop(br, pw);

			br.close();
			pw.close();
			client.close();

		} catch (Exception e) {
			if(!(e instanceof IOException)) {
				if(log.isEnabledFor(Priority.WARN)) {
					log.warn("Unexpected exception thrown",
						e);	
				}
			}
		} finally {
			if(log.isInfoEnabled()) {
				log.info(
					"NNTP Client connection closed");	
			}
		}
	}

}
