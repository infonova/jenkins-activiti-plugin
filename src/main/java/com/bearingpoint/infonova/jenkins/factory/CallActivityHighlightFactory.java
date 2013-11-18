package com.bearingpoint.infonova.jenkins.factory;

import static com.bearingpoint.infonova.jenkins.listener.ResourceBpmnParseListener.CALLACTIVITY_PROPERTY;
import hudson.model.AbstractBuild;

import java.io.File;

import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;

import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.ui.CallActivityTaskHighlight;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;
import com.bearingpoint.infonova.jenkins.util.Assert;

/**
 * Factory class for call activity highlight element.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class CallActivityHighlightFactory {

	private CallActivityHighlightFactory() {
		super();
	}

	/**
	 * Returns the call activity highlight element.
	 * 
	 * @param activity
	 * @param xEff
	 * @param yEff
	 * @param build
	 * @return CallActivityTaskHighlight
	 */
	public static CallActivityTaskHighlight getObject(ActivityImpl activity, int xEff, int yEff,
			AbstractBuild<?, ?> build) {
		int x1 = activity.getX() - xEff;
		int y1 = activity.getY() - yEff;
		int x2 = activity.getX() - xEff + activity.getWidth();
		int y2 = activity.getY() - yEff + activity.getHeight();

		if (ActivitiUtils.isCallActivity(activity)) {
			final String key = (String) activity.getProperty(CALLACTIVITY_PROPERTY);
			ProcessDefinition definition = ActivitiUtils.getCallActivity(key);

			// TODO: exception handling error message
			Assert.notNull(definition, ErrorCode.ACTIVITI05, "(?)");
			
			File picture = ActivitiUtils.storeProcessDiagram(build, definition.getId());

			final String processId = activity.getProcessDefinition().getId();
			CallActivityTaskHighlight callActivity = new CallActivityTaskHighlight(processId,
					activity.getId(), x1, y1, x2, y2, definition, picture);
			callActivity.setElements(ActivitiUtils.getAreas(build, definition.getId()));

			return callActivity;
		}

		// TODO: exception handling
		throw new RuntimeException("unsupported activity element found: "
				+ activity.getProperty("type"));
	}

}
