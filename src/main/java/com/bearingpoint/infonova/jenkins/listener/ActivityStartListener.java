package com.bearingpoint.infonova.jenkins.listener;

import java.util.Observable;
import java.util.Observer;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.apache.log4j.Logger;

import com.bearingpoint.infonova.jenkins.util.DestructionCallback;

/**
 * {@link ExecutionListener} implementation used to signal when an activity starts. <br />
 * Implements the {@link Observable} class in order to inform {@link Observer} instances.
 * 
 * @author christian.weber
 * @since 1.0
 */
@SuppressWarnings("serial")
public class ActivityStartListener extends Observable implements ExecutionListener, DestructionCallback {

    private transient Logger logger = Logger.getLogger(ActivityStartListener.class);

    public void notify(DelegateExecution execution) throws Exception {

        if (logger.isDebugEnabled()) {
            ExecutionEntity executionEntity = (ExecutionEntity)execution;
            logger.debug("activity end listener " + executionEntity.getActivityId());
        }

        super.setChanged();
        super.notifyObservers(execution);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        super.deleteObservers();
    }

}
