package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.Validate;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class DbChangeset20200702125023 extends BaseTaskChange {

    private JdbcConnection dc;

    private int numGenerated = 0;

    @Override
    public String getConfirmationMessage() {
        return "Generated UUIDs for structured objects, count: " + numGenerated;
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        dc = (JdbcConnection) database.getConnection();
        
        try {
            // select all structured objects without UUID
            List<Integer> ids = findAllSobjs();
            if (ids.size() > 0) {
                try (PreparedStatement ps = dc.prepareStatement(
                                                                "UPDATE arr_structured_object SET uuid = ? WHERE structured_object_id = ?;")) {
                    // insert UUID
                    for (Integer id : ids) {
                        generateUUID(ps, id);
                    }

                    numGenerated = ids.size();
                }
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException(
                                            "Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }        

    }

    private void generateUUID(PreparedStatement ps, Integer id) throws SQLException {
        UUID uuid = UUID.randomUUID();
        ps.setString(1, uuid.toString());
        ps.setInt(2, id);
        ps.execute();
        int updateCount = ps.getUpdateCount();
        Validate.isTrue(updateCount == 1, "Unexpected update count: %i", updateCount);
    }

    private List<Integer> findAllSobjs() throws DatabaseException, SQLException {
        PreparedStatement ps = dc.prepareStatement("SELECT structured_object_id FROM arr_structured_object");
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt(1);
                ids.add(id);
            }
            
            return ids;
        }
    }

}
