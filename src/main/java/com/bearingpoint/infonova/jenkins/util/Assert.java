package com.bearingpoint.infonova.jenkins.util;

import com.bearingpoint.infonova.jenkins.exception.ActivitiWorkflowException;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;

/**
 * Assertion utility class.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class Assert {
	
	/**
	 * Asserts if the given object is not null.
	 * 
	 * @param obj
	 */
	public static void notNull(Object obj, ErrorCode errorCode, Object...variables) {
		
		if (errorCode == null) {
			throw new IllegalArgumentException("error code must not be null");			
		}
		
		if (obj == null) {
			throw new ActivitiWorkflowException(errorCode, variables);
		}
		
	}
	
	/**
	 * Asserts if the given expression is true.
	 * 
	 * @param obj
	 */
	public static void isTrue(boolean expression, ErrorCode errorCode) {
		
		if (errorCode == null) {
			throw new IllegalArgumentException("error code must not be null");			
		}
		
		if (!expression) {
			throw new ActivitiWorkflowException(errorCode);
		}
		
	}

}
