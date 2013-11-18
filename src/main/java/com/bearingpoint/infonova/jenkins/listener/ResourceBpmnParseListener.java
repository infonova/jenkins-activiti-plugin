package com.bearingpoint.infonova.jenkins.listener;

import java.util.List;

import org.activiti.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.activiti.engine.impl.pvm.PvmEvent;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.util.xml.Element;
import org.apache.commons.lang.StringUtils;

/**
 * This {@link AbstractBpmnParseListener} implementation is used to extract
 * additional resource information.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class ResourceBpmnParseListener extends AbstractBpmnParseListener {

	public final static String SCRIPT_PROPERTY = "script.resource.property";

	public final static String JENKINS_TASK_PROPERTY = "jenkinstask.resource.property";

	public final static String CALLACTIVITY_PROPERTY = "callactivity.resource.property";

	/**
	 * {@inheritDoc}
	 */
	public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope,
			ActivityImpl activity) {

		Element script = scriptTaskElement.element("script");
		activity.setProperty(SCRIPT_PROPERTY, script.getText());
		
		activity.addExecutionListener(PvmEvent.EVENTNAME_START, new ScriptTaskListener());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope,
			ActivityImpl activity) {

		Element extension = serviceTaskElement.element("extensionElements");

		if (extension == null) {
			return;
		}

		List<Element> fields = extension.elements("field");

		for (Element field : fields) {

			if (StringUtils.equals(field.attribute("name"), "jobName")) {
				String text = field.element("string").getText();

				if (StringUtils.isBlank(text)) {
					continue;
				}

				activity.setProperty(JENKINS_TASK_PROPERTY, text);
				return;
			}

		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void parseCallActivity(Element callActivityElement, ScopeImpl scope,
			ActivityImpl activity) {
		
		String calledElement = callActivityElement.attribute("calledElement");
		activity.setProperty(CALLACTIVITY_PROPERTY, calledElement);
	}

}
