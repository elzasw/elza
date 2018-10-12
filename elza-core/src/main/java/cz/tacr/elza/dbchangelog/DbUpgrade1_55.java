package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

/**
 * Migrace dat pro změnu datace (odstranění polointervalů).
 *
 */
public class DbUpgrade1_55 extends BaseTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Migrace dat pro změnu datace (odstranění polointervalů).";
    }

    @Override
    public void execute(final Database database) throws CustomChangeException {
        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {
            // osoby
            PreparedStatement ps = databaseConnection.prepareStatement("UPDATE par_unitdate SET value_from = value_to, value_from_estimated = value_to_estimated WHERE value_from IS NULL OR value_from = ''");
            ps.executeUpdate();
            ps = databaseConnection.prepareStatement("UPDATE par_unitdate SET value_to = value_from, value_to_estimated = value_from_estimated WHERE value_to IS NULL OR value_to = ''");
            ps.executeUpdate();

            // pořádání
            ps = databaseConnection.prepareStatement("UPDATE arr_data_unitdate SET format = REPLACE(format, '-', ''), " +
                    " value_from = value_to, value_from_estimated = value_to_estimated WHERE value_from IS NULL OR value_from = ''");
            ps.executeUpdate();
            ps = databaseConnection.prepareStatement("UPDATE arr_data_unitdate SET format = REPLACE(format, '-', ''), " +
                    " value_to = value_from, value_to_estimated = value_from_estimated WHERE value_to IS NULL OR value_to = ''");
            ps.executeUpdate();
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}
