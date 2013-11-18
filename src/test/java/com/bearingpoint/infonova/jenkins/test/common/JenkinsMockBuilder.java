package com.bearingpoint.infonova.jenkins.test.common;

import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;

import jenkins.model.Jenkins;

import org.jvnet.hudson.reactor.ReactorException;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.mock.web.MockServletContext;

public final class JenkinsMockBuilder {

	private final Hudson hudson;

	private JenkinsMockBuilder(Hudson hudson) {

		PowerMockito.mockStatic(Jenkins.class);
		PowerMockito.when(Jenkins.getInstance()).thenReturn(hudson);

		this.hudson = hudson;
	}

	public static JenkinsMockBuilder mock() {
		try {
			if (Jenkins.getInstance() == null) {
				Hudson hudson = new Hudson(new File("target"), new MockServletContext());
				return new JenkinsMockBuilder(hudson);
			}
			Hudson hudson = (Hudson) Jenkins.getInstance();
			hudson.cleanUp();
			hudson = new Hudson(new File("target"), new MockServletContext());
			return new JenkinsMockBuilder(hudson);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ReactorException e) {
			throw new RuntimeException(e);
		}
	}

	public JenkinsMockBuilder withFreestyleProject(String name) {

		try {
			ItemGroup<?> parent = Mockito.mock(ItemGroup.class);
			Mockito.when(parent.getFullDisplayName()).thenReturn(name + "_parent");

			TopLevelItem item = new FreeStyleProject(parent, name);
			hudson.putItem(item);

			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public JenkinsMockBuilder withFreestyleProject(FreeStyleProject project) {

		try {
			hudson.putItem(project);

			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public JenkinsMockBuilder withTopLevelItem() {
		try {
			TopLevelItem item = Mockito.mock(TopLevelItem.class);

			Mockito.when(item.getName()).thenReturn("mock");
			Mockito.when(item.getFullDisplayName()).thenReturn("mock");
			Mockito.when(item.hasPermission(Matchers.any(Permission.class))).thenReturn(true);

			hudson.putItem(item);

			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public Hudson hudson() {
		return hudson;
	}

}
