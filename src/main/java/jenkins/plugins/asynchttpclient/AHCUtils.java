/*
 * Copyright 2012 CloudBees, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jenkins.plugins.asynchttpclient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ProxyConfiguration;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jenkins.model.Jenkins;
import org.asynchttpclient.Realm;
import org.asynchttpclient.proxy.ProxyServer;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Utility methods for dealing with {@link com.ning.http.client.AsyncHttpClient} from a Jenkins plugin.
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
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
                        justification = "https://github.com/jenkinsci/jenkins/pull/2094")
    public static ProxyServer getProxyServer() {
        ProxyServer proxyServer;
        if (Jenkins.getInstance() != null && Jenkins.getInstance().proxy != null) {
            final ProxyConfiguration proxy = Jenkins.getInstance().proxy;

            List<String> nonProxyHosts = new ArrayList<>();
            if (proxy.noProxyHost != null) {
                for (String s : proxy.noProxyHost.split("[ \t\n,|]+")) {
                    if (s.length() > 0) {
                        nonProxyHosts.add(s);
                    }
                }
            }
            proxyServer = new ProxyServer(proxy.name, proxy.port, proxy.port, new Realm.Builder(proxy.getUserName(), proxy.getPassword()).build(), nonProxyHosts);
        } else {
            proxyServer = null;
        }
        return proxyServer;
    }

    /**
     * Return the default {@link SSLContext} to use with {@link AsyncHttpClient}.
     *
     * @return the default {@link SSLContext} to use with {@link AsyncHttpClient}.
     * @since 1.7.24.1
     */
    public static SslContext getSSLContext() {
        try {
            return AHC.acceptAnyCertificate ? ResourceHolder.looseTrustManagerSSLContext : SslContextBuilder.forClient().build();
        } catch (SSLException e) {
            throw new IllegalStateException("Could not build SslContext for the client", e);
        }
    }

    /**
     * A blind-trusting {@link X509TrustManager}.
     *
     * Copied from AHC 1.9.x
     *
     * @since 1.7.24.1
     */
    @Restricted(NoExternalUse.class)
    static class LooseTrustManager implements X509TrustManager {

        /**
         * {@inheritDoc}
         */
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        /**
         * {@inheritDoc}
         */
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        /**
         * {@inheritDoc}
         */
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    }

    /**
     * Resource holder for the {@link LooseTrustManager} singleton.
     *
     * @since 1.7.24.1
     */
    private static final class ResourceHolder {
        /**
         * The singleton.
         */
        private static SslContext looseTrustManagerSSLContext = looseTrustManagerSSLContext();

        /**
         * Instantiates the singelton.
         *
         * @return the singleton.
         */
        private static SslContext looseTrustManagerSSLContext() {
            try {
                return SslContextBuilder.forClient()
                                .protocols("TLS")
                                .trustManager(new X509Certificate[0])
                                .build();
            } catch (SSLException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }


}
