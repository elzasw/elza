package cz.tacr.elza.dbchangelog;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.12.16
 */
public class DbUpgrade_201612220713 implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Doplnění pole uuid do AS";
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
    public void execute(Database database) throws CustomChangeException {
        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {
            PreparedStatement ps = databaseConnection.prepareStatement("ALTER TABLE arr_fund ADD COLUMN uuid char(36) UNIQUE");
            ps.execute();

            ps = databaseConnection.prepareStatement("update arr_fund "
                    + " set uuid = (select distinct n.uuid from arr_node n join arr_fund_version v on v.root_node_id = n.node_id where v.fund_id = arr_fund.fund_id) "
                    + " where uuid is null");
            ps.execute();

            ps = databaseConnection.prepareStatement("ALTER TABLE arr_fund ALTER COLUMN uuid SET NOT NULL");
            ps.execute();
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}
