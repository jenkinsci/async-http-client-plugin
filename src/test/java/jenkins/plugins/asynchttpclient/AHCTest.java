package jenkins.plugins.asynchttpclient;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author stephenc
 * @since 18/12/2012 09:58
 */
public class AHCTest  extends HudsonTestCase {

    public void testSmokes() {
        assertNotNull(AHC.instance());
        AHC.instance().close();
        assertNotNull(AHC.instance());
        assertFalse(AHC.instance().isClosed());
    }

}
