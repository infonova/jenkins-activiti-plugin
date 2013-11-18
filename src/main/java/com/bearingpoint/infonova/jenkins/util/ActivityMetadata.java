package com.bearingpoint.infonova.jenkins.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.apache.commons.lang.StringUtils;

public final class ActivityMetadata {

	private Map<String, Object> metadata = new HashMap<String, Object>();

	public ActivityMetadata() {
		super();
	}

	/**
	 * Stores the activity start timestamp.
	 * 
	 * @param area
	 */
	public void storeStart(ExecutionEntity entity) {

		if (metadata == null) {
			return;
		}

		final String processId = StringUtils.substringBefore(entity.getProcessDefinitionId(), ":");
		final String key = processId + "." + entity.getActivityId() + ".start";
		metadata.put(key, System.currentTimeMillis() + "");
	}

	/**
	 * Stores the activity end timestamp.
	 * 
	 * @param area
	 */
	public void storeEnd(ExecutionEntity entity) {

		if (metadata == null) {
			return;
		}

		final String processId = StringUtils.substringBefore(entity.getProcessDefinitionId(), ":");
		final String key = processId + "." + entity.getActivityId() + ".end";
		metadata.put(key, System.currentTimeMillis() + "");
	}

	public String getStart(String processId, String activityId) {
		
		if (metadata == null) {
			return null;
		}
		
		processId = StringUtils.substringBefore(processId, ":");
		final String key = processId + "." + activityId + ".start";

		return (String) metadata.get(key);
	}

	public String getEnd(String processId, String activityId) {
		
		if (metadata == null) {
			return null;
		}
			
		processId = StringUtils.substringBefore(processId, ":");
		final String key = processId + "." + activityId + ".end";

		return (String) metadata.get(key);
	}
	
	public final Properties getProperties() {
		Properties properties = new Properties();
		properties.putAll(metadata);
		
		return properties;
	}

}
