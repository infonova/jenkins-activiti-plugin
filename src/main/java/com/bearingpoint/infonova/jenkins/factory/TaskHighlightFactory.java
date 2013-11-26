package com.bearingpoint.infonova.jenkins.factory;

import hudson.model.AbstractBuild;

import java.io.File;

import org.activiti.engine.impl.bpmn.behavior.ManualTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.apache.commons.lang3.StringUtils;

import com.bearingpoint.infonova.jenkins.activitybehavior.JenkinsActivitiTaskDelegate;
import com.bearingpoint.infonova.jenkins.activitybehavior.RemoteJenkinsActivitiTaskDelegate;
import com.bearingpoint.infonova.jenkins.parsehandler.CustomServiceTaskParseHandler;
import com.bearingpoint.infonova.jenkins.ui.AbstractTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.JenkinsActivitiTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.ManualTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.RemoteJenkinsActivitiTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.ScriptTaskHighlight;
import com.bearingpoint.infonova.jenkins.ui.UserTaskHighlight;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;

/**
 * Factory class for task highlight element.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class TaskHighlightFactory {

    private TaskHighlightFactory() {
        super();
    }

    /**
     * Returns the task highlight element.
     * 
     * @param activity
     * @param xEff
     * @param yEff
     * @param build
     * @return AbstractTaskHighlight
     */
    public static AbstractTaskHighlight getObject(ActivityImpl activity, int xEff, int yEff, AbstractBuild<?, ?> build) {
        int x1 = activity.getX() - xEff;
        int y1 = activity.getY() - yEff;
        int x2 = activity.getX() - xEff + activity.getWidth();
        int y2 = activity.getY() - yEff + activity.getHeight();

        ActivityBehavior behavior = activity.getActivityBehavior();
        final String processId = activity.getProcessDefinition().getId();

        // script task
        if (isScriptTask(behavior)) {
            File script = ActivitiUtils.storeProcessResources(build, activity);
            return new ScriptTaskHighlight(processId, activity.getId(), x1, y1, x2, y2, script);
        }

        // manual task
        if (isManualTask(behavior)) {
            return new ManualTaskHighlight(processId, activity.getId(), x1, y1, x2, y2);
        }

        // user task
        if (isUserTask(behavior)) {
            return new UserTaskHighlight(processId, activity.getId(), x1, y1, x2, y2);
        }

        // remote JENKINS task
        if (isRemoteJenkinsTask(activity)) {
            return new RemoteJenkinsActivitiTaskHighlight(activity, x1, y1, x2, y2);
        }

        // local JENKINS task
        if (isJenkinsTask(activity)) {
            return new JenkinsActivitiTaskHighlight(activity, x1, y1, x2, y2);
        }

        // TODO: exception handling
        throw new RuntimeException("unsupported activity element found: " + activity.getProperty("type"));
    }

    /**
     * Indicates if the task is a script task.
     * 
     * @param behavior
     * @return boolean
     */
    private static boolean isScriptTask(ActivityBehavior behavior) {
        return (behavior instanceof ScriptTaskActivityBehavior);
    }

    /**
     * Indicates if the task is a manual task.
     * 
     * @param behavior
     * @return boolean
     */
    private static boolean isManualTask(ActivityBehavior behavior) {
        return (behavior instanceof ManualTaskActivityBehavior);
    }

    /**
     * Indicates if the task is a user task.
     * 
     * @param behavior
     * @return boolean
     */
    private static boolean isUserTask(ActivityBehavior behavior) {
        return (behavior instanceof UserTaskActivityBehavior);
    }

    /**
     * Indicates if the given {@link ActivityImpl} is a remote JENKINS task.
     * 
     * @param activity
     * @return boolean
     */
    private static boolean isRemoteJenkinsTask(ActivityImpl activity) {
        String impl = (String)activity.getProperty(CustomServiceTaskParseHandler.IMPLEMENTATION);
        return StringUtils.equals(impl, RemoteJenkinsActivitiTaskDelegate.class.getName());
    }

    /**
     * Indicates if the given {@link ActivityImpl} is a JENKINS task.
     * 
     * @param activity
     * @return boolean
     */
    private static boolean isJenkinsTask(ActivityImpl activity) {
        String impl = (String)activity.getProperty(CustomServiceTaskParseHandler.IMPLEMENTATION);
        return StringUtils.equals(impl, JenkinsActivitiTaskDelegate.class.getName());
    }

}
