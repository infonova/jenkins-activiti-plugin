package com.bearingpoint.infonova.jenkins.cause;

import hudson.model.Cause;

import org.activiti.engine.impl.pvm.delegate.ActivityExecution;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link Cause} implementation for workflow usage.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("workflowcause")
public class WorkflowCause extends Cause {

	@XStreamAlias("projectname")
	private final String projectName;

	@XStreamAlias("buildnumber")
	private final int buildNr;

	@XStreamAlias("processDescriptionId")
	private final String processDescriptionId;

	@XStreamAlias("executionid")
	private final String executionId;

	@XStreamAlias("activityid")
	private final String activityId;

	public WorkflowCause(String projectName, int buildNr, String processDescriptionId,
			ActivityExecution execution) {
		this.projectName = projectName;
		this.buildNr = buildNr;
		this.processDescriptionId = processDescriptionId;
		this.executionId = execution.getId();
		this.activityId = execution.getActivity().getId();
	}

	/**
	 * Returns the project name.
	 * 
	 * @return String
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Returns the build number.
	 * 
	 * @return int
	 */
	public int getBuildNr() {
		return buildNr;
	}

	/**
	 * Returns the process description id.
	 * 
	 * @return String
	 */
	public String getProcessDescriptionId() {
		return processDescriptionId;
	}

	/**
	 * Returns the execution id.
	 * 
	 * @return String
	 */
	public String getExecutionId() {
		return executionId;
	}

	/**
	 * Returns the activity id.
	 * 
	 * @return String
	 */
	public String getActivityId() {
		return activityId;
	}

	@Override
	public String getShortDescription() {
		return String.format("Triggered by activiti workflow %s#%s", projectName, buildNr);
	}

}
