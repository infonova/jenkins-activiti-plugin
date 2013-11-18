package com.bearingpoint.infonova.jenkins.ui;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractCoordinates} implementation for gateway highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("gateway")
public class GatewayHighlight extends AbstractArea {

	private final int x1;
	
	private final int y1;
	
	private final int x2;
	
	private final int y2;
	
	private final int x3;
	
	private final int y3;
	
	private final int x4;
	
	private final int y4;
	
	public GatewayHighlight(String processId, String activityId, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
		super(processId, activityId);
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.x3 = x3;
		this.y3 = y3;
		this.x4 = x4;
		this.y4 = y4;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Shape getShape() {
		return Shape.POLY;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getCoordinates() {
		return String.format("%d, %d, %d, %d, %d, %d, %d, %d", x1, y1, x2, y2, x3, y3, x4, y4);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityType getActivityType() {
		return ActivityType.GATEWAY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIconFileName() {
		return "/image/Gateway.png";
	}

}
