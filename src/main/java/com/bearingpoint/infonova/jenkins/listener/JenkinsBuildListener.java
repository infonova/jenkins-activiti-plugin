package com.bearingpoint.infonova.jenkins.listener;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.HashMap;
import java.util.Map;

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

		runtimeService.signal(executionId, variables);

	}
}
