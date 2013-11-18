package com.bearingpoint.infonova.jenkins.ui;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for manual task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("manualtask")
public class ManualTaskHighlight extends AbstractTaskHighlight {

	public ManualTaskHighlight(String processId, String activityId, int x1, int y1, int x2,
			int y2) {
		super(processId, activityId, x1, y1, x2, y2);
	}

}
