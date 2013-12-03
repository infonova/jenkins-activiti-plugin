package com.bearingpoint.infonova.jenkins.activitybehavior.remote.client;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * Based on:
 * http://subversion.jfrog.org/jfrog/build-info/trunk/build-info-client/src/main/java/org/jfrog/build/client/PreemptiveHttpClient.java
 */
public class PreemptiveHttpClient implements HttpClient {

    static class PreemptiveAuth implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
                        ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(
                            new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme, creds);
                }
            }
        }
    }

    private DefaultHttpClient httpClient;
    private BasicHttpContext localContext;

    public PreemptiveHttpClient(int timeout) throws GeneralSecurityException {
        this(null, null, timeout);
    }

    public PreemptiveHttpClient(String userName, String password, int timeout) throws GeneralSecurityException {
        httpClient = createHttpClient(userName, password, timeout);
    }

    private void overrideTrustManager(HttpClient client) throws GeneralSecurityException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        X509TrustManager trustManager = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[] { trustManager }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext);
        socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = client.getConnectionManager().getSchemeRegistry();
        registry.register(new Scheme("https", 443, socketFactory));
    }

    private DefaultHttpClient createHttpClient(String userName, String password, int timeout) throws GeneralSecurityException {
        BasicHttpParams params = new BasicHttpParams();
        int timeoutMilliSeconds = timeout * 1000;
        HttpConnectionParams.setConnectionTimeout(params, timeoutMilliSeconds);
        HttpConnectionParams.setSoTimeout(params, timeoutMilliSeconds);
        DefaultHttpClient client = new DefaultHttpClient(params);

        overrideTrustManager(client);

        if (!StringUtils.isBlank(userName)) {
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(userName, password)
            );
            localContext = new BasicHttpContext();

            // Generate BASIC scheme object and stick it to the local execution context
            BasicScheme basicAuth = new BasicScheme();
            localContext.setAttribute("preemptive-auth", basicAuth);

            // Add as the first request interceptor
            client.addRequestInterceptor(new PreemptiveAuth(), 0);
        }

        // set the following user agent with each request
        String userAgent = "JenkinsActivitiPlugin";
        HttpProtocolParams.setUserAgent(client.getParams(), userAgent);
        return client;
    }

    public void shutdown() {
        httpClient.getConnectionManager().shutdown();
    }

    public void setParams(HttpParams httpParams) {
        httpClient.setParams(httpParams);
    }

    @Override
    public HttpParams getParams() {
        return httpClient.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return httpClient.getConnectionManager();
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        if (localContext != null) {
            return execute(request, localContext);
        } else {
            return httpClient.execute(request);
        }
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException,
                    ClientProtocolException {
        return httpClient.execute(request, context);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return httpClient.execute(target, request);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException,
                    ClientProtocolException {
        return httpClient.execute(target, request, context);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException,
                    ClientProtocolException {
        return httpClient.execute(request, responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context)
                    throws IOException, ClientProtocolException {
        return httpClient.execute(request, responseHandler, context);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler)
                    throws IOException, ClientProtocolException {
        return httpClient.execute(target, request, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler,
                    HttpContext context) throws IOException, ClientProtocolException {
        return httpClient.execute(target, request, responseHandler, context);
    }

}
