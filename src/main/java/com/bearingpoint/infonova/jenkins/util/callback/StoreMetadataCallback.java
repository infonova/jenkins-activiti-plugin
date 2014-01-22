package com.bearingpoint.infonova.jenkins.util.callback;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.bearingpoint.infonova.jenkins.action.ActivitiWorkflowAction;
import com.bearingpoint.infonova.jenkins.util.DestructionCallback;

/**
 * This {@link DestructionCallback} class is used to store metadata information.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class StoreMetadataCallback implements DestructionCallback {

    private final AbstractBuild<?, ?> build;
    private final ActivitiWorkflowAction action;

    public StoreMetadataCallback(AbstractBuild<?, ?> build, ActivitiWorkflowAction action) {
        this.build = build;
        this.action = action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

        try {
            Properties properties = action.getMetadata().getProperties();

            File file = new File(build.getRootDir(), "metadata.properties");

            properties.store(new FileOutputStream(file), "activity metadata");
        } catch (IOException e) {
            throw new RuntimeException("error while storing metadata", e);
        }
    }

}
