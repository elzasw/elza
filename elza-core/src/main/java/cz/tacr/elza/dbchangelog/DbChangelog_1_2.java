package cz.tacr.elza.dbchangelog;

import java.sql.Statement;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Databázová změna realizovaná v Jave.
 *
 * Naplnění číselníků.
 *
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
public class DbChangelog_1_2 implements liquibase.change.custom.CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {

            Statement statement = connection.createStatement();
            statement.executeUpdate("INSERT INTO rul_arrangement_type (arrangement_type_id,code,name) VALUES (1,'MAN','Manipulační seznam')");
            statement.executeUpdate("INSERT INTO rul_arrangement_type (arrangement_type_id,code,name) VALUES (2,'INV','Inventář')");
            statement.executeUpdate("INSERT INTO rul_arrangement_type (arrangement_type_id,code,name) VALUES (3,'KAT','Katalog')");
            statement.executeUpdate("INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('rul_arrangement_type|arrangement_type_id', 4)");



            statement.executeUpdate("INSERT INTO rul_rule_set (rule_set_id,code,name) VALUES (1,'ZP','Základní pravidla')");
            statement.executeUpdate("INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('rul_rule_set|rule_set_id', 2)");
        } catch (Exception e) {
            throw new CustomChangeException(e);

        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Číselníky naplněny výchozími hodnotami.";
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
