package com.bearingpoint.infonova.jenkins.util;

import hudson.model.Action;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.util.RunList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.cause.WorkflowCause;
import com.bearingpoint.infonova.jenkins.exception.ActivitiWorkflowException;
import com.bearingpoint.infonova.jenkins.exception.ErrorCode;

/**
 * Utility class for jenkins functionality.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class JenkinsUtils {

    private JenkinsUtils() {
        super();
    }

    /**
     * Returns the {@link Project} with the given name.
     * 
     * @param projectName
     * @return Project
     */
    public static Project<?, ?> getProject(String projectName) {
        TopLevelItem item = Jenkins.getInstance().getItem(projectName);
        return (Project<?, ?>)item;
    }

    /**
     * Returns the action instance with the given project name and build number.
     * 
     * @param projectName
     * @param buildNr
     * @param actionClass
     * @return Action
     */
    public static <T extends Action> T getProjectAction(String projectName, int buildNr, Class<T> actionClass) {
        TopLevelItem item = Jenkins.getInstance().getItem(projectName);

        Project<?, ?> project = (Project<?, ?>)item;
        AbstractBuild<?, ?> build = project.getBuildByNumber(buildNr);

        return build.getAction(actionClass);
    }

    /**
     * Returns the action instance with the given project name and build number.
     * 
     * @param projectName
     * @param buildNr
     * @param actionClass
     * @return Action
     */
    public static <T extends Action> List<T> getProjectActions(String projectName, int buildNr, Class<T> actionClass) {
        TopLevelItem item = Jenkins.getInstance().getItem(projectName);

        Project<?, ?> project = (Project<?, ?>)item;
        AbstractBuild<?, ?> build = project.getBuildByNumber(buildNr);

        return build.getActions(actionClass);
    }

    /**
     * Returns an {@link AbstractActivitiAction} implementations of the build
     * with the given project name, build number and process definition id.
     * 
     * @param projectName
     * @param buildNr
     * @param processDefinitionId
     * @param clazz
     * @return
     */
    public static ActivitiWorkflowAction getWorkflowAction(String projectName, int buildNr, String processDefinitionId) {

        List<ActivitiWorkflowAction> actions = getProjectActions(projectName, buildNr, ActivitiWorkflowAction.class);

        for (ActivitiWorkflowAction action : actions) {
            String id = action.getProcessDescriptionId();
            if (StringUtils.equals(id, processDefinitionId)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Returns all top level items with the given action.
     * 
     * @param actionClass
     * @return List
     */
    public static List<TopLevelItem> getItemsWithAction(Class<? extends Action> actionClass) {
        List<TopLevelItem> items = Jenkins.getInstance().getItems();
        List<TopLevelItem> relevantItems = new ArrayList<TopLevelItem>();

        for (TopLevelItem item : items) {

            if (!(item instanceof Project)) {
                continue;
            }

            Project<?, ?> project = (Project<?, ?>)item;
            if (project.getAction(actionClass) != null) {
                relevantItems.add(item);
            }
        }

        return relevantItems;
    }

    /**
     * Returns the latest build by the given project name.
     * 
     * @param projectName
     * @return AbstractBuild
     */
    public static AbstractBuild<?, ?> getLatestBuild(String projectName) {
        TopLevelItem item = Jenkins.getInstance().getItem(projectName);
        Project<?, ?> project = (Project<?, ?>)item;

        return project.getLastBuild();
    }

    /**
     * Returns all {link AbstractBuild} instances of the project with the given
     * project name.
     * 
     * @param projectName
     * @return List
     */
    public static List<? extends AbstractBuild<?, ?>> getProjectBuilds(String projectName) {
        TopLevelItem item = Jenkins.getInstance().getItem(projectName);

        if (!(item instanceof Project)) {
            throw new IllegalArgumentException("no project found with name " + projectName);
        }

        Project<?, ?> project = (Project<?, ?>)item;
        return project.getBuilds();
    }

    /**
     * Returns the build number from the build with the given process definition
     * id and project name.
     * 
     * @param processDefinitionId
     * @param projectName
     * @return Integer
     */
    public static Integer getBuildNumberByProcessDefinitionId(String processDefinitionId, String projectName) {

        Project<?, ?> project = JenkinsUtils.getProject(projectName);

        RunList<?> builds = project.getBuilds();
        Iterator<?> iterator = builds.iterator();

        while (iterator.hasNext()) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)iterator.next();
            WorkflowCause cause = build.getCause(WorkflowCause.class);

            if (cause == null) {
                continue;
            }

            if (StringUtils.equals(processDefinitionId, cause.getProcessDescriptionId())) {
                return build.number;
            }
        }
        return null;
    }

    /**
     * Stores the given exception instance.
     * 
     * @param build
     * @param ex
     * @return File
     */
    public static File storeException(AbstractBuild<?, ?> build, Exception ex) {

        File target = new File(build.getRootDir(), "error.txt");

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(target);

            ex.printStackTrace(writer);

            writer.flush();
        } catch (FileNotFoundException e) {
            throw new ActivitiWorkflowException(ErrorCode.ACTIVITI07, e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

        return target;
    }

    /**
     * Prepares the parameters string to a parameters map. The syntax of the
     * parameters string is [key:value]. Multiple parameters could be
     * concatenated by delimiting the entries with a comma.
     * 
     * @param parameters
     * @return Map
     */
    public static Map<String, String> normalizeParameters(String parameters) {
        Map<String, String> params = new HashMap<String, String>();

        if (StringUtils.isBlank(parameters)) {
            return params;
        }
        if (!StringUtils.startsWith(parameters, "[")) {
            throw new IllegalArgumentException("parameter syntax error. leading [ missing");
        }
        if (!StringUtils.endsWith(parameters, "]")) {
            throw new IllegalArgumentException("parameter syntax error. following ] missing");
        }
        parameters = StringUtils.substring(parameters, 1, parameters.length() - 1);
        if (StringUtils.isBlank(parameters)) {
            return params;
        }
        String[] pairs = StringUtils.split(parameters, ",");

        for (String pair : pairs) {
            String[] array = StringUtils.split(pair, ":");
            if (array.length < 2) {
                throw new IllegalArgumentException("parameter syntax error. key value pair invalid");
            }
            params.put(StringUtils.trim(array[0]), StringUtils.trim(array[1]));
        }

        return params;
    }

    /**
     * Indicates whether result1 is better or equals result2.
     * 
     * @param result1
     * @param result2
     * @return boolean
     */
    public static boolean isResultEqualOrBetter(String result1, String result2) {

        if (result1 == null || result2 == null) {
            return false;
        }

        int i1 = getResultAsInt(result1);
        int i2 = getResultAsInt(result2);

        return i1 >= i2;
    }

    private static int getResultAsInt(String result) {

        if (StringUtils.equalsIgnoreCase(result, "SUCCESS")) {
            return 3;
        }
        if (StringUtils.equalsIgnoreCase(result, "UNSTABLE")) {
            return 2;
        }
        if (StringUtils.equalsIgnoreCase(result, "FAILURE")) {
            return 1;
        }

        return 0;
    }

    /**
     * Returns the environment variables from the given build as a map.
     * 
     * @param build
     * @param listener
     * @return Map
     */
    public static Map<String, Object> getEnvironmentVars(AbstractBuild<?, ?> build) {
        try {
            Map<String, String> map = build.getBuildVariables();

            if (map == null) {
                return Collections.emptyMap();
            }

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.putAll(map);

            return variables;
        } catch (Exception e) {
            throw new RuntimeException("error while  preparing environment map", e);
        }
    }

}
