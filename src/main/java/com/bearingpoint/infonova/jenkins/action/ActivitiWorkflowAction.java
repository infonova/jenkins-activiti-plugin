package com.bearingpoint.infonova.jenkins.action;

import hudson.model.Action;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bearingpoint.infonova.jenkins.listener.ActivityEndListener;
import com.bearingpoint.infonova.jenkins.listener.ActivityStartListener;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;
import com.bearingpoint.infonova.jenkins.ui.CallActivityTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.TaskState;
import com.bearingpoint.infonova.jenkins.util.ActivityMetadata;
import com.bearingpoint.infonova.jenkins.util.DestructionCallback;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Persistent action used to store the process description id as well as the
 * diagram picture.<br />
 * This action is not accessible by any URL. The task states will be updates
 * automatically by the use of the observer pattern.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("activitiworkflow")
public class ActivitiWorkflowAction implements Action, Observer, DestructionCallback {

	private static final Logger LOG = Logger.getLogger(ActivitiWorkflowAction.class);

	private final String processDescriptionId;

	private final String workflowName;

	private final String picture;

	private final List<AbstractArea> elements;

	@XStreamOmitField
	private transient PrintStream logger;

	@XStreamOmitField
	private ActivityMetadata metadata;

	public ActivitiWorkflowAction(String workflowName, String processDescriptionId, File picture,
			List<AbstractArea> elements) {
		this.processDescriptionId = processDescriptionId;
		this.workflowName = workflowName;
		this.picture = picture.getName();
		this.elements = elements;

		this.metadata = new ActivityMetadata();
	}

	public void setLogger(PrintStream logger) {
		this.logger = logger;
	}

	public PrintStream getLogger() {
		return this.logger;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIconFileName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUrlName() {
		return null;
	}

	/**
	 * Returns the process description id.
	 * 
	 * @return String
	 */
	public String getProcessDescriptionId() {
		return this.processDescriptionId;
	}

	/**
	 * Returns the workflow name.
	 * 
	 * @return String
	 */
	public String getWorkflowName() {
		return this.workflowName;
	}

	/**
	 * Returns the picture.
	 * 
	 * @return File
	 */
	public String getPicture() {
		return this.picture;
	}

	/**
	 * Returns the {@link AbstractArea} instances.
	 * 
	 * @return List
	 */
	public List<AbstractArea> getElements() {
		return this.elements;
	}

	/**
	 * Returns the {@link TaskState} instances
	 * 
	 * @return Map
	 */
	public Map<String, TaskState> getStates() {
		return getStatesRecursive(elements);
	}

	// TODO: set modifier to private
	public Map<String, TaskState> getStatesRecursive(List<AbstractArea> elements) {
		Map<String, TaskState> map = new HashMap<String, TaskState>();
		for (AbstractArea area : elements) {
			map.put(area.getActivityId(), area.getState());

			// call activity traverse strategy
			if (area instanceof CallActivityTaskHighlight) {
				CallActivityTaskHighlight callActivity = (CallActivityTaskHighlight) area;
				Map<String, TaskState> map2 = getStatesRecursive(callActivity.getElements());
				for (String key : map2.keySet()) {
					map.put(callActivity.getActivityId() + "." + key, map2.get(key));
				}
			}
		}
		return map;
	}

	/**
	 * Returns the {@link TaskState} of the entire workflow process.
	 * 
	 * @return TaskState
	 */
	public TaskState getWorkflowState() {

		for (AbstractArea area : elements) {
			TaskState state = area.getState();

			if (TaskState.FAILURE.compareTo(state) == 0) {
				return TaskState.FAILURE;
			}
			if (TaskState.PENDING.compareTo(state) == 0) {
				return TaskState.PENDING;
			}
			if (TaskState.RUNNING.compareTo(state) == 0) {
				return TaskState.RUNNING;
			}
		}
		return TaskState.SUCCESS;
	}

	/**
	 * Sets all tasks with in state RUNNING to FAILURE.
	 */
	// TODO: set modifier to private
	public void setRunningTasksToFailure() {
		setRunningTasksToFailureRecursive(elements);
	}
	
	private void setRunningTasksToFailureRecursive(List<AbstractArea> areas) {
		for (AbstractArea area : areas) {
			TaskState state = area.getState();
			if (state.compareTo(TaskState.RUNNING) == 0) {
				area.setState(TaskState.FAILURE);
			}
			if (area instanceof CallActivityTaskHighlight) {
				setRunningTasksToFailureRecursive(((CallActivityTaskHighlight)area).getElements());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(Observable o, Object arg) {
		DelegateExecution execution = (DelegateExecution) arg;
		ExecutionEntity executionEntity = (ExecutionEntity) execution;

		if (o instanceof ActivityStartListener) {
			log("start activity " + executionEntity.getActivityId());
			updateState(executionEntity, TaskState.RUNNING);

			// updata metadata
			metadata.storeStart(executionEntity);
		} else if (o instanceof ActivityEndListener) {
			log("activity " + executionEntity.getActivityId() + " finished");
			updateState(executionEntity, TaskState.SUCCESS);

			// updata metadata
			metadata.storeEnd(executionEntity);
		}
	}

	/**
	 * Updates task state of the activity with the given activity id.
	 */
	private void updateState(ExecutionEntity entity, TaskState state) {
		updateStateRecursive(elements, entity, state);
	}

	private void updateStateRecursive(List<AbstractArea> areas, ExecutionEntity entity,
			TaskState state) {

		for (AbstractArea area : areas) {

			// check if the area has the same process id as well as the same
			// activity id
			if (matchActivity(area, entity)) {
				area.setState(state);

				LOG.info(area + " updated");

				// the area to update the state is found
				// leave the recursion
				return;
			}

			// check if the area is a sub process
			else if (matchCallActivity(area, entity)) {

				CallActivityTaskHighlight callActivity = (CallActivityTaskHighlight) area;
				callActivity.setState(state);

				// execute the recursion for all elements withing the call
				// activity
				updateStateRecursive(callActivity.getElements(), entity, state);

				LOG.info(area + " updated");

				// the area to update the state is found
				// leave the recursion
				return;
			}

		}
	}

	/**
	 * Indicates if the given entity matches the call activity.
	 * 
	 * @param area
	 * @param entity
	 * @return boolean
	 */
	private final boolean matchCallActivity(AbstractArea area, ExecutionEntity entity) {

		if (!(area instanceof CallActivityTaskHighlight)) {
			return false;
		}

		CallActivityTaskHighlight callActivity = (CallActivityTaskHighlight) area;

		final String processId = entity.getProcessDefinitionId();
		return StringUtils.equals(callActivity.getProcessDescriptionId(), processId);
	}

	/**
	 * Indicates if the given entity matches the activity.
	 * 
	 * @param area
	 * @param entity
	 * @return boolean
	 */
	private final boolean matchActivity(AbstractArea area, ExecutionEntity entity) {
		boolean expr1 = StringUtils.equals(area.getActivityId(), entity.getActivityId());
		boolean expr2 = StringUtils.equals(area.getProcessId(), entity.getProcessDefinitionId());

		return expr1 && expr2;
	}

	/**
	 * Logs the given message.
	 * 
	 * @param msg
	 */
	private void log(String msg) {
		if (logger != null) {
			logger.append(msg);
			logger.println();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() {
		setRunningTasksToFailure();
	}

	/**
	 * Returns the {@link AbstractArea} with the given process id and activity
	 * id.
	 * 
	 * @param processId
	 * @param activityId
	 * @return AbstractArea
	 */
	public AbstractArea findArea(String processId, String activityId) {
		return findAreaRecursive(processId, activityId, elements);
	}

	private AbstractArea findAreaRecursive(String processId, String activityId,
			List<AbstractArea> areas) {
		for (AbstractArea area : areas) {
			boolean expr1 = StringUtils.equals(area.getActivityId(), activityId);
			boolean expr2 = StringUtils.equals(area.getProcessId(), processId);

			if (expr1 && expr2) {
				return area;
			}

			if (area instanceof CallActivityTaskHighlight) {
				CallActivityTaskHighlight callactivity = (CallActivityTaskHighlight) area;
				AbstractArea tmp = findAreaRecursive(processId, activityId,
						callactivity.getElements());

				if (tmp != null) {
					return tmp;
				}
			}

		}
		return null;
	}

	/**
	 * Returns the {@link AbstractArea} which matches the the given entity.
	 * 
	 * @param processId
	 * @param activityId
	 * @return AbstractArea
	 */
	public AbstractArea findAreaByEntity(ExecutionEntity entity) {
		return findArea(entity.getProcessDefinitionId(), entity.getActivityId());
	}

	/**
	 * Returns the metadata.
	 * 
	 * @return ActivityMetadata
	 */
	public final ActivityMetadata getMetadata() {
		return this.metadata;
	}

	/**
	 * Returns all processIds, main process Id as well as all sub process ids,
	 * from the current process execution.
	 * 
	 * @return List
	 */
	public List<String> getProcessIds() {
		List<String> list = new ArrayList<String>();
		list.add(this.getProcessDescriptionId());
		
		for (AbstractArea area : elements) {
			if (area instanceof CallActivityTaskHighlight) {
				CallActivityTaskHighlight callactivity = (CallActivityTaskHighlight) area;
				list.add(callactivity.getProcessDescriptionId());
			}
		}
		
		return list;
	}

}
