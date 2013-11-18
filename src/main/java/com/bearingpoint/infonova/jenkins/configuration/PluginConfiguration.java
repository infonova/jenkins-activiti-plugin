package com.bearingpoint.infonova.jenkins.configuration;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowErrorAction;
import com.bearingpoint.infonova.jenkins.activitybehavior.remote.AbstractRemoteJenkinsBuild.RemoteFreeStyleBuild;
import com.bearingpoint.infonova.jenkins.activitybehavior.remote.AbstractRemoteJenkinsBuild.RemoteMavenBuild;
import com.bearingpoint.infonova.jenkins.cause.WorkflowCause;
import com.bearingpoint.infonova.jenkins.ui.AbstractTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.CallActivityTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.EventHighlight;
import com.bearingpoint.infonova.jenkins.ui.GatewayHighlight;
import com.bearingpoint.infonova.jenkins.ui.JenkinsActivitiTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.ScriptTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.TaskState;

/**
 * Plugin configuration class. Configures the XStream environment.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class PluginConfiguration {

	/**
	 * Registers the XStream aliases.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void addAliases() {
		Run.XSTREAM2.processAnnotations(AbstractTaskHighlight.class);
		Run.XSTREAM2.processAnnotations(TaskState.class);
		Run.XSTREAM2.processAnnotations(EventHighlight.class);
		Run.XSTREAM2.processAnnotations(GatewayHighlight.class);
		Run.XSTREAM2.processAnnotations(JenkinsActivitiTaskHighlight.class);
		Run.XSTREAM2.processAnnotations(ScriptTaskHighlight.class);
		Run.XSTREAM2.processAnnotations(ActivitiWorkflowAction.class);
		Run.XSTREAM2.processAnnotations(WorkflowCause.class);
		Run.XSTREAM2.processAnnotations(ActivitiWorkflowErrorAction.class);
		Run.XSTREAM2.processAnnotations(CallActivityTaskHighlight.class);

		// remote jenkins activity behavior
		Run.XSTREAM2.processAnnotations(RemoteMavenBuild.class);
		Run.XSTREAM2.processAnnotations(RemoteFreeStyleBuild.class);
	}

}
