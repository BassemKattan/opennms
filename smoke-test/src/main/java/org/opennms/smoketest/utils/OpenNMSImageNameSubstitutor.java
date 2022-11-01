/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
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

package org.opennms.smoketest.utils;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

public class OpenNMSImageNameSubstitutor extends ImageNameSubstitutor {
    @Override
    public DockerImageName apply(DockerImageName original) {
        return original;
        /*
        // These are published for ARM64, ARM/v7, and AMD64, so use them for all platforms
        // https://github.com/seleniumhq-community/docker-seleniarm/releases
        if (original.getUnversionedPart().startsWith("selenium/standalone-firefox")) { // also match -debug for older selenium
            if (!original.getVersionPart().equals("4.5.3")) {
                throw new RuntimeException("This version of selenium is not supported; only 4.5.3 is supported");
            }
            return DockerImageName.parse("seleniarm/standalone-firefox:4.5.3-20221025");
        } else if (original.getUnversionedPart().startsWith("selenium/standalone-chrome")) {
            if (!original.getVersionPart().equals("4.5.3")) {
                throw new RuntimeException("This version of selenium is not supported; only 4.5.3 is supported");
            }
            return DockerImageName.parse("seleniarm/standalone-chromium:4.5.3-20221025");
        } else {
            return original;
        }
        */
    }

    @Override
    protected String getDescription() {
        return "OpenNMS image name substitutor";
    }
}
