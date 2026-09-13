package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import cz.tacr.elza.domain.ArrAsyncRequest;
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
import cz.tacr.elza.repository.ArrAsyncRequestRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * An action is carried out either through the queue of the digital archive or through the
 * asynchronous queue, and both survive a restart. An item with neither left is one nothing will
 * ever report on, and the startup sweep closes it.
 *
 * What the sweep must not touch matters as much as what it closes: a download still waiting in the
 * queue when the server stopped has to be carried on with, not given up on.
 */
public class DaAipActionCarrierSweepTest extends AbstractTest {

    @Autowired
    private DaAipActionService actionService;
    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;
    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private ArrAsyncRequestRepository asyncRequestRepository;
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
            asyncRequestRepository.deleteAll();
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
        repository.setCode("DA-CARRIER-SWEEP");
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

    /** A queue item carrying the given action item, as enqueueing leaves it. */
    private void carryByQueueItem(DaAipActionItem item, DaAip aip, DaSyncQueueItem.QueueItemState state,
                                  boolean active) {
        DaSyncQueueItem queueItem = new DaSyncQueueItem();
        queueItem.setCode(aip.getCode());
        queueItem.setAip(aip);
        queueItem.setDigitalRepository(aip.getDigitalRepository());
        queueItem.setAipVersion("1");
        queueItem.setState(state);
        queueItem.setAipType(AipType.METADATA_BASE);
        queueItem.setActive(active);
        queueItem.setDate(OffsetDateTime.now());
        queueItem.setAipActionItem(item);
        syncQueueItemRepository.save(queueItem);
    }

    private DaAipActionItem onlyItemOf(DaAipAction action) {
        return actionItemRepository.findByAipActionOrderByAipActionItemId(action).get(0);
    }

    private DaAipActionItemState stateOf(Integer itemId) {
        return tx().execute(t -> actionItemRepository.findById(itemId).orElseThrow().getState());
    }

    @Test
    public void anItemNothingCanCarryOutIsGivenUpOn() {
        Integer itemId = tx().execute(t -> {
            DaAip aip = createAip(createRepository(), "aip-no-carrier");
            DaAipAction action = actionService.start(DaAipActionType.LOAD_METADATA, List.of(aip));
            return onlyItemOf(action).getAipActionItemId();
        });

        int swept = tx().execute(t -> actionService.abandonItemsWithoutCarrier());
        assertEquals(1, swept);
        assertEquals(DaAipActionItemState.ERROR, stateOf(itemId));
    }

    /**
     * The download was requested and is still waiting in the queue; the processor picks it up after
     * the restart, so giving up on it here would throw away work that is going to happen.
     */
    @Test
    public void anItemStillWaitingInTheQueueIsLeftAlone() {
        Integer itemId = tx().execute(t -> {
            ArrDigitalRepository repository = createRepository();
            DaAip aip = createAip(repository, "aip-pending");
            DaAipAction action = actionService.start(DaAipActionType.LOAD_METADATA, List.of(aip));
            DaAipActionItem item = onlyItemOf(action);
            carryByQueueItem(item, aip, DaSyncQueueItem.QueueItemState.UPDATE, true);
            return item.getAipActionItemId();
        });

        int swept = tx().execute(t -> actionService.abandonItemsWithoutCarrier());
        assertEquals(0, swept);
        assertEquals(DaAipActionItemState.WAITING, stateOf(itemId));
    }

    /** A queue item that was superseded is never read again, so it carries nothing. */
    @Test
    public void anItemWhoseQueueItemWasDeactivatedIsGivenUpOn() {
        Integer itemId = tx().execute(t -> {
            ArrDigitalRepository repository = createRepository();
            DaAip aip = createAip(repository, "aip-superseded");
            DaAipAction action = actionService.start(DaAipActionType.LOAD_METADATA, List.of(aip));
            DaAipActionItem item = onlyItemOf(action);
            carryByQueueItem(item, aip, DaSyncQueueItem.QueueItemState.UPDATE, false);
            return item.getAipActionItemId();
        });

        int swept = tx().execute(t -> actionService.abandonItemsWithoutCarrier());
        assertEquals(1, swept);
        assertEquals(DaAipActionItemState.ERROR, stateOf(itemId));
    }

    /**
     * A step still queued asynchronously has its own recovery - the executor abandons it when it
     * restores the queue - so the sweep must not take it first.
     */
    @Test
    public void anItemQueuedAsynchronouslyIsLeftToItsOwnRecovery() {
        Integer itemId = tx().execute(t -> {
            DaAip aip = createAip(createRepository(), "aip-async");
            DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE, List.of(aip));
            DaAipActionItem item = onlyItemOf(action);
            asyncRequestRepository.save(ArrAsyncRequest.create(item, 1, null));
            return item.getAipActionItemId();
        });

        int swept = tx().execute(t -> actionService.abandonItemsWithoutCarrier());
        assertEquals(0, swept);
        assertEquals(DaAipActionItemState.WAITING, stateOf(itemId));
    }
}
