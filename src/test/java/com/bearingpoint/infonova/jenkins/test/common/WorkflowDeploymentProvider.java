package com.bearingpoint.infonova.jenkins.test.common;

import hudson.FilePath;

import org.activiti.engine.ProcessEngine;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.bearingpoint.infonova.jenkins.util.ActivitiAccessor;

@Component
public class WorkflowDeploymentProvider implements InitializingBean {

	private String path;

	public void setPath(String path) {
		this.path = path;
	}

	@Autowired
	private ProcessEngine engine;

	public void afterPropertiesSet() throws Exception {
		Resource resource = new ClassPathResource(path);
		ActivitiAccessor.deployProcess(engine, new FilePath(resource.getFile()));
	}

}
