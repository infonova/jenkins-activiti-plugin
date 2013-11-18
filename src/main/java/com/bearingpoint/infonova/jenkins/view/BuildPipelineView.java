package com.bearingpoint.infonova.jenkins.view;

import static com.bearingpoint.infonova.jenkins.util.JenkinsUtils.getProjectAction;
import static com.bearingpoint.infonova.jenkins.util.JenkinsUtils.getProjectActions;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.ViewGroup;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor.FormException;
import hudson.model.Project;
import hudson.model.View;
import hudson.model.ViewDescriptor;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowErrorAction;
import com.bearingpoint.infonova.jenkins.action.RunProjectAction;
import com.bearingpoint.infonova.jenkins.action.WorkflowPictureAction;
import com.bearingpoint.infonova.jenkins.ui.AbstractArea;
import com.bearingpoint.infonova.jenkins.ui.TaskState;
import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;
import com.bearingpoint.infonova.jenkins.util.JenkinsUtils;

/**
 * Jenkins {@link View} implementation used to render <br />
 * the process monitor for activiti workflows.
 * 
 * @author christian.weber
 * @since 1.0
 */
public class BuildPipelineView extends View {

	private String projectName;

	// TODO: rename to NoOfDisplayedBuilds
	private int buildsToDisplay;

	@DataBoundConstructor
	public BuildPipelineView(String name) {
		super(name);
	}

	@Override
	public boolean contains(TopLevelItem item) {
		return this.getItems().contains(item);
	}

	
	
	@Override
	public List<Action> getActions() {
		List<Action> actions = new ArrayList<Action>();
		actions.addAll(super.getActions());
		actions.add(new RunProjectAction(projectName));
		
		return actions;
	}

	@Override
	public Item doCreateItem(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		return Jenkins.getInstance().doCreateItem(req, rsp);
	}

	@Override
	public Collection<TopLevelItem> getItems() {
		return Collections.emptyList();
	}

	/**
	 * Returns the {@link AbstractBuild} instances of the project.
	 * 
	 * @return List
	 */
	public List<? extends AbstractBuild<?, ?>> getProjectBuilds() {
		List<? extends AbstractBuild<?, ?>> builds = JenkinsUtils
				.getProjectBuilds(projectName);
		return builds.subList(0, Math.min(builds.size(), buildsToDisplay));
	}

	/**
	 * Returns the last five {@link AbstractBuild} instances of the project.
	 * 
	 * @return List
	 */
	public List<? extends AbstractBuild<?, ?>> getProjectBuilds(
			int maxBuildsToDisplay) {
		List<? extends AbstractBuild<?, ?>> builds = JenkinsUtils
				.getProjectBuilds(projectName);
		return builds.subList(0, Math.min(builds.size(), maxBuildsToDisplay));
	}

	/**
	 * Returns all {@link TopElementItem} instances with a
	 * {@link WorkflowPictureAction} {@link Action}.
	 * 
	 * @return List
	 */
	public List<TopLevelItem> getRelevantItems() {
		return JenkinsUtils.getItemsWithAction(WorkflowPictureAction.class);
	}

	/**
	 * Javascript method used to return the element coordinates.
	 * 
	 * @param buildNr
	 * @return List
	 */
	@JavaScriptMethod
	public List<AbstractArea> getHighlightElements(String processDefinitionId,
			int buildNr) {
		ActivitiWorkflowAction action = JenkinsUtils.getWorkflowAction(
				projectName, buildNr, processDefinitionId);
		return action.getElements();
	}

	/**
	 * Javascript method used to return the task states.
	 * 
	 * @param buildNr
	 * @return Map
	 */
	@JavaScriptMethod
	public Map<String, TaskState> getTaskStates(String processDefinitionId,
			int buildNr) {
		ActivitiWorkflowAction action = JenkinsUtils.getWorkflowAction(
				projectName, buildNr, processDefinitionId);
		return action.getStates();
	}

	/**
	 * Returns all available process definition IDs.
	 * 
	 * @return List
	 */
	@JavaScriptMethod
	public List<ActivitiWorkflowAction> getProcessDefinitionIds(int buildNr) {
		return getProjectActions(projectName, buildNr,
				ActivitiWorkflowAction.class);
	}

	/**
	 * Returns the first process definition id of the build with the given
	 * number.
	 * 
	 * @param buildNr
	 * @return String
	 */
	@JavaScriptMethod
	public String getFirstProcessDefinitionId(int buildNr) {
		ActivitiWorkflowAction action = getProjectAction(projectName, buildNr,
				ActivitiWorkflowAction.class);

		if (action == null) {
			// TODO: exception handling
			// TODO: load ActivitiWorkflowErrorAction
			return "unknown";
		}

		return action.getProcessDescriptionId();
	}

	/**
	 * Returns the build number of the given project by the process definition
	 * id.
	 * 
	 * @param processDefinitionId
	 * @param projectName
	 * @return Integer
	 */
	@JavaScriptMethod
	public Integer getBuildNumber(String processDefinitionId, String projectName) {
		return JenkinsUtils.getBuildNumberByProcessDefinitionId(
				processDefinitionId, projectName);
	}

	/**
	 * Indicates if the build with the given number is building.
	 * 
	 * @param buildNr
	 * @return boolean
	 */
	@JavaScriptMethod
	public boolean isBuilding(int buildNr) {
		Project<?, ?> project = JenkinsUtils.getProject(projectName);
		AbstractBuild<?, ?> build = project.getBuildByNumber(buildNr);

		return build.isBuilding();
	}

	/**
	 * This method is used to execute asynchronous javascript functions.
	 * 
	 * @return boolean
	 */
	@JavaScriptMethod
	public boolean async() {
		return true;
	}

	/**
	 * Returns an {@link ActivitiWorkflowErrorAction} implementation if exists.
	 * 
	 * @param mainProcess
	 * @param buildNr
	 * @return ActivitiWorkflowErrorAction
	 */
	@JavaScriptMethod
	public ActivitiWorkflowErrorAction getErrorAction(int buildNr) {
		Class<ActivitiWorkflowErrorAction> clazz = ActivitiWorkflowErrorAction.class;
		return JenkinsUtils.getProjectAction(projectName, buildNr, clazz);
	}
	
	/**
	 * Continues the current blocking ACTIVITI user task.
	 */
	@JavaScriptMethod
	public boolean continueUserTask(String processDefinitionId, String taskId) {
		ActivitiAccessor.completeTask(processDefinitionId, taskId);
		return true;
	}
	
	/**
	 * Returns the latest build.
	 * 
	 * @return AbstractBuild
	 */
	public AbstractBuild<?, ?> getLatestBuild() {
		return JenkinsUtils.getLatestBuild(projectName);
	}

	/**
	 * /** If a project name is changed we check if the selected job for this
	 * view also needs to be changed.
	 * 
	 * @param item
	 *            - The Item that has been renamed
	 * @param oldName
	 *            - The old name of the Item
	 * @param newName
	 *            - The new name of the Item
	 * 
	 */
	@Override
	public void onJobRenamed(Item item, String oldName, String newName) {
		// do nothing
	}

	/**
	 * Handles the configuration submission
	 * 
	 * @param req
	 *            Stapler Request
	 * @throws FormException
	 *             Form Exception
	 * @throws IOException
	 *             IO Exception
	 * @throws ServletException
	 *             Servlet Exception
	 */

	@Override
	protected void submit(StaplerRequest req) throws IOException,
			ServletException, FormException {
		req.bindJSON(this, req.getSubmittedForm());
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public void setBuildsToDisplay(int buildsToDisplay) {
		this.buildsToDisplay = buildsToDisplay;
	}

	public int getBuildsToDisplay() {
		return this.buildsToDisplay;
	}

	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {

		public String getDisplayName() {
			return "Process Monitor";
		}

		public ListBoxModel doFillBuildsToDisplayItems() {
			// TODO: use constructor varargs
			ListBoxModel options = new ListBoxModel();
			options.add("1");
			options.add("5");
			options.add("10");
			options.add("20");
			options.add("50");
			options.add("100");
			
			return options;
		}

	}

}
