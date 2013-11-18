package com.bearingpoint.infonova.jenkins.activitybehavior.remote;

import com.thoughtworks.xstream.annotations.XStreamAlias;

public abstract class AbstractRemoteJenkinsBuild {

	private boolean building;

	private String fullDisplayName;

	private int number;

	private String result;

	public boolean isBuilding() {
		return building;
	}

	public String getFullDisplayName() {
		return fullDisplayName;
	}

	public int getNumber() {
		return number;
	}

	public String getResult() {
		return result;
	}
	
	@XStreamAlias("freeStyleBuild")
	public static class RemoteFreeStyleBuild extends AbstractRemoteJenkinsBuild {

	}
	
	@XStreamAlias("mavenModuleSetBuild")
	public class RemoteMavenBuild extends AbstractRemoteJenkinsBuild {

	}

}