package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * A request being queued replaces the ones already waiting for the same AIP, and the processors read
 * active queue items only - so the replaced one is never carried out. The action it was carrying has
 * to end with it; otherwise the user waits for an outcome that can no longer come.
 *
 * The two requests are made in separate transactions, as they are in practice: the replaced queue
 * item is written by the request before it.
 */
public class DaSyncQueueSupersedeTest extends AbstractTest {

    private static final String SUPERSEDED = "Požadavek nahradil novější požadavek na tentýž AIP.";

    @Autowired
    private DaService daService;
    @Autowired
    private DaAipActionService actionService;
    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;
    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            syncQueueItemRepository.deleteAll();
            actionItemRepository.deleteAll();
            actionRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private ArrDigitalRepository createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-SUPERSEDE");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        return digitalRepositoryRepository.save(repository);
    }

    private DaAip createAip(ArrDigitalRepository repository, String code) {
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
    }

    /**
     * A request of a user: an action over the AIP and the queue item carrying it out.
     *
     * @return ids of the action item and of the queue item
     */
    private Integer[] request(Integer aipId, DaSyncQueueItem.QueueItemState state, AipType aipType) {
        return tx().execute(t -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            DaAipAction action = actionService.start(DaAipActionType.LOAD_METADATA, List.of(aip));
            AipOutcomeSink sink = actionService.sinkFor(action);
            DaSyncQueueItem queueItem = daService.createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(),
                                                                      state, "1", aipType, true);
            sink.enqueued(aipId, queueItem);
            return new Integer[] { queueItem.getAipActionItem().getAipActionItemId(), queueItem.getSyncQueueItemId() };
        });
    }

    /** A newer request over the same AIP, as another user or the synchronization makes it. */
    private void queueAgain(Integer aipId, DaSyncQueueItem.QueueItemState state, AipType aipType) {
        tx().executeWithoutResult(t -> {
            DaAip aip = aipRepository.findById(aipId).orElseThrow();
            daService.createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), state, "1", aipType, true);
        });
    }

    private DaAipActionItem reload(Integer itemId) {
        return tx().execute(t -> actionItemRepository.findById(itemId).orElseThrow());
    }

    private Integer createAipInOwnTransaction(String code) {
        return tx().execute(t -> createAip(createRepository(), code).getAipId());
    }

    @Test
    public void theReplacedRequestIsReportedAsReplaced() {
        Integer aipId = createAipInOwnTransaction("aip-supersede");
        Integer[] first = request(aipId, DaSyncQueueItem.QueueItemState.UPDATE, AipType.METADATA_BASE);

        queueAgain(aipId, DaSyncQueueItem.QueueItemState.UPDATE, AipType.AIP_BASE);

        DaAipActionItem item = reload(first[0]);
        assertEquals(DaAipActionItemState.SKIPPED, item.getState(),
                     "akce nahrazeného požadavku musí skončit, ne čekat");
        assertEquals(SUPERSEDED, item.getMessage());
        boolean stillActive = tx().execute(t -> syncQueueItemRepository.findById(first[1]).orElseThrow().getActive());
        assertFalse(stillActive, "nahrazená položka fronty se už nezpracuje");
    }

    /** An item that is already done keeps its outcome - it was carried out, not replaced. */
    @Test
    public void anItemThatIsAlreadyDoneKeepsItsOutcome() {
        Integer aipId = createAipInOwnTransaction("aip-done");
        Integer[] first = request(aipId, DaSyncQueueItem.QueueItemState.UPDATE, AipType.METADATA_BASE);
        tx().executeWithoutResult(t -> actionService.recordOutcome(first[0], DaAipActionItemState.FINISHED, null));

        queueAgain(aipId, DaSyncQueueItem.QueueItemState.UPDATE, AipType.AIP_BASE);

        assertEquals(DaAipActionItemState.FINISHED, reload(first[0]).getState());
    }

    /**
     * Downloads and exports are separate queues; a download must not end an export of the same AIP
     * that is still waiting to be sent.
     */
    @Test
    public void aDownloadDoesNotEndAWaitingExport() {
        Integer aipId = createAipInOwnTransaction("aip-export");
        Integer[] export = request(aipId, DaSyncQueueItem.QueueItemState.EXPORT_NEW, AipType.METADATA_BASE);

        queueAgain(aipId, DaSyncQueueItem.QueueItemState.UPDATE, AipType.METADATA_BASE);

        assertEquals(DaAipActionItemState.RUNNING, reload(export[0]).getState(),
                     "export běží dál, stažení ho nenahrazuje");
    }
}
