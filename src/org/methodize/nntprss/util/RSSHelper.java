package org.methodize.nntprss.util;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version 
 */
public class RSSHelper {

	/**
	 * Parses RSS email string to return email
	 * 
	 * Typically:
	 * 
	 * someone@someone.com (My name) 
	 */
	public static String parseEmail(String email) {
		String parsedEmail = null;
		
		int spacePos = email.indexOf(' ');
		
		if(spacePos == -1) {
			parsedEmail = email;
		} else {
			parsedEmail = email.substring(0, spacePos);
		}
		
		return parsedEmail;
		
	}
}
