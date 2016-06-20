package jenkins.plugins.asynchttpclient;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import hudson.ProxyConfiguration;
import hudson.model.FreeStyleProject;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import jenkins.model.Jenkins;
import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import sun.security.provider.certpath.SunCertPathBuilderException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

public class AHCTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void closeCausesRecycle() {
        assertThat(AHC.instance(), notNullValue());
        AHC.instance().close();
        assertThat(AHC.instance(), notNullValue());
        assertThat(AHC.instance(), hasProperty("closed", is(false)));
    }

    @Test
    public void worksOnJenkinsClasspath() throws Exception {
        final Random entropy = new Random();
        final String jobName = String.format("Random job name %d", entropy.nextLong());
        final String systemMessage = String.format("Random system message %d", entropy.nextLong());
        j.jenkins.setSystemMessage(systemMessage);
        j.getInstance().createProject(j.getInstance().getDescriptorByType(FreeStyleProject.DescriptorImpl.class),
                jobName);
        final ListenableFuture<Response> response =
                AHC.instance().executeRequest(new RequestBuilder("GET").setUrl(j.getURL().toURI().toString()).build());
        assertThat(response.get().getResponseBody(), allOf(
                containsString(Jenkins.getVersion().toString()),
                containsString(systemMessage),
                containsString(jobName)
        ));
    }

    @Test(expected=SunCertPathBuilderException.class)
    public void failsOnSelfSignedCertificate() throws Throwable {
        try {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            URL url = new URL("https://letsencrypt.org");
            HttpURLConnection connection = (HttpURLConnection)
                    (proxy == null ? url.openConnection() : url.openConnection(proxy.createProxy("self-signed.badssl.com")));
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(30000);
            connection.connect();
            throw new AssumptionViolatedException("The certificate for self-signed.badssl.com is not trusted");
        } catch (SSLHandshakeException e) {
            // yeah we have a test
        } catch (SocketTimeoutException e) {
            throw new AssumptionViolatedException("We can connect to self-signed.badssl.com", e);
        }
        AsyncHttpClient ahc = AHC.instance();
        ListenableFuture<Response> response = ahc.prepareGet("https://self-signed.badssl.com/").execute();
        try {
            response.get();
        } catch (ExecutionException e) {
            // walk the cause stack to get the real cause
            throw e.getCause().getCause().getCause().getCause().getCause();
        }
        fail("Self Signed certificate accepted");
    }


    @Test(expected=SunCertPathBuilderException.class)
    public void failsOnExpiredCertificate() throws Throwable {
        try {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            URL url = new URL("https://letsencrypt.org");
            HttpURLConnection connection = (HttpURLConnection)
                    (proxy == null ? url.openConnection() : url.openConnection(proxy.createProxy("expired.badssl.com")));
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(30000);
            connection.connect();
            throw new AssumptionViolatedException("The certificate for expired.badssl.com is expired");
        } catch (SSLHandshakeException e) {
            // yeah we have a test
        } catch (SocketTimeoutException e) {
            throw new AssumptionViolatedException("We can connect to expired.badssl.com", e);
        }
        AsyncHttpClient ahc = AHC.instance();
        ListenableFuture<Response> response = ahc.prepareGet("https://expired.badssl.com/").execute();
        try {
            response.get();
        } catch (ExecutionException e) {
            // walk the cause stack to get the real cause
            throw e.getCause().getCause().getCause().getCause().getCause();
        }
        fail("Expired certificate accepted");
    }

    @Test
    public void acceptGoodCertificate() throws Throwable {
        try {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            URL url = new URL("https://letsencrypt.org");
            HttpURLConnection connection = (HttpURLConnection)
                    (proxy == null ? url.openConnection() : url.openConnection(proxy.createProxy("letsencrypt.org")));
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(30000);
            connection.connect();
        } catch (SSLHandshakeException e) {
            throw new AssumptionViolatedException("The Root CA for letsencrypt.org is in the JVM trust store", e);
        } catch (SocketTimeoutException e) {
            throw new AssumptionViolatedException("We can connect to letsencrypt.org", e);
        }
        AsyncHttpClient ahc = AHC.instance();
        ListenableFuture<Response> response = ahc.prepareGet("https://letsencrypt.org").execute();
        assertTrue(response.get().hasResponseStatus());
    }
}
