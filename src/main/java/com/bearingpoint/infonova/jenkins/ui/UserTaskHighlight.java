package com.bearingpoint.infonova.jenkins.ui;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for user task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("usertask")
public class UserTaskHighlight extends AbstractTaskHighlight {

	public UserTaskHighlight(String processId, String activityId, int x1, int y1, int x2,
			int y2) {
		super(processId, activityId, x1, y1, x2, y2);
	}

	@Override
	public TaskType getTaskType() {
		return TaskType.USER_TASK;
	}

}
