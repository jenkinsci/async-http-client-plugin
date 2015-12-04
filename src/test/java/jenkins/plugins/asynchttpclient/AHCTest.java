package jenkins.plugins.asynchttpclient;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import hudson.model.FreeStyleProject;
import java.util.Random;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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

}
