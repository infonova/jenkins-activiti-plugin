package com.bearingpoint.infonova.jenkins.ui;

import java.io.File;
import java.util.List;

import org.activiti.engine.repository.ProcessDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for call activity highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
// TODO: rename to CallActivityHighlight
// to not extend AbstractTaskHighlight
@XStreamAlias("callactivity")
public class CallActivityTaskHighlight extends AbstractTaskHighlight {

	private final String processDescriptionId;

	private final String workflowName;

	private final String picture;

	private List<AbstractArea> elements;

	public CallActivityTaskHighlight(String processId, String activityId, int x1, int y1, int x2, int y2,
			ProcessDefinition definition, File picture) {
		super(processId, activityId, x1, y1, x2, y2);
		this.processDescriptionId = definition.getId();
		this.workflowName = definition.getName();
		this.picture = picture.getName();
	}

	public String getProcessDescriptionId() {
		return processDescriptionId;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public String getPicture() {
		return picture;
	}

	public List<AbstractArea> getElements() {
		return elements;
	}

	public void setElements(List<AbstractArea> elements) {
		this.elements = elements;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLink() {
		return "#subProcess";
	}

	@Override
	public ActivityType getActivityType() {
		return ActivityType.CALL_ACTIVITY;
	}

}
