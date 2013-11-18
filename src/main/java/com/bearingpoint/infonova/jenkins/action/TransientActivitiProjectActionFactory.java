package com.bearingpoint.infonova.jenkins.action;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.tasks.Builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.bearingpoint.infonova.jenkins.builder.WorkflowBuilder;

/**
 * This factory class is used to register transient {@link Action} instances.
 * 
 * @author christian.weber
 * @since 1.0
 * @see WorkflowPictureAction
 */
@Extension
public class TransientActivitiProjectActionFactory extends TransientProjectActionFactory {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<? extends Action> createFor(AbstractProject abstractProject) {
		
		if (!(abstractProject instanceof Project)) {
			return emptyCollection();
		}
		
		Project project = (Project) abstractProject;
		List<Builder>builders = project.getBuilders();
		
		List<WorkflowBuilder>workflowBuilders = new ArrayList<WorkflowBuilder>();
		
		for (Builder builder : builders) {
			
			if (!(builder instanceof WorkflowBuilder)) {
				continue;
			}	
			workflowBuilders.add((WorkflowBuilder)builder);
		}
		return workflowBuilders.isEmpty() ? emptyCollection() : singletonList(project);
	}

	private static Collection<Action> emptyCollection() {
		return Collections.<Action>emptyList();
	}
	
	private static Collection<? extends Action> singletonList(AbstractProject<?, ?> project) {
		List<Action> actions = new ArrayList<Action>();
		actions.add(new WorkflowPictureAction(project));
		actions.add(new WorkflowErrorAction(project));
		
		return Collections.unmodifiableList(actions);
		//return Collections.singletonList(new WorkflowPictureAction(project));
	}
	
}
