package com.bearingpoint.infonova.jenkins.activitybehavior;

import hudson.model.Result;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bearingpoint.infonova.jenkins.activitybehavior.remote.AbstractRemoteJenkinsBuild;
import com.bearingpoint.infonova.jenkins.activitybehavior.remote.RemoteJenkinsRestClient;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;

/**
 * Task delegate implementation for jenkins job invocation.
 * 
 * @author christian.weber
 * @since 1.0
 */
@SuppressWarnings("serial")
public class RemoteJenkinsActivitiTaskDelegate extends ReceiveTaskActivityBehavior {

    private transient Logger logger = Logger.getLogger(RemoteJenkinsActivitiTaskDelegate.class);

    private Expression jobName;

    private Expression scheme;

    private Expression host;

    private Expression port;

    private Expression variablesMap;

    @Override
    public void execute(final ActivityExecution execution) throws Exception {

        PvmActivity activity = execution.getActivity();
        String activityId = activity.getId();

        PvmProcessDefinition pDef = activity.getProcessDefinition();

        logger.info("execution.id                    : " + execution.getId());
        logger.info("execution.activity.id           : " + activityId);
        logger.info("execution.activity.process.id   : " + pDef.getId());
        logger.info("execution.activity.process.name : " + pDef.getName());

        final Map<String, Object> variables = execution.getVariables();

        new ActivityThread(execution) {

            @Override
            public void run() {
                ProcessEngine engine = ActivitiAccessor.getProcessEngine();
                RuntimeService runtimeService = engine.getRuntimeService();
                try {
                    scheduleJob(variables);
                    // Marks the job as finished
                    runtimeService.signal(this.getExecution().getId());
                } catch (Exception e) {
                    Map<String, Object> variables = new HashMap<String, Object>();
                    variables.put("result", Result.FAILURE.toString());

                    runtimeService.signal(this.getExecution().getId(), variables);
                }

            }
        }.start();
    }


    @Override
    public void signal(ActivityExecution execution, String signalName, Object data) throws Exception {

        Object result = execution.getVariable("result");

        if (result == null) {
            super.signal(execution, signalName, data);
        } else {
            ActivityImpl activity = (ActivityImpl)execution.getActivity();
            activity.setProperty("result", "FAILURE");
        }

    }


    /**
     * Returns the job name.
     * 
     * @return String
     */
    public String getJobName() {
        return jobName.getExpressionText();
    }

    /**
     * Returns the JENKINS URL scheme fragment.
     * 
     * @return String
     */
    public String getScheme() {
        return scheme.getExpressionText();
    }

    /**
     * Returns the JENKINS URL host fragment.
     * 
     * @return String
     */
    public String getHost() {
        return host.getExpressionText();
    }

    /**
     * Returns the JENKINS URL port fragment.
     * 
     * @return String
     */
    public String getPort() {
        return port.getExpressionText();
    }

    /**
     * Returns the JENKINS URL path fragment.
     * 
     * @return String
     */
    public String getPath() {
        return "/job/";
    }

    /**
     * Returns the job variables.
     * 
     * @return Map
     */
    // TODO: remove try catch clause
    public Map<String, String> getVariablesMap() {
        try {
            final String parameters = variablesMap.getExpressionText();
            return JenkinsUtils.normalizeParameters(parameters);
        } catch (RuntimeException ex) {
            return new HashMap<String, String>();
        }
    }

    private void scheduleJob(Map<String, Object> variables) throws Exception {

        final String s1 = getScheme();
        final String s2 = getPort();
        final String s3 = getHost();
        final String s4 = getPath();

        RemoteJenkinsRestClient client = new RemoteJenkinsRestClient(s1, s2, s3, s4);

        // at first load the JENKINS job info so that
        // the previous build number could be extracted
        // in order to evaluate the build status
        AbstractRemoteJenkinsBuild build = client.getJobInfo(getJobName());
        int previousNumber = build.getNumber();

        // schedule the JENKINS job
        client.scheduleJob(getJobName(), variables, getVariablesMap());

        // compare the current version with the previous version
        Thread.sleep(1000);
        build = client.getJobInfo(getJobName());
        while (build.getNumber() == previousNumber) {
            Thread.sleep(1000);
            build = client.getJobInfo(getJobName());
        }

        // wait until the build is finished
        while (build.isBuilding()) {
            Thread.sleep(15000);
            build = client.getJobInfo(getJobName());
        }

        // mark the build as failure if build does not finished with success result
        if (!StringUtils.equalsIgnoreCase("SUCCESS", build.getResult())) {
            throw new RuntimeException("build failure");
        }

    }

    public static interface Callback {

        void doSomething();

    }

    public static class ActivityThread extends Thread {

        private final ActivityExecution execution;

        public ActivityThread(ActivityExecution execution) {
            this.execution = execution;
        }

        public ActivityExecution getExecution() {
            return execution;
        }

    }

}
