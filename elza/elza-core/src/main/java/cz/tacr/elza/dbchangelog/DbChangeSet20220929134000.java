package cz.tacr.elza.dbchangelog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.StringNormalize;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class DbChangeSet20220929134000 extends BaseTaskChange {

    private JdbcConnection dc;

    private List<ArrDataString> prepareArrDataString() throws DatabaseException, SQLException {
        List<ArrDataString> result = new ArrayList<>();
        Statement stmt = dc.createStatement();
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM arr_data_string")) {
            while (rs.next()) {
                int dataId = rs.getInt("data_id");
                String value = rs.getString("string_value");
                // delete any leading and trailing whitespace, replace unprintable chars (exclude 0x0D and 0x0A) and delete double spaces
                String fixedValue = StringNormalize.normalizeString(value);
                if (!value.equals(fixedValue)) {
                    ArrDataString dataString = new ArrDataString();
                    dataString.setDataId(dataId);
                    dataString.setStringValue(fixedValue);
                    result.add(dataString);
                }
            }
        }
        return result;
    }

    private List<ArrDataText> prepareArrDataText() throws DatabaseException, SQLException {
        List<ArrDataText> result = new ArrayList<>();
        Statement stmt = dc.createStatement();
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM arr_data_text")) {
            while (rs.next()) {
                int dataId = rs.getInt("data_id");
                String value = rs.getString("text_value");
                // delete any leading and trailing whitespace and replace unprintable chars (exclude 0x0D and 0x0A)
                String fixedValue = StringNormalize.normalizeText(value);
                if (!value.equals(fixedValue)) {
                    ArrDataText dataText = new ArrDataText();
                    dataText.setDataId(dataId);
                    dataText.setTextValue(fixedValue);
                    result.add(dataText);
                }
            }
        }
        return result;
    }

    private List<ArrDataUnitid> prepareArrDataUnitid() throws DatabaseException, SQLException {
        List<ArrDataUnitid> result = new ArrayList<>();
        Statement stmt = dc.createStatement();
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM arr_data_unitid")) {
            while (rs.next()) {
                int dataId = rs.getInt("data_id");
                String value = rs.getString("unit_value");
                // delete any leading and trailing whitespace
                String fixedValue = StringNormalize.normalizeString(value);
                if (!value.equals(fixedValue)) {
                    ArrDataUnitid dataUnitid = new ArrDataUnitid();
                    dataUnitid.setDataId(dataId);
                    dataUnitid.setUnitId(fixedValue);
                    result.add(dataUnitid);
                }
            }
        }
        return result;
    }

    private List<ArrDataUriRef> prepareArrDataUriRef() throws DatabaseException, SQLException {
        List<ArrDataUriRef> result = new ArrayList<>();
        Statement stmt = dc.createStatement();
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM arr_data_uri_ref")) {
            while (rs.next()) {
                int dataId = rs.getInt("data_id");
                String value = rs.getString("uri_ref_value");
                String schema = rs.getString("schema");
                String description = rs.getString("description");
                // delete any leading and trailing whitespace
                String fixedValue = StringNormalize.normalizeString(value);
                String fixedSchema = StringNormalize.normalizeString(schema);
                String fixedDescription = StringNormalize.normalizeString(description);
                if (!value.equals(fixedValue) || !schema.equals(fixedSchema) || (description != null && !description.equals(fixedDescription))) {
                    ArrDataUriRef dataUriRef = new ArrDataUriRef();
                    dataUriRef.setDataId(dataId);
                    dataUriRef.setUriRefValue(fixedValue);
                    dataUriRef.setSchema(fixedSchema);
                    dataUriRef.setDescription(fixedDescription);
                    result.add(dataUriRef);
                }
            }
        }
        return result;
    }

    @Override
    public void execute(final Database db) throws CustomChangeException {
        dc = (JdbcConnection) db.getConnection();

        // fixed ArrDataString
        try (PreparedStatement prepareSet = dc.prepareStatement("UPDATE arr_data_string SET string_value = ? WHERE data_id = ?")) {
            List<ArrDataString> dataStrings = prepareArrDataString();
            if (!dataStrings.isEmpty()) {
                for (ArrDataString dataString : dataStrings) {
                    prepareSet.setString(1, dataString.getStringValue());
                    prepareSet.setInt(2, dataString.getDataId());
                    prepareSet.addBatch();
                }
                prepareSet.executeBatch();
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }

        // fixed ArrDataText
        try (PreparedStatement prepareSet = dc.prepareStatement("UPDATE arr_data_text SET text_value = ? WHERE data_id = ?")) {
            List<ArrDataText> dataTexts = prepareArrDataText();
            if (!dataTexts.isEmpty()) {
                for (ArrDataText dataText : dataTexts) {
                    prepareSet.setString(1, dataText.getTextValue());
                    prepareSet.setInt(2, dataText.getDataId());
                    prepareSet.addBatch();
                }
                prepareSet.executeBatch();
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }

        // fixed ArrDataText
        try (PreparedStatement prepareSet = dc.prepareStatement("UPDATE arr_data_unitid SET unit_value = ? WHERE data_id = ?")) {
            List<ArrDataUnitid> dataUnitids = prepareArrDataUnitid();
            if (!dataUnitids.isEmpty()) {
                for (ArrDataUnitid dataUnitid : dataUnitids) {
                    prepareSet.setString(1, dataUnitid.getUnitId());
                    prepareSet.setInt(2, dataUnitid.getDataId());
                    prepareSet.addBatch();
                }
                prepareSet.executeBatch();
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }

        // fixed ArrDataUriRef
        try (PreparedStatement prepareSet = dc.prepareStatement("UPDATE arr_data_uri_ref SET schema = ?, uri_ref_value = ?, description = ? WHERE data_id = ?")) {
            List<ArrDataUriRef> dataUriRefs = prepareArrDataUriRef();
            if (!dataUriRefs.isEmpty()) {
                for (ArrDataUriRef dataUriRef : dataUriRefs) {
                    prepareSet.setString(1, dataUriRef.getSchema());
                    prepareSet.setString(2, dataUriRef.getUriRefValue());
                    prepareSet.setString(3, dataUriRef.getDescription());
                    prepareSet.setInt(4, dataUriRef.getDataId());
                    prepareSet.addBatch();
                }
                prepareSet.executeBatch();
            }
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
        }
    }
}
