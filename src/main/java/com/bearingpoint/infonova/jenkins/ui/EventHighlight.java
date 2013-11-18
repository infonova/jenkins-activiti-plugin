package com.bearingpoint.infonova.jenkins.ui;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractCoordinates} implementation for event highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("event")
public class EventHighlight extends AbstractArea {

	private final int x;
	
	private final int y;
	
	private final int radius;
	
	public EventHighlight(String processId, String activityId, int x, int y, int radius) {
		super(processId, activityId);
		this.x = x;
		this.y = y;
		this.radius = radius;
	}

	/**
	 * {@inheritDoc}
	 */
	public Shape getShape() {
		return Shape.CIRCLE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getCoordinates() {
		return String.format("%d, %d, %d", x, y, radius);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityType getActivityType() {
		return ActivityType.EVENT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIconFileName() {
		return "/image/Event.png";
	}
	
}
