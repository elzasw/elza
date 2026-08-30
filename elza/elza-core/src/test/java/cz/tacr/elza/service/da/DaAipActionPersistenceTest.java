package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;

/**
 * The record of an action against a real database and across separate transactions.
 *
 * An action carried out through the synchronization queue is finished item by item, each in its
 * own transaction on the processor thread, and only the database carries the outcome from one to
 * the next. That is what this exercises: a test holding detached objects in a single heap cannot
 * see a mapping that only fails when Hibernate flushes it.
 */
public class DaAipActionPersistenceTest extends AbstractTest {

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
    private DigitalRepositoryRepository digitalRepositoryRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    private ArrDigitalRepository createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-TEST");
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

    private DaSyncQueueItem queueItem(ArrDigitalRepository repository, DaAip aip) {
        DaSyncQueueItem item = new DaSyncQueueItem();
        item.setCode(aip.getCode());
        item.setAip(aip);
        item.setDigitalRepository(repository);
        item.setState(DaSyncQueueItem.QueueItemState.UPDATE);
        item.setActive(true);
        return syncQueueItemRepository.save(item);
    }

    /**
     * The shared cleanup of the base class does not know the DA tables, and the rows created here
     * would keep the next test from deleting the external systems they point at.
     */
    @AfterEach
    public void deleteCreatedRows() {
        new TransactionTemplate(txManager).executeWithoutResult(t -> {
            actionItemRepository.deleteAll();
            actionRepository.deleteAll();
            syncQueueItemRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    /**
     * The whole life of an action requested through the queue, each step in its own transaction,
     * as it happens when the processor thread finishes the items one by one.
     */
    @Test
    public void actionIsCarriedAcrossSeparateTransactions() {
        Integer[] ids = new Integer[3];

        // 1. the action is opened, as it is by the request of the user
        tx().executeWithoutResult(t -> {
            ArrDigitalRepository repository = createRepository();
            DaAip first = createAip(repository, "aip-1");
            DaAip second = createAip(repository, "aip-2");

            DaAipAction action = actionService.start(DaAipActionType.DOWNLOAD_UPDATE, List.of(first, second));
            ids[0] = action.getAipActionId();

            DaSyncQueueItem firstQueue = queueItem(repository, first);
            DaSyncQueueItem secondQueue = queueItem(repository, second);
            AipOutcomeSink sink = actionService.sinkFor(action);
            sink.enqueued(first.getAipId(), firstQueue);
            sink.enqueued(second.getAipId(), secondQueue);
            ids[1] = firstQueue.getSyncQueueItemId();
            ids[2] = secondQueue.getSyncQueueItemId();
        });

        // nothing is done yet, so the action is outstanding and unstamped
        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            assertEquals(DaAipActionState.WAITING, actionService.stateOf(action));
            assertNull(action.getFinishDate());
        });

        // 2. the first item comes back from the archive, in its own transaction
        tx().executeWithoutResult(t -> {
            DaSyncQueueItem item = syncQueueItemRepository.findById(ids[1]).orElseThrow();
            assertNotNull(item.getAipActionItem(), "the queue item has to carry the action item");
            actionService.completeFromQueue(List.of(item), DaAipActionItemState.FINISHED, null);
        });

        // the action sees the item finished by the previous transaction and is still running
        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            assertEquals(DaAipActionState.RUNNING, actionService.stateOf(action));
            assertNull(action.getFinishDate());
        });

        // 3. the second item fails, again in its own transaction
        tx().executeWithoutResult(t -> {
            DaSyncQueueItem item = syncQueueItemRepository.findById(ids[2]).orElseThrow();
            actionService.completeFromQueue(List.of(item), DaAipActionItemState.ERROR,
                                            "Balicek neobsahuje soubor METS.xml");
        });

        // 4. the action is closed and reports what happened to each AIP
        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            assertEquals(DaAipActionState.ERROR, actionService.stateOf(action));
            assertNotNull(action.getFinishDate(), "the action has to be stamped once nothing is outstanding");

            List<DaAipActionItem> items = actionItemRepository.findByAipActionOrderByAipActionItemId(action);
            assertEquals(2, items.size());
            assertEquals(DaAipActionItemState.FINISHED, items.get(0).getState());
            assertEquals(DaAipActionItemState.ERROR, items.get(1).getState());
            assertEquals("Balicek neobsahuje soubor METS.xml", items.get(1).getMessage());
            assertNotNull(items.get(1).getFinishDate());
        });
    }

    /**
     * A skipped item is the outcome the record exists for: the action applied to nothing and has
     * to survive the round trip through the database saying so.
     */
    @Test
    public void aSkippedItemKeepsItsReason() {
        Integer[] actionId = new Integer[1];

        tx().executeWithoutResult(t -> {
            DaAip aip = createAip(createRepository(), "aip-unresolved");
            DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE, List.of(aip));
            actionId[0] = action.getAipActionId();
            actionService.sinkFor(action)
                    .skipped(aip.getAipId(), "V ELZA neni ulozeny balicek s metadaty.");
        });

        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(actionId[0]).orElseThrow();
            List<DaAipActionItem> items = actionItemRepository.findByAipActionOrderByAipActionItemId(action);
            assertEquals(DaAipActionItemState.SKIPPED, items.get(0).getState());
            assertEquals("V ELZA neni ulozeny balicek s metadaty.", items.get(0).getMessage());
            // a skip is not a failure - the action did everything that could be done
            assertEquals(DaAipActionState.FINISHED, actionService.stateOf(action));
        });
    }

    /**
     * The processing that runs over a downloaded package knows what happened to the individual
     * AIP; the batch that follows only knows the exchange with the archive succeeded. The
     * specific outcome has to survive the batch closing behind it.
     */
    @Test
    public void theBatchDoesNotOverwriteAnOutcomeAlreadyRecorded() {
        Integer[] ids = new Integer[2];

        tx().executeWithoutResult(t -> {
            ArrDigitalRepository repository = createRepository();
            DaAip aip = createAip(repository, "aip-partly-broken");
            DaAipAction action = actionService.start(DaAipActionType.DOWNLOAD_UPDATE, List.of(aip));
            ids[0] = action.getAipActionId();
            DaSyncQueueItem queue = queueItem(repository, aip);
            actionService.sinkFor(action).enqueued(aip.getAipId(), queue);
            ids[1] = queue.getSyncQueueItemId();
        });

        // the rebuild of that one AIP failed, and is recorded before the batch is closed
        tx().executeWithoutResult(t -> {
            DaSyncQueueItem queue = syncQueueItemRepository.findById(ids[1]).orElseThrow();
            actionService.sinkForQueueItems(List.of(queue))
                    .failed(queue.getAip().getAipId(), "Balicek neobsahuje soubor PREMIS.xml");
        });

        // the batch reports success afterwards, as it does when the exchange itself went fine
        tx().executeWithoutResult(t -> {
            DaSyncQueueItem queue = syncQueueItemRepository.findById(ids[1]).orElseThrow();
            actionService.completeFromQueue(List.of(queue), DaAipActionItemState.FINISHED, null);
        });

        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            List<DaAipActionItem> items = actionItemRepository.findByAipActionOrderByAipActionItemId(action);
            assertEquals(DaAipActionItemState.ERROR, items.get(0).getState());
            assertEquals("Balicek neobsahuje soubor PREMIS.xml", items.get(0).getMessage());
            assertEquals(DaAipActionState.ERROR, actionService.stateOf(action));
        });
    }

    /**
     * The processor reads its queue items in one transaction and works with them in later ones, so
     * by the time it asks for a sink the items are detached and their associations cannot be
     * navigated. The sink has to be usable from there, which means reading what it needs itself.
     *
     * Called from outside a transaction on purpose - that is where the processor calls it from,
     * and a test that wraps this in one does not exercise the case at all.
     */
    @Test
    public void aSinkCanBeBuiltFromDetachedQueueItems() {
        Integer[] ids = new Integer[3];

        tx().executeWithoutResult(t -> {
            ArrDigitalRepository repository = createRepository();
            DaAip aip = createAip(repository, "aip-detached");
            DaAipAction action = actionService.start(DaAipActionType.DOWNLOAD_UPDATE, List.of(aip));
            ids[0] = action.getAipActionId();
            ids[1] = aip.getAipId();
            DaSyncQueueItem queue = queueItem(repository, aip);
            actionService.sinkFor(action).enqueued(aip.getAipId(), queue);
            ids[2] = queue.getSyncQueueItemId();
        });

        // detached: read in one transaction, used after it closed
        List<DaSyncQueueItem> detached = tx().execute(t -> List.of(
                syncQueueItemRepository.findById(ids[2]).orElseThrow()));

        AipOutcomeSink sink = actionService.sinkForQueueItems(detached);
        sink.failed(ids[1], "Balicek neobsahuje soubor METS.xml");

        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            List<DaAipActionItem> items = actionItemRepository.findByAipActionOrderByAipActionItemId(action);
            assertEquals(DaAipActionItemState.ERROR, items.get(0).getState());
            assertEquals("Balicek neobsahuje soubor METS.xml", items.get(0).getMessage());
        });
    }

    /** The same for an action held over from the request that opened it. */
    @Test
    public void aSinkCanBeBuiltFromADetachedAction() {
        Integer[] ids = new Integer[2];

        DaAipAction detachedAction = tx().execute(t -> {
            DaAip aip = createAip(createRepository(), "aip-detached-action");
            ids[1] = aip.getAipId();
            DaAipAction action = actionService.start(DaAipActionType.DB_UPDATE, List.of(aip));
            ids[0] = action.getAipActionId();
            return action;
        });

        AipOutcomeSink sink = actionService.sinkFor(detachedAction);
        sink.skipped(ids[1], "V ELZA neni ulozeny balicek s metadaty.");

        tx().executeWithoutResult(t -> {
            DaAipAction action = actionRepository.findById(ids[0]).orElseThrow();
            List<DaAipActionItem> items = actionItemRepository.findByAipActionOrderByAipActionItemId(action);
            assertEquals(DaAipActionItemState.SKIPPED, items.get(0).getState());
            assertEquals(DaAipActionState.FINISHED, actionService.stateOf(action));
        });
    }
}
