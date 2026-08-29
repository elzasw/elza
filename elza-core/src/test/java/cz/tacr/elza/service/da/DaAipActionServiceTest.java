package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionState;
import cz.tacr.elza.domain.DaAipActionItem;

/**
 * The state of an action derived from the states of its items.
 *
 * Only the derivation is tested here, without a database: how an action is recorded and how it
 * survives being carried out one AIP per transaction is the subject of
 * {@link DaAipActionPersistenceTest} and {@link DaAipStepAtomicityTest}, which run against a real
 * one. A test of that behaviour built from mocks holds everything in one heap and would report
 * success for mappings the database rejects.
 */
public class DaAipActionServiceTest {

    private static DaAipActionItem item(DaAipActionItemState state) {
        DaAipActionItem item = new DaAipActionItem();
        item.setState(state);
        return item;
    }

    @Test
    void anActionOfNoItemsIsFinished() {
        assertEquals(DaAipActionState.FINISHED, DaAipActionService.stateOf(List.of()));
    }

    @Test
    void anActionWaitsWhileNothingHasBeenDone() {
        assertEquals(DaAipActionState.WAITING, DaAipActionService.stateOf(List.of(
                item(DaAipActionItemState.WAITING),
                item(DaAipActionItemState.RUNNING))));
    }

    @Test
    void anActionRunsWhileSomeItemsAreDoneAndOthersAreNot() {
        assertEquals(DaAipActionState.RUNNING, DaAipActionService.stateOf(List.of(
                item(DaAipActionItemState.FINISHED),
                item(DaAipActionItemState.RUNNING))));
    }

    @Test
    void anActionIsFinishedWhenNothingIsOutstanding() {
        assertEquals(DaAipActionState.FINISHED, DaAipActionService.stateOf(List.of(
                item(DaAipActionItemState.FINISHED),
                item(DaAipActionItemState.SKIPPED))));
    }

    /** A skip is not a failure - the action did everything that could be done. */
    @Test
    void aSkipDoesNotFailTheAction() {
        assertEquals(DaAipActionState.FINISHED, DaAipActionService.stateOf(List.of(
                item(DaAipActionItemState.SKIPPED))));
    }

    @Test
    void oneFailedItemFailsTheAction() {
        assertEquals(DaAipActionState.ERROR, DaAipActionService.stateOf(List.of(
                item(DaAipActionItemState.FINISHED),
                item(DaAipActionItemState.ERROR),
                item(DaAipActionItemState.SKIPPED))));
    }

    /** A failure does not close an action that still has work outstanding. */
    @Test
    void anActionWithAFailureStillRunsWhileAnythingIsOutstanding() {
        assertEquals(DaAipActionState.RUNNING, DaAipActionService.stateOf(List.of(
                item(DaAipActionItemState.ERROR),
                item(DaAipActionItemState.WAITING))));
    }
}
