package com.bearingpoint.infonova.jenkins.exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * Resolves the error messages for activiti errors.
 * 
 * @author christian.weber
 * @since 1.0
 */
public final class ErrorMessageResolver {

	private static final Properties properties;
	
	static {
		properties = new Properties();
		loadProperties();
	}
	
	private ErrorMessageResolver() {
		super();
	}
	
	/**
	 * Loads the error properties.
	 */
	private static void loadProperties() {
		InputStream stream = null;
		try {
			stream = ErrorMessageResolver.class.getClassLoader().getResourceAsStream("errors.properties");
			properties.load(stream);
		} catch (IOException e) {
			throw new IllegalStateException("error while loading error.properties", e);
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}
	
	/**
	 * Returns the error message regarding to the given error code and locale.
	 * 
	 * @param errorCode
	 * @return String
	 */
	public static String resolve(ErrorCode errorCode) {
		
		if (errorCode == null) {
			throw new IllegalArgumentException("error code must not be null");
		}
		
		return properties.getProperty(errorCode.getKey());
	}
	

	/**
	 * Returns the error message regarding to the given error code and locale.
	 * 
	 * @param errorCode
	 * @return String
	 */
	public static String resolve(ErrorCode errorCode, Object...arguments) {		
		final String template = resolve(errorCode);
		return String.format(template, arguments);
	}

}
