package cz.tacr.elza.utils;

import cz.tacr.elza.exception.SystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.sql.SQLException;

/**
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 16.3.2017
 */
@Component
public class DBUtils {

    @Autowired
    private EntityManager entityManager;

    private DatabaseType dbType;

    /**
     * Typ Databáze
     * Zatím existuje pouze 1 specialita pro MSSQL pokud bude potřeba další DB stačí přidat zde nový typ a do kontruktoru kontrolu
     */
    public enum DatabaseType {
        GENERIC,
        MSSQL
    }

    @PostConstruct
    public void init() {
        try {
            final String dbProductName = ((EntityManagerFactoryInfo) entityManager.getEntityManagerFactory()).getDataSource().getConnection().getMetaData().getDatabaseProductName();
            if (dbProductName != null) {
                if (dbProductName.toLowerCase().contains("microsoft")) {
                    dbType = DatabaseType.MSSQL;
                } else { // Zde přidávat kontroly která z DB typů je aktuální
                    dbType = DatabaseType.GENERIC;
                }
            }
        } catch (SQLException e) {
            throw new SystemException("Neočekávaná SQL Exception.", e);
        }
    }


    public DatabaseType getDbType() {
        return dbType;
    }
}
