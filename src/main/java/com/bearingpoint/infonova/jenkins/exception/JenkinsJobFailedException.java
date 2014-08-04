package com.bearingpoint.infonova.jenkins.exception;

/**
 * This exception type is thrown when a Jenkins Job Build fails.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class JenkinsJobFailedException extends RuntimeException {

	
	public JenkinsJobFailedException() {
		super();
	}

	public JenkinsJobFailedException(String message) {
		super(message);
	}

	/** the serial version UID. */
	private static final long serialVersionUID = -5569921737182402699L;

}
