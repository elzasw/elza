package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Odstranění unique constraintu z arr_packet.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 2. 2017
 */
public class DbUpgrade_20170208161823 implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() throws SetupException {}

    @Override
    public void setFileOpener(final ResourceAccessor resourceAccessor) {}

    @Override
    public ValidationErrors validate(final Database database) {
        return null;
    }

    @Override
    public void execute(final Database database) throws CustomChangeException {
        String name = database.getDatabaseProductName();

        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {
            PreparedStatement ps;

            switch (name) {
                case "H2":
                    ps = databaseConnection.prepareStatement("select distinct constraint_name from information_schema.constraints "
                            + "where table_name='ARR_PACKET' AND column_list = 'FUND_ID,STORAGE_NUMBER'");
                    break;
                case "PostgreSQL":
                    ps = databaseConnection.prepareStatement("SELECT conname FROM pg_constraint "
                            + "WHERE conrelid = "
                            + "(SELECT oid "
                            + "FROM pg_class "
                            + "WHERE relname LIKE 'arr_packet') AND contype = 'u'");
                    break;
                case "Microsoft SQL Server":
                    ps = databaseConnection.prepareStatement("SELECT CONSTRAINT_NAME  "
                            + "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC "
                            + "WHERE TC.TABLE_NAME = 'arr_packet' "
                            + "AND TC.CONSTRAINT_TYPE = 'UNIQUE'");
                    break;
                default:
                    throw new IllegalStateException("Neznámý typ db " + name);
            }
            ps.execute();

            if (ps.getResultSet().next()) {
                String constraint = ps.getResultSet().getString(1);

                ps = databaseConnection.prepareStatement("ALTER TABLE ARR_PACKET DROP CONSTRAINT " + constraint);
                ps.execute();
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}
