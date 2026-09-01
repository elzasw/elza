package cz.tacr.elza.bulkaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BulkActionCode;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.ArrangementInternalService;

/**
 * Pure unit test for the batching loop of {@link BulkActionDFS#run(ArrLevel)}. All collaborators
 * are mocked, so no Spring context or database is needed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkActionDFSBatchTest {

    private static final int FUND_VERSION_ID = 2163;

    private static final int ROOT_NODE_ID = 1;

    /** Batch size of BulkActionDFS, the interesting counts are the ones around it */
    private static final int BATCH_SIZE = 1000;

    @Mock
    private LevelRepository levelRepository;

    @Mock
    private ArrangementInternalService arrInternalService;

    /**
     * Records the levels handed to update(), in order.
     */
    private static class RecordingAction extends BulkActionDFS {

        final List<Integer> updated = new ArrayList<>();

        @Override
        protected void update(ArrLevel level) {
            updated.add(level.getLevelId());
        }

        @Override
        protected void done() {
        }

        @Override
        public String getName() {
            return "RecordingAction";
        }
    }

    private RecordingAction action(List<Integer> levelIds) {
        RecordingAction action = new RecordingAction();

        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        action.tm = tm;
        action.levelRepository = levelRepository;
        action.arrInternalService = arrInternalService;

        ArrFundVersion fundVersion = new ArrFundVersion();
        fundVersion.setFundVersionId(FUND_VERSION_ID);
        ArrBulkActionRun bulkActionRun = new ArrBulkActionRun();
        bulkActionRun.setFundVersion(fundVersion);
        action.runContext = new ActionRunContext(List.of(ROOT_NODE_ID), bulkActionRun);

        // do*().when() rather than when(): the helper re-stubs the same mock for every
        // level count, and when() would invoke the answer left over from the previous one
        doReturn(new ArrayList<>(levelIds)).when(levelRepository)
                .findLevelIdsSubtree(anyInt(), anyInt(), anyInt(), anyBoolean());
        // levels are returned for whatever ids the batch asks for
        doAnswer(inv -> levels(inv.<List<Integer>> getArgument(0))).when(levelRepository)
                .findAllById(any());

        return action;
    }

    private static List<ArrLevel> levels(Iterable<Integer> ids) {
        List<ArrLevel> result = new ArrayList<>();
        for (Integer id : ids) {
            result.add(level(id));
        }
        return result;
    }

    private static ArrLevel level(Integer levelId) {
        ArrLevel level = new ArrLevel();
        level.setLevelId(levelId);
        ArrNode node = new ArrNode();
        node.setNodeId(levelId);
        level.setNode(node);
        return level;
    }

    private static List<Integer> ids(int count) {
        // start well above the Integer cache, like real level ids
        return IntStream.range(0, count).mapToObj(i -> 1_000_000 + i).collect(Collectors.toList());
    }

    private static ArrLevel rootLevel() {
        ArrLevel level = new ArrLevel();
        level.setLevelId(ROOT_NODE_ID);
        ArrNode node = new ArrNode();
        node.setNodeId(ROOT_NODE_ID);
        level.setNode(node);
        return level;
    }

    /**
     * Every level is updated exactly once, in the order the subtree query returned them,
     * whether or not the count is a multiple of the batch size.
     */
    @Test
    void updatesEveryLevelExactlyOnceInOrder() throws InterruptedException {
        for (int count : new int[] { 0, 1, BATCH_SIZE - 1, BATCH_SIZE, BATCH_SIZE + 1, 2 * BATCH_SIZE,
                2 * BATCH_SIZE + 500 }) {
            List<Integer> expected = ids(count);

            RecordingAction action = action(expected);
            action.run(rootLevel());

            assertThat(action.updated)
                    .as("level count %d", count)
                    .containsExactlyElementsOf(expected);
        }
    }

    /**
     * Ids are read before the batch transactions start. A level deleted in between must fail the
     * action instead of being skipped.
     */
    @Test
    void failsWhenLevelWasDeletedBeforeItsBatch() {
        List<Integer> expected = ids(BATCH_SIZE + 10);
        Integer deletedId = expected.get(BATCH_SIZE + 5);

        RecordingAction action = action(expected);
        doAnswer(inv -> {
            List<Integer> batch = new ArrayList<>(inv.<List<Integer>> getArgument(0));
            batch.remove(deletedId);
            return levels(batch);
        }).when(levelRepository).findAllById(any());

        assertThatThrownBy(() -> action.run(rootLevel()))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("no longer exist")
                .extracting(e -> ((SystemException) e).getErrorCode())
                .isEqualTo(BulkActionCode.LEVELS_NOT_FOUND);

        // the batch before the deleted level was processed, the failing one contributed nothing
        assertThat(action.updated).containsExactlyElementsOf(expected.subList(0, BATCH_SIZE));
    }
}
