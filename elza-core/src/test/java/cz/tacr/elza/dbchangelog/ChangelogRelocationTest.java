package cz.tacr.elza.dbchangelog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One-off proof for the changelog file relocation (db.elza-3.0.xml →
 * db.elza-3-part-01.xml, db.elza-da.xml → db.elza-3-part-02.xml): a database
 * whose DATABASECHANGELOG carries the OLD identities must see ZERO unrun
 * changesets with the NEW layout, thanks to the logicalFilePath pins.
 *
 * Uses changeLogSync (records identities without executing changes — the
 * changelog contains Spring-dependent custom changes), then asks the new
 * layout what it would run.
 *
 * Requires the old layout extracted to a directory passed via
 * -Delza.test.oldChangelogRoot (containing db/changelog/ with the old files).
 */
public class ChangelogRelocationTest {

    private static final String MASTER = "db/changelog/db.changelog-master.yaml";

    @Test
    @EnabledIfSystemProperty(named = "elza.test.oldChangelogRoot", matches = ".+")
    void relocationExecutesNothing(@TempDir Path tempDir) throws Exception {
        Path oldRoot = Paths.get(System.getProperty("elza.test.oldChangelogRoot"));
        String url = "jdbc:h2:" + tempDir.resolve("reloc").toAbsolutePath();

        // phase 1: record the OLD layout's identities without executing anything
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            Database db = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase lq = new Liquibase(MASTER, new DirectoryResourceAccessor(oldRoot), db)) {
                lq.changeLogSync("");
            }
        }

        // phase 2: the NEW layout (from the classpath) must have nothing to run
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            Database db = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase lq = new Liquibase(MASTER,
                    new ClassLoaderResourceAccessor(getClass().getClassLoader()), db)) {
                List<ChangeSet> unrun = lq.listUnrunChangeSets(new Contexts(), new LabelExpression());
                assertEquals(List.of(), unrun,
                        "relocated changelog would re-execute changesets on an already-migrated database");
            }
        }
    }
}
