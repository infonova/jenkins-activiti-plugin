package com.bearingpoint.infonova.jenkins.activitybehavior;

import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.TopLevelItem;
import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.Cause;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jenkins.model.Jenkins;

import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.bearingpoint.infonova.jenkins.cause.WorkflowCause;
import com.bearingpoint.infonova.jenkins.util.ActivitiUtils;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;
import com.google.common.base.Optional;

/**
 * Task delegate implementation for jenkins job invocation.
 * 
 * @author christian.weber
 * @since 1.0
 */
@SuppressWarnings("serial")
public class JenkinsActivitiTaskDelegate extends ReceiveTaskActivityBehavior {

    private transient Logger logger = Logger.getLogger(JenkinsActivitiTaskDelegate.class);

    private Expression jobName;

    private Expression variablesMap;

    @Override
    public void execute(ActivityExecution execution) throws Exception {

        PvmActivity activity = execution.getActivity();
        String activityId = activity.getId();
        String processDefinitionId = activity.getProcessDefinition().getId();

        PvmProcessDefinition pDef = activity.getProcessDefinition();

        logger.info("execution.id                    : " + execution.getId());
        logger.info("execution.activity.id           : " + activityId);
        logger.info("execution.activity.process.id   : " + pDef.getId());
        logger.info("execution.activity.process.name : " + pDef.getName());

        if (jobName != null) {
            String jobNameValue = (String)jobName.getValue(execution);

            Jenkins jenkins = Jenkins.getInstance();
            TopLevelItem item = jenkins.getItem(jobNameValue);

            if (item == null) {
                logger.error("no jenkins job found");
            } else if (!(item instanceof Project)) {
                logger.error("no jenkins job found");
            } else {
                Project<?, ?> project = (Project<?, ?>)item;

                final String projectName = (String)activity.getProperty(ActivitiUtils.JOB_NAME_PROPERTY);
                final int buildNr = (Integer)activity.getProperty(ActivitiUtils.BUILD_NUMBER_PROPERTY);

                Cause cause = new WorkflowCause(projectName, buildNr, processDefinitionId, execution);
                Action action = new ParametersAction(getDefaultParams(project, execution));
                project.scheduleBuild2(jenkins.getQuietPeriod(), cause, action);

                // avoid leaving execution
                return;
            }

        }

        // Marks the job as finished
        leave(execution);
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object data) throws Exception {

        Optional<Object> result = Optional.fromNullable(execution.getVariable("result"));

        // store the activity result as a property
        ActivityImpl activity = (ActivityImpl)execution.getActivity();
        activity.setProperty("result", (String)result.get());

        // continue the process if result is 'SUCCESS'
        if (isSuccessful(result)) {
            super.signal(execution, signalName, data);
        }

    }

    private static boolean isSuccessful(Optional<Object> result) {
        return result.isPresent() && StringUtils.equalsIgnoreCase(result.get().toString(), "SUCCESS");
    }

    /**
     * Returns a list of default parameters of the given project.
     * 
     * @param project
     * @return List
     */
    private List<ParameterValue> getDefaultParams(Project<?, ?> project, ActivityExecution execution) {

        ParametersDefinitionProperty property = (ParametersDefinitionProperty)project
            .getProperty(ParametersDefinitionProperty.class);

        List<ParameterValue> values = new ArrayList<ParameterValue>();

        if (property == null) {
            return values;
        }

        Set<String> keys = new HashSet<String>();

        // add the job variables configured by the process definition
        Map<String, String> variables = getVariablesMap();
        for (String key : variables.keySet()) {

            // continue when parameter is already registered
            if (keys.contains(key)) {
                continue;
            }

            ParameterDefinition def = parameterDefinition(key, property);
            if (isBooleanParameter(def)) {
                boolean booleanValue = BooleanUtils.toBoolean(variables.get(key));
                BooleanParameterValue value = new BooleanParameterValue(key, booleanValue);
                values.add(value);
            } else {
                StringParameterValue value = new StringParameterValue(key, variables.get(key));
                values.add(value);
            }

            keys.add(key);
        }

        for (ParameterDefinition def : property.getParameterDefinitions()) {

            // continue when parameter is already registered
            if (keys.contains(def.getName())) {
                continue;
            }

            // CheckBox parameter
            if (def instanceof ChoiceParameterDefinition) {
                ChoiceParameterDefinition cpd = (ChoiceParameterDefinition)def;
                values.add(getValue(cpd, execution));
            }
            // TextBox parameter
            else if (def instanceof StringParameterDefinition) {
                StringParameterDefinition spd = (StringParameterDefinition)def;
                values.add(getValue(spd, execution));
            }
            // Default
            else if (def instanceof SimpleParameterDefinition) {
                SimpleParameterDefinition spd = (SimpleParameterDefinition)def;
                values.add(getValue(spd, execution));
            }

        }

        return values;
    }

    /**
     * Returns the {@link ParameterDefinition} with the given name.
     * 
     * @param name
     * @param property
     * @return ParameterDefinition
     */
    private ParameterDefinition parameterDefinition(String name, ParametersDefinitionProperty property) {
        for (ParameterDefinition def : property.getParameterDefinitions()) {

            if (StringUtils.equals(def.getName(), name)) {
                return def;
            }

        }
        return null;
    }

    /**
     * Indicates if the given definition is a {@link BooleanParameterDefinition} .
     * 
     * @param definition
     * @return boolean
     */
    private boolean isBooleanParameter(ParameterDefinition definition) {
        return definition instanceof BooleanParameterDefinition;
    }

    /**
     * Returns either the environment variable from the main process or the
     * default parameter value based on the parameter definition.
     * 
     * @param def
     * @param execution
     * @return ParameterValue
     */
    private ParameterValue getValue(ParameterDefinition def, ActivityExecution execution) {
        Set<String> variableNames = execution.getVariableNames();

        if (variableNames.contains(def.getName())) {
            String variable = (String)execution.getVariable(def.getName());
            return new StringParameterValue(def.getName(), variable);
        }
        return def.getDefaultParameterValue();
    }

    /**
     * Returns the job name.
     * 
     * @return String
     */
    public String getJobName() {
        return jobName.getExpressionText();
    }

    /**
     * Returns the job variables.
     * 
     * @return Map
     */
    public Map<String, String> getVariablesMap() {
        if (variablesMap == null) {
            return Collections.emptyMap();
        }
        final String parameters = variablesMap.getExpressionText();
        return JenkinsUtils.normalizeParameters(parameters);
    }

}
