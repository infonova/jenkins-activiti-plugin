package com.bearingpoint.infonova.jenkins.ui;

/**
 * Stores the activity coordinates information.
 * 
 * @author christian.weber
 * @since 1.0
 */
public interface Coordinates {

	/**
	 * Returns the {@link Shape} to render.
	 * 
	 * @return UIShape
	 */
	Shape getShape();

	/**
	 * Returns the coordinates string.
	 * 
	 * @return String
	 */
	String getCoordinates();

}
