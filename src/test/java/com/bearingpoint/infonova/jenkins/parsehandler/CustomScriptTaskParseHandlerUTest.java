package com.bearingpoint.infonova.jenkins.parsehandler;

import hudson.FilePath;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

import com.bearingpoint.infonova.jenkins.processengine.JenkinsProcessEngine;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;


public class CustomScriptTaskParseHandlerUTest {

    private ProcessEngine engine;

    @Before
    public void setUp() {
        this.engine = JenkinsProcessEngine.getProcessEngine();
    }

    @Test
    public void testDeployProcess() throws FileNotFoundException {
        URL url = getClass().getResource("scripttask.bpmn20.xml");

        FilePath filePath = new FilePath(new File(url.getPath()));
        String pdId = ActivitiAccessor.deployProcess(engine, filePath);

        ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(pdId);
        ActivityImpl activity = getFirstScriptTask(entity.getActivities());

        Assert.notNull(activity);
        Assert.notNull(activity.getProperty(CustomScriptTaskParseHandler.SCRIPT_PROPERTY));
    }

    private ActivityImpl getFirstScriptTask(List<ActivityImpl> activities) {
        for (ActivityImpl activity : activities) {
            if (activity.getActivityBehavior() instanceof ScriptTaskActivityBehavior) {
                return activity;
            }
        }
        return null;
    }

}
