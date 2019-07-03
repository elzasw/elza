package org.dspace.storage.rdbms.migration;

import java.sql.Connection;

import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.storage.rdbms.xmlworkflow.V5_0_2014_11_04__Enable_XMLWorkflow_Migration;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource;

/**
 * inspirace org.dspace.storage.rdbms.xmlworkflow.V5_0_2014_11_04__Enable_XMLWorkflow_Migration
 */
public class V6_1_2019_07_02_DestructTransferRequest implements JdbcMigration, MigrationChecksumProvider {

    @Override
    public void migrate(Connection connection) throws Exception {
        if (!DatabaseUtils.tableExists(connection, "destruc_transfer_request")) {
            String dbtype = connection.getMetaData().getDatabaseProductName();
            String dbFileLocation = null;
            if (dbtype.toLowerCase().contains("postgres")) {
                dbFileLocation = "postgres";
            } else if (dbtype.toLowerCase().contains("oracle")) {
                dbFileLocation = "oracle";
            }


        String packagePath = V6_1_2019_07_02_DestructTransferRequest.class.getPackage().getName().replace(".", "/");

        // Get the contents of our DB Schema migration script, based on path & DB type
        // (e.g. /src/main/resources/[path-to-this-class]/postgres/xml_workflow_migration.sql)
        String dbMigrateSQL = new ClassPathResource(packagePath + "/" +
                dbFileLocation +
                "/V6.1_2019_07_02_DestructTransferRequest.sql", getClass().getClassLoader()).loadAsString(Constants.DEFAULT_ENCODING);

        // Actually execute the Database schema migration SQL
        // This will create the necessary tables for the XMLWorkflow feature
        DatabaseUtils.executeSql(connection, dbMigrateSQL);

        }

    }

    @Override
    public Integer getChecksum() {
        return -1;
    }






}
