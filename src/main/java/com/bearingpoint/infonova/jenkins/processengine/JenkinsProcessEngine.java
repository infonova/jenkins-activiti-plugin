package com.bearingpoint.infonova.jenkins.processengine;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.parse.BpmnParseHandler;

import com.bearingpoint.infonova.jenkins.parsehandler.CustomCallActivityParseHandler;
import com.bearingpoint.infonova.jenkins.parsehandler.CustomScriptTaskParseHandler;
import com.bearingpoint.infonova.jenkins.parsehandler.CustomServiceTaskParseHandler;

/**
 * Configuration class for the ACTIVITI process engine.
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

        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration
            .createStandaloneProcessEngineConfiguration();

        config.setPostBpmnParseHandlers(getBpmnParseHandlers());

        ClassLoader cl1 = JenkinsProcessEngine.class.getClassLoader();
        ClassLoader cl2 = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(cl1);

            config.setJdbcUrl("jdbc:h2:mem:activiti");
            config.setJdbcDriver("org.h2.Driver");
            config.setJdbcUsername("sa");
            config.setJdbcPassword("");

            config.setDatabaseSchemaUpdate("true");
            config.setJobExecutorActivate(false);

            config.setHistoryLevel(HistoryLevel.NONE);
            config.setHistory("none");

            engine = config.buildProcessEngine();
            ProcessEngines.registerProcessEngine(engine);
        } finally {
            Thread.currentThread().setContextClassLoader(cl2);
        }
        return engine;
    }

    private static List<BpmnParseHandler> getBpmnParseHandlers() {
        List<BpmnParseHandler> list = new ArrayList<BpmnParseHandler>();
        list.add(new CustomScriptTaskParseHandler());
        list.add(new CustomServiceTaskParseHandler());
        list.add(new CustomCallActivityParseHandler());

        return list;
    }

}
