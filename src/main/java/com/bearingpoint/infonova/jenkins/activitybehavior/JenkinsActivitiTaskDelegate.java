package com.bearingpoint.infonova.jenkins.activitybehavior;

import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.EnvironmentList;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.Cause;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.apache.commons.lang.StringUtils;
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

	private transient Logger logger = Logger
			.getLogger(JenkinsActivitiTaskDelegate.class);

	private Expression jobName;

	private Expression variablesMap;

	private Expression jobResultCondition;

	private Map<String, String> masterEnVarsMap;
	
	public void initEnvVarsFromMasterJobLocalVar(String jobname, int buildnr)
	{
		TopLevelItem tli = Jenkins.getInstance().getItem(jobname);
		
		if(tli != null)
		{
			if(tli instanceof Project)
			{
				Project masterProject = (Project) tli;				
				AbstractBuild masterBuild = masterProject.getBuildByNumber(buildnr);
				masterEnVarsMap = masterBuild.getEnvVars();
			}
		}
		
		if(masterEnVarsMap == null)
		{
			masterEnVarsMap = new HashMap<String, String>();
		}
	}
	
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
			String jobNameValue = (String) jobName.getValue(execution);

			Jenkins jenkins = Jenkins.getInstance();
			TopLevelItem item = jenkins.getItem(jobNameValue);
		

			if (item == null) {
				logger.error("no jenkins job found");
			} else if (item instanceof Project) {

				Project<?, ?> project = (Project<?, ?>) item;

				//the pipeline job
				final String projectName = (String) activity
						.getProperty(ActivitiUtils.JOB_NAME_PROPERTY);
				final int buildNr = (Integer) activity
						.getProperty(ActivitiUtils.BUILD_NUMBER_PROPERTY);
				initEnvVarsFromMasterJobLocalVar(projectName, buildNr);
				
				Cause cause = new WorkflowCause(projectName, buildNr,
						processDefinitionId, execution);
				Action action = new ParametersAction(getDefaultParams(project,
						execution));
				
				project.scheduleBuild2(jenkins.getQuietPeriod(), cause, action);
				
				isJobAbbortedInQueueCheck(project, execution);
				// avoid leaving execution
				return;
				
			} else if (item instanceof MavenModuleSet) {

				// Maven Build Job Support
				MavenModuleSet mavenModuleSet = (MavenModuleSet) item;
				AbstractProject<?, ?> abstractProject = mavenModuleSet
						.asProject();

				final String projectName = (String) activity
						.getProperty(ActivitiUtils.JOB_NAME_PROPERTY);
				final int buildNr = (Integer) activity
						.getProperty(ActivitiUtils.BUILD_NUMBER_PROPERTY);
				initEnvVarsFromMasterJobLocalVar(projectName, buildNr);

				
				Cause cause = new WorkflowCause(projectName, buildNr,
						processDefinitionId, execution);
				Action action = new ParametersAction(getDefaultParams(
						abstractProject, execution));
				abstractProject.scheduleBuild2(jenkins.getQuietPeriod(), cause,
						action);

				isJobAbbortedInQueueCheck(abstractProject, execution);
				// avoid leaving execution
				return;
				
			} else {
				logger.error("no jenkins job found");
			}
		}
		// Marks the job as finished
		leave(execution);
	}

	
	@Override
	public void signal(ActivityExecution execution, String signalName,
			Object data) throws Exception {

		Optional<Object> result = Optional.fromNullable(execution
				.getVariable("result"));

		// store the activity result as a property
		ActivityImpl activity = (ActivityImpl) execution.getActivity();

		// continue the process if result is 'SUCCESS' or 'UNSTABLE'
		if (isSuccessful(result) || isUnstable(result)) {
			activity.setProperty("result", (String) result.get());
			super.signal(execution, signalName, data);

		} else{
			failureAllParentActivities(activity);
		}

	}

	private void failureAllParentActivities(ActivityImpl activity)
			throws Exception {
		if (activity != null) {
			activity.setProperty("result", "FAILURE");

			if (activity.getParent() != null) {
				failureAllParentActivities(activity.getParentActivity());
			}
		}
	}

	private boolean isSuccessful(Optional<Object> result) {
		try {
			String condition = jobResultCondition == null ? "SUCCESS"
					: jobResultCondition.getExpressionText();
			return result.isPresent()
					&& StringUtils.equalsIgnoreCase(result.get().toString(),
							condition);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean isUnstable(Optional<Object> result) {
		try {
			String condition = jobResultCondition == null ? "UNSTABLE"
					: jobResultCondition.getExpressionText();
			return result.isPresent()
					&& StringUtils.equalsIgnoreCase(result.get().toString(),
							condition);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns a list of default parameters of the given project.
	 * 
	 * @param project
	 * @return List
	 */
	private List<ParameterValue> getDefaultParams(Project<?, ?> project,
			ActivityExecution execution) {

		ParametersDefinitionProperty property = (ParametersDefinitionProperty) project
				.getProperty(ParametersDefinitionProperty.class);

		return getDefaultParametersHelper(execution, property);
	}

	private List<ParameterValue> getDefaultParams(
			AbstractProject<?, ?> project, ActivityExecution execution) {

		ParametersDefinitionProperty property = (ParametersDefinitionProperty) project
				.getProperty(ParametersDefinitionProperty.class);

		return getDefaultParametersHelper(execution, property);
	}


	private List<ParameterValue> getDefaultParametersHelper(
			ActivityExecution execution, ParametersDefinitionProperty property) {
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
				boolean booleanValue = BooleanUtils.toBoolean(variables
						.get(key));
				BooleanParameterValue value = new BooleanParameterValue(key,
						booleanValue);
				values.add(value);
			} else {
				
				StringParameterValue value = new StringParameterValue(key,variables.get(key));
				values.add(value);
				
			}

			keys.add(key);
		}
		
		
		//Environment Variables from Pipeline
		for (ParameterDefinition def : property.getParameterDefinitions()) {

			// continue when parameter is already registered
			if (keys.contains(def.getName())) {
				
				String varFromActualJob = variables.get(def.getName());
				//if ${...}
				varFromActualJob = varFromActualJob.replace("{", "").replace("}", "");
				
				if(varFromActualJob.startsWith("$"))
				{
					varFromActualJob = varFromActualJob.replace('$', ' ').trim();
					Set<String> variableNames = execution.getVariableNames();
						if (variableNames.contains(varFromActualJob)) {
							String variable = (String) execution.getVariable(varFromActualJob);
							
							values.remove(new StringParameterValue(def.getName(),variables.get(def.getName())));
							values.add(new StringParameterValue(def.getName(), variable));
						} else if(masterEnVarsMap.containsKey(varFromActualJob))
						{
							String variable = (String) masterEnVarsMap.get(varFromActualJob);
							
							values.remove(new StringParameterValue(def.getName(),variables.get(def.getName())));
							values.add(new StringParameterValue(def.getName(), variable));
						}
				}		
				continue;
			}

			// CheckBox parameter
			if (def instanceof ChoiceParameterDefinition) {
				ChoiceParameterDefinition cpd = (ChoiceParameterDefinition) def;
				values.add(getValue(cpd, execution));
			}
			// TextBox parameter
			else if (def instanceof StringParameterDefinition) {
				StringParameterDefinition spd = (StringParameterDefinition) def;
				values.add(getValue(spd, execution));
			}
			// Default
			else if (def instanceof SimpleParameterDefinition) {
				SimpleParameterDefinition spd = (SimpleParameterDefinition) def;
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
	private ParameterDefinition parameterDefinition(String name,
			ParametersDefinitionProperty property) {
		for (ParameterDefinition def : property.getParameterDefinitions()) {

			if (StringUtils.equals(def.getName(), name)) {
				return def;
			}

		}
		return null;
	}

	/**
	 * Indicates if the given definition is a {@link BooleanParameterDefinition}
	 * .
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
	private ParameterValue getValue(ParameterDefinition def,
			ActivityExecution execution) {
		Set<String> variableNames = execution.getVariableNames();

		if (variableNames.contains(def.getName())) {
			String variable = (String) execution.getVariable(def.getName());
			return new StringParameterValue(def.getName(), variable);
		}
		return def.getDefaultParameterValue();
	}
	
	private ParameterValue setValue(ParameterDefinition def,
			ActivityExecution execution, String variable) {
		Set<String> variableNames = execution.getVariableNames();

		if (variableNames.contains(def.getName())) {
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
	
	private void isJobAbbortedInQueueCheck(final AbstractProject<?, ?> abstractProject, final ActivityExecution execution)
	{
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				//check if job is aborted in queue
				try {
					while (abstractProject.isInQueue()) {
						Thread.sleep(20);
					}

					// aborted in queue if it is not building
					if (!abstractProject.isBuilding()) {
						throw new Exception("Job aborted in Queue");
					}
					
				} catch (Exception e) {
					
					try {
						failureAllParentActivities((ActivityImpl) execution.getActivity());
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					e.printStackTrace();
					return;
				}
			}
		},"ActivitiIsJobAbortedInQueueCheck for " +execution.getCurrentActivityName());
		
		t.start();
	}
	
	private void isJobAbbortedInQueueCheck(final Project<?, ?> project, final ActivityExecution execution)
	{
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				//check if job is aborted in queue
				try {
					while (project.isInQueue()) {
						Thread.sleep(20);
					}

					// aborted in queue if it is not building
					if (!project.isBuilding()) {
						throw new Exception("Job aborted in Queue");
					}
					
				} catch (Exception e) {
					
					try {
						failureAllParentActivities((ActivityImpl) execution.getActivity());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
					return;
				}
				
			}
		},"ActivitiIsJobAbortedInQueueCheck for " +execution.getCurrentActivityName());
		
		t.start();
	}


	@Override
	public String toString() {
		return "JenkinsActivitiTaskDelegate [jobName=" + jobName + "]";
	}
	

	
}
