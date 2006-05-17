package org.methodize.nntprss.feed;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2006 Jason Brome.  All Rights Reserved.
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: Item.java,v 1.10 2006/05/17 04:13:17 jasonbrome Exp $
 */
public class Item implements Externalizable, Cloneable {

    public static final int VERSION_UTF_STRING = 1;
    public static final int EXTERNAL_VERSION = 2;

    private int articleNumber;
    private String signature;
    private String title;
    private String description;
    private String link;
    private Date date;
    private String comments;
    private String creator;
    private Channel channel;
    private String guid;
    private boolean guidIsPermaLink = true;

    public Item() {
    }

    public Item(int articleNumber, String signature) {
        this.articleNumber = articleNumber;
        this.signature = signature;
    }
    /**
     * Returns the articleNumber.
     * @return int
     */
    public int getArticleNumber() {
        return articleNumber;
    }

    /**
     * Returns the signature.
     * @return String
     */
    public String getSignature() {
        return signature;
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
     * Returns the date.
     * @return Date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets the date.
     * @param date The date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Sets the articleNumber.
     * @param articleNumber The articleNumber to set
     */
    public void setArticleNumber(int articleNumber) {
        this.articleNumber = articleNumber;
    }

    /**
     * Sets the signature.
     * @param signature The signature to set
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Returns the channel.
     * @return Channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets the channel.
     * @param channel The channel to set
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Returns the comments.
     * @return String
     */
    public String getComments() {
        return comments;
    }

    /**
     * Sets the comments.
     * @param comments The comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * @return
     */
    public String getCreator() {
        return creator;
    }

    /**
     * @param string
     */
    public void setCreator(String string) {
        creator = string;
    }

    /**
     * @return
     */
    public String getGuid() {
        return guid;
    }

    /**
     * @return
     */
    public boolean isGuidIsPermaLink() {
        return guidIsPermaLink;
    }

    /**
     * @param string
     */
    public void setGuid(String string) {
        guid = string;
    }

    /**
     * @param b
     */
    public void setGuidIsPermaLink(boolean b) {
        guidIsPermaLink = b;
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {

        // Version
        int version = in.readInt();

        articleNumber = in.readInt();
        signature = in.readUTF();
        title = in.readUTF();

        if (version == VERSION_UTF_STRING) {
            description = in.readUTF();
        } else {
            description = (String) in.readObject();
        }

        link = in.readUTF();
        date = new Date(in.readLong());
        comments = in.readUTF();
        creator = in.readUTF();

        // skip channel...
        in.readInt();

        guid = in.readUTF();
        guidIsPermaLink = in.readBoolean();
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        //		Version
        out.writeInt(EXTERNAL_VERSION);
        out.writeInt(articleNumber);
        out.writeUTF(signature != null ? signature : "");
        out.writeUTF(title != null ? title : "");
        out.writeObject(description != null ? description : "");
        out.writeUTF(link != null ? link : "");
        out.writeLong(date != null ? date.getTime() : 0);
        out.writeUTF(comments != null ? comments : "");
        out.writeUTF(creator != null ? creator : "");
        out.writeInt(channel.getId());
        out.writeUTF(guid != null ? guid : "");
        out.writeBoolean(guidIsPermaLink);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        String d = "";
        if (description != null && description.length() > 80) {
            d =
                description.substring(0, 80)
                    + " <length="
                    + description.length()
                    + ">";
        }
        return "{Item"
            + " articleNumber="
            + articleNumber
            + " signature="
            + signature
            + " title="
            + title
            + " link="
            + link
            + " date="
            + date
            + " comments="
            + comments
            + " creator="
            + creator
            + " channel="
            + channel.getName()
            + " guid="
            + guid
            + " guidIsPermaLink="
            + guidIsPermaLink
            + " description="
            + d
            + "}";
    }

}
