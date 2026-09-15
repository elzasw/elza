package cz.tacr.elza.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/**
 * The state groups decide what may be done with an import batch, so they are pinned here rather
 * than restated at every call site - the notions of "done" that used to be spelled out inside
 * ImpBatchService disagreed with each other (#9991).
 */
public class BatchStateTest {

    @Test
    void onlyTheTwoRunningStatesAreRunning() {
        assertEquals(EnumSet.of(BatchState.IN_PROGRESS, BatchState.TEST_IN_PROGRESS),
                statesWhere(BatchState::isRunning));
    }

    @Test
    void onlyTheStatesWithoutASuccessorAreFinal() {
        assertEquals(EnumSet.of(BatchState.FINISHED, BatchState.CANCELLED),
                statesWhere(BatchState::isFinal));
    }

    @Test
    void aStateIsNeverBothRunningAndFinal() {
        for (BatchState state : BatchState.values()) {
            assertFalse(state.isRunning() && state.isFinal(), state + " is both running and final");
        }
    }

    /**
     * A batch is deletable exactly when the runner is not walking it - the rule ImpBatchService
     * applies. Spelled out per state so a change to the grouping has to be deliberate.
     */
    @Test
    void everyStateAtRestIsDeletable() {
        for (BatchState state : EnumSet.of(BatchState.PREPARATION, BatchState.TEST_FINISHED,
                                           BatchState.PAUSED, BatchState.FAILED,
                                           BatchState.FINISHED, BatchState.CANCELLED)) {
            assertFalse(state.isRunning(), state + " must be deletable");
        }
        assertTrue(BatchState.IN_PROGRESS.isRunning());
        assertTrue(BatchState.TEST_IN_PROGRESS.isRunning());
    }

    private static Set<BatchState> statesWhere(Predicate<BatchState> p) {
        Set<BatchState> out = EnumSet.noneOf(BatchState.class);
        for (BatchState state : BatchState.values()) {
            if (p.test(state)) {
                out.add(state);
            }
        }
        return out;
    }
}
