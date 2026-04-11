package cz.tacr.elza.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.SettingsType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecursiveQueryBuilderTest extends AbstractTest {

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        // no-op: setup is done once in @BeforeAll initOnce()
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup is done once in @AfterAll cleanupOnce()
    }

    private static final String RECURSIVE_QUERY_P1 = "SELECT DISTINCT col1 FROM ";
    private static final String RECURSIVE_QUERY_P2 = "(WITH RECURSIVE recTable(col1, col2) AS (";
    private static final String RECURSIVE_QUERY_P3 = "SELECT * FROM table1 WHERE id IN (:ids) AND url = :url AND active = :active OR entity = :entity ";
    private static final String RECURSIVE_QUERY_P3ms = "SELECT * FROM table1 WHERE id IN (?) AND url = ? AND active = ? OR entity = ? ";
    private static final String RECURSIVE_QUERY_P4 = "UNION ALL SELECT * FROM table2) SELECT * FROM recTable)";

    @Autowired
    private EntityManager em;

    @Test
    public void testMSSQLRecursiveQuery() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(r -> testMSSQLRecursiveQueryInTransaction());
    }

    private void testMSSQLRecursiveQueryInTransaction() {
        Object entity = createEntity();
        String p2 = "(WITH recTable(col1, col2) AS (";

        String expected = RECURSIVE_QUERY_P1 + p2 + RECURSIVE_QUERY_P3ms + RECURSIVE_QUERY_P4;
        String query = createQuery(DatabaseType.MSSQL, entity);

        assertEquals(expected, query);

        em.remove(entity);
    }

    @Test
    public void testH2RecursiveQuery() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(r -> testH2RecursiveQueryInTransaction());
    }

    private void testH2RecursiveQueryInTransaction() {
        Object entity = createEntity();
        Object entityId = em.unwrap(Session.class).getIdentifier(entity);
        String p3 = "SELECT * FROM table1 WHERE id IN (1, 2, 3, 4) AND url = 'localhost' AND active = true OR entity = " + entityId + " ";

        String expected = RECURSIVE_QUERY_P1 + RECURSIVE_QUERY_P2 + p3 + RECURSIVE_QUERY_P4;
        String query = createQuery(DatabaseType.H2, entity);

        assertEquals(expected, query);

        em.remove(entity);
    }

    @Test
    public void testH2UknownSQLParamValue() {
        Object notEntity = new Object();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> createQuery(DatabaseType.H2, notEntity));
    }

    @Test
    public void testH2DetachedEntity() {
        UISettings detachedEntity = new UISettings();
        detachedEntity.setSettingsId(999);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> createQuery(DatabaseType.H2, detachedEntity));
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

        builder.addSqlPart(RECURSIVE_QUERY_P1).addSqlPart(RECURSIVE_QUERY_P2).addSqlPart(RECURSIVE_QUERY_P3).addSqlPart(RECURSIVE_QUERY_P4);

        builder.prepareQuery(em);
        builder.setParameter("ids", Arrays.asList(1, 2, 3, 4));
        builder.setParameter("active", true);
        builder.setParameter("entity", entity);
        builder.setParameter("url", "localhost");
        NativeQuery<Object> query = builder.getQuery();
        return query.getQueryString();
    }
}
