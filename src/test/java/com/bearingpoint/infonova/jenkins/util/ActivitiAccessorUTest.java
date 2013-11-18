package com.bearingpoint.infonova.jenkins.util;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;

public class ActivitiAccessorUTest {

	private ProcessEngine engine;

	@Before
	public void setUp() {
		this.engine = JenkinsProcessEngine.getProcessEngine();
	}

	/**
	 * Tests the successful deployment case.
	 * 
	 * @throws FileNotFoundException
	 */
	@Test
	public void testDeployProcess() throws FileNotFoundException {
		URL url = getClass().getResource("testcase01.bpmn20.xml");

		FilePath filePath = new FilePath(new File(url.getPath()));
		ActivitiAccessor.deployProcess(engine, filePath);
	}

	/**
	 * Tests the unsuccessful deployment case due to unknown file.
	 */
	@Test(expected = FileNotFoundException.class)
	public void testDeployProcessFileNotFound() throws FileNotFoundException {
		FilePath filePath = new FilePath(new File("unknown.bpmn20.xml"));
		ActivitiAccessor.deployProcess(engine, filePath);
	}

	/**
	 * Tests the successful get process definition by id case.
	 */
	@Test
	public void testGetProcessDefinitionById() throws FileNotFoundException {

		// deploy the test process
		final String id = deployProcess("testcase01.bpmn20.xml");

		ProcessDefinition def1 = ActivitiAccessor.getProcessDefinitionById(id);
		ProcessDefinition def2 = ActivitiAccessor.getProcessDefinitionById(id);

		org.junit.Assert.assertNotNull(def1);
		org.junit.Assert.assertNotNull(def2);

		org.junit.Assert.assertEquals(def1.getId(), def2.getId());
	}

	@Test
	public void testGetProcessDefinitionEntity() throws FileNotFoundException {
		final String id = deployProcess("testcase01.bpmn20.xml");
		ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(id);
		
		org.junit.Assert.assertNotNull(entity);
	}
	
	/**
	 * Tests the unsuccessful get process definition by id case due to unknown
	 * id.
	 */
	@Test
	public void testGetProcessDefinitionByIdUnknownId() throws FileNotFoundException {
		ProcessDefinition def = ActivitiAccessor.getProcessDefinitionById("unknown");
		org.junit.Assert.assertNull(def);
	}
	
	@Test
	public void testDeleteAbortedBuildExecution() throws FileNotFoundException {
		
		// deploy the process
		final String id = deployProcess("testcase02.bpmn20.xml");
		final AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		
		RuntimeService service = engine.getRuntimeService();
		
		// start the process
		// the script task should be asynchronous
		ProcessInstance instance = service.startProcessInstanceById(id);
		
		// delete the deployed process
		ActivitiAccessor.deleteAbortedBuildExecution(engine, instance.getId(), build);
		
		instance = service.createProcessInstanceQuery().processDefinitionId(id).singleResult();
		org.junit.Assert.assertNull(instance);
	}

	/**
	 * Deploys the given process.
	 * 
	 * @param resource
	 * @return String the process definition id
	 * @throws FileNotFoundException
	 */
	private final String deployProcess(String resource) throws FileNotFoundException {
		URL url = getClass().getResource(resource);

		FilePath filePath = new FilePath(new File(url.getPath()));
		return ActivitiAccessor.deployProcess(engine, filePath);
	}

}
