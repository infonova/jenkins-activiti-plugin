package com.bearingpoint.infonova.jenkins.ui;


/**
 * Abstract area implementation.
 * 
 * @author christian.weber
 * @since 1.0
 */
public abstract class AbstractArea implements Coordinates, Linkable, StateAware {

	private final String processId;
	
	private final String activityId;

	private TaskState state;

	public AbstractArea(String processId, String activityId) {
		this.processId = processId;
		this.activityId = activityId;
		this.state = TaskState.PENDING;
	}

	/**
	 * Returns the process id.
	 * @return String
	 */
	public String getProcessId() {
		return this.processId;
	}
	
	/**
	 * Returns the activity id.
	 * 
	 * @return String
	 */
	public String getActivityId() {
		return this.activityId;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLink() {
		return "#" + activityId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setState(TaskState state) {
		this.state = state;
	}

	/**
	 * Returns the {@link TaskState}
	 * 
	 * @return TaskState
	 */
	public TaskState getState() {
		return this.state;
	}

	/**
	 * Returns the {@link ActivityType} instance.
	 * 
	 * @return ActivityType
	 */
	public abstract ActivityType getActivityType();
	
	/**
	 * Returns the icon file name.
	 * @return String
	 */
	public abstract String getIconFileName();

	@Override
	public String toString() {
		String className = getClass().getSimpleName();
		return String.format("%s (%s %s)", className, processId, activityId);
	}
	
}
