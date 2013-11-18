package com.bearingpoint.infonova.jenkins.view;

import hudson.util.ListBoxModel;

import org.junit.Test;
import org.springframework.util.Assert;

import com.bearingpoint.infonova.jenkins.view.BuildPipelineView.DescriptorImpl;

public class BuildPipelineViewUTest {

	@Test
	public void testDescriptor() {
		DescriptorImpl descriptor = new DescriptorImpl();
		
		Assert.notNull(descriptor.getDisplayName());
		
		ListBoxModel model = descriptor.doFillBuildsToDisplayItems();
		Assert.notNull(model);
		Assert.isTrue(model.size() > 0);
	}
	
}
