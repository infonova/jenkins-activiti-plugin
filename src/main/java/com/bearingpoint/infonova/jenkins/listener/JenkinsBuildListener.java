package com.bearingpoint.infonova.jenkins.listener;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.apache.log4j.Logger;

import com.bearingpoint.infonova.jenkins.cause.WorkflowCause;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;

// TODO: javadoc
@Extension
@SuppressWarnings("rawtypes")
public class JenkinsBuildListener extends RunListener<Run> {

	private transient Logger logger = Logger.getLogger(JenkinsBuildListener.class);

	@Override
	public void onFinalized(Run r) {

		logger.debug("finalize build " + r);

		@SuppressWarnings("unchecked")
		Cause cause = r.getCause(WorkflowCause.class);

		if (cause == null || !(cause instanceof WorkflowCause)) {
			return;
		}

		WorkflowCause workflowCause = (WorkflowCause) cause;
		final String executionId = workflowCause.getExecutionId();

		ProcessEngine engine = ActivitiAccessor.getProcessEngine();
		RuntimeService runtimeService = engine.getRuntimeService();

		// check build status
		Result result = r.getResult();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("result", result.toString());

		trySignal(runtimeService, executionId, variables, 0);

	}
	
	private void trySignal(RuntimeService runtimeService, String executionId, Map<String, Object> variables, int retry)
	{
		if(retry < 2) //try 2 times with Exception handling
		{
			try
			{
				runtimeService.signal(executionId, variables);
			}
			catch(ActivitiOptimisticLockingException e)
			{
				try {
					Thread.sleep(500);
					trySignal(runtimeService, executionId, variables, retry+1);
				} catch (InterruptedException e1) {
					
				}
				
			}	
		}
		else
		{
			// the third time without (for output)
			runtimeService.signal(executionId, variables);
		}
	}
}
