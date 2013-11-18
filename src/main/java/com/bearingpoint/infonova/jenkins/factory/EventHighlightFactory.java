package com.bearingpoint.infonova.jenkins.factory;

import org.activiti.engine.impl.pvm.process.ActivityImpl;

import com.bearingpoint.infonova.jenkins.ui.EventHighlight;

/**
 * Factory class for event highlight factory.
 * 
 * @author christian.weber
 * @since 1.0
 * 
 */
public final class EventHighlightFactory {

	private EventHighlightFactory() {
		super();
	}

	/**
	 * Returns the event highlight element.
	 * 
	 * @param activity
	 * @param xEff
	 * @param yEff
	 * @return EventHighlight
	 */
	public static EventHighlight getObject(ActivityImpl activity, int xEff,
			int yEff) {

		int width = activity.getWidth();
		int radius = (width % 2) != 0 ? (width / 2) + 1 : width / 2;
		int x = activity.getX() - xEff + radius;
		int y = activity.getY() - yEff + radius;

		final String processId = activity.getProcessDefinition().getId();
		return new EventHighlight(processId, activity.getId(), x, y, radius);
	}

}
