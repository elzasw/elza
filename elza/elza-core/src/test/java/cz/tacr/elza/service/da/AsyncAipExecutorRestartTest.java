package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionState;
import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * What a restart does to a step that was being carried out.
 *
 * The step ran in a transaction of its own, so the restart rolled it back and nothing is half
 * written. The AIP can therefore be run again - but that has to be asked for. Starting it again by
 * itself would mean a step which brings the server down brings it down on every start, which is
 * why the other executors of ELZA do not do it either.
 */
public class AsyncAipExecutorRestartTest extends AbstractTest {

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
    private DigitalRepositoryRepository digitalRepositoryRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            asyncRequestRepository.deleteAll();
            actionItemRepository.deleteAll();
            actionRepository.deleteAll();
            syncQueueItemRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private ArrDigitalRepository createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-RESTART");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        return digitalRepositoryRepository.save(repository);
    }

    private DaAip createAip(ArrDigitalRepository repository, String code) {
        DaAip aip = new DaAip();
        aip.setCode(code);
        aip.setDigitalRepository(repository);
        return aipRepository.save(aip);
    }

    /**
     * A step still queued when the server went down is not started again, and the item it was
     * carrying out says why it never finished. Otherwise the action would stay open for good and
     * the user would wait for something nobody is doing.
     */
    @Test
    public void aStepInterruptedByARestartIsReportedAndNotStartedAgain() {
        Integer[] ids = new Integer[2];

        tx().executeWithoutResult(t -> {
            DaAip aip = createAip(createRepository(), "aip-interrupted");
            DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE, List.of(aip));
            ids[0] = action.getAipActionId();
            DaAipActionItem item = action.getItems().get(0);
            ids[1] = item.getAipActionItemId();

            // the state the server went down in: the step was taken, the item is not finished
            item.setState(DaAipActionItemState.RUNNING);
            actionItemRepository.save(item);
            asyncRequestRepository.save(ArrAsyncRequest.create(item, 1, null));
        });

        // what the executor does with the leftover request when it restores the queue
        boolean dropped = tx().execute(t -> {
            ArrAsyncRequest leftover = asyncRequestRepository.findAll().stream()
                    .filter(r -> r.getAipActionItem() != null)
                    .findFirst().orElseThrow();
            return actionService.abandonInterruptedStep(leftover.getAipActionItem());
        });

        assertTrue(dropped, "the leftover step must not be started again");

        tx().executeWithoutResult(t -> {
            DaAipActionItem item = actionItemRepository.findById(ids[1]).orElseThrow();
            assertEquals(DaAipActionItemState.ERROR, item.getState());
            assertEquals("Byl proveden restart serveru", item.getMessage());

            // the action closes, instead of waiting for a step nobody is carrying out
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            assertEquals(DaAipActionState.ERROR, actionService.stateOf(action));
        });
    }

    /**
     * A step that had already finished before the server went down keeps its outcome; the restart
     * must not overwrite a result the user has been given.
     */
    @Test
    public void aStepThatWasAlreadyDoneKeepsItsOutcome() {
        Integer[] ids = new Integer[2];

        tx().executeWithoutResult(t -> {
            DaAip aip = createAip(createRepository(), "aip-done");
            DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE, List.of(aip));
            ids[0] = action.getAipActionId();
            DaAipActionItem item = action.getItems().get(0);
            ids[1] = item.getAipActionItemId();

            item.setState(DaAipActionItemState.SKIPPED);
            item.setMessage("V ELZA neni ulozeny balicek s metadaty.");
            actionItemRepository.save(item);
            asyncRequestRepository.save(ArrAsyncRequest.create(item, 1, null));
        });

        tx().executeWithoutResult(t -> {
            ArrAsyncRequest leftover = asyncRequestRepository.findAll().stream()
                    .filter(r -> r.getAipActionItem() != null)
                    .findFirst().orElseThrow();
            actionService.abandonInterruptedStep(leftover.getAipActionItem());
        });

        tx().executeWithoutResult(t -> {
            DaAipActionItem item = actionItemRepository.findById(ids[1]).orElseThrow();
            assertEquals(DaAipActionItemState.SKIPPED, item.getState());
            assertEquals("V ELZA neni ulozeny balicek s metadaty.", item.getMessage());
            assertNotNull(actionRepository.findById(ids[0]).orElseThrow());
        });
    }
}
