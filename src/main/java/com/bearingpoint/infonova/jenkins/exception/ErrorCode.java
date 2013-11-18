package com.bearingpoint.infonova.jenkins.exception;

/**
 * Error code enumeration for activiti errors.
 * 
 * @author christian.weber
 * @since 1.0
 */
public enum ErrorCode {

	ACTIVITI01("activiti.notnull.deploymentid"),
	ACTIVITI02("activiti.notnull.resourcename"),
	ACTIVITI03("activiti.resource.notfound"),
	ACTIVITI04("activiti.runtime.invalidfilename"),
	ACTIVITI05("activiti.runtime.missingprocess"),
	ACTIVITI06("activiti.notsupported.receivetask"),
	ACTIVITI07("activiti.ioerror.processdiagram"),
	ACTIVITI08("activiti.notsupported.servicetask"),
	ACTIVITI09("activiti.runtime.error"),
	ACTIVITI10("activiti.runtime.filenotfound");

	private final String key;

	private ErrorCode(String key) {
		this.key = key;
	}

	/**
	 * Returns the error code key.
	 * 
	 * @return String
	 */
	public String getKey() {
		return this.key;
	}

}
