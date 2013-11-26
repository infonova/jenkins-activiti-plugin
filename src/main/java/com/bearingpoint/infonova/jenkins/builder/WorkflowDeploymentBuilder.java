package com.bearingpoint.infonova.jenkins.builder;

import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI09;
import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI10;
import static com.bearingpoint.infonova.jenkins.exception.ErrorMessageResolver.resolve;
import static com.bearingpoint.infonova.jenkins.util.ActivitiAccessor.deleteAbortedBuildExecution;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.BUILD_NUMBER_PROPERTY;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.JOB_NAME_PROPERTY;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.activiti.engine.ProcessEngine;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowErrorAction;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;

/**
 * {@link Builder} implementation used to deploy a activiti workflow. This
 * functionality can be used to register a call activity.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class WorkflowDeploymentBuilder extends Builder {

    private final String pathToWorkflow;

    @DataBoundConstructor
    public WorkflowDeploymentBuilder(String pathToWorkflow) {
        this.pathToWorkflow = pathToWorkflow;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getPathToWorkflow() {
        return pathToWorkflow;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {

        // runs on the JENKINS master or slave
        @SuppressWarnings("serial")
        Callable<Boolean, Exception> call = new Callable<Boolean, Exception>() {

            public Boolean call() throws Exception {
                return performInternal(build, launcher, listener);
            }

        };

        // runs on the JENKINS master
        try {
            return launcher.getChannel().call(call);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean performInternal(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

        final PrintStream logger = listener.getLogger();

        // get the BPMN XML file from the workspace
        final FilePath diagram = getWorkflowDiagram(build.getWorkspace());

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
    private FilePath getWorkflowDiagram(FilePath workspace) {
        return new FilePath(workspace, pathToWorkflow);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link WorkflowDeploymentBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * 
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the actual HTML fragment
     * for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * Performs on-the-fly validation of the form field 'name'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the
         *         browser.
         */
        public FormValidation doCheckPathToWorkflow(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set the path to the workflow diagram");
            }
            return FormValidation.ok();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Activiti Workflow Deployment";
        }

    }
}
