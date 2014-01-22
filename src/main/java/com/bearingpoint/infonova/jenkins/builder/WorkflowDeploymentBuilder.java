package com.bearingpoint.infonova.jenkins.builder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.bearingpoint.infonova.jenkins.remoting.ActivitiDeploymentFileCallable;

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

        try {
            final FilePath workspace = build.getWorkspace();
            return workspace.act(new ActivitiDeploymentFileCallable(listener, build, pathToWorkflow));
        } catch (Exception o_O) {

            // write the stacktrace into a string
            Writer w = new StringWriter();
            PrintWriter pw = new PrintWriter(w);
            o_O.printStackTrace(pw);

            // log the error message
            listener.fatalError(w.toString());

            return false;
        }

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
