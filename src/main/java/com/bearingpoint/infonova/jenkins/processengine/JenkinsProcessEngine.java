package com.bearingpoint.infonova.jenkins.processengine;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.impl.bpmn.parser.BpmnParseListener;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;

import com.bearingpoint.infonova.jenkins.listener.ResourceBpmnParseListener;
import com.bearingpoint.infonova.jenkins.listener.SupportedElementsBpmnParseListener;

/**
 * Configuration class for the activiti process engine.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class JenkinsProcessEngine {

	/** the activiti process engine. */
	private static ProcessEngine engine;

	private JenkinsProcessEngine() {
		super();
	}

	/**
	 * Initializes and returns the {@ProcessEngine}.
	 * 
	 * @return ProcessEngine
	 */
	public synchronized static ProcessEngine getProcessEngine() {

		if (engine != null) {
			return engine;
		}

		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
				.createStandaloneProcessEngineConfiguration();

		config.setPreParseListeners(getBpmnParseListeners());

		ClassLoader cl1 = JenkinsProcessEngine.class.getClassLoader();
		ClassLoader cl2 = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(cl1);

			config.setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000");
			config.setJdbcDriver("org.h2.Driver");
			config.setJdbcUsername("sa");
			config.setJdbcPassword("");

			config.setDatabaseSchemaUpdate("true");
			config.setJobExecutorActivate(false);

			config.setHistoryLevel(ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE);

			engine = config.buildProcessEngine();
			ProcessEngines.registerProcessEngine(engine);
		} finally {
			Thread.currentThread().setContextClassLoader(cl2);
		}
		return engine;
	}

	/**
	 * Returns a list of {@link BpmnParseListener} instances to register to the
	 * {@link ProcessEngine}.
	 * 
	 * @return List
	 */
	private static List<BpmnParseListener> getBpmnParseListeners() {
		List<BpmnParseListener> listeners = new ArrayList<BpmnParseListener>();

		listeners.add(new SupportedElementsBpmnParseListener());
		listeners.add(new ResourceBpmnParseListener());

		return listeners;
	}

}
