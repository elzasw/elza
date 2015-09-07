package cz.tacr.elza.dbchangelog;

import java.sql.ResultSet;
import java.sql.Statement;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;


/**
 * Vytvoření tabulky arr_node
 * Naplnění dat tabulky arr_node podle již vytvořených uzlů + přidání platné sequnce
 * Přidání cizích klíčů
 *
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
public class DbChangelog_1_20 implements liquibase.change.custom.CustomTaskChange {

    private Statement statement;

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {
            statement = connection.createStatement();

            // vytvoření tabulky
            statement.executeUpdate("CREATE TABLE arr_node (node_id int NOT NULL, version integer DEFAULT 0, last_update timestamp, CONSTRAINT pk_nodeId PRIMARY KEY (node_id))");

            // naplnění tabulky
            statement.executeUpdate("INSERT INTO arr_node(node_id) SELECT DISTINCT node_id FROM arr_fa_level");

            // přidání cizích klíčů
            statement.executeUpdate("ALTER TABLE arr_fa_level ADD CONSTRAINT fk_faLevel_node FOREIGN KEY (node_id) REFERENCES arr_node(node_id)");
            statement.executeUpdate("ALTER TABLE arr_fa_level ADD CONSTRAINT fk_faLevel_node_parent FOREIGN KEY (parent_node_id) REFERENCES arr_node(node_id)");
            statement.executeUpdate("ALTER TABLE arr_desc_item ADD CONSTRAINT fk_descItem_node FOREIGN KEY (node_id) REFERENCES arr_node(node_id)");

            // přidání hibernate sequence
            ResultSet rs = statement.executeQuery("SELECT MAX(node_id) AS last_node_id FROM arr_fa_level");
            int lastNodeId = 0;
            while (rs.next()) {
                lastNodeId = rs.getInt("last_node_id");
            }
            statement.executeUpdate("INSERT INTO db_hibernate_sequences(sequence_name, next_val) VALUES ('arr_node|node_id'," + (lastNodeId+1) + ")");

            statement.executeUpdate("ALTER TABLE arr_fa_version ADD root_fa_level_id integer");

            statement.executeUpdate("UPDATE arr_fa_version v SET root_fa_level_id = (SELECT fa_level_id FROM arr_fa_level l WHERE l.node_id = v.root_node_id AND l.delete_fa_change_id IS NULL)");

            statement.executeUpdate("ALTER TABLE arr_fa_version ALTER COLUMN root_fa_level_id SET not null");

            statement.executeUpdate("ALTER TABLE arr_fa_version DROP COLUMN root_node_id");

            statement.executeUpdate("ALTER TABLE arr_fa_version ADD CONSTRAINT fk_faVersion_level FOREIGN KEY (root_fa_level_id) REFERENCES arr_fa_level(fa_level_id)");

        } catch (Exception e) {
            throw new CustomChangeException(e);

        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Vytvoření tabulky arr_node a provedení závislých změn.";
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
