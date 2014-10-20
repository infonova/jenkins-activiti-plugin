package com.bearingpoint.infonova.jenkins.ui;

import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Task state enumeration used to determine the activity element state.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("state")
public enum TaskState {

	RUNNING, SUCCESS, FAILURE, PENDING, UNSTABLE;

	@Override
	public String toString() {
		return StringUtils.lowerCase(name());
	}
	
}
