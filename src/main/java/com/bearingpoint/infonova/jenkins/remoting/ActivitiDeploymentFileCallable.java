package com.bearingpoint.infonova.jenkins.remoting;

import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI09;
import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI10;
import static com.bearingpoint.infonova.jenkins.exception.ErrorMessageResolver.resolve;
import static com.bearingpoint.infonova.jenkins.util.ActivitiAccessor.deleteAbortedBuildExecution;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.BUILD_NUMBER_PROPERTY;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.JOB_NAME_PROPERTY;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowErrorAction;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;

// TODO: javadoc
public class ActivitiDeploymentFileCallable implements FileCallable<Boolean> {

    /** the serial version UID. */
    private static final long serialVersionUID = 8932711690545143173L;

    private final BuildListener listener;
    private final AbstractBuild<?, ?> build;
    private final String pathToWorkflow;

    public ActivitiDeploymentFileCallable(BuildListener listener, AbstractBuild<?, ?> build, String pathToWorkflow) {
        this.listener = listener;
        this.build = build;
        this.pathToWorkflow = pathToWorkflow;
    }

    @Override
    public Boolean invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {

        final PrintStream logger = listener.getLogger();

        // get the BPMN XML file from the workspace
        final FilePath diagram = getWorkflowDiagram(workspace);

        // get the process engine instances
        ProcessEngine engine = JenkinsProcessEngine.getProcessEngine();

        String processId = null;

        try {
            // deploy the BPMN process
            log(logger, "deploy BPMN process: " + diagram.getRemote());
            processId = ActivitiAccessor.deployProcess(engine, diagram);

            // add activity properties
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(JOB_NAME_PROPERTY, build.getProject().getName());
            properties.put(BUILD_NUMBER_PROPERTY, build.getNumber());

            ActivitiUtils.addActivityProperties(processId, properties);

        } catch (RuntimeException ex) {
            // delete the BPMN process execution if the build is aborted
            log(logger, "delete aborted build BPMN process");
            deleteAbortedBuildExecution(engine, processId, build);
            final String obj = ex.getMessage();
            return handleException(build, ACTIVITI09, ex, listener, obj);
        } catch (FileNotFoundException ex) {
            final String obj = diagram.getRemote();
            return handleException(build, ACTIVITI10, ex, listener, obj);
        }

        return true;

    }

    /**
     * Handles the occurred exception.
     * 
     * @param build
     * @param errorCode
     * @param ex
     * @param listener
     * @param variables
     * @return boolean
     */
    private boolean handleException(AbstractBuild<?, ?> build, ErrorCode errorCode, Exception ex,
            BuildListener listener, Object... variables) {

        File errorRef = JenkinsUtils.storeException(build, ex);
        build.addAction(new ActivitiWorkflowErrorAction(errorCode, errorRef));

        listener.fatalError(resolve(errorCode, variables));

        return false;
    }

    /**
     * Logs the given message.
     * 
     * @param logger
     * @param msg
     */
    private void log(PrintStream logger, String msg) {
        logger.append(msg);
        logger.println();
    }

    /**
     * Returns a {@link FilePath} instance to the process diagram file.
     * 
     * @param workspace
     * @return FilePath
     */
    private FilePath getWorkflowDiagram(File workspace) {
        return new FilePath(new File(workspace, pathToWorkflow));
    }

}
