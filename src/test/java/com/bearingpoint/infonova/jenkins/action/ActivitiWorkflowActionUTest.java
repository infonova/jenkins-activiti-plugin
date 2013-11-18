package com.bearingpoint.infonova.jenkins.action;

import static com.bearingpoint.infonova.jenkins.ui.TaskState.FAILURE;
import static com.bearingpoint.infonova.jenkins.ui.TaskState.PENDING;
import static com.bearingpoint.infonova.jenkins.ui.TaskState.RUNNING;
import static com.bearingpoint.infonova.jenkins.ui.TaskState.SUCCESS;
import hudson.model.Run;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.bearingpoint.infonova.jenkins.configuration.PluginConfiguration;
import com.bearingpoint.infonova.jenkins.listener.ActivityEndListener;
import com.bearingpoint.infonova.jenkins.listener.ActivityStartListener;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;
import com.bearingpoint.infonova.jenkins.ui.TaskState;

public class ActivitiWorkflowActionUTest {

	private ActivitiWorkflowAction action;

	@Before
	public void setUp() {
		PluginConfiguration.addAliases();
		InputStream stream = getClass().getResourceAsStream("testreference01.xml");
		this.action = (ActivitiWorkflowAction) Run.XSTREAM2.fromXML(stream);
		this.action.setLogger(Mockito.mock(PrintStream.class));
	}

	/**
	 * Tests the task state update functionality.
	 */
	@Test
	public void testUpdateState() {

		// Test Start Event
		// ****************
		ExecutionEntity entity1 = entity("process1:1:8", "startevent1");

		action.update(new ActivityStartListener(), entity1);
		AbstractArea area1 = action.findAreaByEntity(entity1);
		org.junit.Assert.assertTrue(RUNNING.compareTo(area1.getState()) == 0);

		action.update(new ActivityEndListener(), entity1);
		AbstractArea area2 = action.findAreaByEntity(entity1);
		org.junit.Assert.assertTrue(SUCCESS.compareTo(area2.getState()) == 0);

		// Test End Event
		// ****************
		ExecutionEntity entity2 = entity("process1:1:8", "endevent1");

		action.update(new ActivityStartListener(), entity2);
		AbstractArea area3 = action.findAreaByEntity(entity2);
		org.junit.Assert.assertTrue(RUNNING.compareTo(area3.getState()) == 0);

		action.update(new ActivityEndListener(), entity2);
		AbstractArea area4 = action.findAreaByEntity(entity2);
		org.junit.Assert.assertTrue(SUCCESS.compareTo(area4.getState()) == 0);

	}
	
	@Test
	public void testUpdateStateWithCallActivity() {
		
		// Call activity Task
		// ******************
		ExecutionEntity entity1 = entity("process1:1:8", "callactivity1");
		
		action.update(new ActivityStartListener(), entity1);
		AbstractArea area1 = action.findAreaByEntity(entity1);
		org.junit.Assert.assertTrue(RUNNING.compareTo(area1.getState()) == 0);

		action.update(new ActivityEndListener(), entity1);
		AbstractArea area2 = action.findAreaByEntity(entity1);
		org.junit.Assert.assertTrue(SUCCESS.compareTo(area2.getState()) == 0);
		
		// Start event
		// ***********
		ExecutionEntity entity2 = entity("SubProcess:1:4", "startevent1");
		
		action.update(new ActivityStartListener(), entity2);
		AbstractArea area3 = action.findAreaByEntity(entity2);
		org.junit.Assert.assertTrue(RUNNING.compareTo(area3.getState()) == 0);

		action.update(new ActivityEndListener(), entity2);
		AbstractArea area4 = action.findAreaByEntity(entity2);
		org.junit.Assert.assertTrue(SUCCESS.compareTo(area4.getState()) == 0);
		
	}

	/**
	 * Tests the get task states functionality.
	 */
	@Test
	public void testGetTaskStates() {
		Map<String, TaskState> states = action.getStates();
		org.junit.Assert.assertNotNull(states);
		
		TaskState state1 = states.get("startevent1");
		org.junit.Assert.assertTrue(PENDING.compareTo(state1) == 0);
		
		TaskState state2 = states.get("callactivity1");
		org.junit.Assert.assertTrue(PENDING.compareTo(state2) == 0);
		
		TaskState state3 = states.get("callactivity1.startevent1");
		org.junit.Assert.assertTrue(PENDING.compareTo(state3) == 0);
		
		TaskState state4 = states.get("callactivity1.scripttask1");
		org.junit.Assert.assertTrue(PENDING.compareTo(state4) == 0);
		
		TaskState state5 = states.get("callactivity1.endevent1");
		org.junit.Assert.assertTrue(PENDING.compareTo(state5) == 0);
		
		TaskState state6 = states.get("endevent1");
		org.junit.Assert.assertTrue(PENDING.compareTo(state6) == 0);
	}

	@Test
	public void testGetWorkflowState() {
		
		// PENDING state
		TaskState state = action.getWorkflowState();
		org.junit.Assert.assertTrue(PENDING.compareTo(state) == 0);

		// FAILURE state
		action.getElements().get(0).setState(FAILURE);
		state = action.getWorkflowState();
		org.junit.Assert.assertTrue(FAILURE.compareTo(state) == 0);
		
		// RUNNING state
		action.getElements().get(0).setState(RUNNING);
		state = action.getWorkflowState();
		org.junit.Assert.assertTrue(RUNNING.compareTo(state) == 0);
		
		// SUCCESS state
		action.getElements().get(0).setState(SUCCESS);
		state = action.getWorkflowState();
		org.junit.Assert.assertTrue(PENDING.compareTo(state) == 0);
		
		for (AbstractArea area : action.getElements()) {
			area.setState(SUCCESS);
		}
		state = action.getWorkflowState();
		org.junit.Assert.assertTrue(SUCCESS.compareTo(state) == 0);
	}
	
	@Test
	public void testDestroy() {
		
		for (AbstractArea area : action.getElements()) {
			area.setState(RUNNING);
		}

		action.destroy();
		
		for (AbstractArea area : action.getElements()) {
			org.junit.Assert.assertTrue(FAILURE.compareTo(area.getState()) == 0);
		}
		
	}
	
	@Test
	public void testInitialization() {
		final String workflowName = "workflowname";
		final String processId = "pid";
		final File file = new File(".");
		final List<AbstractArea> elements = new ArrayList<AbstractArea>();
		
		this.action = new ActivitiWorkflowAction(workflowName, processId, file, elements);
		this.action.setLogger(Mockito.mock(PrintStream.class));
		
		org.junit.Assert.assertNull(action.getDisplayName());
		org.junit.Assert.assertNull(action.getIconFileName());
		org.junit.Assert.assertNull(action.getUrlName());
		org.junit.Assert.assertNotNull(action.getPicture());
		org.junit.Assert.assertNotNull(action.getProcessDescriptionId());
		org.junit.Assert.assertNotNull(action.getElements());
		org.junit.Assert.assertNotNull(action.getMetadata());
		org.junit.Assert.assertNotNull(action.getWorkflowName());
		org.junit.Assert.assertNotNull(action.getLogger());
	}
	
	/**
	 * Initializes a ExecutionEntity instance.
	 * 
	 * @param processId
	 * @param activityId
	 * @return ExecutionEntity
	 */
	private final ExecutionEntity entity(String processId, String activityId) {
		ExecutionEntity entity = Mockito.mock(ExecutionEntity.class);

		Mockito.when(entity.getProcessDefinitionId()).thenReturn(processId);
		Mockito.when(entity.getActivityId()).thenReturn(activityId);

		return entity;
	}

}
