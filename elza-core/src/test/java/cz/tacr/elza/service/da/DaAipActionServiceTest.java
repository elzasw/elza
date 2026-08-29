package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionState;
import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.service.UserService;

/**
 * An action reports what it did to each AIP it was asked to act on. Its own state is derived
 * from the items rather than stored, because the items are finished independently of each other.
 */
public class DaAipActionServiceTest {

    private DaAipActionService service;

    private DaAipActionItemRepository actionItemRepository;

    @BeforeEach
    void setUp() {
        DaAipActionRepository actionRepository = mock(DaAipActionRepository.class);
        actionItemRepository = mock(DaAipActionItemRepository.class);
        DaSyncQueueItemRepository syncQueueItemRepository = mock(DaSyncQueueItemRepository.class);
        UserService userService = mock(UserService.class);

        when(actionRepository.save(any(DaAipAction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actionItemRepository.save(any(DaAipActionItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userService.getLoggedUser()).thenReturn(null);

        service = new DaAipActionService();
        setField(service, "actionRepository", actionRepository);
        setField(service, "actionItemRepository", actionItemRepository);
        setField(service, "syncQueueItemRepository", syncQueueItemRepository);
        setField(service, "userService", userService);
    }

    private static DaAip aip(int id) {
        DaAip aip = new DaAip();
        aip.setAipId(id);
        aip.setCode("aip-" + id);
        return aip;
    }

    /**
     * The items of the action are what the recomputation of its state reads, so the repository
     * has to answer with them the way a real one would.
     */
    private void itemsAreReadBack(DaAipAction action) {
        when(actionItemRepository.findByAipActionOrderByAipActionItemId(action))
                .thenAnswer(inv -> action.getItems());
    }

    @Test
    void anActionStartsWithEveryAipOutstanding() {
        DaAipAction action = service.start(DaAipActionType.DB_UPDATE, List.of(aip(1), aip(2)));

        assertEquals(2, action.getItems().size());
        assertEquals(DaAipActionItemState.WAITING, action.getItems().get(0).getState());
        assertEquals(DaAipActionState.WAITING, service.stateOf(action));
        assertNull(action.getFinishDate());
    }

    @Test
    void anActionIsRunningWhileAnyAipIsOutstanding() {
        DaAipAction action = service.start(DaAipActionType.DB_UPDATE, List.of(aip(1), aip(2)));
        itemsAreReadBack(action);

        service.sinkFor(action).finished(1);

        assertEquals(DaAipActionState.RUNNING, service.stateOf(action));
        assertNull(action.getFinishDate());
    }

    /**
     * The AIP 9 case: the rebuild has nothing to rebuild from. The action did nothing to that
     * AIP and has to say so, instead of reporting a success that changed nothing.
     */
    @Test
    void aSkippedAipKeepsTheReasonAndDoesNotFailTheAction() {
        DaAipAction action = service.start(DaAipActionType.DB_UPDATE, List.of(aip(1), aip(2)));
        itemsAreReadBack(action);
        AipOutcomeSink sink = service.sinkFor(action);

        sink.finished(1);
        sink.skipped(2, "AIP není navázaný na archivní soubor.");

        DaAipActionItem skipped = action.getItems().get(1);
        assertEquals(DaAipActionItemState.SKIPPED, skipped.getState());
        assertEquals("AIP není navázaný na archivní soubor.", skipped.getMessage());
        assertEquals(DaAipActionState.FINISHED, service.stateOf(action));
        assertNotNull(action.getFinishDate());
    }

    @Test
    void oneFailedAipFailsTheAction() {
        DaAipAction action = service.start(DaAipActionType.DB_UPDATE, List.of(aip(1), aip(2)));
        itemsAreReadBack(action);
        AipOutcomeSink sink = service.sinkFor(action);

        sink.finished(1);
        sink.failed(2, "Balíček neobsahuje soubor METS.xml");

        assertEquals(DaAipActionState.ERROR, service.stateOf(action));
        assertEquals("Balíček neobsahuje soubor METS.xml", action.getItems().get(1).getMessage());
    }

    /** An AIP handed to the digital archive is outstanding until the queue item comes back. */
    @Test
    void anEnqueuedAipIsFinishedByItsQueueItem() {
        DaAipAction action = service.start(DaAipActionType.DOWNLOAD_UPDATE, List.of(aip(1)));
        itemsAreReadBack(action);
        DaSyncQueueItem queueItem = new DaSyncQueueItem();

        service.sinkFor(action).enqueued(1, queueItem);

        assertEquals(DaAipActionItemState.RUNNING, action.getItems().get(0).getState());
        assertEquals(DaAipActionState.WAITING, service.stateOf(action));
        assertEquals(action.getItems().get(0), queueItem.getAipActionItem());

        service.completeFromQueue(List.of(queueItem), DaAipActionItemState.FINISHED, null);

        assertEquals(DaAipActionState.FINISHED, service.stateOf(action));
    }

    /**
     * The processing of the downloaded package knows what happened to the individual AIP; the
     * batch only knows the exchange with the archive succeeded. The specific outcome has to win.
     */
    @Test
    void theBatchDoesNotOverwriteAnOutcomeAlreadyRecorded() {
        DaAipAction action = service.start(DaAipActionType.DOWNLOAD_UPDATE, List.of(aip(1)));
        itemsAreReadBack(action);
        DaSyncQueueItem queueItem = new DaSyncQueueItem();
        service.sinkFor(action).enqueued(1, queueItem);

        service.sinkForQueueItems(List.of(queueItem)).failed(1, "Balíček neobsahuje soubor PREMIS.xml");
        service.completeFromQueue(List.of(queueItem), DaAipActionItemState.FINISHED, null);

        assertEquals(DaAipActionItemState.ERROR, action.getItems().get(0).getState());
        assertEquals("Balíček neobsahuje soubor PREMIS.xml", action.getItems().get(0).getMessage());
        assertEquals(DaAipActionState.ERROR, service.stateOf(action));
    }

    /** A queue item nothing asked for carries no action item and must not blow up. */
    @Test
    void aQueueItemWithoutAnActionIsIgnored() {
        service.completeFromQueue(List.of(new DaSyncQueueItem()), DaAipActionItemState.FINISHED, null);
        service.completeFromQueue(null, DaAipActionItemState.FINISHED, null);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
