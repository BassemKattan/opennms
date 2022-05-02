/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.reporting.availability.render;

import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;

public class PDFReportRendererTest {
    private String testOutputFile = "target/sampleDocument.pdf";
    private PDFReportRenderer renderer = null;

    @Before
    public void setUp() throws Exception {
        final var tempDir = Files.createTempDirectory("pdf-report-");

        renderer = new PDFReportRenderer();
        renderer.setBaseDir(tempDir.toAbsolutePath().toString());

        final Properties p = new Properties();
        p.setProperty("log4j.logger.FOP", "INFO");
        p.setProperty("log4j.logger.org.apache", "INFO");
        p.setProperty("log4j.logger.org.apache.fop.fonts", "WARN");

        MockLogAppender.setupLogging(true, p);
    }

    @Test
    public void testPdfRendering() throws Exception {
        renderer.render(
                new InputStreamReader(
                        // This is a freely-licensed sample XSL-FO document from IBM developerWorks
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("org/opennms/reporting/availability/render/currency.fo"), 
                        StandardCharsets.UTF_8
                ),
                new FileOutputStream(testOutputFile),
                new InputStreamReader(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("org/opennms/reporting/availability/render/identity.xsl"), 
                        StandardCharsets.UTF_8
                )
        );

        assertTrue(Files.exists(Path.of(testOutputFile)));
    }
}
