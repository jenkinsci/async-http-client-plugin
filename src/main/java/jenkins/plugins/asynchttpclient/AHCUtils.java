package jenkins.plugins.asynchttpclient;

import com.ning.http.client.ProxyServer;
import hudson.ProxyConfiguration;
import hudson.model.Hudson;

/**
 * Utility methods for dealing with {@link com.ning.http.client.AsyncHttpClient} from a Jenkins plugin.
 *
 * @author Stephen Connolly
 */
public final class AHCUtils {

    /**
     * Do not instantiate.
     */
    private AHCUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Get the proxy server.
     *
     * @return the proxy server or {@code null} if no proxy server required.
     */
    public static ProxyServer getProxyServer() {
        ProxyServer proxyServer;
        if (Hudson.getInstance() != null && Hudson.getInstance().proxy != null) {
            final ProxyConfiguration proxy = Hudson.getInstance().proxy;
            proxyServer = new ProxyServer(proxy.name, proxy.port, proxy.getUserName(), proxy.getPassword());
        } else {
            proxyServer = null;
        }
        return proxyServer;
    }

}
