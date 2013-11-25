package com.bearingpoint.infonova.jenkins.parsehandler;

import org.activiti.bpmn.model.CallActivity;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.CallActivityParseHandler;
import org.activiti.engine.impl.pvm.process.ActivityImpl;

/**
 * Custom {@link CallActivityParseHandler} implementation designed to add further logic to call activity task
 * executions.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class CustomCallActivityParseHandler extends CallActivityParseHandler {

    public final static String CALLACTIVITY_PROPERTY = "callactivity.resource.property";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeParse(BpmnParse bpmnParse, CallActivity callActivity) {
        ActivityImpl activity = bpmnParse.getCurrentActivity();
        activity.setProperty(CALLACTIVITY_PROPERTY, callActivity.getCalledElement());
    }


}
