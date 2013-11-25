package com.bearingpoint.infonova.jenkins.test.common;

import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bearingpoint.infonova.jenkins.cause.WorkflowCause;
import com.bearingpoint.infonova.jenkins.listener.JenkinsBuildListener;


public class FreeStyleProjectMockBuilder {

    private final FreeStyleProject project;

    private int buildCounter;

    private List<FreeStyleBuild> builds = new ArrayList<FreeStyleBuild>();

    private FreeStyleProjectMockBuilder(FreeStyleProject project) {
        this.project = project;
    }

    public static FreeStyleProjectMockBuilder mock(String name) {
        FreeStyleProject project = Mockito.mock(FreeStyleProject.class);

        ItemGroup<?> parent = Mockito.mock(ItemGroup.class);
        Mockito.when(parent.getFullDisplayName()).thenReturn(name + "_parent");

        Mockito.when(project.getParent()).thenReturn(parent);
        Mockito.when(project.getName()).thenReturn(name);
        Mockito.when(project.getFullDisplayName()).thenReturn(name);
        Mockito.when(project.hasPermission(Matchers.any(Permission.class))).thenReturn(true);

        return new FreeStyleProjectMockBuilder(project);
    }

    public FreeStyleProjectMockBuilder withBuild() {
        FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);
        Mockito.when(project.getBuildByNumber(++buildCounter)).thenReturn(build);
        Mockito.when(project.getLastBuild()).thenReturn(build);

        ActivityExecution execution = Mockito.mock(ActivityExecution.class);
        Mockito.when(execution.getActivity()).thenReturn(Mockito.mock(PvmActivity.class));
        WorkflowCause cause = new WorkflowCause(project.getName(), buildCounter, "pid", execution);
        Mockito.when(build.getCause(WorkflowCause.class)).thenReturn(cause);

        builds.add(build);
        RunList<FreeStyleBuild> runList = new MockRunList<FreeStyleBuild>();
        runList.addAll(builds);
        Mockito.when(project.getBuilds()).thenReturn(runList);

        return this;
    }

    public <T extends Action> FreeStyleProjectMockBuilder withBuild(T... actions) {
        FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);
        Mockito.when(project.getBuildByNumber(++buildCounter)).thenReturn(build);

        @SuppressWarnings("unchecked")
        List<Action> list = (List<Action>)Arrays.asList(actions);

        Mockito.when(project.getActions()).thenReturn(list);
        for (final T action : actions) {
            build.addAction(action);
            Mockito.when(build.getAction(action.getClass())).thenAnswer(new ActionAnswer(action));

            List<Action> list2 = new ArrayList<Action>();
            list2.add(action);
            Mockito.when(build.getActions(action.getClass())).thenAnswer(new ActionListAnswer(list2));

            Mockito.when(project.getAction(action.getClass())).thenAnswer(new ActionAnswer(action));
        }


        builds.add(build);
        RunList<FreeStyleBuild> runList = new MockRunList<FreeStyleBuild>();
        runList.addAll(builds);

        Mockito.when(project.getBuilds()).thenReturn(runList);

        Mockito.when(project.getLastBuild()).thenReturn(build);

        return this;
    }

    public void mockScheduleBuild2() {

        Answer<?> answer = new Answer<Object>() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            public Object answer(InvocationOnMock invocation) throws Throwable {

                WorkflowCause cause = (WorkflowCause)invocation.getArguments()[1];

                final Run r = Mockito.mock(Run.class);
                Mockito.when(r.getResult()).thenReturn(Result.FAILURE);
                Mockito.when(r.getCause(WorkflowCause.class)).thenReturn(cause);

                Object obj = invocation.callRealMethod();

                new Thread() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            new JenkinsBuildListener().onFinalized(r);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.start();
                return obj;
            }

        };

        Mockito.when(project.scheduleBuild2(Matchers.anyInt(), Matchers.any(Cause.class), Matchers.any(Action.class)))
            .then(answer);
    }

    public FreeStyleProject project() {
        return project;
    }

    private class ActionAnswer implements Answer<Action> {

        private final Action action;

        public ActionAnswer(Action action) {
            this.action = action;
        }

        public Action answer(InvocationOnMock invocation) throws Throwable {
            return action;
        }
    }

    private class ActionListAnswer implements Answer<List<Action>> {

        private final List<Action> actions;

        public ActionListAnswer(List<Action> actions) {
            this.actions = actions;
        }

        public List<Action> answer(InvocationOnMock invocation) throws Throwable {
            return actions;
        }
    }

    public static class MockRunList<R extends Run> extends RunList<R> {

        List<Run> list = new ArrayList<Run>();

        @Override
        public boolean add(Run e) {
            list.add(e);
            return true;
        }

        @Override
        public boolean addAll(Collection c) {
            for (Object obj : c) {
                list.add((Run)obj);
            }
            return true;
        }


    }

}
