package cz.tacr.elza.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.SettingsType;

public class RecursiveQueryBuilderTest extends AbstractTest {

    private static final String RECURSIVE_QUERY_P1 = "SELECT DISTINCT col1 FROM ";
    private static final String RECURSIVE_QUERY_P2 = "(WITH RECURSIVE recTable(col1, col2) AS (";
    private static final String RECURSIVE_QUERY_P3 = "SELECT * FROM table1 WHERE id IN (:ids) AND url = :url AND active=:active OR entity= :entity ";
    private static final String RECURSIVE_QUERY_P4 = "UNION ALL";
    private static final String RECURSIVE_QUERY_P5 = "SELECT * FROM table2)";
    private static final String RECURSIVE_QUERY_P6 = "SELECT * FROM recTable)";

    @Autowired
    private EntityManager em;

    @Test
    @Transactional
    public void testMSSQLRecursiveQuery() {
        String p2 = "(WITH recTable(col1, col2) AS (";

        String expected = RECURSIVE_QUERY_P1 + p2 + RECURSIVE_QUERY_P3 + RECURSIVE_QUERY_P4 + RECURSIVE_QUERY_P5
                + RECURSIVE_QUERY_P6;
        String query = createQuery(DatabaseType.MSSQL, createEntity());

        assertEquals(expected, query);
    }

    @Test
    @Transactional
    public void testH2RecursiveQuery() {
        Object entity = createEntity();
        Object entityId = em.unwrap(Session.class).getIdentifier(entity);
        String p3 = "SELECT * FROM table1 WHERE id IN (1, 2, 3, 4) AND url = 'localhost' AND active=true OR entity= " + entityId + " ";

        String expected = RECURSIVE_QUERY_P1 + RECURSIVE_QUERY_P2 + p3 + RECURSIVE_QUERY_P4 + RECURSIVE_QUERY_P5
                + RECURSIVE_QUERY_P6;
        String query = createQuery(DatabaseType.H2, entity);

        assertEquals(expected, query);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    public void testH2UknownSQLParamValue() {
        Object notEntity = new Object();
        createQuery(DatabaseType.H2, notEntity);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    public void testH2DetachedEntity() {
        UISettings detachedEntity = new UISettings();
        detachedEntity.setSettingsId(999);
        createQuery(DatabaseType.H2, detachedEntity);
    }

    private Object createEntity() {
        // create some persist entity
        UISettings settings = new UISettings();
        settings.setSettingsType(SettingsType.RECORD.toString());
        em.persist(settings);
        return settings;
    }

    private String createQuery(DatabaseType databaseType, Object entity) {
        RecursiveQueryBuilder<Object> builder = databaseType.createRecursiveQueryBuilder(Object.class);

        builder.addSqlPart(RECURSIVE_QUERY_P1).addSqlPart(RECURSIVE_QUERY_P2).addSqlPart(RECURSIVE_QUERY_P3)
                .addSqlPart(RECURSIVE_QUERY_P4).addSqlPart(RECURSIVE_QUERY_P5).addSqlPart(RECURSIVE_QUERY_P6);

        builder.prepareQuery(em);
        builder.setParameter("ids", Arrays.asList(1, 2, 3, 4));
        builder.setParameter("active", true);
        builder.setParameter("entity", entity);
        builder.setParameter("url", "localhost");
        NativeQuery<Object> query = builder.getQuery();
        return query.getQueryString();
    }
}
