package com.bearingpoint.infonova.jenkins.ui;

import java.util.Map;

import org.activiti.engine.impl.pvm.process.ActivityImpl;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for jenkins task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("jenkinstask")
public class JenkinsActivitiTaskHighlight extends AbstractTaskHighlight {

    private final String jobName;

    public JenkinsActivitiTaskHighlight(ActivityImpl activity, int x1, int y1, int x2, int y2) {
        super(getProcessId(activity), activity.getId(), x1, y1, x2, y2);

        Map<String, Object> properties = activity.getProperties();
        this.jobName = (String)properties.get("jobName");
    }

    private static String getProcessId(ActivityImpl activity) {
        return activity.getProcessDefinition().getId();
    }

    /**
     * Returns the job name.
     * 
     * @return String
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLink() {
        return "/job/" + jobName;
    }

}
