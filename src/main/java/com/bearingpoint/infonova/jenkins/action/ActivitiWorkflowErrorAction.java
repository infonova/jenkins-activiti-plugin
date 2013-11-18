package com.bearingpoint.infonova.jenkins.action;

import hudson.model.Action;

import java.io.File;

import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Persistent action used to store an activiti workflow exception information if
 * an error occurs.<br />
 * This action is not accessible by any URL.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("activitiworkflowerror")
public class ActivitiWorkflowErrorAction implements Action {

	private final ErrorCode errorCode;

	private final String errorRef;

	public ActivitiWorkflowErrorAction(ErrorCode errorCode, File errorRef) {
		this.errorCode = errorCode;
		this.errorRef = errorRef.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIconFileName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUrlName() {
		return null;
	}

	/**
	 * Returns the error code
	 * 
	 * @return ErrorCode
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * Returns the error reference path.
	 * 
	 * @return String
	 */
	public String getErrorRef() {
		return errorRef;
	}

}
