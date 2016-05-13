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

import com.ning.http.client.ProxyServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

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
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "https://github.com/jenkinsci/jenkins/pull/2094")
    public static ProxyServer getProxyServer() {
        ProxyServer proxyServer;
        if (Jenkins.getInstance() != null && Jenkins.getInstance().proxy != null) {
            final ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            proxyServer = new ProxyServer(proxy.name, proxy.port, proxy.getUserName(), proxy.getPassword());

            if (proxy.noProxyHost != null) {
                for (String s : proxy.noProxyHost.split("[ \t\n,|]+")) {
                    if (s.length() > 0) {
                        proxyServer.addNonProxyHost(s);
                    }
                }
            }
        } else {
            proxyServer = null;
        }
        return proxyServer;
    }

}
