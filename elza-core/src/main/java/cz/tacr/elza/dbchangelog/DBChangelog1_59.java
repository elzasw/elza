package cz.tacr.elza.dbchangelog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import cz.tacr.elza.domain.ApRecord;
import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.UpdateStatement;

/**
 * Vygenerování UUID pro povinný sloupec v reg_record.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 24. 10. 2016
 */
public class DBChangelog1_59 implements CustomSqlChange {

    @Override
    public SqlStatement[] generateStatements(final Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {
            Statement createStatement = connection.createStatement();
            ResultSet resultSet = createStatement.executeQuery("select record_id from reg_record");

            List<SqlStatement> statements = new LinkedList<>();
            while (resultSet.next()) {
                int recordId = resultSet.getInt(1);
                UpdateStatement updateStatement = new UpdateStatement(null, null, "reg_record");
                updateStatement.addNewColumnValue(ApRecord.UUID, UUID.randomUUID().toString());
                updateStatement.setWhereClause("record_id = " + recordId);

                statements.add(updateStatement);
            }

            return statements.toArray(new SqlStatement[statements.size()]);
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba v db update 55.", e);
        }

    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(final ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(final Database database) {
        return null;
    }
}
