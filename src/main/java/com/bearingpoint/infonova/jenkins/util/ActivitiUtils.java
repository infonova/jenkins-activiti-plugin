package com.bearingpoint.infonova.jenkins.util;

import static com.bearingpoint.infonova.jenkins.util.ActivitiAccessor.getProcessDefinitionById;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.activiti.engine.impl.bpmn.data.SimpleDataInputAssociation;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.exception.ActivitiWorkflowException;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;
import com.bearingpoint.infonova.jenkins.exception.JenkinsJobFailedException;
import com.bearingpoint.infonova.jenkins.factory.CallActivityHighlightFactory;
import com.bearingpoint.infonova.jenkins.factory.EventHighlightFactory;
import com.bearingpoint.infonova.jenkins.factory.GatewayHighlightFactory;
import com.bearingpoint.infonova.jenkins.factory.TaskHighlightFactory;
import com.bearingpoint.infonova.jenkins.listener.NamedExecutionListener;
import com.bearingpoint.infonova.jenkins.parsehandler.CustomCallActivityParseHandler;
import com.bearingpoint.infonova.jenkins.parsehandler.CustomScriptTaskParseHandler;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;

/**
 * Utility class for activity functionality.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class ActivitiUtils {

    public static final String JOB_NAME_PROPERTY = "job.name.property";

    public static final String BUILD_NUMBER_PROPERTY = "build.number.property";

    private ActivitiUtils() {
        super();
    }

    /**
     * Indicates if the given activity is an activity.
     * 
     * @param activity
     * @return boolean
     */
    public static boolean isActivity(ActivityImpl activity) {
        final String type = (String)activity.getProperty("type");
        return StringUtils.contains(type, "Task");
    }

    /**
     * Indicates if the given activity is an event.
     * 
     * @param activity
     * @return boolean
     */
    public static boolean isEvent(ActivityImpl activity) {
        final String type = (String)activity.getProperty("type");
        return StringUtils.contains(type, "Event");
    }

    /**
     * Indicates if the given activity is a gateway.
     * 
     * @param activity
     * @return boolean
     */
    public static boolean isGateway(ActivityImpl activity) {
        final String type = (String)activity.getProperty("type");
        return StringUtils.contains(type, "Gateway");
    }

    /**
     * Indicates if the given activity is a sub process.
     * 
     * @param activity
     * @return boolean
     */
    public static boolean isSubProcess(ActivityImpl activity) {
        final String type = (String)activity.getProperty("type");
        return StringUtils.contains(type, "subProcess");
    }

    /**
     * Indicates if the given activity is a call activity.
     * 
     * @param activity
     * @return boolean
     */
    public static boolean isCallActivity(ActivityImpl activity) {
        final String type = (String)activity.getProperty("type");
        return StringUtils.contains(type, "callActivity");
    }

    /**
     * Returns the diagram resource with the given process definition id as an
     * input stream.
     * 
     * @param processDefinitionId
     * @return InputStream
     */
    public static InputStream getDiagramResourceAsStream(String processDefinitionId) {

        ProcessDefinition pd = getProcessDefinitionById(processDefinitionId);
        ProcessEngine engine = ActivitiAccessor.getProcessEngine();
        RepositoryService repoService = engine.getRepositoryService();

        final String deploymentId = pd.getDeploymentId();
        final String resourceName = pd.getDiagramResourceName();

        Assert.notNull(deploymentId, ErrorCode.ACTIVITI01);
        Assert.notNull(resourceName, ErrorCode.ACTIVITI02);
        
        return repoService.getResourceAsStream(deploymentId, resourceName);
    }

    /**
     * Returns a list of {@link AbstractArea}. by the given process definition
     * id.
     * 
     * @param processDefinitionId
     * @return List
     */
    public static List<AbstractArea> getAreas(AbstractBuild<?, ?> build, String processDefinitionId) {

        ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(processDefinitionId);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (ActivityImpl activity : entity.getActivities()) {
            minX = Math.min(minX, activity.getX());
            minY = Math.min(minY, activity.getY());
        }

        int xEff = minX - 5;
        int yEff = minY - 5;

        return getAreasRecursive(build, xEff, yEff, entity.getActivities());
    }

    /**
     * Returns a list of {@link AbstractArea} recursive.
     * 
     * @param build
     * @param xEff
     * @param yEff
     * @param activities
     * @return
     */
    private static List<AbstractArea> getAreasRecursive(AbstractBuild<?, ?> build, int xEff, int yEff,
            List<ActivityImpl> activities) {
        List<AbstractArea> elements = new ArrayList<AbstractArea>();
        for (ActivityImpl activity : activities) {

            if (ActivitiUtils.isActivity(activity)) {
                elements.add(TaskHighlightFactory.getObject(activity, xEff, yEff, build));
            }

            else if (ActivitiUtils.isEvent(activity)) {
                elements.add(EventHighlightFactory.getObject(activity, xEff, yEff));
            }

            else if (ActivitiUtils.isGateway(activity)) {
                elements.add(GatewayHighlightFactory.getObject(activity, xEff, yEff));
            }

            else if (ActivitiUtils.isSubProcess(activity)) {
                elements.addAll(getAreasRecursive(build, xEff, yEff, activity.getActivities()));
            }

            else if (ActivitiUtils.isCallActivity(activity)) {
                elements.add(CallActivityHighlightFactory.getObject(activity, xEff, yEff, build));
            }

        }
        return elements;
    }

    /**
     * Adds the activity properties to all activities.
     * 
     * @param processDefinitionId
     * @param properties
     */
    public static void addActivityProperties(String processDefinitionId, Map<String, Object> properties) {
        ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(processDefinitionId);
        addActivitiPropertiesRecursive(entity.getActivities(), properties);
    }

    /**
     * Adds the activity properties recursive to all activities.
     * 
     * @param activities
     * @param properties
     */
    private static void addActivitiPropertiesRecursive(List<ActivityImpl> activities, Map<String, Object> properties) {

        for (ActivityImpl activity : activities) {
            for (String key : properties.keySet()) {
                activity.setProperty(key, properties.get(key));

                if (ActivitiUtils.isSubProcess(activity)) {
                    addActivitiPropertiesRecursive(activity.getActivities(), properties);
                }
            }
        }

    }

    /**
     * Adds the activity listeners to all activities.
     * 
     * @param processDefinitionId
     * @param listeners
     */
    public static void addActivityListeners(String processDefinitionId, NamedExecutionListener... listeners) {
        ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(processDefinitionId);
        addActivityListenersRecursive(entity.getActivities(), listeners);
    }

    /**
     * Adds the activity listeners recursive to all activities.
     * 
     * @param activities
     * @param listeners
     */
    private static void addActivityListenersRecursive(List<ActivityImpl> activities,
            NamedExecutionListener... listeners) {
        for (ActivityImpl activity : activities) {
            for (NamedExecutionListener listener : listeners) {
                if (listener.isStartEvent()) {
                    activity.addExecutionListener(listener.getEventName(), listener.getExecutionListener(), 0);
                } else {
                    activity.addExecutionListener(listener.getEventName(), listener.getExecutionListener());
                }

                if (ActivitiUtils.isSubProcess(activity)) {
                    addActivityListenersRecursive(activity.getActivities(), listeners);
                } else if (ActivitiUtils.isCallActivity(activity)) {
                    String key = (String)activity.getProperty(CustomCallActivityParseHandler.CALLACTIVITY_PROPERTY);
                    ProcessDefinition definition = ActivitiUtils.getCallActivity(key);
                    ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(definition.getId());

                    addActivityListenersRecursive(entity.getActivities(), listeners);
                }
            }
        }
    }

    /**
     * Stores the process diagram with the given process definition id to the
     * file system.
     * 
     * @param build
     * @param processDefinitionId
     * @return File
     */
    public static File storeProcessDiagram(AbstractBuild<?, ?> build, String processDefinitionId) {

        File rootDir = new File(build.getRootDir(), "diagrams");
        mkdir(rootDir);

        final String processId = StringUtils.substringBefore(processDefinitionId, ":");
        File target = new File(rootDir, processId + ".png");

        InputStream in = null;
        OutputStream out = null;
        try {
            in = getDiagramResourceAsStream(processDefinitionId);
            out = new FileOutputStream(target);

            IOUtils.copy(in, out);
        } catch (FileNotFoundException e) {
            throw new ActivitiWorkflowException(ErrorCode.ACTIVITI07, e);
        } catch (IOException e) {
            throw new ActivitiWorkflowException(ErrorCode.ACTIVITI07, e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        return target;
    }

    /**
     * Stores the process resources with the given process definition id to the
     * file system.
     * 
     * @param build
     * @param mainProcess
     * @return File
     */
    public static File storeProcessResources(AbstractBuild<?, ?> build, ActivityImpl activity) {

        File rootDir = new File(build.getRootDir(), "scripts");
        mkdir(rootDir);

        String script = (String)activity.getProperty(CustomScriptTaskParseHandler.SCRIPT_PROPERTY);

        if (script == null) {
            return new File(StringUtils.EMPTY);
        }
        File target = new File(rootDir, activity.getId() + ".groovy");

        InputStream in = null;
        OutputStream out = null;
        try {
            in = IOUtils.toInputStream(script);
            out = new FileOutputStream(target);

            IOUtils.copy(in, out);
        } catch (FileNotFoundException e) {
            throw new ActivitiWorkflowException(ErrorCode.ACTIVITI07, e);
        } catch (IOException e) {
            throw new ActivitiWorkflowException(ErrorCode.ACTIVITI07, e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }

        return target;
    }

    /**
     * Returns the call activity process definition with the given key.
     * 
     * @param key
     * @return ProcessDefinition
     */
    public static ProcessDefinition getCallActivity(String key) {
        ProcessEngine engine = ActivitiAccessor.getProcessEngine();
        RepositoryService repositoryService = engine.getRepositoryService();

        // load the latest version of the sub process
        return repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).latestVersion()
            .singleResult();
    }

    /**
     * Creates the directory if necessary.
     * 
     * @param file
     */
    private static final void mkdir(File file) {
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * Returns the list of failed {@link Execution} instances of the process
     * with the given process instance id.
     * 
     * @param processInstanceId
     * @return List
     */
    public static boolean hasFailedExecutions(String processInstanceId) {

        ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(processInstanceId);

        return hasFailedExecutionsInternal(entity.getActivities());
    }

    private static boolean hasFailedExecutionsInternal(List<ActivityImpl> activities) {

        for (ActivityImpl activity : activities) {

            // sub process handling
            if (ActivitiUtils.isSubProcess(activity)) {
                if (hasFailedExecutionsInternal(activity.getActivities())) {
                    return true;
                }
            }

            // call activity handling
            else if (ActivitiUtils.isCallActivity(activity)) {
                String key = (String)activity.getProperty(CustomCallActivityParseHandler.CALLACTIVITY_PROPERTY);
                ProcessDefinition definition = ActivitiUtils.getCallActivity(key);
                ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(definition.getId());

                if (hasFailedExecutionsInternal(entity.getActivities())) {
                    return true;
                }
            }

            else {
                Object property = activity.getProperty("result");

                if (property != null && StringUtils.equals(property.toString(), "FAILURE")) {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * Returns the history of the process with the given process instance id.
     * 
     * @param processInstanceId
     * @return HistoricProcessInstance
     */
    public static HistoricProcessInstance getProcessHistory(String processInstanceId) {
        ProcessEngine engine = ActivitiAccessor.getProcessEngine();
        HistoryService historyService = engine.getHistoryService();

        return historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstanceId)
            .singleResult();
    }

    /**
     * Prepares the data association between the main process and the sub
     * process so that the sub process is aware of the main processes
     * environment variables.
     * 
     * @param processDefinitionId
     * @param variableNames
     */
    public static void prepareDataAssociation(String processDefinitionId, Set<String> variableNames) {

        ProcessDefinitionEntity entity = ActivitiAccessor.getProcessDefinitionEntity(processDefinitionId);

        for (ActivityImpl activity : entity.getActivities()) {

            if (ActivitiUtils.isCallActivity(activity)) {
                for (String variableName : variableNames) {
                    CallActivityBehavior behavior = (CallActivityBehavior)activity.getActivityBehavior();
                    behavior.addDataInputAssociation(new SimpleDataInputAssociation(variableName, variableName));
                }
            }

        }

    }

    /**
     * Polls the workflow engine continuously till the process execution with
     * the given process description id has finished.
     * 
     * @param engine
     * @param processInstanceId
     */
    // TODO: add timeout
    // or complete running tasks when new execution starts
    public static void waitForProcessFinalization(AbstractBuild<?, ?> build, ProcessInstance pi)
            throws InterruptedException {

        ActivitiWorkflowAction action = build.getAction(ActivitiWorkflowAction.class);

        // TODO: return failed task names
        boolean hasFailedExecutions = false;
        do {
            // logger.debug("wait for process finalization for process with id " + processInstanceId);

            Thread.sleep(1000);

            for (String processId : action.getProcessIds()) {
                if (hasFailedExecutions == false) {
                    hasFailedExecutions = ActivitiUtils.hasFailedExecutions(processId);
                }
            }

            ProcessInstance instance = getProcessInstanceById(pi.getId());
            if (instance == null) {
                return;
            }
            // failed executions check
            if (hasFailedExecutions) {
                throw new JenkinsJobFailedException();
            }

        } while (true);
    }

    /**
     * Returns a {@link ProcessInstance} instance by the given id.
     * 
     * @param id
     * @return ProcessInstance
     */
    public static ProcessInstance getProcessInstanceById(String id) {

        ProcessEngine engine = ActivitiAccessor.getProcessEngine();
        RuntimeService rs = engine.getRuntimeService();

        return rs.createProcessInstanceQuery().processInstanceId(id).singleResult();
    }

}
