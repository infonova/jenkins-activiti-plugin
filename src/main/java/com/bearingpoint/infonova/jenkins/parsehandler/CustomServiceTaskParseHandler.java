package com.bearingpoint.infonova.jenkins.parsehandler;

import java.util.Collections;
import java.util.List;

import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.ServiceTaskParseHandler;
import org.activiti.engine.impl.pvm.process.ActivityImpl;

/**
 * Custom {@link ServiceTaskParseHandler} implementation designed to add further logic to service task executions.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class CustomServiceTaskParseHandler extends ServiceTaskParseHandler {

    public final static String IMPLEMENTATION = "jenkinstask.property.implementation";

    @Override
    protected void executeParse(BpmnParse bpmnParse, ServiceTask task) {

        ActivityImpl activity = bpmnParse.getCurrentActivity();
        activity.setProperty(IMPLEMENTATION, task.getImplementation());

        for (FieldExtension extension : getFieldExtensions(task)) {
            activity.setProperty(extension.getFieldName(), extension.getStringValue());
        }

        // TODO: validate if all necessary properties are set

    }

    private static List<FieldExtension> getFieldExtensions(ServiceTask task) {
        List<FieldExtension> extensions = task.getFieldExtensions();

        if (extensions == null) {
            return Collections.emptyList();
        }

        return extensions;
    }


}
