package com.bearingpoint.infonova.jenkins.ui;

/**
 * Abstract class for task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
public abstract class AbstractTaskHighlight extends AbstractArea {

	private final int x1;

	private final int y1;

	private final int x2;

	private final int y2;

	public AbstractTaskHighlight(String processId, String activityId, int x1, int y1, int x2, int y2) {
		super(processId, activityId);
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	/**
	 * {@inheritDoc}
	 */
	public Shape getShape() {
		return Shape.RECTANGLE;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCoordinates() {
		return String.format("%d, %d, %d, %d", x1, y1, x2, y2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityType getActivityType() {
		return ActivityType.TASK;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIconFileName() {
		return "/image/Task.png";
	}
	
	public TaskType getTaskType() {
		return TaskType.DEFAULT;
	}
	
}
