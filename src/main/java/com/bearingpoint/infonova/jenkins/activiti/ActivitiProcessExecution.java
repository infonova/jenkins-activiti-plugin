package com.bearingpoint.infonova.jenkins.activiti;

import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI09;
import static com.bearingpoint.infonova.jenkins.exception.ErrorCode.ACTIVITI10;
import static com.bearingpoint.infonova.jenkins.exception.ErrorMessageResolver.resolve;
import static com.bearingpoint.infonova.jenkins.util.ActivitiAccessor.deleteAbortedBuildExecution;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.BUILD_NUMBER_PROPERTY;
import static com.bearingpoint.infonova.jenkins.util.ActivitiUtils.JOB_NAME_PROPERTY;
import static org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END;
import static org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_START;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import jenkins.model.Jenkins;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.StringUtils;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowErrorAction;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.exception.JenkinsJobFailedException;
import com.bearingpoint.infonova.jenkins.listener.ActivityEndListener;
import com.bearingpoint.infonova.jenkins.listener.ActivityStartListener;
import com.bearingpoint.infonova.jenkins.listener.NamedExecutionListener;
import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;
import com.bearingpoint.infonova.jenkins.ui.CallActivityTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.JenkinsActivitiTaskHighlight;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;
import com.bearingpoint.infonova.jenkins.util.Assert;
import com.bearingpoint.infonova.jenkins.util.DestructionCallbacks;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;
import com.bearingpoint.infonova.jenkins.util.callback.StoreMetadataCallback;

/**
 * {@link FileCallable} instance for ACTIVITI process execution.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class ActivitiProcessExecution {

	private final BuildListener listener;
	private final AbstractBuild<?, ?> build;
	private final String pathToWorkflow;

	public ActivitiProcessExecution(BuildListener listener,
			AbstractBuild<?, ?> build, String pathToWorkflow) {
		this.listener = listener;
		this.build = build;
		this.pathToWorkflow = pathToWorkflow;
	}

	public Boolean executeActivitProcess(File diagram) throws IOException,
			InterruptedException {

		final PrintStream logger = listener.getLogger();

		log(logger, "****************************");
		log(logger, "Start activiti BPMN workflow");
		log(logger, "****************************");

		// get the process engine instances
		ProcessEngine engine = JenkinsProcessEngine.getProcessEngine();

		String processId = null;

		DestructionCallbacks callbacks = new DestructionCallbacks();

		try {
			Assert.isTrue(
					StringUtils.endsWith(diagram.getName(), ".bpmn"),
					ErrorCode.ACTIVITI04);

			// deploy the BPMN process
			log(logger, "deploy BPMN process: " + diagram.getName());
			processId = ActivitiAccessor.deployProcessFromDiagramFile(engine,
					diagram);

			addActions(build, processId, logger, callbacks);

			Map<String, Object> variables = JenkinsUtils
					.getEnvironmentVars(build);
			ActivitiUtils.prepareDataAssociation(processId, variables.keySet());
			
			// start the BPMN process
			log(logger, "start BPMN process: " + diagram.getName());
			RuntimeService runtimeService = engine.getRuntimeService();
			ProcessInstance pi = runtimeService.startProcessInstanceById(
					processId, variables);

			ActivitiUtils.waitForProcessFinalization(build, pi);
			log(logger, "BPMN process finished");
		} catch (JenkinsJobFailedException ex) {
			log(logger, "delete aborted build BPMN process");
			log(logger, "[ERROR] " + ex.getMessage());
			deleteAbortedBuildExecution(engine, processId, build);

			return false;
		} catch (RuntimeException ex) {
			// delete the BPMN process execution if the build is aborted
			log(logger, "delete aborted build BPMN process");
			deleteAbortedBuildExecution(engine, processId, build);
			final String obj = ex.getMessage();
			return handleException(build, ACTIVITI09, ex, listener, obj);
		} catch (FileNotFoundException ex) {
			final String obj = diagram.getName();
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
	private boolean handleException(AbstractBuild<?, ?> build,
			ErrorCode errorCode, Exception ex, BuildListener listener,
			Object... variables) {

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
	 * Adds all persistent actions to the builder instance. <br />
	 * Returns the TaskStateAction Observer instance in order to register it to
	 * the Observable instance.
	 * 
	 * @param build
	 * @param processDefinitionId
	 * @return TaskStateAction
	 */
	private void addActions(AbstractBuild<?, ?> build,
			String processDefinitionId, PrintStream logger,
			DestructionCallbacks callbacks) throws JenkinsJobFailedException {

		File picture = ActivitiUtils.storeProcessDiagram(build,
				processDefinitionId);

		// add element coordinates action
		List<AbstractArea> elements = ActivitiUtils.getAreas(build,
				processDefinitionId);

		String workflowName = getWorkflowName();

		ActivitiWorkflowAction action = new ActivitiWorkflowAction(
				workflowName, processDefinitionId, picture, elements);
		action.setLogger(logger);
		build.addAction(action);
		callbacks.addDestructionCallback(action);
		callbacks.addDestructionCallback(new StoreMetadataCallback(build,
				action));

		// add activity properties
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(JOB_NAME_PROPERTY, build.getProject().getName());
		properties.put(BUILD_NUMBER_PROPERTY, build.getNumber());

		ActivitiUtils.addActivityProperties(processDefinitionId, properties);
		checkIfJobsExist(build);
		
		// add activity execution listeners
		ActivitiUtils.addActivityListeners(processDefinitionId,
				getExecutionListeners(action, callbacks));
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
			String workflowName = StringUtils.substringAfterLast(
					pathToWorkflow, File.separator);
			return StringUtils.substringBefore(workflowName, ".bpmn");
		}
		return StringUtils.substringBefore(pathToWorkflow, ".bpmn");
	}

	private boolean checkIfJobsExist(AbstractBuild build)
			throws JenkinsJobFailedException {
		List<JenkinsActivitiTaskHighlight> jenkinsTaskList = new ArrayList<JenkinsActivitiTaskHighlight>();

		List<ActivitiWorkflowAction> actionList = build
				.getActions(ActivitiWorkflowAction.class);
		if (actionList != null) {
			for (ActivitiWorkflowAction awa : actionList) {
				for (AbstractArea aa : awa.getElements()) {
					if (aa instanceof CallActivityTaskHighlight) {
						for (AbstractArea aa2 : ((CallActivityTaskHighlight) aa)
								.getElements()) {
							if (aa2 instanceof JenkinsActivitiTaskHighlight) {
								JenkinsActivitiTaskHighlight jath = (JenkinsActivitiTaskHighlight) aa2;
								if (!Jenkins.getInstance().getJobNames()
										.contains(jath.getJobName())) {
									throw new JenkinsJobFailedException(
											"could not find job \""
													+ jath.getJobName()
													+ "\" on jenkins!");
								}
							}
						}
					} else if (aa instanceof JenkinsActivitiTaskHighlight) {
						JenkinsActivitiTaskHighlight jath = (JenkinsActivitiTaskHighlight) aa;
						if (!Jenkins.getInstance().getJobNames()
								.contains(jath.getJobName())) {
							throw new JenkinsJobFailedException(
									"could not find job \"" + jath.getJobName()
											+ "\" on jenkins!");
						}
					}
				}
			}
		}
		return false;
	}
}
