package org.opennms.smoketest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.opennms.smoketest.containers.OpenNMSContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests that sites deployed in the osgi container can be reached from jetty (see NMS-7785).
 *
 * In addition it verifies that access to /osgi/** is not allowed (see NMS-8431).
 *
 * @author mvrueden
 */
public class OsgiDeployedApplicationsIT extends OpenNMSSeleniumIT {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiDeployedApplicationsIT.class);

    // See NMS-7785
    @Test
    public void verifyOsgiUrlIsAccessibleViaBridge() throws IOException {
        final String[] paths = new String[]{
                "jmx-config-tool",
                "vaadin-surveillance-views?dashboard=true",
                "vaadin-surveillance-views?dashboard=false",
                "wallboard-config",
                "node-maps",
                "topology"
        };

        verifyUrls(paths, 200, "OK");
    }

    private void verifyUrls(final String[] paths, final int expectedStatus, final String expectedStatusText) throws IOException {
        for (final String eachPath : paths) {
            final String urlString = getBaseUrlExternal() + "opennms/" + eachPath;
            LOG.info("Verifying url '{}' ...", urlString);

            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty ("Authorization", "Basic " + Base64.getEncoder().encodeToString((OpenNMSContainer.ADMIN_USER + ":" + OpenNMSContainer.ADMIN_PASSWORD).getBytes()));
            connection.setConnectTimeout(250);
            connection.setUseCaches(false);
            connection.setDoOutput(false);
            connection.setInstanceFollowRedirects(false); // we do not want to follow redirects, otherwise 200 OK might be returned
            connection.connect();
            LOG.info("Status: {} {}", connection.getResponseCode(), connection.getResponseMessage());

            // All unauthorized requests are forwarded to login.jsp which will result in 200 OK, we
            // therefore fail, because we should have been logged in correctly.
            if (302 == connection.getResponseCode()) {
                LOG.info("Try redirecting to: {}", connection.getHeaderField("Location"));
                Assert.fail("Request was forwarded. This is not allowed.");
            }

            // Valid request, now check for OK
            final String errorMessage = String.format("URL: %s: status %s (%s) expected but %s (%s) received.",
                urlString, expectedStatus, expectedStatusText, connection.getResponseCode(),  connection.getResponseMessage());
            Assert.assertEquals(errorMessage, expectedStatus, connection.getResponseCode());
            LOG.info("Test passed");
        }
    }

}
