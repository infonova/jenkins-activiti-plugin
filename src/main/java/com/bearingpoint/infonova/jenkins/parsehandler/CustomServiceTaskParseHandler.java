package com.bearingpoint.infonova.jenkins.parsehandler;

import java.util.List;

import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.ServiceTaskParseHandler;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.apache.commons.lang3.StringUtils;

/**
 * Custom {@link ServiceTaskParseHandler} implementation designed to add further logic to service task executions.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class CustomServiceTaskParseHandler extends ServiceTaskParseHandler {

    public final static String JENKINS_TASK_PROPERTY = "jenkinstask.resource.property";

    @Override
    protected void executeParse(BpmnParse bpmnParse, ServiceTask task) {

        List<FieldExtension> extensions = task.getFieldExtensions();

        if (extensions == null) {
            return;
        }

        for (FieldExtension extension : extensions) {

            if (StringUtils.equals(extension.getFieldName(), extension.getFieldName())) {
                ActivityImpl activity = bpmnParse.getCurrentActivity();
                activity.setProperty(JENKINS_TASK_PROPERTY, extension.getStringValue());
                return;
            }

        }

    }


}
