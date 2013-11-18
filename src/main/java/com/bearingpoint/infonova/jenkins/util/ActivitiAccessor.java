package com.bearingpoint.infonova.jenkins.util;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.log4j.Logger;

import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;

/**
 * Utility class for {@link ProcessEngine} functionality.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class ActivitiAccessor {

	private static Logger logger = Logger.getLogger(ActivitiAccessor.class);

	/**
	 * Returns the {@link ProcessEngine},
	 * 
	 * @return ProcessEngine
	 */
	public static ProcessEngine getProcessEngine() {
		return JenkinsProcessEngine.getProcessEngine();
	}

	/**
	 * Returns the {@link ProcessDefinition} instance by the given process
	 * definition id.
	 * 
	 * @param processDefinitionId
	 * @return ProcessDefinition
	 */
	public static ProcessDefinition getProcessDefinitionById(String processDefinitionId) {

		logger.info("get process definition by id " + processDefinitionId);

		return getProcessEngine().getRepositoryService().createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
	}

	// TODO: javadoc
	public static ProcessDefinitionEntity getProcessDefinitionEntity(String processDefinitionId) {
		RepositoryServiceImpl repositoryService = (RepositoryServiceImpl) ActivitiAccessor
				.getProcessEngine().getRepositoryService();

		return (ProcessDefinitionEntity) repositoryService
				.getDeployedProcessDefinition(processDefinitionId);
	}

	/**
	 * Deletes the current running process execution in case when the job
	 * execution is aborted during the execution of the BPMN process.
	 * 
	 * @param engine
	 *            the process engine
	 * @param processId
	 *            the process id to delete
	 * @param build
	 *            the aborted build
	 */
	public static void deleteAbortedBuildExecution(ProcessEngine engine, String processId,
			AbstractBuild<?, ?> build) {

		if (processId == null) {
			return;
		}

		RuntimeService runtimeService = engine.getRuntimeService();
		ExecutionQuery query = runtimeService.createExecutionQuery();
		Execution execution = query.processInstanceId(processId).singleResult();

		if (execution != null) {
			runtimeService
					.deleteProcessInstance(processId, build.getFullDisplayName() + " aborted");
		}
	}

	/**
	 * Deploys the given BPMN process diagram.
	 * 
	 * @param engine
	 *            the process engine
	 * @param diagram
	 *            the BPMN process diagram to deploy
	 * @return String the deployment id
	 * @throws FileNotFoundException
	 */
	public static String deployProcess(ProcessEngine engine, FilePath diagram)
			throws FileNotFoundException {

		// get the repository service
		RepositoryService repositoryService = engine.getRepositoryService();

		// deploy the BPMN process
		DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

		File file = new File(diagram.getRemote());
		deploymentBuilder.addInputStream(diagram.getBaseName() + ".xml", new FileInputStream(file));
		deploymentBuilder.name(diagram.getBaseName());
		Deployment deployment = deploymentBuilder.deploy();

		// return the BPMN process execution id
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

		return query.deploymentId(deployment.getId()).singleResult().getId();
	}

	/**
	 * Completes the task with the given id in order to continue the process
	 * execution. For example by user tasks.
	 * <br /><br />
	 * Continues the current blocking ACTIVITI user task with the given activity
	 * id executed by the process with the given process id.
	 * 
	 * @param processId
	 * @param activityId
	 */
	public static void completeTask(String processDefinitionId, String taskId) {

		TaskService service = getProcessEngine().getTaskService();
		TaskQuery query = service.createTaskQuery();

		Task task = query.processDefinitionId(processDefinitionId).taskDefinitionKey(taskId)
				.singleResult();
		service.complete(task.getId());

	}

}
