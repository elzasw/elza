package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ArchivalDescription;
import cz.tacr.elza.aiprovider.client.vo.GetArchivalBranchResult;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;

/**
 * The {@code getArchivalBranch} tool: the flat and the deep ordering over the
 * tree cache, the window (offset, requested size, the safety cap), the
 * batched level build, fund read permission
 * enforced from the conversation owner's stored permissions, and the
 * indistinguishable answer for a missing vs. an unreadable level.
 */
class GetArchivalBranchToolTest {

    private static final int FUND_ID = 7;
    private static final int USER_ID = 42;
    private static final int ROOT = 100;

    private final UserService userService = mock(UserService.class);
    private final NodeRepository nodeRepository = mock(NodeRepository.class);
    private final FundVersionRepository fundVersionRepository = mock(FundVersionRepository.class);
    private final LevelTreeCacheService levelTreeCacheService = mock(LevelTreeCacheService.class);
    private final AiContextResolver contextResolver = mock(AiContextResolver.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    private final GetArchivalBranchTool tool = new GetArchivalBranchTool(userService, nodeRepository,
            fundVersionRepository, levelTreeCacheService, contextResolver, transactionTemplate,
            new ObjectMapper());

    private final ArrFundVersion version = new ArrFundVersion();

    @BeforeEach
    void wire() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation
                .<TransactionCallback<Object>>getArgument(0)
                .doInTransaction(mock(TransactionStatus.class)));
        ArrFund fund = new ArrFund();
        fund.setFundId(FUND_ID);
        ArrNode root = new ArrNode();
        root.setNodeId(ROOT);
        root.setFund(fund);
        when(nodeRepository.findById(ROOT)).thenReturn(Optional.of(root));
        when(fundVersionRepository.findByFundIdAndLockChangeIsNull(FUND_ID)).thenReturn(version);

        // root 100 → 1 (→ 11, 12), 2, 3 (→ 31): flat = [1, 2, 3], deep = [1, 11, 12, 2, 3, 31]
        TreeNode rootNode = node(ROOT, 0);
        TreeNode one = node(1, 1);
        one.addChild(node(11, 1));
        one.addChild(node(12, 2));
        TreeNode three = node(3, 3);
        three.addChild(node(31, 1));
        rootNode.addChild(one);
        rootNode.addChild(node(2, 2));
        rootNode.addChild(three);
        Map<Integer, TreeNode> cache = new HashMap<>();
        index(cache, rootNode);
        when(levelTreeCacheService.getVersionTreeCache(version)).thenReturn(cache);

        when(userService.getUserPermissions(USER_ID))
                .thenReturn(Arrays.asList(new UserPermission(Permission.FUND_RD_ALL)));
        // The batch builder is the resolver's job; here each id becomes a bare level.
        when(contextResolver.buildArchivalDescriptions(any(), anyList()))
                .thenAnswer(invocation -> {
                    List<ArchivalDescription> levels = new ArrayList<>();
                    for (Integer id : invocation.<List<Integer>>getArgument(1)) {
                        levels.add(new ArchivalDescription().nodeId(id));
                    }
                    return levels;
                });
    }

    @Test
    void flatViewListsTheDirectChildrenInOrder() {
        GetArchivalBranchResult result = execute(Map.of("nodeId", ROOT, "view", "flat"));

        assertThat(result.getFundId()).isEqualTo(FUND_ID);
        assertThat(result.getFrom()).isZero();
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getNodes()).extracting(ArchivalDescription::getNodeId).containsExactly(1, 2, 3);
    }

    @Test
    void deepViewWalksTheSubtreeDepthFirstWithoutTheRoot() {
        GetArchivalBranchResult result = execute(Map.of("nodeId", ROOT, "view", "deep"));

        assertThat(result.getTotalCount()).isEqualTo(6);
        assertThat(result.getNodes()).extracting(ArchivalDescription::getNodeId)
                .containsExactly(1, 11, 12, 2, 3, 31);
    }

    @Test
    void windowIsCutByOffsetAndRequestedSizeAndReportsTheWholeList() {
        GetArchivalBranchResult result = execute(Map.of("nodeId", ROOT, "view", "deep", "from", 2, "maxNodes", 2));

        assertThat(result.getFrom()).isEqualTo(2);
        assertThat(result.getTotalCount()).isEqualTo(6);
        assertThat(result.getNodes()).extracting(ArchivalDescription::getNodeId).containsExactly(12, 2);
        verify(contextResolver).buildArchivalDescriptions(version, List.of(12, 2));
    }

    @Test
    void requestedSizeIsCappedAtTheSafetyLimit() {
        execute(Map.of("nodeId", ROOT, "view", "deep", "maxNodes", 100_000));

        // Six levels exist, so the window holds all six — but the builder was
        // asked for at most MAX_NODES ids, never the requested hundred thousand.
        verify(contextResolver).buildArchivalDescriptions(version, List.of(1, 11, 12, 2, 3, 31));
        assertThat(GetArchivalBranchTool.MAX_NODES).isEqualTo(200);
    }

    @Test
    void offsetBeyondTheListYieldsAnEmptyWindow() {
        GetArchivalBranchResult result = execute(Map.of("nodeId", ROOT, "view", "flat", "from", 10));

        assertThat(result.getNodes()).isEmpty();
        assertThat(result.getFrom()).isEqualTo(3);
        assertThat(result.getTotalCount()).isEqualTo(3);
    }

    @Test
    void unreadableFundAndMissingLevelAreNotDistinguishable() {
        when(userService.getUserPermissions(USER_ID))
                .thenReturn(Arrays.asList(fundRead(FUND_ID + 1)));
        assertThatThrownBy(() -> execute(Map.of("nodeId", ROOT, "view", "flat")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description level not found: nodeId=" + ROOT);

        when(nodeRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> execute(Map.of("nodeId", 999, "view", "flat")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description level not found: nodeId=999");
    }

    @Test
    void viewIsRequired() {
        assertThatThrownBy(() -> execute(Map.of("nodeId", ROOT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("view");
    }

    private GetArchivalBranchResult execute(final Map<String, Object> arguments) {
        return (GetArchivalBranchResult) tool.execute(arguments, new AiToolContext(USER_ID));
    }

    private static TreeNode node(final int id, final int position) {
        TreeNode node = new TreeNode(id, position);
        node.setChildren(new ArrayList<>());
        return node;
    }

    private static void index(final Map<Integer, TreeNode> cache, final TreeNode node) {
        cache.put(node.getId(), node);
        for (TreeNode child : node.getChildren()) {
            index(cache, child);
        }
    }

    private static UserPermission fundRead(final Integer... fundIds) {
        UserPermission permission = new UserPermission(Permission.FUND_RD);
        permission.getFundIds().addAll(Arrays.asList(fundIds));
        return permission;
    }
}
