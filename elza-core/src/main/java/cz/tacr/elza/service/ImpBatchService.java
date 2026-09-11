package cz.tacr.elza.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.dataexchange.input.DEImportParams;
import cz.tacr.elza.dataexchange.input.DEImportParams.ImportDirection;
import cz.tacr.elza.dataexchange.input.DEImportParams.ImportPositionParams;
import cz.tacr.elza.dataexchange.input.DEImportService;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.BatchState;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.FundImportStrategy;
import cz.tacr.elza.domain.FundPairKey;
import cz.tacr.elza.domain.ImpBatch;
import cz.tacr.elza.domain.ImpBatchDescCsv;
import cz.tacr.elza.domain.ImpBatchEdx;
import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.domain.ImpItemResult;
import cz.tacr.elza.domain.ImpResultType;
import cz.tacr.elza.domain.ItemState;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ImpBatchRepository;
import cz.tacr.elza.repository.ImpItemRepository;
import cz.tacr.elza.repository.ImpItemResultRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.imp.CsvDescItemsImporter;
import cz.tacr.elza.service.imp.EdxHeader;

/**
 * Manages import batches: their creation, lookup and state changes.
 *
 * The domain-level rules the specification lays down for the state machine live here; carrying
 * out an import is the executor's job, not this service's.
 */
@Service
public class ImpBatchService {

    private static final Logger logger = LoggerFactory.getLogger(ImpBatchService.class);

    // retention period for old import files
    private static final int DATA_RETENTION_DAYS = 30;

    /** One page of the batches listing, with the batches and the unpaged total count. */
    public record BatchPage(List<ImpBatch> items, long totalCount) { }

    // What a batch may become from each state. A missing entry means the source state has no
    // legal successors from a user action (finalized).
    private static final Map<BatchState, Set<BatchState>> ALLOWED_TRANSITIONS;
    static {
        Map<BatchState, Set<BatchState>> t = new EnumMap<>(BatchState.class);
        t.put(BatchState.PREPARATION,      EnumSet.of(BatchState.IN_PROGRESS, BatchState.TEST_IN_PROGRESS, BatchState.CANCELLED));
        t.put(BatchState.IN_PROGRESS,      EnumSet.of(BatchState.PAUSED, BatchState.FAILED, BatchState.FINISHED, BatchState.CANCELLED));
        t.put(BatchState.TEST_IN_PROGRESS, EnumSet.of(BatchState.TEST_FINISHED, BatchState.FAILED, BatchState.PREPARATION, BatchState.CANCELLED));
        t.put(BatchState.TEST_FINISHED,    EnumSet.of(BatchState.PREPARATION, BatchState.IN_PROGRESS, BatchState.TEST_IN_PROGRESS, BatchState.CANCELLED));
        t.put(BatchState.PAUSED,           EnumSet.of(BatchState.IN_PROGRESS, BatchState.CANCELLED));
        t.put(BatchState.FAILED,           EnumSet.of(BatchState.IN_PROGRESS, BatchState.CANCELLED));
        ALLOWED_TRANSITIONS = t;
    }

    @Autowired
    private ImpBatchRepository batchRepository;
    @Autowired
    private ImpItemRepository itemRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private DmsService dmsService;
    @Autowired
    private DEImportService deImportService;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private ImpItemResultRepository itemResultRepository;
    @Autowired
    private CsvDescItemsImporter csvImporter;
    @Autowired
    private IEventNotificationService eventNotificationService;

    /**
     * Creates a new EDX2 import batch in the PREPARATION state.
     */
    @Transactional
    public ImpBatchEdx createEdxBatch(String name,
                                      FundImportStrategy fundImportStrategy,
                                      FundPairKey fundPairKey,
                                      boolean ignoreRootNodes,
                                      ApScope scope,
                                      boolean skipError,
                                      boolean keepFiles) {
        ImpBatchEdx batch = new ImpBatchEdx();
        applyCommonDefaults(batch, name, skipError, keepFiles);
        batch.setFundImportStrategy(fundImportStrategy);
        batch.setFundPairKey(fundPairKey);
        batch.setIgnoreRootNodes(ignoreRootNodes);
        batch.setScope(scope);
        return batchRepository.save(batch);
    }

    /**
     * Creates a new CSV import batch in the PREPARATION state.
     */
    @Transactional
    public ImpBatchDescCsv createDescCsvBatch(String name,
                                              String separator,
                                              String encoding,
                                              boolean skipError,
                                              boolean keepFiles) {
        ImpBatchDescCsv batch = new ImpBatchDescCsv();
        applyCommonDefaults(batch, name, skipError, keepFiles);
        batch.setSeparator(separator);
        batch.setEncoding(encoding);
        return batchRepository.save(batch);
    }

    @Transactional
    public ImpBatch findById(int batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ObjectNotFoundException("Import batch not found: " + batchId, BaseCode.ID_NOT_EXIST).setId(batchId));
    }

    /**
     * Transitions the batch to a new state, updating the change timestamp. A transition that the
     * batch's current state does not permit is rejected.
     */
    @Transactional
    public void changeState(ImpBatch batch, BatchState newState) {
        // Re-read inside this transaction: the caller may have loaded batch in a different
        // transaction (it is then detached and setter calls on it are not persisted at commit).
        ImpBatch managed = batchRepository.findById(batch.getBatchId())
                .orElseThrow(() -> new ObjectNotFoundException("Import batch not found: " + batch.getBatchId(), BaseCode.ID_NOT_EXIST).setId(batch.getBatchId()));
        BatchState current = managed.getState();
        if (current == newState) {
            return;
        }
        Set<BatchState> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newState)) {
            throw new BusinessException(
                    "Illegal batch state transition: " + current + " -> " + newState,
                    BaseCode.INVALID_STATE);
        }
        OffsetDateTime now = OffsetDateTime.now();
        managed.setState(newState);
        managed.setLastStateChangeAt(now);
        if (newState == BatchState.IN_PROGRESS || newState == BatchState.TEST_IN_PROGRESS) {
            managed.setExecutedAt(now);
            managed.setExecutedByUser(userService.getLoggedUser());
        }
        batchRepository.save(managed);
        logger.info("Batch {} state {} -> {}", managed.getBatchId(), current, newState);
        Integer id = managed.getBatchId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.IMPORT_BATCH_STATE_CHANGE, id));
        } else {
            // Self-invocation from runBatch bypasses the @Transactional proxy; publishEvent needs
            // an active synchronization to register its after-commit hook, so open a short one.
            transactionTemplate.executeWithoutResult(s ->
                    eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.IMPORT_BATCH_STATE_CHANGE, id)));
        }
    }

    /**
     * On startup, walk back state left behind by the previous run: an in-progress import is
     * paused so the user decides whether to resume it, a running dry-run is dropped back to
     * preparation (its progress is not worth keeping), and any item recorded as running becomes
     * ready to be picked up again.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverAfterRestart() {
        List<ImpBatch> stuck = batchRepository.findByStateIn(EnumSet.of(BatchState.IN_PROGRESS, BatchState.TEST_IN_PROGRESS));
        for (ImpBatch batch : stuck) {
            BatchState target = batch.getState() == BatchState.IN_PROGRESS
                    ? BatchState.PAUSED
                    : BatchState.PREPARATION;
            changeState(batch, target);
        }
        int items = itemRepository.resetRunningToReady();
        if (!stuck.isEmpty() || items > 0) {
            logger.info("Import recovery on startup: {} batches settled, {} items reset to READY", stuck.size(), items);
        }
    }

    /**
     * Removes DMS files of terminal batches whose retention window has elapsed. Called from
     * {@link ScheduledCleanupWorker#scheduledCleanup()} - shares one cron with the rest of the
     * housekeeping. Batches marked keep_files=true are left alone. Each batch is processed in its
     * own sub-transaction so a failure on one batch does not roll back the whole sweep.
     */
    public void cleanupOldFiles() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(DATA_RETENTION_DAYS);
        List<Integer> eligibleIds = transactionTemplate.execute(status ->
                batchRepository.findEligibleForCleanup(
                        EnumSet.of(BatchState.FINISHED, BatchState.FAILED, BatchState.CANCELLED),
                        threshold)
                        .stream()
                        .map(ImpBatch::getBatchId)
                        .toList());
        if (eligibleIds == null || eligibleIds.isEmpty()) {
            return;
        }
        int filesRemovedTotal = 0;
        int batchesCleaned = 0;
        for (Integer batchId : eligibleIds) {
            Integer removed;
            try {
                removed = transactionTemplate.execute(status -> cleanupBatchFiles(batchId));
            } catch (RuntimeException ex) {
                logger.warn("Failed to clean up files of batch {}: {}", batchId, ex.getMessage());
                continue;
            }
            if (removed != null) {
                filesRemovedTotal += removed;
                batchesCleaned++;
            }
        }
        logger.info("Import batch retention: cleaned {} batches, removed {} files", batchesCleaned, filesRemovedTotal);
    }

    /**
     * Removes the DMS files of one batch and stamps files_deleted_at. Runs in its own transaction
     * so a failure on a single batch does not roll back the whole retention sweep.
     */
    private int cleanupBatchFiles(int batchId) {
        ImpBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            return 0;
        }
        List<ImpItem> items = itemRepository.findByBatchOrderByExecOrderAsc(batch);
        int removed = 0;
        for (ImpItem item : items) {
            DmsFile file = item.getDmsFile();
            if (file == null) {
                continue;
            }
            try {
                dmsService.deleteFile(file);
            } catch (RuntimeException ex) {
                logger.warn("Failed to delete DMS file {} of batch {}: {}",
                        file.getFileId(), batchId, ex.getMessage());
                continue;
            }
            item.setDmsFile(null);
            itemRepository.save(item);
            removed++;
        }
        batch.setFilesDeletedAt(OffsetDateTime.now());
        batchRepository.save(batch);
        return removed;
    }

    /**
     * Runs the batch. Whether this is a real import or a dry run is read from the batch's current
     * state: the caller (controller) drives the batch into IN_PROGRESS or TEST_IN_PROGRESS before
     * enqueueing, and this runner picks up the appropriate path. Any other state on entry means a
     * pause or a cancel has raced in between - the runner steps aside.
     *
     * Not marked transactional on purpose - per-item transactions are opened through
     * TransactionTemplate and must not be nested inside a wider one.
     */
    public void runBatch(int batchId) {
        ImpBatch batch = findById(batchId);
        BatchState entry = batch.getState();
        boolean dryRun;
        BatchState success;
        if (entry == BatchState.IN_PROGRESS) {
            dryRun = false;
            success = BatchState.FINISHED;
        } else if (entry == BatchState.TEST_IN_PROGRESS) {
            dryRun = true;
            success = BatchState.TEST_FINISHED;
        } else {
            logger.debug("Batch {} entered runner in state {}, skipping", batchId, entry);
            return;
        }

        Set<ItemState> pick = dryRun
                ? EnumSet.of(ItemState.READY, ItemState.ERROR)
                : EnumSet.of(ItemState.READY, ItemState.ERROR, ItemState.TEST_RUN_FINISHED);
        List<ImpItem> items = itemRepository.findByBatchAndStateInOrderByExecOrderAsc(batch, pick);
        boolean hadError = false;

        for (ImpItem item : items) {
            BatchState now = batchRepository.findById(batchId).map(ImpBatch::getState).orElse(null);
            if (now != entry) {
                logger.debug("Batch {} interrupted (state={}), stopping runner", batchId, now);
                return;
            }

            boolean itemFailed;
            try {
                itemFailed = !transactionTemplate.execute(status -> processOneItem(item.getItemId(), dryRun));
            } catch (RuntimeException ex) {
                logger.warn("Import batch {} item {} failed", batchId, item.getItemId(), ex);
                transactionTemplate.executeWithoutResult(status -> markItemError(item.getItemId(), ex));
                itemFailed = true;
            }

            if (itemFailed) {
                hadError = true;
                if (!batch.isSkipError()) {
                    break;
                }
            }
        }

        ImpBatch reread = findById(batchId);
        logger.info("Batch {} runner finished cycle: hadError={}, currentState={}, entry={}",
                batchId, hadError, reread.getState(), entry);
        if (reread.getState() == entry) {
            changeState(reread, hadError ? BatchState.FAILED : success);
        } else {
            logger.info("Batch {} state was changed externally to {}; runner leaves it as is",
                    batchId, reread.getState());
        }
    }

    /**
     * Runs one item. Returns true on success. The kind of batch decides how the file is
     * processed; on a dry run the successful terminal state is TEST_RUN_FINISHED so the item is
     * skipped in subsequent dry runs.
     */
    private boolean processOneItem(int itemId, boolean dryRun) {
        ImpItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ObjectNotFoundException("Import item not found: " + itemId, BaseCode.ID_NOT_EXIST).setId(itemId));
        OffsetDateTime now = OffsetDateTime.now();
        UsrUser me = userService.getLoggedUser();
        item.setState(ItemState.RUNNING);
        item.setExecutedAt(now);
        item.setExecutedByUser(me);
        item.setError(null);
        item.setErrorDetail(null);

        ImpBatch batch = (ImpBatch) Hibernate.unproxy(item.getBatch());
        if (batch instanceof ImpBatchEdx edx) {
            processEdxItem(item, edx, dryRun);
        } else if (batch instanceof ImpBatchDescCsv csv) {
            processCsvItem(item, csv, dryRun);
        } else {
            throw new BusinessException("Unsupported batch type: " + batch.getClass().getSimpleName(), BaseCode.INVALID_STATE);
        }

        // Re-read: the importer may have flushed/evicted the persistence context, leaving `item`
        // detached; setter calls on a detached entity are silently ignored at commit.
        ImpItem managed = itemRepository.findById(itemId)
                .orElseThrow(() -> new ObjectNotFoundException("Import item not found: " + itemId, BaseCode.ID_NOT_EXIST).setId(itemId));
        managed.setState(dryRun ? ItemState.TEST_RUN_FINISHED : ItemState.FINISHED);
        managed.setFinishedAt(OffsetDateTime.now());
        itemRepository.save(managed);
        return true;
    }

    /**
     * Processes one item of an EDX2 batch. Depending on {@code fund_import_strategy}, the payload
     * either creates a new archival file (ALWAYS_NEW), pairs with an existing one (ALWAYS_PAIR),
     * or falls back from pairing to creation (PAIR_AND_CREATE).
     */
    private void processEdxItem(ImpItem item, ImpBatchEdx batch, boolean dryRun) {
        if (batch.getScope() == null) {
            throw new BusinessException(
                    "The batch scope has been removed; the item cannot be imported",
                    BaseCode.INVALID_STATE);
        }
        if (item.getDmsFile() == null) {
            throw new BusinessException(
                    "Source file is no longer stored in DMS",
                    BaseCode.INVALID_STATE);
        }

        ImportPositionParams positionParams = resolvePairing(item, batch);

        DEImportParams params = new DEImportParams(
                batch.getScope().getScopeId(),
                1000,
                10000,
                positionParams,
                batch.isIgnoreRootNodes());

        ImportSummary summary = new ImportSummary();
        params.addImportPhaseChangeListeners(new SummaryCapture(summary));

        try (InputStream in = dmsService.newInputStream(item.getDmsFile())) {
            if (dryRun) {
                deImportService.validateData(in);
                return;
            }
            deImportService.importData(in, params);
        } catch (IOException e) {
            throw new BusinessException("Failed to read the source file: " + e.getMessage(), BaseCode.INVALID_STATE);
        }

        applyImportSummary(item.getItemId(), summary);
    }

    /**
     * Item-level statistics collected while an EDX2 payload is imported. Populated by
     * {@link SummaryCapture}, read once after {@code DEImportService.importData} returns.
     */
    private static final class ImportSummary {
        ArrFund fund;
        ArrChange createChange;
        int nodesCreated;
        int apsCreated;
        int apsPaired;
    }

    /**
     * Bridges the only public hook of {@code DEImportService} (phase-change) to the
     * section-processed listener, which is where the real per-section numbers arrive.
     */
    private static final class SummaryCapture implements ImportPhaseChangeListener {
        private final ImportSummary summary;
        private boolean registered;

        SummaryCapture(ImportSummary summary) {
            this.summary = summary;
        }

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase,
                                     cz.tacr.elza.dataexchange.input.context.ImportContext context) {
            if (!registered) {
                registered = true;
                context.getSections().registerSectionProcessedListener(section -> {
                    if (summary.fund == null) {
                        summary.fund = section.getFund();
                        summary.createChange = section.getCreateChange();
                    }
                    summary.nodesCreated += section.getImportedNodeIds().size();
                });
            }
            if (nextPhase == ImportPhase.FINISHED) {
                for (var info : context.getAccessPoints().getAllAccessPointInfo()) {
                    if (info.getApState() != null) {
                        summary.apsCreated++;
                    } else {
                        summary.apsPaired++;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Persists the numbers and the outcome row for one successfully imported item.
     */
    private void applyImportSummary(int itemId, ImportSummary summary) {
        ImpItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return;
        }
        item.setNodesCreated(summary.nodesCreated);
        item.setApsCreated(summary.apsCreated);
        item.setApsPaired(summary.apsPaired);
        if (summary.fund != null && item.getFund() == null) {
            item.setFund(summary.fund);
        }
        itemRepository.save(item);

        if (summary.fund != null && summary.createChange != null) {
            ImpItemResult result = new ImpItemResult();
            result.setItem(item);
            result.setResultType(ImpResultType.ARR_CHANGE);
            result.setFund(summary.fund);
            result.setFundChange(summary.createChange);
            itemResultRepository.save(result);
        }
    }

    /**
     * Processes one item of a CSV batch: appends description items to nodes identified by UUID
     * in the file. Dry-run is treated as a no-op for now - the format has no XSD to validate.
     */
    private void processCsvItem(ImpItem item, ImpBatchDescCsv batch, boolean dryRun) {
        if (dryRun) {
            return;
        }
        if (item.getDmsFile() == null) {
            throw new BusinessException("Source file is no longer stored in DMS", BaseCode.INVALID_STATE);
        }

        CsvDescItemsImporter.Result result;
        try (InputStream in = dmsService.newInputStream(item.getDmsFile())) {
            result = csvImporter.importCsv(in, batch.getSeparator(), batch.getEncoding());
        } catch (IOException e) {
            throw new BusinessException("Failed to read the source file: " + e.getMessage(), BaseCode.INVALID_STATE);
        }

        ImpItem managed = itemRepository.findById(item.getItemId()).orElse(null);
        if (managed == null) {
            return;
        }
        managed.setNodesUpdated(result.nodesUpdated());
        // for the "open fund" icon in UI pick one representative fund (usually the only one)
        ArrFund firstFund = result.touchedFunds().isEmpty() ? null : result.touchedFunds().iterator().next();
        if (firstFund != null) {
            managed.setFund(firstFund);
        }
        itemRepository.save(managed);

        // record one result row per touched fund so the operator can see every AS the item
        // changed - not only the first one
        for (ArrFund fund : result.touchedFunds()) {
            ImpItemResult r = new ImpItemResult();
            r.setItem(managed);
            r.setResultType(ImpResultType.ARR_CHANGE);
            r.setFund(fund);
            itemResultRepository.save(r);
        }
    }

    /**
     * Determines the import target for an EDX2 item given the batch's fund-import strategy.
     * Returns non-null position params when the item is being imported into an existing fund;
     * null means a new archival file is created.
     */
    private ImportPositionParams resolvePairing(ImpItem item, ImpBatchEdx batch) {
        if (batch.getFundImportStrategy() == FundImportStrategy.ALWAYS_NEW) {
            return null;
        }
        if (batch.getFundPairKey() == null) {
            throw new BusinessException(
                    "Fund pairing is requested but no pairing key is configured",
                    BaseCode.INVALID_STATE);
        }
        // Only one pairing key is defined for now; the check keeps this obvious when more arrive.
        if (batch.getFundPairKey() != FundPairKey.INSTITUTION_AND_FUND_NUMBER) {
            throw new BusinessException(
                    "Unsupported fund pairing key: " + batch.getFundPairKey(),
                    BaseCode.INVALID_STATE);
        }

        EdxHeader.Header header;
        try (InputStream in = dmsService.newInputStream(item.getDmsFile())) {
            header = EdxHeader.readHeader(in);
        } catch (IOException e) {
            throw new BusinessException("Failed to read the source file: " + e.getMessage(), BaseCode.INVALID_STATE);
        }

        ParInstitution institution = institutionRepository.findByInternalCode(header.institutionInternalCode());
        List<ArrFund> matched = institution == null || header.fundNumber() == null
                ? List.of()
                : fundRepository.findByInstitutionAndFundNumber(institution, header.fundNumber());
        logger.info("Pairing lookup: header ic='{}', num={}, institutionFound={}, matchedFunds={}",
                header.institutionInternalCode(), header.fundNumber(),
                institution == null ? "null" : institution.getInstitutionId(),
                matched.size());

        if (matched.isEmpty()) {
            if (batch.getFundImportStrategy() == FundImportStrategy.ALWAYS_PAIR) {
                throw new BusinessException(
                        "No matching archival file found for institution '" + header.institutionInternalCode()
                                + "', fund number " + header.fundNumber(),
                        BaseCode.INVALID_STATE);
            }
            return null; // PAIR_AND_CREATE: fall back to creation
        }
        if (matched.size() > 1) {
            throw new BusinessException(
                    "Fund pairing is ambiguous: " + matched.size() + " matches for institution '"
                            + header.institutionInternalCode() + "', fund number " + header.fundNumber(),
                    BaseCode.INVALID_STATE);
        }

        ArrFund fund = matched.get(0);
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fund.getFundId());
        if (version == null) {
            throw new BusinessException("Matched archival file has no open version", BaseCode.INVALID_STATE);
        }
        item.setFund(fund);

        ImportPositionParams pp = new ImportPositionParams();
        pp.setFundVersionId(version.getFundVersionId());
        pp.setParentNode(ArrNodeVO.valueOf(version.getRootNode()));
        pp.setDirection(ImportDirection.AFTER);
        return pp;
    }

    /**
     * Records an item's failure in its own transaction so the message survives the rollback of
     * the work transaction that threw.
     */
    private void markItemError(int itemId, Throwable error) {
        ImpItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return;
        }
        item.setState(ItemState.ERROR);
        item.setFinishedAt(OffsetDateTime.now());
        item.setError(shortMessage(error));
        item.setErrorDetail(stackTrace(error));
    }

    private static String shortMessage(Throwable t) {
        // The root-cause message is what usually explains the actual failure; the outer wrapper
        // typically adds only "Reading of XML element failed, ..." which alone is not actionable.
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) msg = t.getMessage();
        if (msg == null || msg.isBlank()) msg = t.getClass().getSimpleName();
        return msg.length() > 4000 ? msg.substring(0, 4000) : msg;
    }

    private static String stackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Returns a page of batches ordered newest-first by creation time.
     */
    @Transactional
    public BatchPage list(int from, int count) {
        int size = Math.max(1, count);
        int pageIndex = Math.max(0, from) / size;
        var page = batchRepository.findAll(
                PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new BatchPage(page.getContent(), page.getTotalElements());
    }

    /**
     * Updates the settings of an EDX2 batch. Allowed in PREPARATION; a batch in TEST_FINISHED is
     * walked back to PREPARATION first - the test run's verdict does not extend to settings that
     * changed after it.
     */
    @Transactional
    public ImpBatchEdx updateEdxSettings(ImpBatchEdx batch,
                                         String name,
                                         FundImportStrategy fundImportStrategy,
                                         FundPairKey fundPairKey,
                                         boolean ignoreRootNodes,
                                         ApScope scope,
                                         boolean skipError,
                                         boolean keepFiles) {
        // Re-read inside this transaction so setter calls persist at commit.
        ImpBatchEdx managed = (ImpBatchEdx) batchRepository.findById(batch.getBatchId())
                .orElseThrow(() -> new ObjectNotFoundException("Import batch not found: " + batch.getBatchId(), BaseCode.ID_NOT_EXIST).setId(batch.getBatchId()));
        assertSettingsEditable(managed);
        managed.setName(name);
        managed.setFundImportStrategy(fundImportStrategy);
        managed.setFundPairKey(fundPairKey);
        managed.setIgnoreRootNodes(ignoreRootNodes);
        managed.setScope(scope);
        managed.setSkipError(skipError);
        managed.setKeepFiles(keepFiles);
        return managed;
    }

    /**
     * Updates the settings of a CSV batch. Same PREPARATION / TEST_FINISHED handling as EDX2.
     */
    @Transactional
    public ImpBatchDescCsv updateCsvSettings(ImpBatchDescCsv batch,
                                             String name,
                                             String separator,
                                             String encoding,
                                             boolean skipError,
                                             boolean keepFiles) {
        ImpBatchDescCsv managed = (ImpBatchDescCsv) batchRepository.findById(batch.getBatchId())
                .orElseThrow(() -> new ObjectNotFoundException("Import batch not found: " + batch.getBatchId(), BaseCode.ID_NOT_EXIST).setId(batch.getBatchId()));
        assertSettingsEditable(managed);
        managed.setName(name);
        managed.setSeparator(separator);
        managed.setEncoding(encoding);
        managed.setSkipError(skipError);
        managed.setKeepFiles(keepFiles);
        return managed;
    }

    /**
     * Removes a batch. Allowed for any batch the runner is not walking right now - a batch at rest
     * holds no entry in the asynchronous queue, so there is nothing left to remove it from under.
     * The items and their outcome rows cascade with the batch through FK definitions; the DMS
     * files are shared entities and must be dropped explicitly here.
     */
    @Transactional
    public void delete(ImpBatch batch) {
        ImpBatch managed = batchRepository.findById(batch.getBatchId())
                .orElseThrow(() -> new ObjectNotFoundException("Import batch not found: " + batch.getBatchId(), BaseCode.ID_NOT_EXIST).setId(batch.getBatchId()));
        BatchState s = managed.getState();
        if (s.isRunning()) {
            throw new BusinessException(
                    "A batch in " + s + " cannot be deleted",
                    BaseCode.INVALID_STATE);
        }
        // Bulk-update the DMS references off the items before the batch is dropped: pulling every
        // item back through Hibernate to null the reference triggers a save-cascade over the
        // detached graph and fails on subtype-inherited associations. The physical files are then
        // removed by id.
        List<Integer> dmsFileIds = itemRepository.findDmsFileIdsByBatch(managed);
        itemRepository.clearDmsRefsForBatch(managed);
        for (Integer fileId : dmsFileIds) {
            DmsFile file = dmsService.getFile(fileId);
            if (file == null) {
                continue;
            }
            try {
                dmsService.deleteFile(file);
            } catch (RuntimeException ex) {
                logger.warn("Failed to delete DMS file {} of batch {}: {}",
                        fileId, managed.getBatchId(), ex.getMessage());
            }
        }
        batchRepository.delete(managed);
    }

    private void assertSettingsEditable(ImpBatch batch) {
        if (batch.getState() == BatchState.TEST_FINISHED) {
            changeState(batch, BatchState.PREPARATION);
        }
        if (batch.getState() != BatchState.PREPARATION) {
            throw new BusinessException(
                    "Batch settings can only be edited in PREPARATION; current state: " + batch.getState(),
                    BaseCode.INVALID_STATE);
        }
    }

    private void applyCommonDefaults(ImpBatch batch, String name, boolean skipError, boolean keepFiles) {
        OffsetDateTime now = OffsetDateTime.now();
        UsrUser me = userService.getLoggedUser();
        batch.setName(name);
        batch.setCreatedAt(now);
        batch.setCreatedByUser(me);
        batch.setState(BatchState.PREPARATION);
        batch.setLastStateChangeAt(now);
        batch.setSkipError(skipError);
        batch.setKeepFiles(keepFiles);
    }
}
