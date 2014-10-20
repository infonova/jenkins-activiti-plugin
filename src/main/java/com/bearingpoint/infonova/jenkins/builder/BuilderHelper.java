package com.bearingpoint.infonova.jenkins.builder;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BuilderHelper {
	
	public static File getWorkflowFromSlave(final AbstractBuild<?, ?> build,
			String pathToWorkflow) throws IOException, InterruptedException,
			FileNotFoundException {
		
		final FilePath workspace = build.getWorkspace();
		FilePath workflow = workspace.child(pathToWorkflow);
		//make folder in tmp folder, then make a build specific one
		File wFile = new File("/tmp/activiti-wfs/" + build.getFullDisplayName()
				+ "/" + workflow.getName());
		wFile.getParentFile().mkdirs();
		workflow.copyTo(new FileOutputStream(wFile));
		return wFile;
	}
	
	public static void deleteWfFoldersOnMaster(File wFile)
	{
		//delete file from master
        wFile.delete();     
        //..and delete the build specified folder
        wFile.getParentFile().delete();
	}
}
