package com.bearingpoint.infonova.jenkins.exception;

/**
 * {@RuntimeException} implementation for activiti workflow errors.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class ActivitiWorkflowException extends RuntimeException {

	/** the serial version UID. */
	private static final long serialVersionUID = 6437014171765485481L;

	private final ErrorCode errorCode;

	public ActivitiWorkflowException(ErrorCode errorCode, Throwable e,
			Object... arguments) {
		super("(" + errorCode + ")"
				+ ErrorMessageResolver.resolve(errorCode, arguments), e);
		this.errorCode = errorCode;
	}

	public ActivitiWorkflowException(ErrorCode errorCode, Object... arguments) {
		super("(" + errorCode + ")"
				+ ErrorMessageResolver.resolve(errorCode, arguments));
		this.errorCode = errorCode;
	}

	/**
	 * Returns the error code.
	 * 
	 * @return ErrorCode
	 */
	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

}
