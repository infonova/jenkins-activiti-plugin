package com.bearingpoint.infonova.jenkins.action;

import hudson.model.ProminentProjectAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import com.bearingpoint.infonova.jenkins.util.WorkflowErrorMessage;

/**
 * Transient action used to display the process error. <br />
 * This action is registered by the
 * {@link TransientActivitiProjectActionFactory} class.
 *
 * @author christian.weber
 * @since 1.0
 * @see TransientActivitiProjectActionFactory
 */
public class WorkflowErrorAction implements ProminentProjectAction {

	private final AbstractProject<?, ?> project;
	
	public WorkflowErrorAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	/**
	 * Renders the workflow error message.
	 *
	 * @param req
	 * @return WorkflowDiagram
	 */
	public WorkflowErrorMessage doErrorMessage(StaplerRequest req) {

		String restOfPath = req.getRestOfPath();

		if (StringUtils.isBlank(restOfPath)) {
			throw new IllegalArgumentException("build number must be set");
		}
		
		String[] array = restOfPath.substring(1).split("/");
		final String number = getArrayValue(array, 0);
		final String errorRef = getArrayValue(array, 1);
		
		if (!StringUtils.isNumeric(number)) {
			throw new IllegalArgumentException("build number must be numeric");
		}
		
		if (StringUtils.isBlank(errorRef)) {
			throw new IllegalArgumentException("error reference must be set");
		}
		
		int buildNr = Integer.parseInt(number);
		
		AbstractBuild<?, ?> build = project.getBuildByNumber(buildNr);
		return new WorkflowErrorMessage(build, errorRef);
	}

	private <T> T getArrayValue(T[] array, int index) {
		if (array.length <= index) {
			return null;
		}
		return array[index];
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDisplayName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIconFileName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUrlName() {
		return "error";
	}

}
