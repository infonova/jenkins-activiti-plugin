package com.bearingpoint.infonova.jenkins.util;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;

import org.h2.util.IOUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

// TODO: javadoc
public class WorkflowErrorMessage implements HttpResponse {

	private final AbstractBuild<?, ?> build;

	private final String errorRef;

	public WorkflowErrorMessage(AbstractBuild<?, ?> build, String errorRef) {
		this.build = build;
		this.errorRef = errorRef;
	}

	/**
	 * Renders the process diagram.
	 * 
	 * @param req
	 *            the request
	 * @param rsp
	 *            the response
	 */
	public void generateResponse(StaplerRequest req, StaplerResponse rsp,
			Object node) throws IOException, ServletException {

		rsp.setContentType("text/plain");

		InputStream in = getErrorMessageAsStream();
		OutputStream out = rsp.getOutputStream();

		assert in != null;

		IOUtils.copy(in, out);
	}

	// TODO: javadoc
	private InputStream getErrorMessageAsStream() {

		try {
			File errorMessage = new File(build.getRootDir(), errorRef);
			return new FileInputStream(errorMessage);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("no error reference found: " + errorRef);
		}
	}

}
