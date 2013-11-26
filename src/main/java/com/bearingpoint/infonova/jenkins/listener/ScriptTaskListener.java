package com.bearingpoint.infonova.jenkins.listener;

import hudson.model.Item;

import java.io.Serializable;
import java.util.List;

import jenkins.model.Jenkins;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;


/**
 * This {@link ExecutionListener} class is used to register custom script task
 * variables.
 * 
 * @author christian.weber
 * @since 1.0.0
 * 
 */
@SuppressWarnings("serial")
public class ScriptTaskListener implements ExecutionListener {

    public void notify(DelegateExecution execution) throws Exception {
        execution.setVariableLocal("jenkins", new JenkinsAdapter());
    }

    public static class JenkinsAdapter implements Serializable {

        private transient Jenkins jenkins = Jenkins.getInstance();

        /**
         * @see Jenkins#getAllItems()
         * @return List
         */
        public List<Item> getAllItems() {
            return jenkins.getAllItems();
        }

    }

}
