package cz.tacr.elza.dbchangelog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Vygenerování UUID pro uzly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 10. 11. 2015
 */
public class DbUpgrade1_12 implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Update dat 1_12 dokončen.";
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        Statement statement = null;
        ResultSet nodeIds = null;
        try {
            statement = connection.createStatement();
            nodeIds = statement.executeQuery("select node_id from arr_node");

            if (nodeIds.isBeforeFirst()) {
                List<Integer> ids = new LinkedList<Integer>();
                while (nodeIds.next()) {
                    ids.add(nodeIds.getInt(1));
                }
                for (Integer nodeId : ids) {
                    statement.executeUpdate("update arr_node set uuid = '" + UUID.randomUUID().toString() + "' where node_id = " + nodeId);
                }
            }
        } catch (Exception e) {
            throw new CustomChangeException(e);
        } finally {
            if (nodeIds != null) {
                try {
                    nodeIds.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
