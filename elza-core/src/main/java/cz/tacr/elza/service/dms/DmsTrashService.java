package cz.tacr.elza.service.dms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.exception.SystemException;

/**
 * On-disk trash for the DMS. Files never leave {@code dms/} — they move
 * to {@code _trash/<yyyy-MM-dd>/} first, then get purged after their
 * retention window.
 */
@Service
public class DmsTrashService {

    private static final Logger logger = LoggerFactory.getLogger(DmsTrashService.class);

    private static final String REPORT_FILE = "report.txt";
    private static final DateTimeFormatter COLLISION_SUFFIX = DateTimeFormatter.ofPattern("HHmmss");

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    /**
     * Přesune {@code source} do {@code _trash/<dnes>/} pod původním
     * názvem; při kolizi v adresáři přidá suffix {@code -HHmmss}.
     * Připíše řádek do {@code report.txt}.
     *
     * @return cesta v koši, nebo {@code null}, pokud zdroj neexistoval
     *         (již smazaný soubor není chyba)
     */
    public Path moveToTrash(Path source, String reason) {
        if (!Files.exists(source)) {
            logger.debug("Trash source does not exist: {}", source);
            return null;
        }
        try {
            Path trashDir = DmsStorageLayout.trashDir(resourcePathResolver.getDmsDir(), LocalDate.now());
            Files.createDirectories(trashDir);
            String leaf = source.getFileName().toString();
            Path target = trashDir.resolve(leaf);
            if (Files.exists(target)) {
                target = trashDir.resolve(leaf + "-" + COLLISION_SUFFIX.format(LocalTime.now()));
            }
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            appendReportLine(trashDir, target, reason);
            return target;
        } catch (IOException e) {
            throw new SystemException("Failed to move file to trash: " + source, e);
        }
    }

    /**
     * Smaže všechny denní adresáře v {@code _trash/}, jejichž datum je
     * starší než {@code dnes - retentionDays}.
     *
     * @return počet smazaných denních adresářů
     */
    public int purgeExpired(int retentionDays) {
        Path trashRoot = resourcePathResolver.getDmsDir().resolve(DmsStorageLayout.TRASH_DIR);
        if (!Files.exists(trashRoot)) {
            return 0;
        }
        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        int purged = 0;
        try (Stream<Path> stream = Files.list(trashRoot)) {
            for (Path day : (Iterable<Path>) stream::iterator) {
                if (!Files.isDirectory(day)) {
                    continue;
                }
                LocalDate dayDate;
                try {
                    dayDate = LocalDate.parse(day.getFileName().toString());
                } catch (DateTimeParseException e) {
                    logger.warn("Non-date directory in DMS trash root: {}", day.getFileName());
                    continue;
                }
                if (dayDate.isBefore(cutoff)) {
                    deleteRecursively(day);
                    purged++;
                }
            }
        } catch (IOException e) {
            throw new SystemException("Failed to list DMS trash root: " + trashRoot, e);
        }
        return purged;
    }

    private void appendReportLine(Path trashDir, Path storedAt, String reason) throws IOException {
        String line = String.format("%s;%d;%s;%s%n",
                storedAt.getFileName(),
                Files.size(storedAt),
                Files.getLastModifiedTime(storedAt).toInstant(),
                reason);
        Files.writeString(trashDir.resolve(REPORT_FILE), line, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    private void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    logger.warn("Failed to delete trash entry {}: {}", p, e.toString());
                }
            });
        }
    }
}
