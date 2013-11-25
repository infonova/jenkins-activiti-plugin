package com.bearingpoint.infonova.jenkins.parsehandler;

import org.activiti.bpmn.model.ScriptTask;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.ScriptTaskParseHandler;
import org.activiti.engine.impl.pvm.PvmEvent;
import org.activiti.engine.impl.pvm.process.ActivityImpl;

import com.bearingpoint.infonova.jenkins.listener.ScriptTaskListener;

/**
 * Custom {@link ScriptTaskParseHandler} implementation designed to add further logic to script task executions.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class CustomScriptTaskParseHandler extends ScriptTaskParseHandler {

    public final static String SCRIPT_PROPERTY = "script.resource.property";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeParse(BpmnParse bpmnParse, ScriptTask scriptTask) {

        ActivityImpl activity = bpmnParse.getCurrentActivity();
        activity.setProperty(SCRIPT_PROPERTY, scriptTask.getScript());

        activity.addExecutionListener(PvmEvent.EVENTNAME_START, new ScriptTaskListener());

    }

}
