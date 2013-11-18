package com.bearingpoint.infonova.jenkins.ui;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for jenkins task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("jenkinstask")
public class JenkinsActivitiTaskHighlight extends AbstractTaskHighlight {

	private final String jobName;

	public JenkinsActivitiTaskHighlight(String processId, String activityId, int x1, int y1,
			int x2, int y2, String jobName) {
		super(processId, activityId, x1, y1, x2, y2);
		this.jobName = jobName;
	}

	/**
	 * Returns the job name.
	 * 
	 * @return String
	 */
	public String getJobName() {
		return this.jobName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLink() {
		return "/job/" + jobName;
	}

}
