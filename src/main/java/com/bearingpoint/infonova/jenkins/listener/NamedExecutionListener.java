package com.bearingpoint.infonova.jenkins.listener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.pvm.PvmEvent;
import org.h2.util.StringUtils;

/**
 * Stores an {@link ExecutionListener} instance in combination with the
 * listener event name used by the registration of the listener.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class NamedExecutionListener {

	private final String eventName;

	private final ExecutionListener listener;

	public NamedExecutionListener(String eventName, ExecutionListener listener) {
		this.eventName = eventName;
		this.listener = listener;
	}

	public void notify(DelegateExecution execution) throws Exception {
		this.listener.notify(execution);
	}

	/**
	 * Returns the event name.
	 * 
	 * @return String
	 */
	public String getEventName() {
		return this.eventName;
	}

	/**
	 * Returns the execution listener.
	 * 
	 * @return ExecutionListener
	 */
	public ExecutionListener getExecutionListener() {
		return this.listener;
	}

	/**
	 * Indicates if the {@link ExecutionListener} is registered on the start
	 * event.
	 * 
	 * @return boolean
	 */
	public boolean isStartEvent() {
		return StringUtils.equals(eventName, PvmEvent.EVENTNAME_START);
	}

	/**
	 * Indicates if the {@link ExecutionListener} is registered on the end
	 * event.
	 * 
	 * @return boolean
	 */
	public boolean isEndEvent() {
		return StringUtils.equals(eventName, PvmEvent.EVENTNAME_END);
	}

	/**
	 * Indicates if the {@link ExecutionListener} is registered on the transition
	 * event.
	 * 
	 * @return boolean
	 */
	public boolean isTransitionEvent() {
		return StringUtils.equals(eventName, PvmEvent.EVENTNAME_TAKE);
	}

}
