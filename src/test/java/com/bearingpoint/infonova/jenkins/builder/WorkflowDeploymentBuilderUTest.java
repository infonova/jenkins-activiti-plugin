package com.bearingpoint.infonova.jenkins.builder;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Project;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jcifs.util.LogStream;
import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest(AbstractBuild.class)
public class WorkflowDeploymentBuilderUTest {

	@Test
	public void testProcessExecution() throws InterruptedException, IOException {
		WorkflowDeploymentBuilder builder = new WorkflowDeploymentBuilder("testcase01.bpmn20.xml");
		boolean result = performBuilder(builder);

		Assert.assertTrue(result);
	}

	@Test
	public void testProcessExceptionHandling() throws InterruptedException, IOException {
		WorkflowDeploymentBuilder builder = new WorkflowDeploymentBuilder("testcase02.bpmn20.xml");
		boolean result = performBuilder(builder);

		Assert.assertTrue(result);
	}

	@Test
	public void testFileNotfound() throws InterruptedException, IOException {
		WorkflowDeploymentBuilder builder = new WorkflowDeploymentBuilder("notfound.bpmn20.xml");
		boolean result = performBuilder(builder);

		Assert.assertFalse(result);
	}

	private boolean performBuilder(WorkflowDeploymentBuilder builder) {
		URL url = getClass().getResource(builder.getPathToWorkflow());
		File file = FileUtils.toFile(url);
		FilePath filePath = url == null ? new FilePath(new File(builder.getPathToWorkflow()))
				: new FilePath(file.getParentFile());

		Project<?, ?> project = Mockito.mock(Project.class);

		@SuppressWarnings("rawtypes")
		AbstractBuild build = PowerMockito.mock(AbstractBuild.class);

		Launcher launcher = Mockito.mock(Launcher.class);
		BuildListener listener = Mockito.mock(BuildListener.class);
		Mockito.when(listener.getLogger()).thenReturn(LogStream.getInstance());
		Mockito.when(build.getWorkspace()).thenReturn(filePath);
		Mockito.when(build.getProject()).thenReturn(project);

		return builder.perform(build, launcher, listener);
	}

}
