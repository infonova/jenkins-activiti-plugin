package com.bearingpoint.infonova.jenkins.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a list of {@link DestructionCallback} instances in order to execute
 * them at once at the end of the build execution.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class DestructionCallbacks {

	private final List<DestructionCallback> callbacks = new ArrayList<DestructionCallback>();

	/**
	 * Adds the given {@link DestructionCallback} instance to the list of
	 * registered callbacks.
	 * 
	 * @param callback
	 */
	public void addDestructionCallback(DestructionCallback callback) {
		this.callbacks.add(callback);
	}

	/**
	 * Calls all registered {@link DestructionCallback#destroy} methods.
	 */
	public void destroy() {
		for (DestructionCallback callback : callbacks) {
			callback.destroy();
		}
	}

}
