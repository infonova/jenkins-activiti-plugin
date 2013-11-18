package com.bearingpoint.infonova.jenkins.action;

import hudson.model.Action;
import hudson.model.ProminentProjectAction;

/**
 * This {@link Action} implementation ensures the ability to run the project
 * direct from the jenkins process viewer view.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class RunProjectAction implements ProminentProjectAction {

	private final String projectName;

	public RunProjectAction(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIconFileName() {
		return "/plugin/jenkins-activiti-plugin/image/run_build.png";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayName() {
		return Messages.RunProjectAction_DisplayName();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUrlName() {
		return String.format("/job/%s/build?delay=0sec", projectName);
	}

}
