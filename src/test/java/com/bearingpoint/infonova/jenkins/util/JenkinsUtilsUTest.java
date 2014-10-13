package com.bearingpoint.infonova.jenkins.util;

import hudson.model.Action;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.jvnet.hudson.reactor.ReactorException;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.exception.ActivitiWorkflowException;
import com.bearingpoint.infonova.jenkins.test.common.FreeStyleProjectMockBuilder;
import com.bearingpoint.infonova.jenkins.test.common.IntegrationTest;
import com.bearingpoint.infonova.jenkins.test.common.JenkinsMockBuilder;
import com.bearingpoint.infonova.jenkins.test.common.UnitTest;

@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest({ IOUtils.class, Jenkins.class })
public class JenkinsUtilsUTest {

	@Test
	@Category(UnitTest.class)
	public void testStoreException() {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		Mockito.when(build.getRootDir()).thenReturn(new File("target"));

		File target = JenkinsUtils.storeException(build, Mockito.mock(Exception.class));
		org.junit.Assert.assertNotNull(target);
	}

	@Category(UnitTest.class)
	@Test(expected = ActivitiWorkflowException.class)
	public void testStoreExceptionWithFailure1() throws IOException {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		Mockito.when(build.getRootDir()).thenReturn(new File("target"));

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(
				IOUtils.copy(Matchers.any(InputStream.class), Matchers.any(OutputStream.class)))
				.thenThrow(new FileNotFoundException());

		JenkinsUtils.storeException(build, Mockito.mock(Exception.class));
	}

	@Category(UnitTest.class)
	@Test(expected = ActivitiWorkflowException.class)
	public void testStoreExceptionWithFailure2() throws IOException {
		AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);
		Mockito.when(build.getRootDir()).thenReturn(new File("target"));

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(
				IOUtils.copy(Matchers.any(InputStream.class), Matchers.any(OutputStream.class)))
				.thenThrow(new IOException());

		JenkinsUtils.storeException(build, Mockito.mock(Exception.class));
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetProject() throws IOException, InterruptedException, ReactorException {
/*
		// jenkins configuration
		JenkinsMockBuilder.mock().withFreestyleProject("projectName");

		Project<?, ?> project = JenkinsUtils.getProject("projectName");
		org.junit.Assert.assertNotNull(project);

		project = JenkinsUtils.getProject("unknown");
		org.junit.Assert.assertNull(project); */
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetProjectAction() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild(new MockAction());

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder.project());

		Action action = JenkinsUtils.getProjectAction("projectName", 1, MockAction.class);
		org.junit.Assert.assertNotNull(action);
		org.junit.Assert.assertTrue(MockAction.class == action.getClass());
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetProjectActions() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild(new MockAction());

		// jenkins configuration
		JenkinsMockBuilder.mock().withFreestyleProject(builder.project());

		List<MockAction> actions = JenkinsUtils.getProjectActions("projectName", 1,
				MockAction.class);
		org.junit.Assert.assertNotNull(actions);
		org.junit.Assert.assertFalse(actions.isEmpty());
		org.junit.Assert.assertEquals(1, actions.size());
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetWorkflowAction() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild(activitiWorkflowAction("workflowName", "pid"));

		// jenkins configuraiton
		JenkinsMockBuilder.mock().withFreestyleProject(builder.project());

		ActivitiWorkflowAction action2 = JenkinsUtils.getWorkflowAction("projectName", 1, "pid");
		org.junit.Assert.assertNotNull(action2);
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetWorkflowActionNotFound() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild(activitiWorkflowAction("workflowName", "pid"));

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder.project());

		ActivitiWorkflowAction action2 = JenkinsUtils
				.getWorkflowAction("projectName", 1, "unknown");
		org.junit.Assert.assertNull(action2);
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetItemsWithAction() {

		// project1 configuration
		FreeStyleProjectMockBuilder builder1 = FreeStyleProjectMockBuilder.mock("projectName1");
		builder1.withBuild(activitiWorkflowAction("workflowName", "pid"));

		// project2 configuration
		FreeStyleProjectMockBuilder builder2 = FreeStyleProjectMockBuilder.mock("projectName2");
		builder1.withBuild(activitiWorkflowAction("workflowName", "pid"));

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder1.project());
		jenkins.withFreestyleProject(builder2.project());
		jenkins.withTopLevelItem();
		jenkins.withTopLevelItem();

		List<TopLevelItem> list = JenkinsUtils.getItemsWithAction(ActivitiWorkflowAction.class);
		org.junit.Assert.assertNotNull(list);
		org.junit.Assert.assertFalse(list.isEmpty());
		// org.junit.Assert.assertEquals(2, list.size());
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetLatestBuild() throws IOException, InterruptedException, ReactorException {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild().withBuild();

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder.project());

		AbstractBuild<?, ?> build = JenkinsUtils.getLatestBuild("projectName");
		org.junit.Assert.assertNotNull(build);
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetProjectBuilds() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild().withBuild();

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder.project());

		List<? extends AbstractBuild<?, ?>> builds = JenkinsUtils.getProjectBuilds("projectName");
		org.junit.Assert.assertNotNull(builds);
	}

	@Test(expected = IllegalArgumentException.class)
	@Category(IntegrationTest.class)
	public void testGetProjectBuildsWithException() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild().withBuild();

		// jenkins configuration
		JenkinsMockBuilder.mock().withTopLevelItem();

		JenkinsUtils.getProjectBuilds("projectName");
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetBuildNumberByProcessDefinitionId() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild().withBuild();

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder.project());

		int number = JenkinsUtils.getBuildNumberByProcessDefinitionId("pid", "projectName");
		System.out.println(number);
	}

	@Test
	@Category(IntegrationTest.class)
	public void testGetBuildNumberByProcessDefinitionIdUnknownBuild() {

		// project configuration
		FreeStyleProjectMockBuilder builder = FreeStyleProjectMockBuilder.mock("projectName");
		builder.withBuild().withBuild();

		// jenkins configuration
		JenkinsMockBuilder jenkins = JenkinsMockBuilder.mock();
		jenkins.withFreestyleProject(builder.project());

		Integer number = JenkinsUtils.getBuildNumberByProcessDefinitionId("pid2", "projectName");
		org.junit.Assert.assertNull(number);
	}
	
	@Test
	@Category(UnitTest.class)
	public void testNormalizeParameters() {
		Map<String, String> result1 = JenkinsUtils.normalizeParameters(null);
		org.junit.Assert.assertTrue(result1.isEmpty());

		Map<String, String> result2 = JenkinsUtils.normalizeParameters("");
		org.junit.Assert.assertTrue(result2.isEmpty());
		
		Map<String, String> result3 = JenkinsUtils.normalizeParameters(" ");
		org.junit.Assert.assertTrue(result3.isEmpty());
		
		Map<String, String> result4 = JenkinsUtils.normalizeParameters("[ ]");
		org.junit.Assert.assertTrue(result4.isEmpty());
		
		Map<String, String> result5 = JenkinsUtils.normalizeParameters("[name:value]");
		org.junit.Assert.assertFalse(result5.isEmpty());
		
		Map<String, String> result6 = JenkinsUtils.normalizeParameters("[name1:value1, name2:value2]");
		org.junit.Assert.assertFalse(result6.isEmpty());
	}

	private ActivitiWorkflowAction activitiWorkflowAction(String workflow,
			String processDefinitionId) {
		File picture = Mockito.mock(File.class);
		return new ActivitiWorkflowAction(workflow, processDefinitionId, picture, null);
	}

	/**
	 * Action mock implementation
	 * 
	 * @author christian.weber
	 * 
	 */
	public static class MockAction implements Action {

		public String getIconFileName() {
			return StringUtils.EMPTY;
		}

		public String getDisplayName() {
			return StringUtils.EMPTY;
		}

		public String getUrlName() {
			return StringUtils.EMPTY;
		}

	}

}
