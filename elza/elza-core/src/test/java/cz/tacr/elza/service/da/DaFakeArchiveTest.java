package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.domain.DaSyncQueueItem.QueueItemState;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * The queue against a digital archive answering over HTTP like a real one: the processors of
 * the application talk to it through the DA client, so the requests, the JSON of the answers,
 * the failures of the DA API and the states of the queue are checked together.
 */
public class DaFakeArchiveTest extends AbstractServiceTest {

    private static final String AIP_CODE = "04ccc520-c5a9-4c9f-a83f-28d91fd37aa7";

    private static final Duration TIMEOUT = Duration.ofSeconds(40);

    @Autowired
    private DaSyncQueueItemRepository queueRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DaCommunicationLock communicationLock;
    @Autowired
    private DaImportExtSyncsProcessor importProcessor;
    @Autowired
    private DaExportExtSyncsProcessor exportProcessor;

    private HttpServer server;
    private ArrDigitalRepository repository;

    /** The download batch is ready once the test says so. */
    private final AtomicBoolean batchReady = new AtomicBoolean();
    private final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @BeforeEach
    public void startArchive() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::answer);
        server.start();

        repository = tx().execute(t -> {
            ArrDigitalRepository r = new ArrDigitalRepository();
            r.setCode("DA-FAKE");
            r.setName("Falešný digitální archiv");
            r.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
            r.setDigitalRepositoryType(DigitalRepositoryType.DA);
            r.setSendNotification(false);
            r.setSyncDelay(0);
            r.setStatusPollInterval(1);
            return digitalRepositoryRepository.save(r);
        });
    }

    @AfterEach
    public void stopArchive() {
        server.stop(0);
        tx().executeWithoutResult(t -> {
            queueRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        requests.computeIfAbsent(path, p -> new AtomicInteger()).incrementAndGet();
        exchange.getRequestBody().readAllBytes();
        switch (path) {
            case "/download/aips" -> json(exchange, 200, "\"d1\"");
            case "/download/status/d1" -> json(exchange, 200,
                    batchReady.get() ? "{\"state\":\"FINISHED\"}" : "{\"state\":\"PREPARING\"}");
            case "/download/result/d1" -> send(exchange, 200, "application/zip", packageInfoBatch());
            case "/ingest/status/e1" -> json(exchange, 200,
                    requests.get(path).get() == 1 ? "{\"state\":\"PENDING\"}" : "{\"state\":\"FINISHED\"}");
            case "/ingest/result/e1" -> json(exchange, 200, """
                    {"accepted":[{"sipId":"aip-ok","aipId":"aip-ok","aipVersion":"2"}],
                     "packageIngestError":[{"sipId":"aip-bad","errorInfo":{"errorTitle":"Neplatný balíček","errorDetail":"chybí EAD"}}]}
                    """);
            case "/ingest/status/e404" -> json(exchange, 404,
                    "{\"errorTitle\":\"Dávka neexistuje\",\"errorCode\":\"BATCH_UNKNOWN\"}");
            default -> json(exchange, 500, "{\"errorTitle\":\"Neočekávaný dotaz " + path + "\"}");
        }
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, "application/json", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    /** A package_info batch: the directory of the package named by the AIP, its PACKAGE-INFO inside. */
    private static byte[] packageInfoBatch() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes);
             InputStream packageInfo = DaFakeArchiveTest.class.getResourceAsStream("/da/fake/PACKAGE-INFO.xml")) {
            zip.putNextEntry(new ZipEntry(AIP_CODE + "/PACKAGE-INFO.xml"));
            zip.write(packageInfo.readAllBytes());
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private Integer item(String code, QueueItemState state, AipType aipType, String batchId) {
        return tx().execute(t -> {
            DaSyncQueueItem item = new DaSyncQueueItem();
            item.setCode(code);
            item.setDigitalRepository(repository);
            item.setState(state);
            item.setAipType(aipType);
            item.setAipVersion("1");
            item.setActive(true);
            item.setBatchId(batchId);
            return queueRepository.save(item).getSyncQueueItemId();
        });
    }

    /** Items written straight to the queue announce themselves as a request would. */
    private void wakeProcessors() {
        importProcessor.wakeUp();
        exportProcessor.wakeUp();
    }

    private DaSyncQueueItem reload(Integer id) {
        return tx().execute(t -> queueRepository.findById(id).orElseThrow());
    }

    private DaSyncQueueItem awaitItem(Integer id, Predicate<DaSyncQueueItem> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            DaSyncQueueItem item = reload(id);
            if (condition.test(item)) {
                return item;
            }
            Thread.sleep(200);
        }
        DaSyncQueueItem item = reload(id);
        throw new AssertionError("Položka fronty nedošla do očekávaného stavu, je " + item.getState()
                + " / " + item.getStateMessage());
    }

    /** True when the lock can be taken by somebody else within a few seconds. */
    private boolean lockIsFree() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(() -> {
                communicationLock.lock();
                communicationLock.unlock();
                return true;
            }).get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aDownloadBatchWaitsForTheDaWithoutHoldingTheQueue() throws Exception {
        Integer itemId = item(AIP_CODE, QueueItemState.IMPORT_NEW, AipType.PACKAGE_INFO, null);
        wakeProcessors();

        DaSyncQueueItem requested = awaitItem(itemId, i -> i.getState() == QueueItemState.DOWNLOAD_REQUESTED);
        assertEquals("d1", requested.getBatchId());
        awaitItem(itemId, i -> requests.getOrDefault("/download/status/d1", new AtomicInteger()).get() >= 2);
        assertTrue(lockIsFree(), "while the DA prepares the batch, the queue is free for the others");

        batchReady.set(true);
        DaSyncQueueItem done = awaitItem(itemId, i -> i.getState() == QueueItemState.IMPORT_OK
                || i.getState() == QueueItemState.IMPORT_ERROR);

        assertEquals(QueueItemState.IMPORT_OK, done.getState(), done.getStateMessage());
        assertEquals(1, requests.get("/download/result/d1").get());
        tx().executeWithoutResult(t -> assertTrue(aipRepository.findByCode(AIP_CODE) != null,
                                                  "the AIP of the received PACKAGE-INFO exists"));
    }

    @Test
    void eachExportedPackageEndsByWhatTheDaSaid() throws Exception {
        Integer ok = item("aip-ok", QueueItemState.EXPORT_SENT, AipType.METADATA_BASE, "e1");
        Integer bad = item("aip-bad", QueueItemState.EXPORT_SENT, AipType.METADATA_BASE, "e1");
        Integer silent = item("aip-silent", QueueItemState.EXPORT_SENT, AipType.METADATA_BASE, "e1");
        wakeProcessors();

        DaSyncQueueItem accepted = awaitItem(ok, i -> i.getState() != QueueItemState.EXPORT_SENT);

        assertEquals(QueueItemState.EXPORT_OK, accepted.getState());
        assertEquals("Přijato jako verze 2.", accepted.getStateMessage());
        assertEquals(QueueItemState.EXPORT_ERROR, reload(bad).getState());
        assertEquals("Digitální archiv balíček odmítl: Neplatný balíček: chybí EAD", reload(bad).getStateMessage());
        assertEquals(DaExportExtSyncsProcessor.NOT_CONFIRMED, reload(silent).getStateMessage());
        assertTrue(requests.get("/ingest/status/e1").get() >= 2, "the DA was asked again after PENDING");
    }

    @Test
    void anExportTheDaGaveUpEndsWithItsReason() throws Exception {
        Integer itemId = item("aip-gone", QueueItemState.EXPORT_SENT, AipType.METADATA_BASE, "e404");
        wakeProcessors();

        DaSyncQueueItem ended = awaitItem(itemId, i -> i.getState() != QueueItemState.EXPORT_SENT);

        assertEquals(QueueItemState.EXPORT_ERROR, ended.getState());
        assertEquals("Digitální archiv zpracování balíčku ukončil: Dávka neexistuje [BATCH_UNKNOWN] (HTTP 404)",
                     ended.getStateMessage());
        assertEquals(List.of(), queueRepository.findRepositoriesWithState(QueueItemState.EXPORT_SENT));
    }
}
