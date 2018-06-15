/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.core.test.db;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.opennms.core.db.install.SimpleDataSource;
import org.opennms.core.schema.Migration;
import org.opennms.core.schema.Migrator;
import org.opennms.core.test.MockLogAppender;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class TemporaryDatabasePostgreSQLIT {
    //private static final Logger LOG = LoggerFactory.getLogger(DatabasePopulatorIT.class);

    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
    }

    @Test
    public void testRealChangelog() throws Throwable {
        String dbName = TemporaryDatabasePostgreSQL.TEMPLATE_DATABASE_NAME_PREFIX + System.currentTimeMillis();

        GenericApplicationContext context = TemporaryDatabasePostgreSQL.ensureLiquibaseFilesInClassPath(new StaticApplicationContext());

        final Migration migration = TemporaryDatabasePostgreSQL.createMigration(dbName);

        DataSource dataSource = new SimpleDataSource("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/" + migration.getDatabaseName(), migration.getDatabaseUser(), migration.getDatabasePassword());
        DataSource adminDataSource = new SimpleDataSource("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/template1", migration.getDatabaseUser(), migration.getDatabasePassword());

        final Migrator migrator = TemporaryDatabasePostgreSQL.createMigrator(dataSource, adminDataSource);

        TemporaryDatabasePostgreSQL.createIntegrationTestDatabase(context, migration, migrator, dataSource);

        migrator.databaseRemoveDB(migration);
    }

    @Test
    public void testGetIntegrationTestDatabaseName() throws Throwable {
        TemporaryDatabasePostgreSQL.getIntegrationTestDatabaseName();
    }

    @Test
    public void testHashesMatch() throws IOException, Exception {
        GenericApplicationContext context = TemporaryDatabasePostgreSQL.ensureLiquibaseFilesInClassPath(new StaticApplicationContext());
        assertEquals("liquibase configuration hash", TemporaryDatabasePostgreSQL.generateLiquibaseHash(context), TemporaryDatabasePostgreSQL.generateLiquibaseHash(context));
    }
}
