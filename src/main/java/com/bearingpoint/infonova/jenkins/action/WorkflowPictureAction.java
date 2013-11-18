package com.bearingpoint.infonova.jenkins.action;

import hudson.model.ProminentProjectAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;

import com.bearingpoint.infonova.jenkins.util.WorkflowDiagram;
import com.bearingpoint.infonova.jenkins.util.WorkflowDiagram.BlankWorkflowDiagram;
import com.bearingpoint.infonova.jenkins.util.WorkflowSubDiagram;

/**
 * Transient action used to display the deployed process diagram. <br />
 * This action is registered by the
 * {@link TransientActivitiProjectActionFactory} class.
 *
 * @author christian.weber
 * @since 1.0
 * @see TransientActivitiProjectActionFactory
 */
public class WorkflowPictureAction implements ProminentProjectAction {

	private final AbstractProject<?, ?> project;

	public WorkflowPictureAction() {
		this.project = null;
	}

	public WorkflowPictureAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	/**
	 * Renders the workflow diagram.
	 *
	 * @param req the request
	 * @return WorkflowDiagram
	 */
	public HttpResponse doDiagram(final StaplerRequest req) {

		if (project == null) {
			return null;
		}

		String restOfPath = req.getRestOfPath();

		if (StringUtils.isBlank(restOfPath)) {
			return new BlankWorkflowDiagram();
		}

		String[] array = restOfPath.substring(1).split("/");
		final String number = getArrayValue(array, 0);
		final String workflow = getArrayValue(array, 1);
		final String subWorkflow = getArrayValue(array, 2);

		if (!StringUtils.isNumeric(number)) {
			return new BlankWorkflowDiagram();
		}

		int buildNr = Integer.parseInt(number);
		AbstractBuild<?, ?> build = project.getBuildByNumber(buildNr);

		if (subWorkflow != null) {
			return new WorkflowSubDiagram(build, workflow, subWorkflow);
		}
		return new WorkflowDiagram(build, workflow);
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
		return "diagram";
	}

}
