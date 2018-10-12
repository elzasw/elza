package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

/**
 * Migrace dat pro změnu datace (odstranění polointervalů).
 */
public class DbUpgrade1_64 extends BaseTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "Migrace číselníků osob pod balíček CZ_BASE.";
    }

    @Override
    public void execute(final Database database) throws CustomChangeException {
        final JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try {

            int id = 0;
            try (PreparedStatement ps = databaseConnection
                    .prepareStatement("SELECT next_val FROM db_hibernate_sequences WHERE sequence_name = 'rul_package|package_id'");) {
                ps.execute();
                try(ResultSet rs = ps.getResultSet();) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                        try (PreparedStatement ps2 = databaseConnection
                                .prepareStatement("UPDATE db_hibernate_sequences SET next_val = next_val + 1 WHERE sequence_name = 'rul_package|package_id'");) {
                            ps2.executeUpdate();
                        }
                    } else {
                        id = 0;
                    }
                    id += 1000; // TODO slapa: je asi potřeba opravit generátor - prověřit                    
                }
            }

            try(PreparedStatement ps = databaseConnection.prepareStatement("INSERT INTO rul_package (package_id, name, code, description, version) " +
                    "VALUES (?, ?, ?, ?, ?);");)
            {
                ps.setInt(1, id);
                ps.setString(2, "Nastavení pro ČR");
                ps.setString(3, "CZ_BASE");
                ps.setString(4, "Výchozí nastavení implementace v ČR");
                ps.setInt(5, 0);
                ps.executeUpdate();
            }

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
                try(PreparedStatement ps = databaseConnection.prepareStatement("UPDATE " + table + " SET package_id = ? WHERE package_id IS NULL");)
                {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                    "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }

}

