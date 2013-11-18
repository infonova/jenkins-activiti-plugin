package com.bearingpoint.infonova.jenkins.util;

import hudson.model.AbstractBuild;

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
public abstract class AbstractWorkflowDiagram implements HttpResponse {

	private transient Logger logger = Logger.getLogger(AbstractWorkflowDiagram.class);

	private final AbstractBuild<?, ?> build;

	// the process definition id
	private final String processId;

	public AbstractWorkflowDiagram(AbstractBuild<?, ?> build, String processDefinitionId) {
		this.build = build;
		this.processId = processDefinitionId;
	}

	/**
	 * Renders the process diagram.
	 * 
	 * @param req
	 *            the request
	 * @param rsp
	 *            the response
	 */
	public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node)
			throws IOException, ServletException {

		rsp.setContentType("image/png");

		InputStream in = getDiagramResourceAsStream(build);
		
		// exception handling
		if (in == null) {
			in = getClass().getResourceAsStream("404.png");
		}
		
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
	protected abstract InputStream getDiagramResourceAsStream(AbstractBuild<?, ?> build);
	
	protected ActivitiWorkflowAction getActivitiWorkflowAction() {

		if (StringUtils.isNotBlank(processId)) {
			List<ActivitiWorkflowAction> actions = build.getActions(ActivitiWorkflowAction.class);
			for (ActivitiWorkflowAction action : actions) {
				if (StringUtils.equals(processId, action.getProcessDescriptionId())) {
					return action;
				}
			}
			logger.error("no action found with id " + processId);
			return null;
		}

		ActivitiWorkflowAction action = build.getAction(ActivitiWorkflowAction.class);

		return action;
	}

	/**
	 * Blank workflow diagram implementation. Does not render any image.
	 * 
	 * @author christian.weber
	 * @since 1.0
	 */
	public static class BlankWorkflowDiagram extends AbstractWorkflowDiagram {

		public BlankWorkflowDiagram() {
			super(null, null);
		}

		@Override
		public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node)
				throws IOException, ServletException {
			// do nothing
		}

		@Override
		protected InputStream getDiagramResourceAsStream(AbstractBuild<?, ?> build) {
			return null;
		}

	}

}
