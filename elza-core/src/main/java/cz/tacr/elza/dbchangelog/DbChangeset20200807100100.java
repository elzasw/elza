package cz.tacr.elza.dbchangelog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public class DbChangeset20200807100100 extends BaseTaskChange {

	private final String ARCHIVY_CSV = "db/csv/archivy.csv";
	
	private final String UPDATE_POSTGRE_SQL = "UPDATE ap_access_point SET uuid = ?" 
			+ "FROM par_institution " 
			+ "WHERE par_institution.access_point_id = ap_access_point.access_point_id " 
			+ "AND par_institution.internal_code = ?";

    private JdbcConnection dc;

    private int numChanged = 0;

    @Override
    public String getConfirmationMessage() {
        return "Changed UUIDs for table ap_access_point, count: " + numChanged;
    }

    @Override
	public void execute(Database database) throws CustomChangeException {
        dc = (JdbcConnection) database.getConnection();
        Map<String, String> archyMap = getMapFromCsv(ARCHIVY_CSV);

        try (PreparedStatement ps = dc.prepareStatement(UPDATE_POSTGRE_SQL)) {

        	for (Map.Entry<String, String> map : archyMap.entrySet()) {
                ps.setString(1, map.getValue());
                ps.setString(2, map.getKey());
                ps.execute();
                int updateCount = ps.getUpdateCount();
                Validate.isTrue(updateCount == 1, "Unexpected update count: %i", updateCount);
                numChanged++;
        	}

        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("Chyba při vykonávání sql příkazu " + e.getLocalizedMessage(), e);
		}

    }
    
    private Map<String, String> getMapFromCsv(String csvFile) throws CustomChangeException {
    	Map<String, String> result = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource(csvFile)
                .getInputStream(), "UTF-8"))) {
            String s = br.readLine();
            while ((s = br.readLine()) != null) {
            	String[] fields = s.split(",");
            	result.put(fields[1], fields[0]);
            }
        } catch (IOException e) {
            throw new CustomChangeException("Chyba při čtení souboru " + csvFile, e);
        }

        return result;
    }

}
