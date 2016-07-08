package cz.tacr.elza.doc;

import java.sql.Statement;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;


/**
 * Ukázkový příklad DbChangelog. Pro třídy s db updatem je připraven balík cz.tacr.elza.dbchangelog v modulu elsa-core.
 *
 * Volání v XML <customChange class="cz.tacr.elza.dbchangelog.DbChangelog_1_0" />
 *
 * @author Lukáš Bendík
 * @since 14. 9. 2015
 */
public class DbChangelog_1_0 implements liquibase.change.custom.CustomTaskChange {

    private Statement statement;

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {
            statement = connection.createStatement();

            // volané příkazy
            // statement.executeUpdate("INSERT INTO rul_rule_set (rule_set_id,code,name) VALUES (1,'ZP','Základní pravidla')");
            // statement.executeUpdate("INSERT INTO db_hibernate_sequences (sequence_name, next_val) VALUES ('rul_rule_set|rule_set_id', 2)");

        } catch (Exception e) {
            throw new CustomChangeException(e);

        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Text zprávy...";
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
