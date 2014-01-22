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
 * Task delegate implementation for JENKINS job invocation.
 * 
 * @author christian.weber
 * @since 1.0
 */
@SuppressWarnings("serial")
public class RemoteJenkinsActivitiTaskDelegate extends ReceiveTaskActivityBehavior {

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
                } catch (Exception o_O) {
                    // TODO log this exception
                    o_O.printStackTrace();
                    logger.error("remote jenkins activity error", o_O);

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
        if (port == null) {
            return null;
        }
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
        AbstractRemoteJenkinsBuild previousBuild = client.getJobInfo(getJobName());
        // if build could not be determined, set previousNumber to -1
        int previousNumber = previousBuild != null ? previousBuild.getNumber() : -1;

        // schedule the JENKINS job
        System.out.println("Scheduling Job...");
        client.scheduleJob(getJobName(), variables, getVariablesMap());

        AbstractRemoteJenkinsBuild build = waitForJobToBeScheduled(client);

        // compare the current version with the previous version (it's possible that there is already a previous build
        // scheduled)
        while ((build = client.getJobInfo(getJobName())).getNumber() == previousNumber) {
            System.out.println("Waiting for the previous Job " + build.getFullDisplayName() + " to finish...");
            Thread.sleep(5000);
        }

        // wait until the build is finished
        final String buildNumber = String.valueOf(build.getNumber());
        while ((build = client.getJobInfo(getJobName(), buildNumber)).isBuilding()) {
            System.out.println("Waiting for the current Job " + build.getFullDisplayName() + " to finish...");
            Thread.sleep(15000);
        }

        // mark the build as failure if build does not finished with success result
        if (!StringUtils.equalsIgnoreCase("SUCCESS", build.getResult())) {
            throw new RuntimeException("build failure");
        }

    }

    private AbstractRemoteJenkinsBuild waitForJobToBeScheduled(final RemoteJenkinsRestClient client) throws Exception {
        /*
         * TODO (ederst) add a timeout?
         */
        AbstractRemoteJenkinsBuild build;

        do {
            System.out.println("Waiting for Job to be scheduled...");
            Thread.sleep(5000);
        } while ((build = client.getJobInfo(getJobName())) == null);

        return build;
    }

}
