package com.bearingpoint.infonova.jenkins.activitybehavior.remote;

import hudson.model.Run;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.bearingpoint.infonova.jenkins.activitybehavior.remote.AbstractRemoteJenkinsBuild.RemoteFreeStyleBuild;
import com.bearingpoint.infonova.jenkins.activitybehavior.remote.AbstractRemoteJenkinsBuild.RemoteMavenBuild;
import com.bearingpoint.infonova.jenkins.activitybehavior.remote.client.PreemptiveHttpClient;

public class RemoteJenkinsRestClient {

    /*
     * TODO USERNAME and PASSWORD have to be made configurable
     */
    private static final String PASSWORD = "eT$061113+";
    private static final String USERNAME = "wallview";

    private final String scheme;

    private final String port;

    private final String host;

    private final String path;

    public RemoteJenkinsRestClient(String scheme, String port, String host, String path) {
        this.scheme = scheme;
        this.port = port;
        this.host = host;
        this.path = preparePath(path);
    }

    private String preparePath(String path) {
        if (!StringUtils.endsWith(path, "/")) {
            path = path + "/";
        }
        if (!StringUtils.startsWith(path, "/")) {
            path = "/" + path;
        }
        return path;
    }

    /**
     * Returns the set of parameter names of the JENKINS job with the given
     * name.
     * 
     * @param jobName
     * @return Set
     * @throws Exception
     */
    public Set<String> getParameterNames(String jobName) throws Exception {

        // establish the HTTP connection
        HttpClient client = new PreemptiveHttpClient(USERNAME, PASSWORD, 30);

        // GetMethod method = new GetMethod(getProjectConfigURI(jobName));
        HttpGet request = new HttpGet(getProjectConfigURI(jobName));

        // execute the HTTP method
        String response = new BasicResponseHandler().handleResponse(client.execute(request));
        InputStream inputStream = IOUtils.toInputStream(response);

        return getParameterNames(inputStream);
    }

    /**
     * Returns the REST URI which returns the project configuration XML.
     * 
     * @param jobName
     * @return String
     * @throws Exception
     */
    private String getProjectConfigURI(String jobName) throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(scheme);
        builder.setHost(host);
        if (port != null) {
            builder.setPort(Integer.parseInt(port));
        }
        builder.setPath(path + jobName + "/config.xml");

        System.out.println("URI: " + builder.build());
        return builder.build().toString();
    }

    /**
     * Returns the set of parameter names of the JENKINS job with the given
     * name.
     * 
     * @param configStream
     * @return Set
     * @throws Exception
     */
    private Set<String> getParameterNames(InputStream configStream) throws Exception {
        Set<String> parameterNames = new HashSet<String>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        String expression = "//properties//name";
        XPathExpression xPathExpression = xPath.compile(expression);
        InputSource inputSource = new InputSource(configStream);

        NodeList list = (NodeList)xPathExpression.evaluate(inputSource, XPathConstants.NODESET);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = (Node)list.item(i);
            parameterNames.add(node.getTextContent());
        }

        return parameterNames;
    }

    /**
     * Schedules the JENKINS job with the given name. If the JENKINS job is
     * parameterized matching parameters from the ACTIVITI context will be used.
     * 
     * @param uri
     * @param parameters
     * @throws Exception
     */
    public void scheduleJob(String jobName, Map<String, Object> variables, Map<String, String> params) throws Exception {
        HttpClient client = new PreemptiveHttpClient(USERNAME, PASSWORD, 30);

        Set<String> parameterNames = getParameterNames(jobName);

        HttpUriRequest request = null;
        if (CollectionUtils.isEmpty(parameterNames) && CollectionUtils.isEmpty(params)) {
            String uri = getExecutionURI(jobName);
            request = new HttpPost(uri);
            System.out.println(uri);
        } else {
            String uri = getParameterizedExecutionURI(jobName, parameterNames, variables, params);
            request = new HttpPost(uri);
            System.out.println(uri);
        }

        client.execute(request);

        System.out.println("finished");
    }

    /**
     * Returns the JENKINS URI which executes the JENKINS Job with the given
     * name.
     * 
     * @return String the URI
     * @throws Exception
     */
    private String getExecutionURI(String jobName) throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(scheme);
        builder.setHost(host);
        if (port != null) {
            builder.setPort(Integer.parseInt(port));
        }
        builder.setPath(path + jobName + "/build");

        System.out.println("URI: " + builder.build());
        return builder.build().toString();
    }

    /**
     * Returns the JENKINS URI which executes the JENKINS Job with the given
     * name.
     * 
     * @return String the URI
     * @throws Exception
     */
    private String getParameterizedExecutionURI(String jobName, Set<String> parameters,
            Map<String, Object> globalVariables, Map<String, String> taskVariables) throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(scheme);
        builder.setHost(host);
        if (port != null) {
            builder.setPort(Integer.parseInt(port));
        }
        builder.setPath(path + jobName + "/buildWithParameters");

        Map<String, Object> variables = new HashMap<String, Object>(globalVariables);
        variables.putAll(taskVariables);

        StringBuilder query = new StringBuilder();
        for (String parameter : parameters) {

            Object variable = variables.get(parameter);
            if (variable != null) {
                if (!query.toString().isEmpty()) {
                    query.append("&");
                }
                query.append(parameter + "=" + variable);
            }

        }

        builder.setQuery(query.toString());

        System.out.println("URI: " + builder.build());
        return builder.build().toString();
    }

    /**
     * Returns an {@link AbstractRemoteJenkindBuild} instance which contains
     * META information about the JENKINS job with the given name.
     * 
     * @param jobName
     * @return AbstractRemoteJenkinsBuild
     * @throws Exception
     */
    public AbstractRemoteJenkinsBuild getJobInfo(String jobName, String buildNumberOrLink) throws Exception {
        HttpClient client = new PreemptiveHttpClient(USERNAME, PASSWORD, 30);

        HttpUriRequest request = new HttpPost(getJobInfoUri(jobName, buildNumberOrLink));

        HttpResponse response = client.execute(request);
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() == HttpStatus.SC_OK) {
            BasicResponseHandler handler = new BasicResponseHandler();
            String xml = handler.handleResponse(response);

            if (xml.contains("freeStyleBuild")) {
                return (RemoteFreeStyleBuild)Run.XSTREAM2.fromXML(xml);
            } else {
                return (RemoteMavenBuild)Run.XSTREAM2.fromXML(xml);
            }
        } else {
            System.out.println("Job information could not be determined. Code: " + status.getStatusCode() + ", "
                + status.getReasonPhrase() + ")");
            return null;
        }

    }

    /**
     * Returns an {@link AbstractRemoteJenkindBuild} instance which contains
     * META information about the JENKINS job with the given name.
     * 
     * @param jobName
     * @return AbstractRemoteJenkinsBuild
     * @throws Exception
     */
    public AbstractRemoteJenkinsBuild getJobInfo(String jobName) throws Exception {
        return getJobInfo(jobName, "lastBuild");
    }

    /**
     * Returns the JENKINS REST URI which returns the META information about the
     * jenkins job with the given name.
     * 
     * @param jobName
     * @return String
     * @throws Exception
     */
    private String getJobInfoUri(String jobName, String buildNumberOrLink) throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(scheme);
        builder.setHost(host);
        if (port != null) {
            builder.setPort(Integer.parseInt(port));
        }
        builder.setPath(path + jobName + "/" + buildNumberOrLink + "/api/xml");
        builder.setQuery("tree=fullDisplayName,building,number,result");

        System.out.println("URI: " + builder.build());
        return builder.build().toString();
    }

}
