package jenkins.plugins.asynchttpclient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.netty.handler.ssl.SslContextBuilder;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.logging.Logger;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Provides a global shared {@link AsyncHttpClient} instance, for use from the master,
 * configured with the master's proxy settings.
 * This shared instance will be gracefully closed when Jenkins is terminating, and will be recycled if the proxy
 * settings change or if somebody closes it by accident.
 * The recommendation is not to cache the instance longer than a user's request.
 *
 * @since 1.7.8
 */
@Extension
public class AHC extends Descriptor<AHC> implements Describable<AHC> {

    /**
     * Override to enable insecure handling of TLS connections.
     * @see <a href="https://www.cvedetails.com/cve/CVE-2013-7397/">CVE-2013-7397</a> and
     * <a href="https://www.cvedetails.com/cve/CVE-2013-7398/">CVE-2013-7398</a>
     * @since 1.7.24.1
     */
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Allow runtime modification")
    @Restricted(NoExternalUse.class) // no direct linking against this field please
    public static boolean acceptAnyCertificate = Boolean.getBoolean(AHC.class.getName() + ".acceptAnyCertificate");

    /**
     * Our logger.
     */
    private Logger logger = Logger.getLogger(AHC.class.getName());

    /**
     * Our instance.
     */
    private AsyncHttpClient instance;

    /**
     * A memo instance of the proxy settings.
     */
    private ProxyConfiguration memo;

    /**
     * Our constructor.
     */
    @SuppressWarnings("unused") // used by Jenkins
    public AHC() {
        super(AHC.class);
    }

    /**
     * Returns the shared {@link AsyncHttpClient} instance.
     *
     * @return the shared {@link AsyncHttpClient} instance.
     * @throws IllegalStateException if executed on a slave JVM.
     */
    public static AsyncHttpClient instance() {
        Jenkins master = Jenkins.getInstance();
        if (master == null) {
            throw new IllegalStateException("The shared AsyncHttpClient instance is only available on the master");
        }
        return AHC.class.cast(master.getDescriptorOrDie(AHC.class)).getInstance();
    }

    /**
     * Get the instance, refreshing if needed and creating on demand.
     *
     * @return the instance.
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "https://github.com/jenkinsci/jenkins/pull/2094")
    private synchronized AsyncHttpClient getInstance() {
        if (instance != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (!equals(proxy, memo)) {
                logger.fine("Proxy configuration changed, recycling shared AsyncHttpClient instance");
                if (instance != null) {
                    closeInstance();
                }
                instance = null;
                memo = proxy;
            }
        }
        if (instance == null || instance.isClosed()) {
            logger.fine("Starting shared AsyncHttpClient instance");

            // TODO:  As far as I can tell, there's no way to disable hostname checking without using an alpha version.
            // Before a hostname verifier was set here, but now it's baked into the client.
            // See https://github.com/AsyncHttpClient/async-http-client/commit/d59fd205a4eca4c9514ce74440ababd93e74b0bd#diff-d1cc10c802a858bddfc6442d3333f576
            instance = new DefaultAsyncHttpClient(
                    new DefaultAsyncHttpClientConfig.Builder()
                            .setProxyServer(AHCUtils.getProxyServer())
                            .setAcceptAnyCertificate(acceptAnyCertificate)
                            .setSslContext(AHCUtils.getSSLContext())
                            .build());
        }
        return instance;
    }

    /**
     * Compare two {@link ProxyConfiguration} instances.
     *
     * @param p1 the first.
     * @param p2 the second.
     * @return {@code true} if and only if the two instances are effectively the same.
     */
    private boolean equals(ProxyConfiguration p1, ProxyConfiguration p2) {
        if (p1 == p2) {
            return true;
        }
        if (p1 == null || p2 == null) {
            return false;
        }
        if (p1.port != p2.port) {
            return false;
        }
        if (!StringUtils.equals(p1.name, p2.name)) {
            return false;
        }
        if (!StringUtils.equals(p1.getUserName(), p2.getUserName())) {
            return false;
        }
        if (!StringUtils.equals(p1.getEncryptedPassword(), p2.getEncryptedPassword())) {
            return false;
        }
        return true;
    }

    /**
     * Shut down the instance if it exists.
     */
    synchronized void shutdown() {
        if (instance != null) {
            if (!instance.isClosed()) {
                logger.fine("Shutting down shared AsyncHttpClient instance");
                closeInstance();
            }
            instance = null;
            memo = null;
        }
    }

    private void closeInstance() {
        try {
            instance.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not close async http client", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "https://github.com/jenkinsci/jenkins/pull/2094")
    public AHC getDescriptor() {
        return (AHC) Jenkins.getInstance().getDescriptor(AHC.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "";
    }

}
