package com.bearingpoint.infonova.jenkins.ui;

// TODO: javadoc
public interface Linkable {

	/**
	 * Returns the link which can be absolute or relative. A relative link
	 * starts with a leading / and represents a link which is relative to the
	 * jenkins root URL. If the link starts with an # than the same page
	 * will be displayed again.
	 * 
	 * @return String
	 */
	String getLink();
	
}
