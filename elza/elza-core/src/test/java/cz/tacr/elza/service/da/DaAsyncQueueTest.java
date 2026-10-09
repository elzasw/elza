package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.domain.DaSyncQueueItem.QueueItemState;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * The queue of the DA does not wait for a batch the DA works on: a batch in flight blocks only
 * the next batch of its own direction and repository, an item is taken only when due, and a
 * request the DA already knows is never withdrawn.
 *
 * The test holds {@link DaCommunicationLock}, so the processors running in the application do
 * not take the items it creates.
 */
public class DaAsyncQueueTest extends AbstractTest {

    @Autowired
    private DaService daService;
    @Autowired
    private DaCommunicationLock communicationLock;
    @Autowired
    private DaSyncQueueItemRepository queueRepository;
    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    private ArrDigitalRepository repository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @BeforeEach
    public void holdTheQueue() {
        communicationLock.lock();
        repository = tx().execute(t -> {
            ArrDigitalRepository r = new ArrDigitalRepository();
            r.setCode("DA-ASYNC");
            r.setName("Testovaci digitalni archiv");
            r.setDigitalRepositoryType(DigitalRepositoryType.DA);
            r.setSendNotification(false);
            r.setSyncDelay(0);
            return digitalRepositoryRepository.save(r);
        });
    }

    @AfterEach
    public void deleteCreatedRows() {
        try {
            tx().executeWithoutResult(t -> {
                queueRepository.deleteAll();
                actionItemRepository.deleteAll();
                actionRepository.deleteAll();
                aipStateRepository.deleteAll();
                changeRepository.deleteAll();
                aipRepository.deleteAll();
                digitalRepositoryRepository.deleteAll();
            });
        } finally {
            communicationLock.unlock();
        }
    }

    private DaAip aip(String code) {
        return tx().execute(t -> {
            DaAip aip = new DaAip();
            aip.setCode(code);
            aip.setDigitalRepository(repository);
            aipRepository.save(aip);
            DaChange change = new DaChange();
            change.setChangeDate(LocalDateTime.now());
            change.setDaAip(aip);
            change.setType(DaChangeType.AIP_CREATE);
            changeRepository.save(change);
            DaAipState state = new DaAipState();
            state.setDaAip(aip);
            state.setCreateChange(change);
            state.setAipVersion("1");
            aipStateRepository.save(state);
            return aip;
        });
    }

    /** A queue item of the AIP in the given state, in flight when a batch is given. */
    private Integer item(DaAip aip, QueueItemState state, String batchId, OffsetDateTime nextAttemptAt) {
        return tx().execute(t -> {
            DaSyncQueueItem item = daService.createSyncQueueItem(aip.getCode(), aip, repository, state, "1",
                                                                 AipType.METADATA_BASE, true);
            item.setBatchId(batchId);
            item.setNextAttemptAt(nextAttemptAt);
            return queueRepository.save(item).getSyncQueueItemId();
        });
    }

    private DaSyncQueueItem reload(Integer itemId) {
        return tx().execute(t -> queueRepository.findById(itemId).orElseThrow());
    }

    private List<Integer> nextDownloads() {
        return daService.getNextItems(10, QueueItemState.DOWNLOAD_REQUESTED,
                                      QueueItemState.UPDATE, QueueItemState.IMPORT_NEW)
                .stream().map(DaSyncQueueItem::getSyncQueueItemId).toList();
    }

    @Test
    void aBatchInFlightHoldsBackOnlyTheNextBatchOfItsDirection() {
        Integer inFlight = item(aip("aip-1"), QueueItemState.DOWNLOAD_REQUESTED, "b1", null);
        Integer waiting = item(aip("aip-2"), QueueItemState.UPDATE, null, null);
        Integer export = item(aip("aip-3"), QueueItemState.EXPORT_NEW, null, null);

        assertEquals(List.of(), nextDownloads(), "one download batch per repository at a time");
        assertEquals(List.of(export), daService.getNextItems(10, QueueItemState.EXPORT_SENT, QueueItemState.EXPORT_NEW)
                .stream().map(DaSyncQueueItem::getSyncQueueItemId).toList(),
                     "an export is not held back by a download");

        daService.changeQueueItemsState(List.of(reload(inFlight)), QueueItemState.IMPORT_OK);
        assertEquals(List.of(waiting), nextDownloads());
    }

    @Test
    void anItemIsTakenWhenDueAndRetryNowTakesItAtOnce() {
        DaAip aip = aip("aip-1");
        item(aip, QueueItemState.UPDATE, null, OffsetDateTime.now().plusMinutes(5));

        assertEquals(List.of(), nextDownloads());
        assertEquals(1, daService.retryNow(List.of(aip.getAipId())));
        assertEquals(1, nextDownloads().size());
    }

    @Test
    void theDueBatchLeavesOutItemsWithdrawnMeanwhile() {
        Integer kept = item(aip("aip-1"), QueueItemState.DOWNLOAD_REQUESTED, "b1", null);
        Integer withdrawn = item(aip("aip-2"), QueueItemState.DOWNLOAD_REQUESTED, "b1", null);
        Integer notDue = item(aip("aip-3"), QueueItemState.DOWNLOAD_REQUESTED, "b2", OffsetDateTime.now().plusMinutes(5));
        tx().executeWithoutResult(t -> {
            DaSyncQueueItem item = queueRepository.findById(withdrawn).orElseThrow();
            item.setActive(false);
            queueRepository.save(item);
        });

        assertEquals(List.of(kept), daService.getDueBatch(QueueItemState.DOWNLOAD_REQUESTED).stream()
                .map(DaSyncQueueItem::getSyncQueueItemId).toList());
        assertNotNull(reload(notDue).getNextAttemptAt());
    }

    @Test
    void onlyRequestsTheDaHasNotReceivedCanBeWithdrawn() {
        DaAip toExport = aip("aip-1");
        DaAip sent = aip("aip-2");
        DaAip requested = aip("aip-3");
        DaAip toDownload = aip("aip-4");
        Integer exportNew = item(toExport, QueueItemState.EXPORT_NEW, null, null);
        Integer exportSent = item(sent, QueueItemState.EXPORT_SENT, "e1", null);
        Integer downloadRequested = item(requested, QueueItemState.DOWNLOAD_REQUESTED, "d1", null);
        Integer update = item(toDownload, QueueItemState.UPDATE, null, null);

        assertEquals(2, daService.withdrawRequests(List.of(toExport.getAipId(), sent.getAipId(),
                                                           requested.getAipId(), toDownload.getAipId())));

        assertFalse(reload(exportNew).getActive());
        assertFalse(reload(update).getActive());
        assertTrue(reload(exportSent).getActive());
        assertTrue(reload(downloadRequested).getActive());
    }

    /** The result of a sent export is awaited even when the AIP is exported again meanwhile. */
    @Test
    void aNewExportDoesNotSupersedeTheSentOne() {
        DaAip aip = aip("aip-1");
        Integer sent = item(aip, QueueItemState.EXPORT_SENT, "e1", null);
        Integer again = item(aip, QueueItemState.EXPORT_NEW, null, null);

        assertTrue(reload(sent).getActive());
        assertTrue(reload(again).getActive());
    }

    @Test
    void aTransientFailureIsRetriedLaterWithEveryFailure() {
        Integer itemId = item(aip("aip-1"), QueueItemState.EXPORT_NEW, null, null);

        daService.scheduleRetry(List.of(reload(itemId)), "Nelze se spojit");
        OffsetDateTime first = reload(itemId).getNextAttemptAt();
        daService.scheduleRetry(List.of(reload(itemId)), "Nelze se spojit");
        DaSyncQueueItem item = reload(itemId);

        assertEquals(QueueItemState.EXPORT_NEW, item.getState());
        assertEquals(2, item.getAttemptCount());
        assertEquals("Nelze se spojit (pokusů: 2)", item.getStateMessage());
        assertTrue(item.getNextAttemptAt().isAfter(first), "the second delay is longer");
    }

    /** A batch the DA gave up is requested again - a download is never given up. */
    @Test
    void aDownloadBatchTheDaGaveUpReturnsToWaiting() {
        Integer itemId = item(aip("aip-1"), QueueItemState.DOWNLOAD_REQUESTED, "d1", null);

        daService.returnToPending(List.of(reload(itemId)), new IllegalStateException("404"));
        DaSyncQueueItem item = reload(itemId);

        assertEquals(QueueItemState.UPDATE, item.getState());
        assertNull(item.getBatchId());
        assertEquals(1, item.getAttemptCount());
        assertTrue(item.getNextAttemptAt().isAfter(OffsetDateTime.now()));
    }

    @Test
    void markingABatchSchedulesTheFirstQuestion() {
        Integer itemId = item(aip("aip-1"), QueueItemState.UPDATE, null, null);
        OffsetDateTime before = OffsetDateTime.now();

        daService.markBatch(List.of(reload(itemId)), QueueItemState.DOWNLOAD_REQUESTED, "d1", repository);
        DaSyncQueueItem item = reload(itemId);

        assertEquals(QueueItemState.DOWNLOAD_REQUESTED, item.getState());
        assertEquals("d1", item.getBatchId());
        assertFalse(item.getNextAttemptAt().isBefore(before.plus(Duration.ofSeconds(2))));
        assertEquals(List.of(), daService.getDueBatch(QueueItemState.DOWNLOAD_REQUESTED), "not due before the interval");
    }
}
