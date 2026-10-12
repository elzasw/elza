package cz.tacr.elza.dbchangelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.tacr.elza.service.dms.DmsStorageLayout;

/**
 * DMS migration to the yyyy/MM/block layout: the flat layout, and the state
 * left by the 3.4.9 migration (four-digit ids parked in _migrating/).
 */
public class DbChangeset20261012120000Test {

    private static final OffsetDateTime MAY_2022 = OffsetDateTime.of(2022, 5, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    @TempDir
    Path dmsRoot;

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection("jdbc:h2:mem:dmsmig" + System.nanoTime());
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE dms_file (file_id INT PRIMARY KEY, file_name VARCHAR(255),"
                    + " created_at TIMESTAMP WITH TIME ZONE NOT NULL, storage_path VARCHAR(255))");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    @Test
    void migratesFlatLayoutIncludingFourDigitIds() throws Exception {
        // 5 and 1500 make the year directory 2022 before the row of file 2022 is reached
        for (int id : new int[] { 5, 1500, 2022, 51000 }) {
            insertRow(id, "f" + id + ".pdf", null);
            writeFile(dmsRoot.resolve(String.valueOf(id)), "content " + id);
        }

        DbChangeset20261012120000.Counters c = DbChangeset20261012120000.migrate(conn, dmsRoot);

        assertEquals(4, c.staged);
        assertEquals(4, c.migrated);
        assertEquals(0, c.missing);
        assertEquals(0, c.failed);
        assertEquals(0, c.orphans);
        for (int id : new int[] { 5, 1500, 2022, 51000 }) {
            String rel = DmsStorageLayout.relativePath(MAY_2022, id, "f" + id + ".pdf");
            assertEquals(rel, storagePath(id));
            assertEquals("content " + id, Files.readString(dmsRoot.resolve(rel)));
        }
        assertFalse(Files.exists(dmsRoot.resolve("2022").resolve("2022")));
    }

    @Test
    void repairsStateLeftBy349Migration() throws Exception {
        // 3.4.9: four-digit ids parked in _migrating/, storage_path written, file missing
        String rel1234 = DmsStorageLayout.relativePath(MAY_2022, 1234, "a.rtf");
        insertRow(1234, "a.rtf", rel1234);
        writeFile(dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR).resolve("1234"), "a");
        // 3.4.9: id equal to a year - the move failed, storage_path stays NULL
        insertRow(2022, "b.rtf", null);
        writeFile(dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR).resolve("2022"), "b");
        // correctly migrated by 3.4.9
        String rel7 = DmsStorageLayout.relativePath(MAY_2022, 7, "c.pdf");
        insertRow(7, "c.pdf", rel7);
        writeFile(dmsRoot.resolve(rel7), "c");
        // file without a row
        writeFile(dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR).resolve("9999"), "orphan");
        // row without a file
        insertRow(8000, "d.pdf", null);

        DbChangeset20261012120000.Counters c = DbChangeset20261012120000.migrate(conn, dmsRoot);

        assertEquals(0, c.staged);
        assertEquals(2, c.migrated);
        assertEquals(1, c.inPlace);
        assertEquals(1, c.missing);
        assertEquals(0, c.failed);
        assertEquals(1, c.orphans);
        assertEquals("a", Files.readString(dmsRoot.resolve(rel1234)));
        String rel2022 = DmsStorageLayout.relativePath(MAY_2022, 2022, "b.rtf");
        assertEquals(rel2022, storagePath(2022));
        assertEquals("b", Files.readString(dmsRoot.resolve(rel2022)));
        assertEquals("c", Files.readString(dmsRoot.resolve(rel7)));
        assertEquals(DmsStorageLayout.relativePath(MAY_2022, 8000, "d.pdf"), storagePath(8000));
        assertTrue(Files.exists(dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR).resolve("9999")));
    }

    @Test
    void secondRunChangesNothing() throws Exception {
        insertRow(1500, "x.pdf", null);
        writeFile(dmsRoot.resolve("1500"), "x");
        DbChangeset20261012120000.migrate(conn, dmsRoot);

        DbChangeset20261012120000.Counters c = DbChangeset20261012120000.migrate(conn, dmsRoot);

        assertEquals(0, c.staged);
        assertEquals(0, c.migrated);
        assertEquals(1, c.inPlace);
        assertEquals(0, c.orphans);
        assertEquals("x", Files.readString(dmsRoot.resolve(storagePath(1500))));
    }

    private void insertRow(int fileId, String fileName, String storagePath) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dms_file (file_id, file_name, created_at, storage_path) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, fileId);
            ps.setString(2, fileName);
            ps.setObject(3, MAY_2022);
            ps.setString(4, storagePath);
            ps.executeUpdate();
        }
    }

    private String storagePath(int fileId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT storage_path FROM dms_file WHERE file_id = ?")) {
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
