package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Migrace dat v dbupradu changelog-1_3. Provede naplnění dat ve vazební tabulce
 * rul_desc_item_spec_register z původního sloupce register_type_id v tabulce rul_desc_item_spec.
 * 
 * @author vavrejn
 *
 */
public class DbUpgrade1_3 implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Migrace dat 1-3 dokoncena.";
    }

    @Override
    public void setFileOpener(ResourceAccessor arg0) {}

    @Override
    public void setUp() throws SetupException {}

    @Override
    public ValidationErrors validate(Database arg0) {
        return null;
    }

    @Override
    public void execute(final Database database) throws CustomChangeException {
        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {
            final Statement st = databaseConnection.createStatement();
            final ResultSet rsCount =
                    st.executeQuery("SELECT count(*) FROM rul_desc_item_spec_register");
            rsCount.next();
            int pocet = rsCount.getInt(1);
            if (pocet > 0) {
                throw new CustomChangeException(
                        "Již existují záznamy v tabulce rul_desc_item_spec_register");
            }

            final ResultSet rsInsert = st.executeQuery(
                    "SELECT register_type_id, desc_item_spec_id FROM rul_desc_item_spec"
                            + " where register_type_id is not null");
            int index = 1;
            while (rsInsert.next()) {
                int descItemSpecRegisterId = index++;
                int registerTypeId = rsInsert.getInt(1);
                int descItemSpecId = rsInsert.getInt(2);
                PreparedStatement ps = databaseConnection
                        .prepareStatement("INSERT INTO rul_desc_item_spec_register("
                                + "desc_item_spec_register_id, register_type_id, desc_item_spec_id) VALUES (?, ?, ?)");
                ps.setInt(1, descItemSpecRegisterId);
                ps.setInt(2, registerTypeId);
                ps.setInt(3, descItemSpecId);
                ps.executeUpdate();
            }
            PreparedStatement ps =
                    databaseConnection.prepareStatement("INSERT INTO db_hibernate_sequences("
                            + "sequence_name, next_val) VALUES (?, ?)");
            ps.setString(1, "rul_desc_item_spec_register|desc_item_spec_register_id");
            ps.setInt(2, index);
            ps.executeUpdate();
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}
