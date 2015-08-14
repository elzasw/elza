package cz.tacr.elza.dbchangelog;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Queue;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
/**
 * Vygenerování výchozích dat.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 8. 2015
 */
public class DbChangelog_1_4 implements liquibase.change.custom.CustomTaskChange {


    private static final int DEPTH = 4;
    private static final int NODES_IN_LEVEL = 15;

    private Statement statement;

    private int nodeCount = 1;

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {

            statement = connection.createStatement();
            //FA
            statement.executeUpdate("INSERT INTO arr_finding_aid(finding_aid_id, create_date, name) VALUES (1, '2015-08-13 12:00:00', 'Archiv 1')");

            //Change
            statement.executeUpdate("INSERT INTO arr_fa_change(change_id, change_date) VALUES (1, '2015-08-13 12:00:00')");

            //Root
            statement.executeUpdate("INSERT INTO arr_fa_level(fa_level_id, create_change_id, delete_change_id, node_id, parent_node_id, position) "
                    + "VALUES (1, 1, null, 1, null, 1)");

            //Version
            statement.executeUpdate("INSERT INTO arr_fa_version(fa_version_id, arrangement_type_id, create_change_id, finding_aid_id, lock_change_id, root_node_id, rule_set_id) "
                    +" VALUES (1, 1, 1, 1, null, 1, 1)");

            createTree(DEPTH, NODES_IN_LEVEL);

            statement.executeUpdate("INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('arr_finding_aid|finding_aid_id', 2)");
            statement.executeUpdate("INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('arr_fa_change|change_id', 2)");
            statement.executeUpdate("INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('arr_fa_version|fa_version_id', 2)");
            statement.executeUpdate("INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('arr_fa_level|fa_level_id', " + ++nodeCount + ")");
        } catch (Exception e) {
            throw new CustomChangeException(e);

        }
    }

    private void createTree(int depth, int nodesInLevel) throws SQLException {
        Queue<Integer> parents = new LinkedList<Integer>();
        parents.add(1);

        while (depth > 0) {
            depth--;
            Queue<Integer> newParents = new LinkedList<Integer>();
            while (!parents.isEmpty()) {
                Integer parentNodeId = parents.poll();
                for (int position = 1; position <= nodesInLevel; position++) {
                    createLevel(parentNodeId, position);
                    newParents.add(nodeCount);
                };
            }
            parents = newParents;
        }

        statement.executeBatch();
    }

    private void createLevel(int parentNodeId, int position) throws SQLException {
        statement.addBatch("INSERT INTO arr_fa_level(fa_level_id, create_change_id, delete_change_id, node_id, parent_node_id, position) "
                + "VALUES (" + ++nodeCount + ", 1, null, " + nodeCount + ", " + parentNodeId + ", " + position + ")");
    }

    @Override
    public String getConfirmationMessage() {
        return "Vygenerování výchozího stromu.";
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
