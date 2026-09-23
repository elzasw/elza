package cz.tacr.elza.service.dms;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Pure utility encoding the DMS storage layout rules. Never touches the
 * filesystem, holds no state, and is the only place in the codebase that
 * knows how DMS relative paths are shaped.
 */
public final class DmsStorageLayout {

    public static final String TRASH_DIR = "_trash";
    public static final String MIGRATING_DIR = "_migrating";
    public static final String README_FILE = "README.txt";
    public static final String README_CLASSPATH = "/dms/" + README_FILE;
    public static final String TMP_SUFFIX = ".tmp";
    public static final String MIGRATION_LOG_PREFIX = "_migration-";

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern EXT_ALLOWED = Pattern.compile("^[a-z0-9]{1,10}$");
    private static final Pattern LEGACY_LEAF_NAME = Pattern.compile("^\\d+$");
    private static final int BLOCK_SIZE = 1000;

    /** Server time zone used to derive yyyy/MM. Package-private for tests. */
    static ZoneId SERVER_ZONE = ZoneId.systemDefault();

    private DmsStorageLayout() {
    }

    /**
     * @return relative POSIX path used as {@code dms_file.storage_path},
     *         e.g. {@code "2026/09/000051/51230.pdf"}
     */
    public static String relativePath(OffsetDateTime createdAt, int fileId, String fileName) {
        var zoned = createdAt.atZoneSameInstant(SERVER_ZONE);
        String ext = sanitizedExtension(fileName);
        String leaf = ext.isEmpty() ? Integer.toString(fileId) : fileId + "." + ext;
        return YEAR.format(zoned)
                + "/" + MONTH.format(zoned)
                + "/" + String.format("%06d", fileId / BLOCK_SIZE)
                + "/" + leaf;
    }

    /**
     * @return lower-cased extension of {@code fileName} if it matches
     *         {@code ^[a-z0-9]{1,10}$}; empty string otherwise
     */
    public static String sanitizedExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        String raw = fileName.substring(lastDot + 1).toLowerCase();
        return EXT_ALLOWED.matcher(raw).matches() ? raw : "";
    }

    /**
     * @return true for the flat legacy leaf name ({@code "<digits>"} with no extension)
     */
    public static boolean isLegacyName(String leafName) {
        return leafName != null && LEGACY_LEAF_NAME.matcher(leafName).matches();
    }

    /** {@code <target>.tmp} in the same directory as {@code target}. */
    public static Path tmpPath(Path target) {
        return target.resolveSibling(target.getFileName() + TMP_SUFFIX);
    }

    /** {@code <dmsRoot>/_trash/<yyyy-MM-dd>/} */
    public static Path trashDir(Path dmsRoot, LocalDate day) {
        return dmsRoot.resolve(TRASH_DIR).resolve(DAY.format(day));
    }
}
