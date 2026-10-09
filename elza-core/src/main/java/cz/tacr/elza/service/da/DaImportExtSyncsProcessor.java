package cz.tacr.elza.service.da;

import cz.tacr.da.ApiException;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaDownloadMethod;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cz.tacr.elza.connector.DaConnector.FILE_TRANSFER_ERROR_CODE;
import cz.tacr.elza.api.DaOnReceivedAction;
import java.util.Map;
import cz.tacr.elza.common.io.SpooledContent;

/**
 * Downloads packages from the DA.
 *
 * A download is two exchanges with the DA, each a short step under {@link DaCommunicationLock}:
 * requesting a batch, after which its items wait as {@link DaSyncQueueItem.QueueItemState#DOWNLOAD_REQUESTED},
 * and asking whether the batch is ready - once per status interval - until it is fetched and
 * processed. While the DA prepares the batch, the lock is free for the synchronization and the
 * exports. One batch per repository is in flight at a time.
 */
@Component
public class DaImportExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaImportExtSyncsProcessor.class);

    @Autowired
    private DaService daService;
    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private DaAipAutoLinkService aipAutoLinkService;
    @Autowired
    private DaAipActionService actionService;
    @Autowired
    private DaCommunicationLock communicationLock;
    @Autowired
    private UserService userService;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    /**
     * Ends the idle wait early. A flag, not a monitor: whoever wakes the processor must never
     * wait for the step it is running, and the idle wait must release {@link #lock}.
     */
    private volatile boolean wakeRequested;

    /** How often the idle wait looks whether it was woken. */
    private static final long WAKE_CHECK_MS = 200;

    private static final long QUEUE_CHECK_TIME_INTERVAL = 10000;

    private static final int DEFAULT_IMPORT_LIST_SIZE = 100;

    private static final List<DaSyncQueueItem.QueueItemState> QUEUE_STATES = List.of(
            DaSyncQueueItem.QueueItemState.IMPORT_NEW,
            DaSyncQueueItem.QueueItemState.UPDATE,
            DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED);

    private int importListSize = DEFAULT_IMPORT_LIST_SIZE;

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private ThreadStatus status;

    public void startExtSyncs() {
        synchronized (lock) {
            status = ThreadStatus.RUNNING;
            if (this.asyncThread == null) {
                this.asyncThread = new Thread(this,"DaImportExtSyncsProcessor");
                this.asyncThread.start();
            }
        }
    }

    /**
     * Downloads the prepared batch using the method configured on the repository.
     * The standard HTTP download is refused by the DA with 413 when the batch is too
     * large; in that case the administrator has to switch the repository to File Transfer.
     */
    private SpooledContent downloadBatch(ArrDigitalRepository digitalRepository, String batchId) throws ApiException, IOException {
        DaDownloadMethod method = digitalRepository.getDownloadMethod() == null
                ? DaDownloadMethod.STANDARD : digitalRepository.getDownloadMethod();
        if (method == DaDownloadMethod.FILE_TRANSFER) {
            return daService.downloadFileTransfer(digitalRepository, batchId);
        }
        try {
            return daService.downloadDownload(digitalRepository, batchId);
        } catch (ApiException e) {
            if (e.getCode() == FILE_TRANSFER_ERROR_CODE) {
                throw new IllegalStateException("Repository " + digitalRepository.getCode()
                        + " refused the standard download of batch " + batchId
                        + " (HTTP 413); switch the repository download method to File Transfer.", e);
            }
            throw e;
        }
    }

    /**
     * Asks the DA for a batch of the waiting items. A failure keeps them waiting, to be asked
     * for again after a growing delay.
     *
     * @return true when a batch was requested
     */
    boolean requestNextBatch() {
        List<DaSyncQueueItem> items = daService.getNextItems(importListSize,
                DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED,
                DaSyncQueueItem.QueueItemState.UPDATE, DaSyncQueueItem.QueueItemState.IMPORT_NEW);
        if (items.isEmpty()) {
            return false;
        }
        ArrDigitalRepository digitalRepository = repositoryOf(items);
        try {
            String batchId = daService.downloadAips(digitalRepository, items, items.get(0).getAipType());
            daService.markBatch(items, DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED, batchId, digitalRepository);
            logger.debug("Requested download batch {} of {} item(s)", batchId, items.size());
            return true;
        } catch (Exception ex) {
            logger.error("Failed to request a download batch of {} queue item(s), the items will be retried.",
                         items.size(), ex);
            daService.recordDownloadFailure(items, ex);
            importListSize = 1;
            return false;
        }
    }

    /**
     * Asks the DA once about the batch in flight whose question is due, and fetches and processes
     * it when it is ready.
     *
     * @return true when a batch was processed
     */
    boolean checkRequestedBatch() {
        List<DaSyncQueueItem> batch = daService.getDueBatch(DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED);
        if (batch.isEmpty()) {
            return false;
        }
        ArrDigitalRepository digitalRepository = repositoryOf(batch);
        String batchId = batch.get(0).getBatchId();
        SpooledContent zip;
        try {
            if (!daService.downloadStatusFinished(digitalRepository, batchId)) {
                daService.scheduleNextCheck(batch, digitalRepository);
                return false;
            }
            zip = downloadBatch(digitalRepository, batchId);
        } catch (Exception ex) {
            if (DaRequestFailure.isPermanent(ex)) {
                // the DA stopped preparing the batch; the packages are still there, ask again
                logger.warn("The DA gave up download batch {}: {}; the items will be requested again.",
                            batchId, DaRequestFailure.describe(ex));
                daService.returnToPending(batch, ex);
            } else {
                logger.error("Failed to obtain download batch {}, it will be asked for again.", batchId, ex);
                daService.recordDownloadFailure(batch, ex);
            }
            return false;
        }
        processBatch(digitalRepository, batch, zip);
        return true;
    }

    /**
     * Processes a downloaded batch. A package that arrived and failed is not retried - its items
     * are closed with the problem, which is written on their AIPs as well.
     */
    private void processBatch(ArrDigitalRepository digitalRepository, List<DaSyncQueueItem> syncQueueItemList,
                              SpooledContent zip) {
        AipType aipType = syncQueueItemList.get(0).getAipType();
        try {
            try (zip; InputStream inputStream = zip.openStream()) {
                daService.processPackageInfo(digitalRepository, inputStream, aipType, syncQueueItemList);
            }

            daService.updateAipToQueueItems(syncQueueItemList);

            boolean autoProcess = digitalRepository.getOnReceived() == DaOnReceivedAction.DOWNLOAD_METADATA;
            List<Integer> receivedAipIds = autoProcess && aipType == AipType.PACKAGE_INFO
                    ? receivedAipIds(syncQueueItemList) : List.of();
            if (aipType == AipType.METADATA_BASE || aipType == AipType.AIP_BASE) {
                List<Integer> aipids = syncQueueItemList.stream().map(q -> q.getAip().getAipId()).toList();
                Map<Integer, List<String>> uuidsByAip = daService.doCreateDaoStructure(aipids,
                        actionService.sinkForQueueItems(syncQueueItemList));
                if (autoProcess) {
                    aipAutoLinkService.linkReceivedAips(uuidsByAip);
                }
            }

            daService.changeQueueItemsState(syncQueueItemList, DaSyncQueueItem.QueueItemState.IMPORT_OK);
            actionService.completeFromQueue(syncQueueItemList, DaAipActionItemState.FINISHED, null);

            // Enqueued only after the batch is saved as IMPORT_OK: the new queue
            // item deactivates the AIP's previous items and a later save of the
            // batch entities must not restore their active flag.
            requestMetadataOfReceivedAips(receivedAipIds);

            // pokud je vše v pořádku - maximální velikost dávky pro čtení
            importListSize = DEFAULT_IMPORT_LIST_SIZE;
        } catch (Exception ex) {
            // The package was in hand and its processing failed, so - unlike a failed
            // download - the items are closed; the problem is written on their AIPs
            // and their action items as well, because a terminal failure the user can
            // only find in the queue is a failure they do not find.
            AipProblem problem = AipProblem.of(ex);
            daService.failQueueItems(problemPerItem(syncQueueItemList, problem),
                                     DaSyncQueueItem.QueueItemState.IMPORT_ERROR);

            logger.error("Failed to process item. ", ex);
            // v případě chyby číst po 1 záznamu
            importListSize = 1;
        }
    }

    private ArrDigitalRepository repositoryOf(List<DaSyncQueueItem> items) {
        return externalSystemService.getDigitalRepository(items.get(0).getDigitalRepository().getExternalSystemId());
    }

    /**
     * Requests the metadata package of the AIPs whose PACKAGE-INFO has just been received - the
     * repository is configured to download metadata automatically. The metadata import later
     * attaches the AIP to its node. AIPs that cannot or need not be asked are skipped by
     * {@link DaService#createDaoStructure(List)}: without a fund, with the metadata stored, or
     * with a download waiting.
     */
    private void requestMetadataOfReceivedAips(List<Integer> receivedAipIds) {
        if (!receivedAipIds.isEmpty()) {
            logger.info("Requesting metadata of {} received AIP(s): {}", receivedAipIds.size(), receivedAipIds);
            daService.createDaoStructure(receivedAipIds);
        }
    }

    /**
     * The one problem of the whole batch, described on each of its items - the batch failed as
     * a whole and nothing tells its items apart.
     */
    private static Map<DaSyncQueueItem, AipProblem> problemPerItem(@Nullable List<DaSyncQueueItem> syncQueueItemList,
                                                                   AipProblem problem) {
        return CollectionUtils.isEmpty(syncQueueItemList)
                ? Map.of()
                : syncQueueItemList.stream().collect(Collectors.toMap(Function.identity(), item -> problem));
    }

    /**
     * @return ids of the AIPs the batch has received PACKAGE-INFO of - new ones as well as new
     *         versions of known ones: a new version may name a fund the previous one did not, and
     *         then it has to be placed like a package received for the first time
     */
    private static List<Integer> receivedAipIds(List<DaSyncQueueItem> syncQueueItemList) {
        return syncQueueItemList.stream()
                .filter(q -> q.getAip() != null)
                .map(q -> q.getAip().getAipId())
                .toList();
    }

    /**
     * A request waits sooner than the idle wait ends; it is taken right away. A wake-up that comes
     * while a step runs is kept, so the next idle wait ends at once.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onQueueChanged(DaQueueChangedEvent event) {
        wakeUp();
    }

    public void wakeUp() {
        wakeRequested = true;
    }

    /** Waits on {@link #lock} - releasing it - until the time passes or the processor is woken. */
    private void idle(long millis) throws InterruptedException {
        long end = System.currentTimeMillis() + millis;
        long remaining = millis;
        while (!wakeRequested && remaining > 0) {
            lock.wait(Math.min(remaining, WAKE_CHECK_MS));
            remaining = end - System.currentTimeMillis();
        }
        wakeRequested = false;
    }

    /** One exchange with the DA: from reading the queue to saving what it brought. */
    private boolean exchange(Supplier<Boolean> step) {
        communicationLock.lock();
        try {
            return step.get();
        } catch (Exception e) {
            logger.error("DaImportExtSyncsProcessor - step failed", e);
            return false;
        } finally {
            communicationLock.unlock();
        }
    }

    @Override
    public void run() {
        synchronized (lock) {
            // the import creates levels and listeners of their events (e.g. the level tree cache)
            // read the fund version through secured services
            SecurityContextHolder.setContext(userService.createSecurityContextSystem());
            try {
                while (status == ThreadStatus.RUNNING) {
                    boolean processed = exchange(this::checkRequestedBatch);
                    boolean requested = exchange(this::requestNextBatch);
                    if (!processed && !requested) {
                        try {
                            idle(DaService.idleMillis(daService.getEarliestAttempt(QUEUE_STATES), QUEUE_CHECK_TIME_INTERVAL));
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("DaImportExtSyncsProcessor - processor thread error", e);
            } finally {
                SecurityContextHolder.clearContext();
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
            logger.error("DaImportExtSyncsProcessor - thread finished");
        }
    }

}
