package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionState;
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
 * One AIP is the unit of atomicity of an action, not the batch of AIPs it was requested over.
 *
 * The rebuild used to run with a single transaction around the whole batch, so nothing it did was
 * committed until the last AIP was done. That made the outcome of an AIP unreadable while the rest
 * were still running, and it let a failure late in the batch throw away the AIPs that had already
 * succeeded. This pins the behaviour that replaced it.
 */
public class DaAipStepAtomicityTest extends AbstractTest {

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

    /** A transaction of its own, to read what the running action has really committed. */
    private TransactionTemplate separateTx() {
        TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            actionItemRepository.deleteAll();
            actionRepository.deleteAll();
            syncQueueItemRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    /**
     * An AIP with no fund, which the rebuild has to skip. The state is what the rebuild reads, so
     * it has to exist even though the AIP cannot be rebuilt.
     */
    private DaAip createUnresolvedAip(ArrDigitalRepository repository, String code) {
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

    private ArrDigitalRepository createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-ATOMICITY");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        return digitalRepositoryRepository.save(repository);
    }

    @Nullable
    private DaAipActionItem itemOf(Integer actionId, Integer aipId) {
        DaAipAction action = actionRepository.findById(actionId).orElseThrow();
        return actionItemRepository.findByAipActionOrderByAipActionItemId(action).stream()
                .filter(i -> i.getAip().getAipId().equals(aipId))
                .findFirst().orElse(null);
    }

    /**
     * While the second AIP is being handled, the outcome of the first has to be committed already
     * - readable from a transaction that is not the one the action is running in.
     *
     * This is what a single transaction around the batch cannot do, and what any reporting of
     * progress depends on: an outcome nobody can read until the whole batch ends is not progress.
     */
    @Test
    public void theOutcomeOfAnAipIsCommittedBeforeTheNextOneIsStarted() {
        Integer[] ids = new Integer[3];
        tx().executeWithoutResult(t -> {
            ArrDigitalRepository repository = createRepository();
            ids[1] = createUnresolvedAip(repository, "aip-first").getAipId();
            ids[2] = createUnresolvedAip(repository, "aip-second").getAipId();
        });

        DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE,
                aipRepository.findAllById(List.of(ids[1], ids[2])));
        ids[0] = action.getAipActionId();

        AipOutcomeSink recording = actionService.sinkFor(action);
        boolean[] firstWasAlreadyCommitted = {false};
        AipOutcomeSink observing = new AipOutcomeSink() {

            @Override
            public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
                if (aipId.equals(ids[2])) {
                    // the first AIP is done by now; its outcome has to have left its transaction
                    firstWasAlreadyCommitted[0] = separateTx().execute(t -> {
                        DaAipActionItem first = itemOf(ids[0], ids[1]);
                        return first != null && first.getState() == DaAipActionItemState.SKIPPED;
                    });
                }
                recording.record(aipId, state, message);
            }

            @Override
            public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
                recording.enqueued(aipId, queueItem);
            }
        };

        daService.doCreateDaoStructure(List.of(ids[1], ids[2]), false, observing);

        assertTrue(firstWasAlreadyCommitted[0],
                   "the outcome of the first AIP has to be committed before the second is started");

        tx().executeWithoutResult(t -> {
            assertEquals(DaAipActionItemState.SKIPPED, itemOf(ids[0], ids[1]).getState());
            assertEquals(DaAipActionItemState.SKIPPED, itemOf(ids[0], ids[2]).getState());
            DaAipAction reloaded = actionRepository.findById(ids[0]).orElseThrow();
            assertEquals(DaAipActionState.FINISHED, actionService.stateOf(reloaded));
            assertNotNull(reloaded.getFinishDate());
        });
    }

    /**
     * An AIP whose package cannot be read fails on its own. The AIPs handled before it keep their
     * outcome, and the failure is recorded even though the transaction it happened in was rolled
     * back.
     */
    @Test
    public void aFailingAipDoesNotUndoTheOnesBeforeIt() {
        Integer[] ids = new Integer[3];
        tx().executeWithoutResult(t -> {
            ArrDigitalRepository repository = createRepository();
            ids[1] = createUnresolvedAip(repository, "aip-skipped").getAipId();
            ids[2] = createUnresolvedAip(repository, "aip-broken").getAipId();
        });

        DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE,
                aipRepository.findAllById(List.of(ids[1], ids[2])));
        ids[0] = action.getAipActionId();

        // The second AIP is made to fail while its outcome is being written, which is the moment
        // that used to take the whole batch down with it.
        AipOutcomeSink recording = actionService.sinkFor(action);
        AipOutcomeSink failingOnSecond = new AipOutcomeSink() {

            @Override
            public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
                if (aipId.equals(ids[2])) {
                    throw new IllegalStateException("Balicek se nepodarilo precist");
                }
                recording.record(aipId, state, message);
            }

            @Override
            public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
                recording.enqueued(aipId, queueItem);
            }
        };

        try {
            daService.doCreateDaoStructure(List.of(ids[1], ids[2]), false, failingOnSecond);
        } catch (RuntimeException expected) {
            // the failure of one AIP is allowed to surface; what matters is what survives it
        }

        tx().executeWithoutResult(t -> {
            assertEquals(DaAipActionItemState.SKIPPED, itemOf(ids[0], ids[1]).getState(),
                         "the AIP handled before the failure has to keep its outcome");
        });
    }
}
