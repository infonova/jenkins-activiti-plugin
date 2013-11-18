package com.bearingpoint.infonova.jenkins.util;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ActivityMetadataUTest {

	private ActivityMetadata metadata;

	private AbstractBuild<?, ?> build;

	@Before
	public void setUp() {
		this.build = Mockito.mock(AbstractBuild.class);
		this.metadata = new ActivityMetadata();
	}

	/**
	 * Tests if properties were stored.
	 */
	@Test
	public void testGetters() throws IOException {
		metadata.storeStart(entity("process1:1:8", "activity1"));
		metadata.storeEnd(entity("process1:1:8", "activity1"));
		metadata.storeStart(entity("process2:1:8", "activity2"));
		metadata.storeEnd(entity("process2:1:8", "activity2"));
				
		org.junit.Assert.assertNotNull(metadata.getStart("process1:1:8", "activity1"));
		org.junit.Assert.assertNotNull(metadata.getEnd("process1:1:8", "activity1"));
		org.junit.Assert.assertNotNull(metadata.getStart("process2:1:8", "activity2"));
		org.junit.Assert.assertNotNull(metadata.getEnd("process2:1:8", "activity2"));
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
