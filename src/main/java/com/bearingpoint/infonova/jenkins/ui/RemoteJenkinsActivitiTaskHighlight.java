package com.bearingpoint.infonova.jenkins.ui;

import java.util.Map;

import org.activiti.engine.impl.pvm.process.ActivityImpl;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AbstractTaskHighlight} implementation for remote JENKINS task highlighting.
 * 
 * @author christian.weber
 * @since 1.0
 */
@XStreamAlias("remotejenkinstask")
public class RemoteJenkinsActivitiTaskHighlight extends AbstractTaskHighlight {

    private final Map<String, Object> properties;

    public RemoteJenkinsActivitiTaskHighlight(ActivityImpl activity, int x1, int y1, int x2, int y2) {
        super(getProcessId(activity), activity.getId(), x1, y1, x2, y2);
        this.properties = activity.getProperties();
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
        return (String)properties.get("jobName");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLink() {
        final String scheme = (String)properties.get("scheme");
        final String host = (String)properties.get("host");
        final String port = (String)properties.get("port");

        return String.format("%s://%s:%s/job/%s", scheme, host, port, getJobName());
    }

}
