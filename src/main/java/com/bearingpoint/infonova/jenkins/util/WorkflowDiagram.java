package com.bearingpoint.infonova.jenkins.util;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.h2.util.IOUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;

/**
 * Renders the workflow diagram. The workflow diagram picture is either loaded
 * from the file system or from the deployed process definition.
 * 
 * @author christian.weber
 * @since 1.0
 */
// TODO: create AbstractWorkflowDiagram
public class WorkflowDiagram implements HttpResponse {

	private transient Logger logger = Logger.getLogger(WorkflowDiagram.class);

	private final AbstractBuild<?, ?> build;

	private final String processDefinitionId;

	public WorkflowDiagram(AbstractBuild<?, ?> build, String processDefinitionId) {
		this.build = build;
		this.processDefinitionId = processDefinitionId;
	}

	/**
	 * Renders the process diagram.
	 * 
	 * @param req
	 *            the request
	 * @param rsp
	 *            the response
	 */
	public void generateResponse(StaplerRequest req, StaplerResponse rsp,
			Object node) throws IOException, ServletException {

		rsp.setContentType("image/png");

		InputStream in = getDiagramResourceAsStream();
		OutputStream out = rsp.getOutputStream();

		assert in != null;

		IOUtils.copy(in, out);
	}

	/**
	 * Returns the diagram resource as stream either from the file system or
	 * from the process definition.
	 * 
	 * @param action
	 * @return InputStream
	 */
	private InputStream getDiagramResourceAsStream() {

		ActivitiWorkflowAction action = getActivitiWorkflowAction();
		
		// TODO: if action is null render error image

		try {
			File rootDir = new File(build.getRootDir(), "diagrams");
			File picture = new File(rootDir, action.getPicture());
			return new FileInputStream(picture);
		} catch (FileNotFoundException e) {
			return ActivitiUtils.getDiagramResourceAsStream(action
					.getProcessDescriptionId());
		}
	}

	private ActivitiWorkflowAction getActivitiWorkflowAction() {

		if (StringUtils.isNotBlank(processDefinitionId)) {
			List<ActivitiWorkflowAction> actions = build
					.getActions(ActivitiWorkflowAction.class);
			for (ActivitiWorkflowAction action : actions) {
				if (StringUtils.equals(processDefinitionId,
						action.getProcessDescriptionId())) {
					return action;
				}
			}
			logger.error("no action found with id " + processDefinitionId);
			// TODO: exception handling
			return null;
		}

		ActivitiWorkflowAction action = build
				.getAction(ActivitiWorkflowAction.class);

		return action;
	}

	/**
	 * Blank workflow diagram implementation. Does not render any image.
	 * 
	 * @author christian.weber
	 * @since 1.0
	 */
	public static class BlankWorkflowDiagram extends WorkflowDiagram {

		public BlankWorkflowDiagram() {
			super(null, null);
		}

		@Override
		public void generateResponse(StaplerRequest req, StaplerResponse rsp,
				Object node) throws IOException, ServletException {
			// do nothing
		}

	}

}
