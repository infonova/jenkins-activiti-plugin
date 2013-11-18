package com.bearingpoint.infonova.jenkins.builder;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Project;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jcifs.util.LogStream;
import jenkins.model.Jenkins;
import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.test.common.FreeStyleProjectMockBuilder;
import com.bearingpoint.infonova.jenkins.test.common.JenkinsMockBuilder;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;

@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest({ AbstractBuild.class, Jenkins.class })
public class WorkflowBuilderUTest {

	@Rule
	public static Timeout timeout = new Timeout(20 * 1000);

	@SuppressWarnings("rawtypes")
	private AbstractBuild build;

	@Before
	public void setUp() {
		this.build = PowerMockito.mock(AbstractBuild.class);

		File rootDir = new File("target/builder");
		if (!rootDir.exists()) {
			rootDir.mkdir();
		}
		Mockito.when(build.getRootDir()).thenReturn(rootDir);
		Mockito.doCallRealMethod().when(build).addAction(Matchers.any(Action.class));
		Mockito.when(build.getAction(ActivitiWorkflowAction.class)).thenCallRealMethod();
		Mockito.when(build.getActions()).thenCallRealMethod();
	}

	/**
	 * Tests the successful run of the WorkflowBuilder.
	 */
	@Test
	public void testProcessExecution() throws InterruptedException, IOException {
		WorkflowBuilder builder = new WorkflowBuilder("testcase01.bpmn20.xml");
		boolean result = performBuilder(builder);

		Assert.assertTrue(result);
	}

	@Test
	public void testProcessExceptionHandling() throws InterruptedException, IOException {
		WorkflowBuilder builder = new WorkflowBuilder("testcase02.bpmn20.xml");
		boolean result = performBuilder(builder);

		Assert.assertFalse(result);
	}

	@Test
	public void testFileNotfound() throws InterruptedException, IOException {
		WorkflowBuilder builder = new WorkflowBuilder("notfound.bpmn20.xml");
		boolean result = performBuilder(builder);

		Assert.assertFalse(result);
	}
	
	@Test
	public void testInvalidFileName() throws InterruptedException, IOException {
		WorkflowBuilder builder = new WorkflowBuilder("testcase07.xml");
		boolean result = performBuilder(builder);
		
		Assert.assertFalse(result);
	}

	@Test
	public void testUserTask() throws InterruptedException {

		final WorkflowBuilder builder = new WorkflowBuilder("testcase03.bpmn20.xml");

		// the builder call have to run in a separate thread
		// due to the blocking user tasks
		Thread thread = new Thread() {

			@Override
			public void run() {
				boolean result = performBuilder(builder);
				Assert.assertTrue(result);
			}

		};

		thread.start();

		// wait until build action is set
		// this have to be done because the separate thread of the builder
		ActivitiWorkflowAction action = null;
		do {
			Thread.sleep(1000);
			action = build.getAction(ActivitiWorkflowAction.class);
		} while (action == null);

		// complete the user task
		// this simulates the user interaction with the process engine
		ActivitiAccessor.completeTask(action.getProcessDescriptionId(), "usertask1");

		thread.join();
	}

	/**
	 * Tests the successful run of the WorkflowBuilder.
	 */
	@Test
	public void testJenkinsJobExecution() throws InterruptedException, IOException {

		FreeStyleProjectMockBuilder builder1 = FreeStyleProjectMockBuilder.mock("DummyJob");
		builder1.mockScheduleBuild2();

		JenkinsMockBuilder jenkinsBuilder = JenkinsMockBuilder.mock();
		jenkinsBuilder.withFreestyleProject(builder1.project());

		WorkflowBuilder builder = new WorkflowBuilder("testcase04.bpmn20.xml");

		boolean result = performBuilder(builder);

		Assert.assertTrue(result);
	}
	
	/**
	 * Tests the successful run of the WorkflowBuilder.
	 */
	@Test
	public void testEmbeddedSubProcessJenkinsJobExecution() throws InterruptedException, IOException {
		
		FreeStyleProjectMockBuilder builder1 = FreeStyleProjectMockBuilder.mock("DummyJob");
		builder1.mockScheduleBuild2();
		
		JenkinsMockBuilder jenkinsBuilder = JenkinsMockBuilder.mock();
		jenkinsBuilder.withFreestyleProject(builder1.project());
		
		WorkflowBuilder builder = new WorkflowBuilder("testcase05.bpmn20.xml");
		
		boolean result = performBuilder(builder);
		
		Assert.assertTrue(result);
	}
	
	/**
	 * Tests the successful run of the WorkflowBuilder.
	 */
	@Test
	public void testSubProcessJenkinsJobExecution() throws InterruptedException, IOException {
		
		FreeStyleProjectMockBuilder builder1 = FreeStyleProjectMockBuilder.mock("DummyJob");
		builder1.mockScheduleBuild2();
		
		JenkinsMockBuilder jenkinsBuilder = JenkinsMockBuilder.mock();
		jenkinsBuilder.withFreestyleProject(builder1.project());
		
		WorkflowDeploymentBuilder builder2 = new WorkflowDeploymentBuilder("testcase06-prerequisite.bpmn20.xml");
		performDeploymentBuilder(builder2);
		
		WorkflowBuilder builder = new WorkflowBuilder("testcase06.bpmn20.xml");
		
		boolean result = performBuilder(builder);
		
		Assert.assertTrue(result);
	}
	
	/**
	 * Tests the successful run of the WorkflowBuilder.
	 */
	@Test
	public void testPrepareIOSpecification() throws InterruptedException, IOException {
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("testParameter1", "test");
		map.put("testParameter2", "test");

		Mockito.when(build.getBuildVariables()).thenReturn(map);
		
		FreeStyleProjectMockBuilder builder1 = FreeStyleProjectMockBuilder.mock("DummyJob");
		builder1.mockScheduleBuild2();
		
		JenkinsMockBuilder jenkinsBuilder = JenkinsMockBuilder.mock();
		jenkinsBuilder.withFreestyleProject(builder1.project());
		
		WorkflowDeploymentBuilder builder2 = new WorkflowDeploymentBuilder("testcase06-prerequisite.bpmn20.xml");
		performDeploymentBuilder(builder2);
		
		WorkflowBuilder builder = new WorkflowBuilder("testcase06.bpmn20.xml");
		
		boolean result = performBuilder(builder);
		
		Assert.assertTrue(result);
	}
	
	private boolean performBuilder(WorkflowBuilder builder) {
		URL url = getClass().getResource(builder.getPathToWorkflow());
		File file = FileUtils.toFile(url);
		FilePath filePath = url == null ? new FilePath(new File(builder.getPathToWorkflow()))
				: new FilePath(file.getParentFile());

		Project<?, ?> project = Mockito.mock(Project.class);

		Launcher launcher = Mockito.mock(Launcher.class);
		BuildListener listener = Mockito.mock(BuildListener.class);
		Mockito.when(listener.getLogger()).thenReturn(LogStream.getInstance());
		Mockito.when(build.getWorkspace()).thenReturn(filePath);
		Mockito.when(build.getProject()).thenReturn(project);

		return builder.perform(build, launcher, listener);
	}
	
	private boolean performDeploymentBuilder(WorkflowDeploymentBuilder builder) {
		URL url = getClass().getResource(builder.getPathToWorkflow());
		File file = FileUtils.toFile(url);
		FilePath filePath = url == null ? new FilePath(new File(builder.getPathToWorkflow()))
		: new FilePath(file.getParentFile());
		
		Project<?, ?> project = Mockito.mock(Project.class);
		
		Launcher launcher = Mockito.mock(Launcher.class);
		BuildListener listener = Mockito.mock(BuildListener.class);
		Mockito.when(listener.getLogger()).thenReturn(LogStream.getInstance());
		Mockito.when(build.getWorkspace()).thenReturn(filePath);
		Mockito.when(build.getProject()).thenReturn(project);
		
		return builder.perform(build, launcher, listener);
	}

}
