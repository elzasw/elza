package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Migrace dat pro změnu datace (odstranění polointervalů).
 *
 * @author Martin Šlapa
 * @since 24.10.2016
 */
public class DbUpgrade1_64 implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Migrace číselníků osob pod balíček CZ_BASE.";
    }

    @Override
    public void setFileOpener(final ResourceAccessor arg0) {}

    @Override
    public void setUp() throws SetupException {}

    @Override
    public ValidationErrors validate(final Database arg0) {
        return null;
    }

    @Override
    public void execute(final Database database) throws CustomChangeException {
        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {

            PreparedStatement ps = databaseConnection.prepareStatement("SELECT next_val FROM db_hibernate_sequences WHERE sequence_name = 'rul_package|package_id'");
            ps.execute();
            int id;
            if (ps.getResultSet().next()) {
                id = ps.getResultSet().getInt(1);
                ps = databaseConnection.prepareStatement("UPDATE db_hibernate_sequences SET next_val = next_val + 1 WHERE sequence_name = 'rul_package|package_id'");
                ps.executeUpdate();
            } else {
                id = 0;
            }
            id += 1000; // TODO slapa: je asi potřeba opravit generátor - prověřit

            ps = databaseConnection.prepareStatement("INSERT INTO rul_package (package_id, name, code, description, version) " +
                    "VALUES (?, ?, ?, ?, ?);");
            ps.setInt(1, id);
            ps.setString(2, "Nastavení pro ČR");
            ps.setString(3, "CZ_BASE");
            ps.setString(4, "Výchozí nastavení implementace v ČR");
            ps.setInt(5, 0);
            ps.executeUpdate();

            List<String> tables = Arrays.asList(
                    "par_relation_role_type",
                    "par_party_name_form_type",
                    "par_relation_class_type",
                    "par_complement_type",
                    "ui_party_group",
                    "par_party_type_complement_type",
                    "par_party_type_relation",
                    "par_relation_type",
                    "par_relation_type_role_type",
                    "par_registry_role",
                    "reg_register_type");
            for (String table : tables) {
                ps = databaseConnection.prepareStatement("UPDATE " + table + " SET package_id = ? WHERE package_id IS NULL");
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}

