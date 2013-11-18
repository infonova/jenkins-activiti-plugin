package com.bearingpoint.infonova.jenkins.listener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

/**
 * This {@link ExecutionListener} class is used to register custom script task
 * variables.
 * 
 * @author christian.weber
 * @since 1.0.0
 * 
 */
public class ScriptTaskListener implements ExecutionListener {

	public void notify(DelegateExecution execution) throws Exception {
//		execution.setVariableLocal("jenkins", Jenkins.getInstance());
	}

}
