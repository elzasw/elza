package org.dspace.storage.rdbms.migration;

import java.sql.Connection;

import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

/**
 * inspirace org.dspace.storage.rdbms.xmlworkflow.V5_0_2014_11_04__Enable_XMLWorkflow_Migration
 */
public class V6_1_2019_06_18__DBUpgradeTest implements JdbcMigration, MigrationChecksumProvider {

    @Override
    public void migrate(Connection connection) throws Exception {
        if (!DatabaseUtils.tableExists(connection, "cwf_workflowitem")) {
            String dbtype = connection.getMetaData().getDatabaseProductName();
            String dbFileLocation = null;
            if (dbtype.toLowerCase().contains("postgres")) {
                dbFileLocation = "postgres";
            } else if (dbtype.toLowerCase().contains("oracle")) {
                dbFileLocation = "oracle";
            }
        }
    }

    @Override
    public Integer getChecksum() {
        return -1;
    }
}
