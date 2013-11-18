package com.bearingpoint.infonova.jenkins.listener;

import org.activiti.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.util.xml.Element;

import com.bearingpoint.infonova.jenkins.exception.ActivitiWorkflowException;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;

/**
 * This {@link BpmnParseListener} implementation is used to detect unsupported
 * activity elements before execution. If an unsupported activity element is
 * detected an {@link ActivitiWorkflowException} is thrown.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class SupportedElementsBpmnParseListener extends
		AbstractBpmnParseListener {

	@Override
	public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope,
			ActivityImpl activity) {
		throw new ActivitiWorkflowException(ErrorCode.ACTIVITI06, activity.getId());
	}

	@Override
	public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope,
			ActivityImpl activity) {
		Element extension = serviceTaskElement.element("extensionElements");
		if (extension == null) {
			throw new ActivitiWorkflowException(ErrorCode.ACTIVITI08, activity.getId());	
		}
	}

}
