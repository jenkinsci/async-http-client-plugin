package jenkins.plugins.asynchttpclient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Plugin;
import jenkins.model.Jenkins;

/**
 * @author stephenc
 * @since 18/12/2012 10:10
 */
public class AHCPlugin extends Plugin {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "https://github.com/jenkinsci/jenkins/pull/2094")
    @Override
    public void stop() throws Exception {
        AHC ahc = AHC.class.cast(Jenkins.getInstance().getDescriptor(AHC.class));
        if (ahc != null) {
            ahc.shutdown();
        }
        super.stop();
    }
}
