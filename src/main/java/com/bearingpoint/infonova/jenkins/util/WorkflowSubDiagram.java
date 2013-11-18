package com.bearingpoint.infonova.jenkins.util;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;
import com.bearingpoint.infonova.jenkins.ui.CallActivityTaskHighlight;

/**
 * Renders the workflow diagram. The workflow diagram picture is either loaded
 * from the file system or from the deployed process definition.
 * 
 * @author christian.weber
 * @since 1.0
 */
// TODO: create AbstractWorkflowDiagram
public class WorkflowSubDiagram extends AbstractWorkflowDiagram {

	private final String subProcessId;

	public WorkflowSubDiagram(AbstractBuild<?, ?> build, String processDefinitionId,
			String subProcess) {
		super(build, processDefinitionId);
		this.subProcessId = subProcess;
	}

	/**
	 * Returns the diagram resource as stream either from the file system or
	 * from the process definition.
	 * 
	 * @param action
	 * @return InputStream
	 */
	protected InputStream getDiagramResourceAsStream(AbstractBuild<?, ?> build) {

		ActivitiWorkflowAction action = getActivitiWorkflowAction();
		
		if (action == null) {
			return null;
		}
		
		String pictureName = traverseElementsForSubDiagram(action);
		
		if (pictureName == null) {
			return null;
		}
		// TODO: if action is null render error image

		try {
			File rootDir = new File(build.getRootDir(), "diagrams");
			File picture = new File(rootDir, pictureName);
			return new FileInputStream(picture);
		} catch (FileNotFoundException e) {
			return ActivitiUtils.getDiagramResourceAsStream(action.getProcessDescriptionId());
		}
	}

	private String traverseElementsForSubDiagram(ActivitiWorkflowAction action) {
		List<AbstractArea> areas = action.getElements();

		for (AbstractArea area : areas) {

			if (area instanceof CallActivityTaskHighlight) {
				CallActivityTaskHighlight callActivity = (CallActivityTaskHighlight) area;

				if (StringUtils.equals(callActivity.getProcessDescriptionId(), this.subProcessId)) {
					return callActivity.getPicture();
				}

			}
		}
		return null;
	}


}
