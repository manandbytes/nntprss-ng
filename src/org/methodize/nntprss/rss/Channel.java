package org.methodize.nntprss.rss;

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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.rss.db.ChannelManagerDAO;
import org.methodize.nntprss.rss.parser.LooseParser;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.Base64;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;
import sun.net.www.http.HttpClient;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Channel.java,v 1.9 2003/03/22 17:21:27 jasonbrome Exp $
 */
public class Channel implements Runnable {

	public static final int STATUS_OK = 0;
	public static final int STATUS_NOT_FOUND = 1;
	public static final int STATUS_INVALID_CONTENT = 2;
	public static final int STATUS_CONNECTION_TIMEOUT = 3;
	public static final int STATUS_UNKNOWN_HOST = 4;
	public static final int STATUS_NO_ROUTE_TO_HOST = 5;
	public static final int STATUS_SOCKET_EXCEPTION = 6;

	private static final int PUSHBACK_BUFFER_SIZE = 4;

	private static final int HTTP_CONNECTION_TIMEOUT = 
		1000 * 60 * 5;

	private Logger log = Logger.getLogger(Channel.class);

	private String author;
	private String name;
	private URL url;
	private int id;

	private String title;
	private String link;
	private String description;
	
	private Date lastPolled;
	private long lastModified;
	private String lastETag;

	private Date created;

	private int firstArticleNumber = 1;
	private int lastArticleNumber = 0;
	private int totalArticles = 0;
	
	private String rssVersion;
	
	private String managingEditor;
	
	private boolean historical = true;
	private boolean enabled = true;
	private boolean parseAtAllCost = false;


// Publishing related
	private boolean postingEnabled = false;
	private String publishAPI = null;
	private Map publishConfig = null;

	private int status = STATUS_OK;

	private ChannelManager channelManager;
	private ChannelManagerDAO channelManagerDAO;

	private transient boolean polling = false;
	private transient boolean connected = false;

	public static final long DEFAULT_POLLING_INTERVAL = 0;

	private long pollingIntervalSeconds = DEFAULT_POLLING_INTERVAL;

	
	private HttpURLConnection httpCon = null;

	public Channel(String name, String urlString)
		throws MalformedURLException {
		this.name = name;
		this.url = new URL(urlString);

		channelManager = ChannelManager.getChannelManager();
		channelManagerDAO = channelManager.getChannelManagerDAO();
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the url.
	 * @return String
	 */
	public String getUrl() {
		return url.toString();
	}

	private String stripControlChars(String string) {
		StringBuffer strippedString = new StringBuffer();
		for(int charCount = 0; charCount < string.length();
			charCount++) {
			char c = string.charAt(charCount);
			if(c >= 32) {
				strippedString.append(c);
			}
		}
		return strippedString.toString();
	}

	/**
	 * Retrieves the latest RSS doc from the remote site
	 */
	public synchronized void poll() {
// Use method-level variable
// Guard against change in history mid-poll
		polling = true;
		
		boolean keepHistory = historical;

		lastPolled = new Date();

		try {
//			HttpURLConnection httpCon =
			httpCon =
				(HttpURLConnection) url.openConnection();
			httpCon.setDoInput(true);
			httpCon.setDoOutput(false);
			httpCon.setRequestMethod("GET");
			httpCon.setRequestProperty("User-Agent", AppConstants.getUserAgent());
			httpCon.setRequestProperty("Accept-Encoding", "gzip");


			// ETag
			if (lastETag != null) {
				httpCon.setRequestProperty("If-None-Match", lastETag);
			}

			// Last Modified
			if (lastModified != 0) {
				httpCon.setIfModifiedSince(lastModified);
			}

			InputStream is = null;
			try {
				httpCon.connect();
				connected = true;
				is = httpCon.getInputStream();
			} catch(ConnectException ce) {
				if (log.isDebugEnabled()) {
					log.debug(
						"Channel=" + name + " - Connection Timeout, skipping");
				}
				status = STATUS_CONNECTION_TIMEOUT;				
			} catch(UnknownHostException ue) {
				if (log.isDebugEnabled()) {
					log.debug(
						"Channel=" + name + " - Unknown Host Exception, skipping");
				}
				status = STATUS_UNKNOWN_HOST;
			} catch(NoRouteToHostException re) {
				if (log.isDebugEnabled()) {
					log.debug(
						"Channel=" + name + " - No Route To Host Exception, skipping");
				}
				status = STATUS_NO_ROUTE_TO_HOST;
			} catch(SocketException se) {
				if (log.isDebugEnabled()) {
					log.debug(
						"Channel=" + name + " - Socket Exception, skipping");
				}
				status = STATUS_SOCKET_EXCEPTION;
			}

			// Only process if ok - if not ok (e.g. not modified), don't do anything
			if (connected && httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
				String contentEncoding = httpCon.getContentEncoding();
				if(contentEncoding != null && contentEncoding.equals("gzip")) {
					is = new GZIPInputStream(is);
				}

				PushbackInputStream pbis = new PushbackInputStream(is, 
					PUSHBACK_BUFFER_SIZE);
				skipBOM(pbis);
				BufferedInputStream bis = new BufferedInputStream(pbis);
				DocumentBuilder db = AppConstants.newDocumentBuilder();

				try {
					Document rssDoc = null;
					if(!parseAtAllCost) {
						rssDoc = db.parse(bis);
					} else {
// Parse-at-all-costs selected
// Read in document to local array - may need to parse twice
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						int bytesRead = bis.read(buf);
						while(bytesRead > -1) {
							if(bytesRead > 0) {
								bos.write(buf, 0, bytesRead);
							}
							bytesRead = bis.read(buf);
						}
						bos.flush();
						bos.close();
						
						byte[] rssDocBytes = bos.toByteArray();						
						
						try {
// Try the XML document parser first - just in case
// the doc is well-formed
							rssDoc = db.parse(new ByteArrayInputStream(rssDocBytes));
						} catch(SAXParseException spe) {
							if(log.isDebugEnabled()) {
								log.debug("XML parse failed, trying tidy");
							}
// Fallback to parse-at-all-costs parser
							rssDoc = LooseParser.parse(new ByteArrayInputStream(rssDocBytes));
						}
					}
					
					Element rootElm = rssDoc.getDocumentElement();

					if(rootElm.getNodeName().equals("rss")) {
						rssVersion = rootElm.getAttribute("version");
					} else if(rootElm.getNodeName().equals("rdf:RDF")) {
						rssVersion = "RDF";
					}

					Element rssDocElm =
						(Element) rootElm.getElementsByTagName(
							"channel").item(
							0);
	
					// Read header...
					title =
						XMLHelper.getChildElementValue(rssDocElm, "title");
					// XXX Currently assign channelTitle to author
					author = title;

					link =
						XMLHelper.getChildElementValue(rssDocElm, "link");
					description =
						XMLHelper.getChildElementValue(rssDocElm, "description");
					managingEditor = 
						XMLHelper.getChildElementValue(rssDocElm, "managingEditor");
					
						
					// Check for items within channel element and outside 
					// channel element
					NodeList itemList = rssDocElm.getElementsByTagName("item");
					
					if(itemList.getLength() == 0) {
						itemList = rootElm.getElementsByTagName("item");
					}
	
					Calendar retrievalDate = Calendar.getInstance();
					retrievalDate.add(Calendar.SECOND, -itemList.getLength());
	
					// Get current item signatures
					Set currentSignatures =
						channelManagerDAO.getItemSignatures(this.id);

					Set newSignatures = null;
					if(!keepHistory) {
						newSignatures = new HashSet();
					}

					// Calculate signature
					MessageDigest md = MessageDigest.getInstance("MD5");
	
					for (int itemCount = itemList.getLength() - 1;
						itemCount >= 0;
						itemCount--) {
						Element itemElm = (Element) itemList.item(itemCount);
						String title =
							XMLHelper.getChildElementValue(itemElm, "title", "");
						String link =
							XMLHelper.getChildElementValue(itemElm, "link", "");
						String comments =
							XMLHelper.getChildElementValue(itemElm, "comments", "");

						// Fix for content:encoded section of RSS 1.0/2.0
						String description =
						    XMLHelper.getChildElementValue(
						        itemElm,
						        "content:encoded");

						if ((description == null) || (description.length() == 0)) {
						        description =
						            XMLHelper.getChildElementValue(
						                itemElm,
						                "description",
						                "");
						}
	
						String signatureStr = null;
						ByteArrayOutputStream bos = new ByteArrayOutputStream();

						// Used trimmed forms of content, ignore whitespace changes
						bos.write(title.trim().getBytes());
						bos.write(link.trim().getBytes());
						bos.write(description.trim().getBytes());
						bos.flush();
						bos.close();
	
						byte[] signatureSource = bos.toByteArray();
	
						md.reset();
						byte[] signature = md.digest(signatureSource);
	
						signatureStr = Base64.encodeBytes(signature);

						if(!keepHistory) {
							newSignatures.add(signatureStr);
						}
	
						if (!currentSignatures.contains(signatureStr)) {
							// New item, lets add...
							currentSignatures.add(signatureStr);
	
							Item item = new Item(++lastArticleNumber, signatureStr);
							item.setChannel(this);
	
							if (title.length() > 0) {
								item.setTitle(title);
							} else {
								// We need to create a initial title from the description, because
								// we do have a description, don't we???
								String strippedDesc =
									stripControlChars(XMLHelper.stripTags(description));
								int length =
									strippedDesc.length() > 64
										? 64
										: strippedDesc.length();
								item.setTitle(strippedDesc.substring(0, length));
							}
							item.setDescription(description);
							item.setLink(link);
							item.setComments(comments);
	
							// FIXME what to do about date?
							item.setDate(retrievalDate.getTime());
							// Add 1 second - to introduce some distinction date-wise
							// between items
							retrievalDate.add(Calendar.SECOND, 1);
		
							// persist to database...
							channelManagerDAO.saveItem(item);
							totalArticles++;
						}
					}

					if(!keepHistory) {
						currentSignatures.removeAll(newSignatures);
// We're left with the old items that have to be purged...
						if(currentSignatures.size() > 0) {
							channelManagerDAO.deleteItemsBySignature(this, currentSignatures);
							totalArticles -= currentSignatures.size();
						}
					}
	
					// Update last modified / etag from headers
					lastETag = httpCon.getHeaderField("ETag");
					lastModified = httpCon.getHeaderFieldDate("Last-Modified", 0);

					status = STATUS_OK;
				} catch(SAXParseException spe) {
					if(log.isEnabledFor(Priority.WARN)) {
						log.warn("Channel=" + name + " - Error parsing RSS document - check feed");
					}
					status = STATUS_INVALID_CONTENT;
				}
				
				bis.close();

				// end if response code == HTTP_OK
			} else if(connected &&
				httpCon.getResponseCode()
					== HttpURLConnection.HTTP_NOT_MODIFIED) {
				if (log.isDebugEnabled()) {
					log.debug(
						"Channel=" + name + " - HTTP_NOT_MODIFIED, skipping");
				}
			} 

			// Update channel in database...
			channelManagerDAO.updateChannel(this);

			httpCon.disconnect();

		} catch (FileNotFoundException fnfe) {
			if(log.isEnabledFor(Priority.WARN)) {
				log.warn("Channel=" + name + " - File not found returned by web server - check feed");
			}
			status = STATUS_NOT_FOUND;
		} catch (Exception e) {
			if(log.isEnabledFor(Priority.WARN)) {
				log.warn("Channel=" + name + " - Exception while polling channel", e);
			}
		} finally {
			connected = false;
			httpCon = null;
			polling = false;
		}


	}


	/**
	 * Simple channel validation - ensures URL
	 * is valid, XML document is returned, and
	 * document has an rss root element with a 
	 * version, or rdf root element, 
	 */
	public static boolean isValid(URL url) {
		boolean valid = false;
		try {
			HttpURLConnection httpCon =
				(HttpURLConnection) url.openConnection();
			httpCon.setDoInput(true);
			httpCon.setDoOutput(false);
			httpCon.setRequestMethod("GET");
			httpCon.setRequestProperty("User-Agent", AppConstants.getUserAgent());
			httpCon.setRequestProperty("Accept-Encoding", "gzip");
			httpCon.connect();
			InputStream is = httpCon.getInputStream();

			// Only process if ok - if not ok (e.g. not modified), don't do anything
			if (httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
				String contentEncoding = httpCon.getContentEncoding();
				if(contentEncoding != null && contentEncoding.equals("gzip")) {
					is = new GZIPInputStream(is);
				}


				PushbackInputStream pbis = new PushbackInputStream(is, PUSHBACK_BUFFER_SIZE);
				skipBOM(pbis);

				BufferedInputStream bis = new BufferedInputStream(pbis);
				DocumentBuilder db = AppConstants.newDocumentBuilder();
				Document rssDoc = db.parse(bis);
				Element rootElm = rssDoc.getDocumentElement();
				String rssVersion = rootElm.getAttribute("version");
				if((rootElm.getNodeName().equals("rss") && rssVersion != null) ||
					rootElm.getNodeName().equals("rdf:RDF") ) {
					valid = true;
				}
			} 
			
			httpCon.disconnect();

		} catch (Exception e) {
//			e.printStackTrace();
		}
		return valid;
	}


	private static void skipBOM(PushbackInputStream is) throws IOException {
		byte[] header = new byte[PUSHBACK_BUFFER_SIZE];
		int bytesRead = is.read(header);
		if(header[0] == 0 &&
			header[1] == 0 &&
			(header[2]&0xff) == 0xFE &&
			(header[3]&0xff) == 0xFF) {
	// UTF-32, big-endian
		} else if((header[0]&0xff) == 0xFF &&
			(header[1]&0xff) == 0xFE &&
			header[2] == 0 &&
			header[3] == 0) {
	// UTF-32, little-endian
		} else if((header[0]&0xff) == 0xFE &&
			(header[1]&0xff) == 0xFF) {
			is.unread(header, 2, 2);
	// UTF-16, big-endian
		} else if((header[0]&0xff) == 0xFF &&
			(header[1]&0xff) == 0xFE) {
			is.unread(header, 2, 2);
	// UTF-16, little-endian
		} else if((header[0]&0xff) == 0xEF &&
			(header[1]&0xff) == 0xBB &&
			(header[2]&0xff) == 0xBF) {
	// UTF-8
			is.unread(header, 3, 1);
		} else {
			is.unread(header, 0, PUSHBACK_BUFFER_SIZE);
		}
	}
	

	public void save() {
		// Update channel in database...
		channelManagerDAO.updateChannel(this);
	}	

	/**
	 * Validates the channel
	 */
	public boolean isValid() {
		return isValid(url);
	}


	/**
	 * Returns the firstArticleNumber.
	 * @return long
	 */
	public int getFirstArticleNumber() {
		return firstArticleNumber;
	}

	/**
	 * Returns the lastArticleNumber.
	 * @return long
	 */
	public int getLastArticleNumber() {
		return lastArticleNumber;
	}

	/**
	 * Sets the firstArticleNumber.
	 * @param firstArticleNumber The firstArticleNumber to set
	 */
	public void setFirstArticleNumber(int firstArticleNumber) {
		this.firstArticleNumber = firstArticleNumber;
	}

	/**
	 * Sets the lastArticleNumber.
	 * @param lastArticleNumber The lastArticleNumber to set
	 */
	public void setLastArticleNumber(int lastArticleNumber) {
		this.lastArticleNumber = lastArticleNumber;
	}

	/**
	 * Returns the author.
	 * @return String
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Sets the author.
	 * @param author The author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Returns the id.
	 * @return int
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the lastPolled.
	 * @return Date
	 */
	public Date getLastPolled() {
		return lastPolled;
	}

	/**
	 * Sets the lastPolled.
	 * @param lastPolled The lastPolled to set
	 */
	public void setLastPolled(Date lastPolled) {
		this.lastPolled = lastPolled;
	}

	public boolean isAwaitingPoll() {
		// Need intelligent algorithm to handle this...
		// Currently just poll once an hour
		boolean awaitingPoll = false;

		if (lastPolled != null) {
			long currentTimeMillis = System.currentTimeMillis();

			long pollingInterval = this.pollingIntervalSeconds;
			if(pollingInterval == DEFAULT_POLLING_INTERVAL) {
				pollingInterval = channelManager.getPollingIntervalSeconds();
			}

			if ((currentTimeMillis - lastPolled.getTime()) > (pollingInterval * 1000)) {
				awaitingPoll = true;
			}
			
		} else {
			awaitingPoll = true;
		}
				
		return awaitingPoll;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (log.isInfoEnabled()) {
			log.info("Polling channel " + name);
		}

		poll();

		if (log.isInfoEnabled()) {
			log.info("Finished polling channel " + name);
		}

	}

	/**
	 * Returns the lastETag.
	 * @return String
	 */
	public String getLastETag() {
		return lastETag;
	}

	/**
	 * Returns the lastModified.
	 * @return long
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Sets the lastETag.
	 * @param lastETag The lastETag to set
	 */
	public void setLastETag(String lastETag) {
		this.lastETag = lastETag;
	}

	/**
	 * Sets the lastModified.
	 * @param lastModified The lastModified to set
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Returns the created.
	 * @return Date
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * Sets the created.
	 * @param created The created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}

	/**
	 * Returns the rssVersion.
	 * @return String
	 */
	public String getRssVersion() {
		return rssVersion;
	}

	/**
	 * Sets the rssVersion.
	 * @param rssVersion The rssVersion to set
	 */
	public void setRssVersion(String rssVersion) {
		this.rssVersion = rssVersion;
	}

	/**
	 * Sets the url.
	 * @param url The url to set
	 */
	public void setUrl(URL url) {
		if(!this.url.equals(url)) {
			this.url = url;

// If we change the URL, then reset the 
// polling characteristics associated with the channel
			this.lastModified = 0;
			this.lastETag = null;
			this.lastPolled = null;
		}

	}

	/**
	 * Returns the historical.
	 * @return boolean
	 */
	public boolean isHistorical() {
		return historical;
	}

	/**
	 * Sets the historical.
	 * @param historical The historical to set
	 */
	public void setHistorical(boolean historical) {
		this.historical = historical;
	}

	/**
	 * Returns the totalArticles.
	 * @return int
	 */
	public int getTotalArticles() {
		return totalArticles;
	}

	/**
	 * Sets the totalArticles.
	 * @param totalArticles The totalArticles to set
	 */
	public void setTotalArticles(int totalArticles) {
		this.totalArticles = totalArticles;
	}

	/**
	 * Returns the status.
	 * @return int
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the status.
	 * @param status The status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the description.
	 * @return String
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the link.
	 * @return String
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the description.
	 * @param description The description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Sets the link.
	 * @param link The link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the enabled.
	 * @return boolean
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the enabled.
	 * @param enabled The enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the managingEditor.
	 * @return String
	 */
	public String getManagingEditor() {
		return managingEditor;
	}

	/**
	 * Sets the managingEditor.
	 * @param managingEditor The managingEditor to set
	 */
	public void setManagingEditor(String managingEditor) {
		this.managingEditor = managingEditor;
	}

	/**
	 * Returns the postingEnabled.
	 * @return boolean
	 */
	public boolean isPostingEnabled() {
		return postingEnabled;
	}

	/**
	 * Sets the postingEnabled.
	 * @param postingEnabled The postingEnabled to set
	 */
	public void setPostingEnabled(boolean postingEnabled) {
		this.postingEnabled = postingEnabled;
	}

	/**
	 * Returns the parseAtAllCost.
	 * @return boolean
	 */
	public boolean isParseAtAllCost() {
		return parseAtAllCost;
	}

	/**
	 * Sets the parseAtAllCost.
	 * @param parseAtAllCost The parseAtAllCost to set
	 */
	public void setParseAtAllCost(boolean parseAtAllCost) {
		this.parseAtAllCost = parseAtAllCost;
	}

	/**
	 * Returns the publishAPI.
	 * @return String
	 */
	public String getPublishAPI() {
		return publishAPI;
	}

	/**
	 * Sets the publishAPI.
	 * @param publishAPI The publishAPI to set
	 */
	public void setPublishAPI(String publishAPI) {
		this.publishAPI = publishAPI;
	}

	/**
	 * Returns the polling.
	 * @return boolean
	 */
	public boolean isPolling() {
		return polling;
	}
	
	/**
	 * Checks any current http connection.
	 * 
	 * Called from the Channel Poller's thread - will invoke
	 * disconnect on the HttpUrlConnection is the poll has
	 * exceeded 5 minutes.  This'll cause an exception to
	 * be thrown in the polling thread, within the poll method.
	 * 
	 * It'd be preferable to use the timeout setting feature
	 * within HttpUrlConnection, but this is only a parameter
	 * in the 1.4 release of the JRE.
	 */
	
	public void checkConnection() {
		if(polling && httpCon != null && connected &&
			((lastPolled == null) ||
			((System.currentTimeMillis() - lastPolled.getTime()) >
				HTTP_CONNECTION_TIMEOUT)
			)) {
			try {
				if(log.isDebugEnabled()) {
					log.debug("Timeout exceeded, attempting to disconnect HttpUrlConnection");
				}

				httpCon.disconnect();
				connected = false;
			} catch(Exception e) {
				if(log.isDebugEnabled()) {
					log.debug("Error disconnecting HttpUrlConnection in checkConnection");
				}
			} finally {
//				httpCon = null;
			}
		}
	}

	/**
	 * Returns the publishConfig.
	 * @return Map
	 */
	public Map getPublishConfig() {
		return publishConfig;
	}

	/**
	 * Sets the publishConfig.
	 * @param publishConfig The publishConfig to set
	 */
	public void setPublishConfig(Map publishConfig) {
		this.publishConfig = publishConfig;
	}

	/**
	 * Returns the pollingIntervalSeconds.
	 * @return long
	 */
	public long getPollingIntervalSeconds() {
		return pollingIntervalSeconds;
	}

	/**
	 * Sets the pollingIntervalSeconds.
	 * @param pollingIntervalSeconds The pollingIntervalSeconds to set
	 */
	public void setPollingIntervalSeconds(long pollingIntervalSeconds) {
		this.pollingIntervalSeconds = pollingIntervalSeconds;
	}

}
