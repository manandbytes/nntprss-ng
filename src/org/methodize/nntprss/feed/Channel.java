package org.methodize.nntprss.feed;

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
import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PushbackInputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.methodize.nntprss.feed.db.ChannelDAO;
import org.methodize.nntprss.feed.parser.AtomParser;
import org.methodize.nntprss.feed.parser.GenericParser;
import org.methodize.nntprss.feed.parser.LooseParser;
import org.methodize.nntprss.feed.parser.RSSParser;
import org.methodize.nntprss.util.AppConstants;
import org.methodize.nntprss.util.HttpUserException;
import org.methodize.nntprss.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Channel.java,v 1.12 2004/09/05 17:12:27 aslom Exp $
 */
public class Channel
    extends ItemContainer
    implements Runnable, Externalizable {

    public static final int EXTERNAL_VERSION = 2;

    public static final int STATUS_OK = 0;
    public static final int STATUS_NOT_FOUND = 1;
    public static final int STATUS_INVALID_CONTENT = 2;
    public static final int STATUS_CONNECTION_TIMEOUT = 3;
    public static final int STATUS_UNKNOWN_HOST = 4;
    public static final int STATUS_NO_ROUTE_TO_HOST = 5;
    public static final int STATUS_SOCKET_EXCEPTION = 6;
    public static final int STATUS_PROXY_AUTHENTICATION_REQUIRED = 7;
    public static final int STATUS_USER_AUTHENTICATION_REQUIRED = 8;

    public static final long EXPIRATION_KEEP = -1;

    private static final int PUSHBACK_BUFFER_SIZE = 4;

    private Logger log = Logger.getLogger(Channel.class);

    private String author;
    private URL url;
    private int id;
    private String title;
    private String link;
    private String description;
    private Date lastPolled;
    private Date lastCleaned;
    private long lastModified;
    private String lastETag;
    private String rssVersion;
    private String managingEditor;

    //  private boolean historical = true;
    private boolean enabled = true;
    private boolean parseAtAllCost = false;

    private long expiration = EXPIRATION_KEEP;

    // Publishing related
    private boolean postingEnabled = false;
    private String publishAPI = null;
    private Map publishConfig = null;

    private int status = STATUS_OK;
    private long pollingIntervalSeconds = DEFAULT_POLLING_INTERVAL;

    private ChannelManager channelManager;
    private ChannelDAO channelDAO;
    private Category category = null;

    private transient boolean polling = false;
    private transient boolean connected = false;

    public static final long DEFAULT_POLLING_INTERVAL = 0;
    // Clean channels every 24 hours
    public static final long CLEANING_INTERVAL = 1000 * 60 * 60 * 24;
    private static final int HTTP_CONNECTION_TIMEOUT = 1000 * 60 * 5;

    //  private HttpURLConnection httpCon = null;

    private HttpClient httpClient = null;
    private SimpleDateFormat httpDate =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    // Channel Doc Parsers
    private static GenericParser[] parsers =
        new GenericParser[] { RSSParser.getParser(), AtomParser.getParser()};

    public Channel() {
    }

    public Channel(String name, String urlString)
        throws MalformedURLException {
        this.name = name;
        this.url = new URL(urlString);
        initialize();
    }

    private void initialize() {
        channelManager = ChannelManager.getChannelManager();
        channelDAO = channelManager.getChannelDAO();
        //              httpClient = channelManager.getHttpClient();
        //              httpClient = new HttpClient();

        httpClient = new HttpClient(channelManager.getHttpConMgr());
        httpClient.setConnectionTimeout(HTTP_CONNECTION_TIMEOUT);
        httpClient.setTimeout(HTTP_CONNECTION_TIMEOUT);

        // Initialize user id / password for protected feeds
        if (url.getUserInfo() != null) {
            httpClient.getState().setCredentials(
                null,
                null,
                new UsernamePasswordCredentials(url.getUserInfo()));
        }

        TimeZone gmt = TimeZone.getTimeZone("GMT");
        httpDate.setTimeZone(gmt);
    }

    /**
     * Returns the url.
     * @return String
     */
    public String getUrl() {
        return url.toString();
    }

    private HttpClient getHttpClient() {
        HttpClient httpClient = new HttpClient();
        httpClient.setConnectionTimeout(HTTP_CONNECTION_TIMEOUT);
        httpClient.setTimeout(HTTP_CONNECTION_TIMEOUT);

        // Initialize user id / password for protected feeds
        if (url.getUserInfo() != null) {
            httpClient.getState().setCredentials(
                null,
                null,
                new UsernamePasswordCredentials(url.getUserInfo()));
        }
        return httpClient;

    }

    /**
     * Retrieves the latest RSS doc from the remote site
     */
    public synchronized void poll() {
        // Use method-level variable
        // Guard against change in history mid-poll
        polling = true;

        //              boolean keepHistory = historical;
        long keepExpiration = expiration;

        lastPolled = new Date();

        int statusCode = -1;
        HttpMethod method = null;
        String urlString = url.toString();
        try {
            HttpClient httpClient = getHttpClient();
            channelManager.configureHttpClient(httpClient);
            HttpResult result = null;

            try {

                connected = true;
                boolean redirected = false;
                int count = 0;
                do {
                    //                                  URL url = new URL(urlString);
                    method = new GetMethod(urlString);
                    method.setRequestHeader(
                        "User-agent",
                        AppConstants.getUserAgent());
                    method.setRequestHeader("Accept-Encoding", "gzip");
                    method.setFollowRedirects(false);
                    method.setDoAuthentication(true);

                    // ETag
                    if (lastETag != null) {
                        method.setRequestHeader("If-None-Match", lastETag);
                    }

                    // Last Modified
                    if (lastModified != 0) {
                        final String NAME = "If-Modified-Since";
                        //defend against such fun like net.freeroller.rickard got If-Modified-Since "Thu, 24 Aug 2028 12:29:54 GMT"
                        if(lastModified < System.currentTimeMillis()) {
                            final String DATE = httpDate.format(new Date(lastModified));
                            method.setRequestHeader(NAME, DATE);
                            log.debug("channel "+this.name+" using "+NAME+" "+DATE); //ALEK
                        }
                    }

                    method.setFollowRedirects(false);
                    method.setDoAuthentication(true);

                    HostConfiguration hostConfig = new HostConfiguration();
                    hostConfig.setHost(
                        url.getHost(),
                        url.getPort(),
                        url.getProtocol());

                    result = executeHttpRequest(httpClient, hostConfig, method);
                    statusCode = result.getStatusCode();
                    if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                        || statusCode == HttpStatus.SC_MOVED_TEMPORARILY
                        || statusCode == HttpStatus.SC_SEE_OTHER
                        || statusCode == HttpStatus.SC_TEMPORARY_REDIRECT) {

                        redirected = true;
                        urlString = result.getLocation();
                        if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                            && channelManager.isObserveHttp301()) {
                            try {
                                url = new URL(urlString);
                                if (log.isInfoEnabled()) {
                                    log.info(
                                        "Channel = "
                                            + this.name
                                            + ", updated URL from HTTP Permanent Redirect");
                                }
                            } catch (MalformedURLException mue) {
                                // Ignore URL permanent redirect for now...
                            }
                        }
                    } else {
                        redirected = false;
                    }

                    //                                  method.getResponseBody();
                    //                                  method.releaseConnection();
                    count++;
                } while (count < 5 && redirected);

            } catch (HttpRecoverableException hre) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Channel="
                            + name
                            + " - Temporary Http Problem - "
                            + hre.getMessage());
                }
                status = STATUS_CONNECTION_TIMEOUT;
                statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            } catch (ConnectException ce) {
                // @TODO Might also be a connection refused - not only a timeout...
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Channel="
                            + name
                            + " - Connection Timeout, skipping - "
                            + ce.getMessage());
                }
                status = STATUS_CONNECTION_TIMEOUT;
                statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            } catch (UnknownHostException ue) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Channel="
                            + name
                            + " - Unknown Host Exception, skipping");
                }
                status = STATUS_UNKNOWN_HOST;
                statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            } catch (NoRouteToHostException re) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Channel="
                            + name
                            + " - No Route To Host Exception, skipping");
                }
                status = STATUS_NO_ROUTE_TO_HOST;
                statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            } catch (SocketException se) {
                // e.g. Network is unreachable
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Channel=" + name + " - Socket Exception, skipping");
                }
                status = STATUS_SOCKET_EXCEPTION;
                statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }

            // Only process if ok - if not ok (e.g. not modified), don't do anything
            if (connected && statusCode == HttpStatus.SC_OK) {

                PushbackInputStream pbis =
                    new PushbackInputStream(
                        new ByteArrayInputStream(result.getResponse()),
                        PUSHBACK_BUFFER_SIZE);
                skipBOM(pbis);
                BufferedInputStream bis = new BufferedInputStream(pbis);
                DocumentBuilder db = AppConstants.newDocumentBuilder();

                try {
                    Document rssDoc = null;
                    if (!parseAtAllCost) {
                        try {
                            rssDoc = db.parse(bis);
                        } catch (InternalError ie) {
                            // Crimson library throws InternalErrors
                            if (log.isDebugEnabled()) {
                                log.debug(
                                    "InternalError thrown by Crimson",
                                    ie);
                            }
                            throw new SAXException(
                                "InternalError thrown by Crimson: "
                                    + ie.getMessage());
                        }
                    } else {
                        // Parse-at-all-costs selected
                        // Read in document to local array - may need to parse twice
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        int bytesRead = bis.read(buf);
                        while (bytesRead > -1) {
                            if (bytesRead > 0) {
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
                            rssDoc =
                                db.parse(new ByteArrayInputStream(rssDocBytes));
                        } catch (SAXParseException spe) {
                            if (log.isDebugEnabled()) {
                                log.debug("XML parse failed, trying tidy");
                            }
                            // Fallback to parse-at-all-costs parser
                            rssDoc =
                                LooseParser.parse(
                                    new ByteArrayInputStream(rssDocBytes));
                        }
                    }

                    processChannelDocument(expiration, rssDoc);

                    // Update last modified / etag from headers
                    //                                  lastETag = httpCon.getHeaderField("ETag");
                    //                                  lastModified = httpCon.getHeaderFieldDate("Last-Modified", 0);

                    Header hdrETag = method.getResponseHeader("ETag");
                    lastETag = hdrETag != null ? hdrETag.getValue() : null;

                    Header hdrLastModified =
                        method.getResponseHeader("Last-Modified");
                    lastModified =
                        hdrLastModified != null
                            ? parseHttpDate(hdrLastModified.getValue())
                            : 0;
                    log.debug("channel "+this.name+" parsed Last-Modifed "+hdrLastModified+" to "
                                  +(lastModified != 0 ? ""+(new Date(lastModified)) : ""+lastModified)); //ALEK
                    
                    status = STATUS_OK;
                } catch (SAXParseException spe) {
                    if (log.isEnabledFor(Priority.WARN)) {
                        log.warn(
                            "Channel="
                                + name
                                + " - Error parsing RSS document - check feed");
                    }
                    status = STATUS_INVALID_CONTENT;
                }

                bis.close();

                // end if response code == HTTP_OK
            } else if (connected && statusCode == HttpStatus.SC_NOT_MODIFIED) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Channel=" + name + " - HTTP_NOT_MODIFIED, skipping");
                }
                status = STATUS_OK;
            } else if (
                statusCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                if (log.isEnabledFor(Priority.WARN)) {
                    log.warn(
                        "Channel=" + name + " - Proxy authentication required");
                }
                status = STATUS_PROXY_AUTHENTICATION_REQUIRED;
            } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                if (log.isEnabledFor(Priority.WARN)) {
                    log.warn("Channel=" + name + " - Authentication required");
                }
                status = STATUS_USER_AUTHENTICATION_REQUIRED;
            }

            // Update channel in database...
            channelDAO.updateChannel(this);

        } catch (FileNotFoundException fnfe) {
            if (log.isEnabledFor(Priority.WARN)) {
                log.warn(
                    "Channel="
                        + name
                        + " - File not found returned by web server - check feed");
            }
            status = STATUS_NOT_FOUND;
        } catch (Exception e) {
            if (log.isEnabledFor(Priority.WARN)) {
                log.warn(
                    "Channel=" + name + " - Exception while polling channel",
                    e);
            }
        } catch (NoClassDefFoundError ncdf) {
            // Throw if SSL / redirection to HTTPS
            if (log.isEnabledFor(Priority.WARN)) {
                log.warn("Channel=" + name + " - NoClassDefFound", ncdf);
            }
        } finally {
            connected = false;
            polling = false;
        }

    }

    private void processChannelDocument(long keepExpiration, Document rssDoc)
        throws NoSuchAlgorithmException, IOException {
        Element rootElm = rssDoc.getDocumentElement();

        GenericParser docParser = null;
        for (int i = 0; i < parsers.length; i++) {
            if (parsers[i].isParsable(rootElm)) {
                docParser = parsers[i];
                break;
            }
        }

        if (docParser != null) {
            rssVersion = docParser.getFormatVersion(rootElm);
            docParser.extractFeedInfo(rootElm, this);
            docParser.processFeedItems(
                rootElm,
                this,
                channelDAO,
                keepExpiration != 0);
            channelDAO.updateChannel(this); //ALEK
        } // end if docParser != null

    }

    private long parseHttpDate(String dateString) {
        long time = 0;
        try {
            time = httpDate.parse(dateString).getTime();
        } catch (ParseException pe) {
            // Ignore date if parse error
        }
        return time;
    }

    /**
     * Simple channel validation - ensures URL
     * is valid, XML document is returned, and
     * document has an rss root element with a
     * version, or rdf root element,
     */
    public static boolean isValid(URL url) throws HttpUserException {
        boolean valid = false;
        try {
            //                  System.setProperty("networkaddress.cache.ttl", "0");

            HttpClient client = new HttpClient();
            ChannelManager.getChannelManager().configureHttpClient(client);
            if (url.getUserInfo() != null) {
                client.getState().setCredentials(
                    null,
                    null,
                    new UsernamePasswordCredentials(url.getUserInfo()));
            }

            String urlString = url.toString();
            HttpMethod method = null;

            int count = 0;
            HttpResult result = null;
            int statusCode = HttpStatus.SC_OK;
            boolean redirected = false;
            do {
                method = new GetMethod(urlString);
                method.setRequestHeader(
                    "User-agent",
                    AppConstants.getUserAgent());
                method.setRequestHeader("Accept-Encoding", "gzip");
                method.setFollowRedirects(false);
                method.setDoAuthentication(true);

                HostConfiguration hostConfiguration =
                    client.getHostConfiguration();
                hostConfiguration.setHost(new URI(urlString));

                result = executeHttpRequest(client, hostConfiguration, method);
                statusCode = result.getStatusCode();
                if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                    || statusCode == HttpStatus.SC_MOVED_TEMPORARILY
                    || statusCode == HttpStatus.SC_SEE_OTHER
                    || statusCode == HttpStatus.SC_TEMPORARY_REDIRECT) {
                    redirected = true;
                    urlString = result.getLocation();
                } else {
                    redirected = false;
                }

                //                              method.getResponseBody();
                //                              method.releaseConnection();
                count++;
            } while (count < 5 && redirected);

            // Only process if ok - if not ok (e.g. not modified), don't do anything
            if (statusCode == HttpStatus.SC_OK) {
                PushbackInputStream pbis =
                    new PushbackInputStream(
                        new ByteArrayInputStream(result.getResponse()),
                        PUSHBACK_BUFFER_SIZE);
                skipBOM(pbis);

                BufferedInputStream bis = new BufferedInputStream(pbis);
                DocumentBuilder db = AppConstants.newDocumentBuilder();
                Document rssDoc = db.parse(bis);
                Element rootElm = rssDoc.getDocumentElement();

                for (int i = 0; i < parsers.length; i++) {
                    if (parsers[i].isParsable(rootElm)) {
                        valid = true;
                        break;
                    }
                }
            } else if (
                statusCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                throw new HttpUserException(statusCode);
            } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new HttpUserException(statusCode);
            }

        } catch (URIException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return valid;
    }

    private static void skipBOM(PushbackInputStream is) throws IOException {
        byte[] header = new byte[PUSHBACK_BUFFER_SIZE];
        int bytesRead = is.read(header);
        if (header[0] == 0
            && header[1] == 0
            && (header[2] & 0xff) == 0xFE
            && (header[3] & 0xff) == 0xFF) {
            // UTF-32, big-endian
        } else if (
            (header[0] & 0xff) == 0xFF
                && (header[1] & 0xff) == 0xFE
                && header[2] == 0
                && header[3] == 0) {
            // UTF-32, little-endian
        } else if ((header[0] & 0xff) == 0xFE && (header[1] & 0xff) == 0xFF) {
            is.unread(header, 2, 2);
            // UTF-16, big-endian
        } else if ((header[0] & 0xff) == 0xFF && (header[1] & 0xff) == 0xFE) {
            is.unread(header, 2, 2);
            // UTF-16, little-endian
        } else if (
            (header[0] & 0xff) == 0xEF
                && (header[1] & 0xff) == 0xBB
                && (header[2] & 0xff) == 0xBF) {
            // UTF-8
            is.unread(header, 3, 1);
        } else {
            is.unread(header, 0, PUSHBACK_BUFFER_SIZE);
        }
    }

    public void save() {
        // Update channel in database...
        channelDAO.updateChannel(this);
    }

    /**
     * Validates the channel
     */
    public boolean isValid() throws HttpUserException {
        return isValid(url);
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
            if (pollingInterval == DEFAULT_POLLING_INTERVAL) {
                pollingInterval = channelManager.getPollingIntervalSeconds();
            }

            if ((currentTimeMillis - lastPolled.getTime())
                > (pollingInterval * 1000)) {
                awaitingPoll = true;
            } else if (lastPolled.getTime() > currentTimeMillis) {
                // Sanity date check - if the last polling time is greater than the
                // current time, assume that there was an issue with the system clock,
                // and repoll
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

        if (!polling)
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
        if (this.url == null || !this.url.equals(url)) {
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
    //  public boolean isHistorical() {
    //          return historical;
    //  }

    /**
     * Sets the historical.
     * @param historical The historical to set
     */
    //  public void setHistorical(boolean historical) {
    //          this.historical = historical;
    //  }

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

    /**
     * Executes HTTP request
     * @param client
     * @param config
     * @param method
     */

    private static HttpResult executeHttpRequest(
        HttpClient client,
        HostConfiguration config,
        HttpMethod method)
        throws HttpException, IOException {

        HttpResult result;
        int statusCode = -1;
        int attempt = 0;

        try {
            statusCode = client.executeMethod(config, method);

            //          while (statusCode == -1 && attempt < 3) {
            //                  try {
            //                          // execute the method.
            //                          statusCode = client.executeMethod(config, method);
            //                  } catch (HttpRecoverableException e) {
            //                          log.(
            //                                  "A recoverable exception occurred, retrying."
            //                                          + e.getMessage());
            //                          method.releaseConnection();
            //                          method.recycle();
            //                          try {
            //                                  Thread.sleep(250);
            //                          } catch(InterruptedException ie) {
            //                          }
            //                  }
            //          }

            result = new HttpResult(statusCode);
            Header locationHeader = method.getResponseHeader("location");
            if (locationHeader != null) {
                result.setLocation(locationHeader.getValue());
            }

            if (statusCode == HttpStatus.SC_OK) {
                Header contentEncoding =
                    method.getResponseHeader("Content-Encoding");
                if (contentEncoding != null
                    && contentEncoding.getValue().equals("gzip")) {
                    InputStream is = method.getResponseBodyAsStream();
                    is = new GZIPInputStream(is);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buf)) > -1) {
                        if (bytesRead > 0) {
                            baos.write(buf, 0, bytesRead);
                        }
                    }
                    baos.flush();
                    baos.close();
                    is.close();
                    result.setResponse(baos.toByteArray());
                } else {
                    result.setResponse(method.getResponseBody());
                }
            } else {
                // Process response
                InputStream is = method.getResponseBodyAsStream();
                if (is != null) {
                    byte[] buf = new byte[1024];
                    while (is.read(buf) != -1);
                    is.close();
                }
                //      result.setResponse(method.getResponseBody());
            }

            return result;
        } finally {
            method.releaseConnection();
        }
    }

    private static class HttpResult {

        private int statusCode;
        private byte[] response;
        private String location;

        public HttpResult(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public byte[] getResponse() {
            return response;
        }

        public void setResponse(byte[] response) {
            this.response = response;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        in.readInt();
        author = in.readUTF();
        name = in.readUTF();
        url = new URL(in.readUTF());
        id = in.readInt();
        title = in.readUTF();
        link = in.readUTF();
        description = in.readUTF();
        lastPolled = new Date(in.readLong());
        lastCleaned = new Date(in.readLong());
        lastModified = in.readLong();
        lastETag = in.readUTF();
        created = new Date(in.readLong());
        firstArticleNumber = in.readInt();
        lastArticleNumber = in.readInt();
        totalArticles = in.readInt();
        rssVersion = in.readUTF();
        managingEditor = in.readUTF();

        //                      historical = in.readBoolean();
        //                      in.readBoolean();

        enabled = in.readBoolean();
        parseAtAllCost = in.readBoolean();

        postingEnabled = in.readBoolean();
        publishAPI = in.readUTF();
        publishConfig = XMLHelper.xmlToStringHashMap(in.readUTF());
        status = in.readInt();
        pollingIntervalSeconds = in.readLong();

        expiration = in.readLong();
        int categoryId = in.readInt();

        initialize();

        if (categoryId != 0) {
            category = channelManager.categoryById(categoryId);
            if (category != null) {
                category.getChannels().put(new Integer(id), this);
            } else {
                // If category not found, reset to zero
                categoryId = 0;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(EXTERNAL_VERSION);
        out.writeUTF(author != null ? author : "");
        out.writeUTF(name != null ? name : "");
        out.writeUTF(url.toString());
        out.writeInt(id);
        out.writeUTF(title != null ? title : "");
        out.writeUTF(link != null ? link : "");
        out.writeUTF(description != null ? description : "");
        out.writeLong(lastPolled != null ? lastPolled.getTime() : 0);
        out.writeLong(lastCleaned != null ? lastCleaned.getTime() : 0);
        out.writeLong(lastModified);
        out.writeUTF(lastETag != null ? lastETag : "");
        out.writeLong(created != null ? created.getTime() : 0);
        out.writeInt(firstArticleNumber);
        out.writeInt(lastArticleNumber);
        out.writeInt(totalArticles);
        out.writeUTF(rssVersion != null ? rssVersion : "");
        out.writeUTF(managingEditor != null ? managingEditor : "");

        //              out.writeBoolean(historical);
        out.writeBoolean(enabled);
        out.writeBoolean(parseAtAllCost);

        out.writeBoolean(postingEnabled);
        out.writeUTF(publishAPI != null ? publishAPI : "");
        out.writeUTF(XMLHelper.stringMapToXML(publishConfig));
        out.writeInt(status);
        out.writeLong(pollingIntervalSeconds);

        out.writeLong(expiration);
        out.writeInt(category != null ? category.getId() : 0);
    }

    /**
     * @return
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * @param l
     */
    public void setExpiration(long l) {
        expiration = l;
    }

    /**
     * @return
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @param category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * @return
     */
    public Date getLastCleaned() {
        return lastCleaned;
    }

    /**
     * @param date
     */
    public void setLastCleaned(Date date) {
        lastCleaned = date;
    }

    public String toString() {
        return "{Channel"
            + " author="
            + author
            + " url="
            + url
            + " id="
            + id
            + " title="
            + title
            + " link="
            + link
            + " description="
            + description
            + " lastPolled="
            + lastPolled
            + " lastCleaned="
            + lastCleaned
            + " lastModified="
            + lastModified+" ("+new Date(lastModified)+")"
            + " lastETag="
            + lastETag
            + " rssVersion="
            + rssVersion
            + " managingEditor="
            + managingEditor
            + " lastArticleNumber="
            + lastArticleNumber
            + " expiration="
            + expiration
            + " enabled="
            + enabled
            + " parseAtAllCost="
            + parseAtAllCost
        //+" publishConfig
                +"}";
    }
}



