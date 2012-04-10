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
