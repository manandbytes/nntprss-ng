/*
 * Created on Jul 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.methodize.nntprss.feed.parser;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.methodize.nntprss.feed.Channel;
import org.methodize.nntprss.feed.db.ChannelManagerDAO;
import org.w3c.dom.Element;

/**
 * @author jason
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class GenericParser {

	public abstract boolean isParsable(Element docRootElement);
	public abstract String getFormatVersion(Element docRootElement);	
	public abstract void extractFeedInfo(Element docRootElement, Channel channel);
	public abstract void processFeedItems(
		Element rootElm,
		Channel channel,
		ChannelManagerDAO channelManagerDAO,
		boolean keepHistory) throws NoSuchAlgorithmException, IOException ;

	String stripControlChars(String string) {
		StringBuffer strippedString = new StringBuffer();
		for (int charCount = 0; charCount < string.length(); charCount++) {
			char c = string.charAt(charCount);
			if (c >= 32) {
				strippedString.append(c);
			}
		}
		return strippedString.toString();
	}	
}