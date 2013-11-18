package com.bearingpoint.infonova.jenkins.factory;

import org.activiti.engine.impl.pvm.process.ActivityImpl;

import com.bearingpoint.infonova.jenkins.ui.GatewayHighlight;

/**
 * Factory class for gateway highlight element.
 * 
 * @author christian.weber
 * @since 1.0
 * 
 */
public final class GatewayHighlightFactory {

	private GatewayHighlightFactory() {
		super();
	}

	/**
	 * Returns the gateway highlight element.
	 * 
	 * @param activity
	 * @param xEff
	 * @param yEff
	 * @return GatewayHighlight
	 */
	public static GatewayHighlight getObject(ActivityImpl activity, int xEff,
			int yEff) {

		int x1 = activity.getX() - xEff;
		int y1 = activity.getY() - yEff;
		int x2 = activity.getX() - xEff + activity.getWidth();
		int y2 = activity.getY() - yEff + activity.getHeight();

		int a1 = x1;
		int b1 = y1 + ((y2 - y1) / 2);
		int a2 = x1 + ((x2 - x1) / 2);
		int b2 = y1;
		int a3 = x2;
		int b3 = y1 + ((y2 - y1) / 2);
		int a4 = x1 + ((x2 - x1) / 2);
		int b4 = y2;

		final String processId = activity.getProcessDefinition().getId();
		return new GatewayHighlight(processId, activity.getId(), a1, b1, a2, b2, a3, b3,
				a4, b4);
	}

}
