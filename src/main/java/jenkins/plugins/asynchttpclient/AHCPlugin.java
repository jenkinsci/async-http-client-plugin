package jenkins.plugins.asynchttpclient;

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
    @Override
    public void stop() throws Exception {
        AHC ahc = AHC.class.cast(Jenkins.getInstance().getDescriptor(AHC.class));
        if (ahc != null) {
            ahc.shutdown();
        }
        super.stop();
    }
}
