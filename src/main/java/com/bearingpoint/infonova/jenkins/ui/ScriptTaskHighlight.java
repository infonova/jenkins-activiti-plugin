package com.bearingpoint.infonova.jenkins.ui;

import java.io.File;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for script task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("scripttask")
public class ScriptTaskHighlight extends AbstractTaskHighlight {

	private final String script;

	public ScriptTaskHighlight(String processId, String activityId, int x1, int y1, int x2,
			int y2, File script) {
		super(processId, activityId, x1, y1, x2, y2);
		this.script = script.getName();
	}

	/**
	 * Returns the job name.
	 * 
	 * @return File
	 */
	public String getScript() {
		return this.script;
	}
	
}
