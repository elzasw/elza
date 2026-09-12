package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * A folder configured but missing on disk is a deployment fault, and the listing is called from
 * the UI on every visit of the batch detail page. It therefore has to be reported once, not once
 * per call - an ERROR per page visit is how a log stops being read (#9991).
 */
public class ImpItemServiceFolderLogTest {

    private ImpItemService service;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new ImpItemService();
        logger = (Logger) LoggerFactory.getLogger(ImpItemService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private List<ILoggingEvent> events() {
        return appender.list;
    }

    @Test
    void anUnconfiguredFolderIsSilent() throws IOException {
        ReflectionTestUtils.setField(service, "batchInputDir", "");

        assertThat(service.listServerFolder(null)).isEmpty();
        assertThat(service.listServerFolder(null)).isEmpty();

        assertThat(events()).isEmpty();
    }

    @Test
    void aMissingFolderIsReportedOnceAndAsAWarning(@TempDir Path tmp) throws IOException {
        ReflectionTestUtils.setField(service, "batchInputDir", tmp.resolve("absent").toString());

        for (int i = 0; i < 5; i++) {
            assertThat(service.listServerFolder(null)).isEmpty();
        }

        assertThat(events()).hasSize(1);
        assertThat(events().get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(events().get(0).getFormattedMessage()).contains("does not exist");
    }

    /**
     * The flag is cleared once the folder is there, so a deployment that breaks a second time is
     * reported a second time rather than staying silent for the life of the process.
     */
    @Test
    void aFolderThatComesBackAndBreaksAgainIsReportedAgain(@TempDir Path tmp) throws IOException {
        Path folder = tmp.resolve("inbox");
        ReflectionTestUtils.setField(service, "batchInputDir", folder.toString());

        service.listServerFolder(null);
        service.listServerFolder(null);
        assertThat(events()).hasSize(1);

        Files.createDirectory(folder);
        assertThat(service.listServerFolder(null)).isEmpty();
        assertThat(events()).hasSize(1);

        Files.delete(folder);
        service.listServerFolder(null);
        assertThat(events()).hasSize(2);
    }
}
