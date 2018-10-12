package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

/**
 * 
 */
public class DbUpgrade_201612220713 extends BaseTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Doplnění pole uuid do AS";
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {
            PreparedStatement ps;

            ps = databaseConnection.prepareStatement("update arr_fund "
                    + " set uuid = (select distinct n.uuid from arr_node n join arr_fund_version v on v.root_node_id = n.node_id where v.fund_id = arr_fund.fund_id) "
                    + " where uuid is null");
            ps.execute();

        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}
