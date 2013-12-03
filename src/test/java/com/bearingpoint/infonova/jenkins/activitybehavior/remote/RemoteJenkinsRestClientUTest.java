package com.bearingpoint.infonova.jenkins.activitybehavior.remote;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.mockito.Mockito;

import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;

public class RemoteJenkinsRestClientUTest {

//	@Test
	public void testGetJobInfo() throws Exception {

		RemoteJenkinsRestClient client = new RemoteJenkinsRestClient("http", "8080",
				"192.168.9.201", "/view/EasyTax/view/Easytax_Sectioned_View/job/");

		AbstractRemoteJenkinsBuild build = client.getJobInfo("Easytax_Post_Commit");

		Assert.assertNotNull(build.getFullDisplayName());
		Assert.assertNotNull(build.getResult());
		Assert.assertNotNull(build.getNumber());
	}
	
//	@Test
	public void testScheduleJob() throws Exception {
		
		RemoteJenkinsRestClient client = new RemoteJenkinsRestClient("http", "8080",
				"192.168.9.201", "/view/EasyTax/view/Easytax_Sectioned_View/job/");
		
//		client.scheduleJob("Easytax_Post_Commit", Mockito.mock(ActivityExecution.class));
		client.scheduleJob("Easytax_Database_Oracle10_Regression_Test", new HashMap<String, Object>(), new HashMap<String, String>());
		
	}
	
//	@Test
	public void testParameterizedScheduleJob() throws Exception {
		
		RemoteJenkinsRestClient client = new RemoteJenkinsRestClient("http", "8080",
				"192.168.9.201", "/view/EasyTax/view/Easytax_Sectioned_View/job/");
		
//		client.scheduleJob("Easytax_Performance", Mockito.mock(ActivityExecution.class), new HashMap<String, String>());
		
		String parameters = "[testParameter1:3,testParameter2:4]";
		Map<String, String> params = JenkinsUtils.normalizeParameters(parameters);
		
//		client.scheduleJob("Easytax_Tmp", Mockito.mock(ActivityExecution.class), params);
		client.scheduleJob("Easytax-Archive-Test", new HashMap<String, Object>(), new HashMap<String, String>());
		
	}
	
//	@Test
	public void testParameterizedScheduleJobOverride() throws Exception {
		
		RemoteJenkinsRestClient client = new RemoteJenkinsRestClient("http", "8080",
				"192.168.9.201", "/view/EasyTax/view/Easytax_Sectioned_View/job/");
		
		ActivityExecution execution = Mockito.mock(ActivityExecution.class);
		Mockito.when(execution.getVariable("testParameter1")).thenReturn("2");
		Mockito.when(execution.getVariable("testParameter2")).thenReturn("true");
//		client.scheduleJob("Easytax_Post_Commit", Mockito.mock(ActivityExecution.class));
//		client.scheduleJob("Easytax_Tmp", execution);
		
	}

}
