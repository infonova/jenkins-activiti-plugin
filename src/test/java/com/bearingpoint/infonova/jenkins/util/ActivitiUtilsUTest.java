package com.bearingpoint.infonova.jenkins.util;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bearingpoint.infonova.jenkins.exception.ActivitiWorkflowException;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.factory.CallActivityHighlightFactory;
import com.bearingpoint.infonova.jenkins.factory.EventHighlightFactory;
import com.bearingpoint.infonova.jenkins.factory.GatewayHighlightFactory;
import com.bearingpoint.infonova.jenkins.factory.TaskHighlightFactory;
import com.bearingpoint.infonova.jenkins.listener.NamedExecutionListener;
import static com.bearingpoint.infonova.jenkins.listener.ResourceBpmnParseListener.SCRIPT_PROPERTY;

@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest({ ActivitiAccessor.class, TaskHighlightFactory.class,
		EventHighlightFactory.class, GatewayHighlightFactory.class,
		CallActivityHighlightFactory.class, IOUtils.class })
public class ActivitiUtilsUTest {

	private RepositoryService repositoryService;

	@Before
	public void setUp() {

		PowerMockito.mockStatic(ActivitiAccessor.class);
		PowerMockito.mockStatic(TaskHighlightFactory.class);
		PowerMockito.mockStatic(EventHighlightFactory.class);
		PowerMockito.mockStatic(GatewayHighlightFactory.class);
		PowerMockito.mockStatic(CallActivityHighlightFactory.class);
		PowerMockito.mockStatic(IOUtils.class);

		ProcessEngine engine = Mockito.mock(ProcessEngine.class);
		this.repositoryService = Mockito.mock(RepositoryService.class);
		Mockito.reset(repositoryService);

		PowerMockito.when(ActivitiAccessor.getProcessEngine()).thenReturn(engine);
		PowerMockito.when(engine.getRepositoryService()).thenReturn(repositoryService);
	}

	@Test
	public void testIsActivity() {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getProperty("type")).thenReturn("Task");

		org.junit.Assert.assertTrue(ActivitiUtils.isActivity(activity));
	}

	@Test
	public void testIsEvent() {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getProperty("type")).thenReturn("Event");

		org.junit.Assert.assertTrue(ActivitiUtils.isEvent(activity));
	}

	@Test
	public void testIsGateway() {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getProperty("type")).thenReturn("Gateway");

		org.junit.Assert.assertTrue(ActivitiUtils.isGateway(activity));
	}

	@Test
	public void testIsSubProcess() {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getProperty("type")).thenReturn("callActivity");

		org.junit.Assert.assertTrue(ActivitiUtils.isCallActivity(activity));
	}

	@Test
	public void testIsCallActivity() {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getProperty("type")).thenReturn("subProcess");

		org.junit.Assert.assertTrue(ActivitiUtils.isSubProcess(activity));
	}

	@Test(expected = ActivitiWorkflowException.class)
	public void testGetDiagramResourceAsStreamExpectErrorCodeActiviti01() {
		ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionById(Matchers.anyString()))
				.thenReturn(definition);

		try {
			ActivitiUtils.getDiagramResourceAsStream("test");
		} catch (ActivitiWorkflowException ex) {
			org.junit.Assert.assertTrue(ex.getErrorCode().compareTo(ErrorCode.ACTIVITI01) == 0);
			throw ex;
		}
	}

	@Test(expected = ActivitiWorkflowException.class)
	public void testGetDiagramResourceAsStreamExpectErrorCodeActiviti02() {
		ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionById(Matchers.anyString()))
				.thenReturn(definition);

		Mockito.when(definition.getDeploymentId()).thenReturn("test");

		try {
			ActivitiUtils.getDiagramResourceAsStream("test");
		} catch (ActivitiWorkflowException ex) {
			org.junit.Assert.assertTrue(ex.getErrorCode().compareTo(ErrorCode.ACTIVITI02) == 0);
			throw ex;
		}
	}

	@Test
	public void testGetDiagramResourceAsStream() {
		ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionById(Matchers.anyString()))
				.thenReturn(definition);

		Mockito.when(definition.getDeploymentId()).thenReturn("deploymentId");
		Mockito.when(definition.getDiagramResourceName()).thenReturn("resourceName");

		ActivitiUtils.getDiagramResourceAsStream("test");

		Mockito.verify(repositoryService).getResourceAsStream("deploymentId", "resourceName");
	}

	@Test
	public void testGetAreas() {

		mockActivities();
		
		AbstractBuild<?, ?> build = mockBuild();
		ActivitiUtils.getAreas(build, "processDefinition");

		// verify hightlight factories
		PowerMockito.verifyStatic();
		
		TaskHighlightFactory.getObject(Matchers.any(ActivityImpl.class), Matchers.anyInt(),
				Matchers.anyInt(), Matchers.eq(build));
		
		EventHighlightFactory.getObject(Matchers.any(ActivityImpl.class), Matchers.anyInt(),
				Matchers.anyInt());
		
		GatewayHighlightFactory.getObject(Matchers.any(ActivityImpl.class), Matchers.anyInt(),
				Matchers.anyInt());
		
		CallActivityHighlightFactory.getObject(Matchers.any(ActivityImpl.class), Matchers.anyInt(),
				Matchers.anyInt(), Matchers.eq(build));
	}

	@Test
	public void testAddActivityProperties() {
		List<ActivityImpl> activities = mockActivities();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("test1", "test");
		properties.put("test2", "test");

		ActivitiUtils.addActivityProperties("processDefinition", properties);

		for (ActivityImpl activity : activities) {
			Mockito.verify(activity).setProperty("test1", "test");
			Mockito.verify(activity).setProperty("test2", "test");
		}
	}

	@Test
	public void testAddActivityListeners() {

		List<ActivityImpl> activities = mockActivities("processDefinition");

		NamedExecutionListener listener1 = Mockito.mock(NamedExecutionListener.class);
		Mockito.when(listener1.isStartEvent()).thenReturn(true);
		NamedExecutionListener listener2 = Mockito.mock(NamedExecutionListener.class);

		// mock process definition query
		ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
		ProcessDefinitionQuery query = Mockito.mock(ProcessDefinitionQuery.class);
		Mockito.when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
		Mockito.when(query.processDefinitionKey(Matchers.anyString())).thenReturn(query);
		Mockito.when(query.latestVersion()).thenReturn(query);
		Mockito.when(query.singleResult()).thenReturn(definition);
		Mockito.when(definition.getId()).thenReturn("subProcessId");

		ProcessDefinitionEntity entity = Mockito.mock(ProcessDefinitionEntity.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionEntity("subProcessId")).thenReturn(
				entity);

		Mockito.when(entity.getActivities()).thenReturn(new ArrayList<ActivityImpl>());

		ActivitiUtils.addActivityListeners("processDefinition", listener1, listener2);

		// verify execution listener
		for (ActivityImpl activity : activities) {
			Mockito.verify(activity).addExecutionListener(Matchers.anyString(),
					Matchers.any(ExecutionListener.class));
			Mockito.verify(activity).addExecutionListener(Matchers.anyString(),
					Matchers.any(ExecutionListener.class), Matchers.eq(0));
		}
	}

	/**
	 * Configures the activity mock instance.
	 * 
	 * @return ActivityImpl
	 */
	private final ActivityImpl mockActivity() {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getProperty(SCRIPT_PROPERTY)).thenReturn("script");

		return activity;
	}

	@Test
	public void testStoreProcessDiagram() {
		mockProcessDefinition();
		ActivitiUtils.storeProcessDiagram(mockBuild(), "processDefinition");
	}

	@Test(expected = ActivitiWorkflowException.class)
	public void testStoreProcessDiagramExpectFileNotFoundException() throws IOException {
		mockProcessDefinition();

		// throw exception when IOUtils method is invoked
		InputStream in = Matchers.any(InputStream.class);
		OutputStream out = Matchers.any(OutputStream.class);
		PowerMockito.when(IOUtils.copy(in, out)).thenThrow(new FileNotFoundException());

		ActivitiUtils.storeProcessDiagram(mockBuild(), "processDefinition");
	}

	@Test(expected = ActivitiWorkflowException.class)
	public void testStoreProcessDiagramExpectIOException() throws IOException {
		mockProcessDefinition();

		// throw exception when IOUtils method is invoked
		InputStream in = Matchers.any(InputStream.class);
		OutputStream out = Matchers.any(OutputStream.class);
		PowerMockito.when(IOUtils.copy(in, out)).thenThrow(new IOException());

		ActivitiUtils.storeProcessDiagram(mockBuild(), "processDefinition");
	}

	@Test
	public void testStoreProcessResources() {

		// mock configuration
		mockProcessDefinition();

		File target = ActivitiUtils.storeProcessResources(mockBuild(), mockActivity());
		org.junit.Assert.assertNotNull(target);
	}

	@Test(expected = ActivitiWorkflowException.class)
	public void testStoreProcessResourcesExpectFileNotFoundException() throws IOException {

		// throw exception when IOUtils method is invoked
		InputStream in = Matchers.any(InputStream.class);
		OutputStream out = Matchers.any(OutputStream.class);
		PowerMockito.when(IOUtils.copy(in, out)).thenThrow(new FileNotFoundException());

		mockProcessDefinition();
		ActivitiUtils.storeProcessResources(mockBuild(), mockActivity());
	}

	@Test(expected = ActivitiWorkflowException.class)
	public void testStoreProcessResourcesExpectIOException() throws IOException {

		// mock configuration
		ActivityImpl activity = mockActivity();
		mockProcessDefinition();

		// throw exception when IOUtils method is invoked
		InputStream in = Matchers.any(InputStream.class);
		OutputStream out = Matchers.any(OutputStream.class);
		PowerMockito.when(IOUtils.copy(in, out)).thenThrow(new IOException());

		ActivitiUtils.storeProcessResources(mockBuild(), activity);
	}

	private final AbstractBuild<?, ?> mockBuild() {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		Mockito.when(build.getRootDir()).thenReturn(new File("target"));
		return build;
	}

	private final ProcessDefinition mockProcessDefinition() {
		ProcessDefinition definition = Mockito.mock(ProcessDefinition.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionById("processDefinition"))
				.thenReturn(definition);

		Mockito.when(definition.getDeploymentId()).thenReturn("deploymentId");
		Mockito.when(definition.getDiagramResourceName()).thenReturn("resourceName");

		return definition;
	}

	private final List<ActivityImpl> mockActivities(String processDefinition) {
		ProcessDefinitionEntity entity = Mockito.mock(ProcessDefinitionEntity.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionEntity(processDefinition))
				.thenReturn(entity);

		List<ActivityImpl> activities = new ArrayList<ActivityImpl>();
		activities.add(activity("Task"));
		activities.add(activity("Event"));
		activities.add(activity("Gateway"));
		activities.add(activity("callActivity"));
		activities.add(activity("subProcess"));

		Mockito.when(entity.getActivities()).thenReturn(activities);

		return activities;
	}

	private final List<ActivityImpl> mockActivities() {
		ProcessDefinitionEntity entity = Mockito.mock(ProcessDefinitionEntity.class);
		PowerMockito.when(ActivitiAccessor.getProcessDefinitionEntity(Matchers.anyString()))
				.thenReturn(entity);

		List<ActivityImpl> activities = new ArrayList<ActivityImpl>();
		activities.add(activity("Task"));
		activities.add(activity("Event"));
		activities.add(activity("Gateway"));
		activities.add(activity("callActivity"));
		activities.add(activity("subProcess"));

		Mockito.when(entity.getActivities()).thenReturn(activities);

		return activities;
	}

	private ActivityImpl activity(String type) {
		ActivityImpl activity = Mockito.mock(ActivityImpl.class);
		Mockito.when(activity.getX()).thenReturn(0);
		Mockito.when(activity.getY()).thenReturn(0);
		Mockito.when(activity.getProperty("type")).thenReturn(type);

		return activity;
	}

}
