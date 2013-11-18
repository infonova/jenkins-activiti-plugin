package com.bearingpoint.infonova.jenkins.builder;

import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI09;
import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI10;
import static com.bearingpoint.infonova.jenkins.exception.ErrorMessageResolver.resolve;
import static com.bearingpoint.infonova.jenkins.util.ActivitiAccessor.deleteAbortedBuildExecution;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.BUILD_NUMBER_PROPERTY;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.JOB_NAME_PROPERTY;
import static org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END;
import static org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_START;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Properties;

import javax.servlet.ServletException;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowErrorAction;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.exception.JenkinsJobFailedException;
import com.bearingpoint.infonova.jenkins.listener.ActivityEndListener;
import com.bearingpoint.infonova.jenkins.listener.ActivityStartListener;
import com.bearingpoint.infonova.jenkins.listener.NamedExecutionListener;
import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;
import com.bearingpoint.infonova.jenkins.util.Assert;
import com.bearingpoint.infonova.jenkins.util.DestructionCallback;
import com.bearingpoint.infonova.jenkins.util.DestructionCallbacks;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;

/**
 * {@link Builder} implementation used to run a activiti workflow.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class WorkflowBuilder extends Builder {

	private final String pathToWorkflow;

	private static Logger logger = Logger.getLogger(WorkflowBuilder.class);

	@DataBoundConstructor
	public WorkflowBuilder(String pathToWorkflow) {
		this.pathToWorkflow = pathToWorkflow;
	}

	/**
	 * We'll use this from the <tt>config.jelly</tt>.
	 */
	public String getPathToWorkflow() {
		return pathToWorkflow;
	}

	// TODO: set ActivitiWorkflowAction into the variable map
	// each task should add the UI element instance to the variable map
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

		final PrintStream logger = listener.getLogger();

		log(logger, "****************************");
		log(logger, "Start activiti BPMN workflow");
		log(logger, "****************************");

		// get the BPMN XML file from the workspace
		final FilePath diagram = getWorkflowDiagram(build.getWorkspace());

		// get the process engine instances
		ProcessEngine engine = JenkinsProcessEngine.getProcessEngine();

		String processId = null;

		DestructionCallbacks callbacks = new DestructionCallbacks();

		try {
			Assert.isTrue(StringUtils.endsWith(diagram.getName(), ".bpmn20.xml"),
					ErrorCode.ACTIVITI04);

			// deploy the BPMN process
			log(logger, "deploy BPMN process: " + diagram.getRemote());
			processId = ActivitiAccessor.deployProcess(engine, diagram);

			addActions(build, processId, logger, callbacks);

			Map<String, Object> variables = getEnvironments(build, listener);
			ActivitiUtils.prepareDataAssociation(processId, variables.keySet());

			// start the BPMN process
			log(logger, "start BPMN process: " + diagram.getRemote());
			RuntimeService runtimeService = engine.getRuntimeService();
			runtimeService.startProcessInstanceById(processId, variables);

			waitForProcessFinalization(engine, build);
			log(logger, "BPMN process finished");
		} catch (JenkinsJobFailedException ex) {
			log(logger, "delete aborted build BPMN process");
			deleteAbortedBuildExecution(engine, processId, build);
			
			return false;
		} catch (RuntimeException ex) {
			// delete the BPMN process execution if the build is aborted
			log(logger, "delete aborted build BPMN process");
			deleteAbortedBuildExecution(engine, processId, build);
			final String obj = ex.getMessage();
			return handleException(build, ACTIVITI09, ex, listener, obj);
		} catch (FileNotFoundException ex) {
			final String obj = diagram.getRemote();
			return handleException(build, ACTIVITI10, ex, listener, obj);
		} catch (InterruptedException ex) {
			log(logger, "delete aborted build BPMN process");
			deleteAbortedBuildExecution(engine, processId, build);
			
			return false;
		} finally {
			callbacks.destroy();
		}

		return true;
	}

	// TODO: move to JenkinsUtils
	private Map<String, Object> getEnvironments(AbstractBuild<?, ?> build, TaskListener listener) {
		try {
			Map<String, String> map = build.getBuildVariables();
			Map<String, Object> variables = new HashMap<String, Object>();
			for (String key : map.keySet()) {
				variables.put(key, map.get(key));
			}
			return variables;
		} catch (Exception e) {
			throw new RuntimeException("error while  preparing environment map", e);
		}
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
	 * Polls the workflow engine continuously till the process execution with
	 * the given process description id has finished.
	 * 
	 * @param engine
	 * @param processInstanceId
	 */
	// TODO: add timeout
	// or complete running tasks when new execution starts
	private void waitForProcessFinalization(ProcessEngine engine, AbstractBuild<?, ?> build)
			throws InterruptedException {

		ActivitiWorkflowAction action = build.getAction(ActivitiWorkflowAction.class);

		final String processInstanceId = action.getProcessDescriptionId();

		HistoricProcessInstance history = null;
		// TODO: return failed task names
		boolean hasFailedExecutions = false;
		do {
			logger.debug("wait for process finalization for process with id " + processInstanceId);

			waitFor(1000);

			for (String processId : action.getProcessIds()) {
				if (hasFailedExecutions == false) {
					hasFailedExecutions = ActivitiUtils.hasFailedExecutions(processId);
				}
			}

			history = ActivitiUtils.getProcessHistory(processInstanceId);

			// process history check
			if (history != null && history.getEndTime() != null) {
				return;
			}
			// failed executions check
			if (hasFailedExecutions) {
				throw new JenkinsJobFailedException();
			}

		} while (true);
	}

	/**
	 * Waits for the given amount of millis.
	 * 
	 * @param millis
	 */
	private static void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	/**
	 * Adds all persistent actions to the builder instance. <br />
	 * Returns the TaskStateAction Observer instance in order to register it to
	 * the Observable instance.
	 * 
	 * @param build
	 * @param processDefinitionId
	 * @return TaskStateAction
	 */
	private void addActions(AbstractBuild<?, ?> build, String processDefinitionId,
			PrintStream logger, DestructionCallbacks callbacks) {

		File picture = ActivitiUtils.storeProcessDiagram(build, processDefinitionId);

		// add element coordinates action
		List<AbstractArea> elements = ActivitiUtils.getAreas(build, processDefinitionId);

		String workflowName = getWorkflowName();

		ActivitiWorkflowAction action = new ActivitiWorkflowAction(workflowName,
				processDefinitionId, picture, elements);
		action.setLogger(logger);
		build.addAction(action);
		callbacks.addDestructionCallback(action);
		callbacks.addDestructionCallback(new StoreMetadataCallback(build, action));

		// add activity properties
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(JOB_NAME_PROPERTY, build.getProject().getName());
		properties.put(BUILD_NUMBER_PROPERTY, build.getNumber());

		ActivitiUtils.addActivityProperties(processDefinitionId, properties);

		// add activity execution listeners
		ActivitiUtils.addActivityListeners(processDefinitionId,
				getExecutionListeners(action, callbacks));
	}

	// TODO: move to util package
	private final class StoreMetadataCallback implements DestructionCallback {

		private final AbstractBuild<?, ?> build;
		private final ActivitiWorkflowAction action;

		public StoreMetadataCallback(AbstractBuild<?, ?> build, ActivitiWorkflowAction action) {
			this.build = build;
			this.action = action;
		}

		public void destroy() {
			Properties properties = action.getMetadata().getProperties();

			File file = new File(build.getRootDir(), "metadata.properties");
			try {
				properties.store(new FileOutputStream(file), "activity metadata");
			} catch (IOException e) {
				throw new RuntimeException("error while storing metadata", e);
			}
		}

	}

	/**
	 * Returns the {@link NamedExecutionListener} instances to register. The
	 * listeners adds the given {@link Observer} instance and are registered to
	 * be destroyed by the end of the execution by the use of the
	 * DestructionCallback mechanism.
	 * 
	 * @param observer
	 * @param callbacks
	 * @return ActivitiExecutionListener[]
	 */
	private NamedExecutionListener[] getExecutionListeners(Observer observer,
			DestructionCallbacks callbacks) {
		final String event1 = EVENTNAME_START;
		final String event2 = EVENTNAME_END;

		ActivityStartListener startListener = new ActivityStartListener();
		ActivityEndListener endListener = new ActivityEndListener();

		startListener.addObserver(observer);
		endListener.addObserver(observer);

		List<NamedExecutionListener> list = new ArrayList<NamedExecutionListener>();
		list.add(new NamedExecutionListener(event1, startListener));
		list.add(new NamedExecutionListener(event2, endListener));

		callbacks.addDestructionCallback(startListener);
		callbacks.addDestructionCallback(endListener);

		return list.toArray(new NamedExecutionListener[list.size()]);
	}

	/**
	 * Returns the workflow name. Removes the leading string till the first
	 * slash as well as the file extension.
	 * 
	 * @return String
	 */
	private String getWorkflowName() {

		if (StringUtils.contains(pathToWorkflow, File.separator)) {
			String workflowName = StringUtils.substringAfterLast(pathToWorkflow, File.separator);
			return StringUtils.substringBefore(workflowName, ".bpmn20.xml");
		}
		return StringUtils.substringBefore(pathToWorkflow, ".bpmn20.xml");
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
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link WorkflowBuilder}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See
	 * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
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
		public FormValidation doCheckPathToWorkflow(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set the path to the workflow diagram");
			return FormValidation.ok();
		}

		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Activiti Workflow";
		}

	}
}
