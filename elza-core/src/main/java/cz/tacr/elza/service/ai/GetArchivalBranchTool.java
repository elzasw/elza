package cz.tacr.elza.service.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ArchivalDescription;
import cz.tacr.elza.aiprovider.client.vo.BranchView;
import cz.tacr.elza.aiprovider.client.vo.GetArchivalBranchParams;
import cz.tacr.elza.aiprovider.client.vo.GetArchivalBranchResult;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;

/**
 * Standard {@code getArchivalBranch} tool — one window of the levels under a
 * level, every level with its complete description items: the direct children
 * ({@code flat}, the list of units on one level) or the whole subtree
 * ({@code deep}, depth-first in arrangement order). The provider pulls the
 * material of a branch-wide task through it (the branch check of
 * {@code elza.revision}; later branch indexing and overviews) and iterates over
 * it with the model on its own side — this executor only answers the paged
 * download. Argument/result shapes are defined by the AI provider contract
 * ({@code GetArchivalBranchParams} / {@code GetArchivalBranchResult}).
 *
 * <p>{@link #MAX_NODES} is the safety limit: whatever the provider asks for, a
 * window never exceeds it, and the result reports the window position and the
 * true size of the list ({@code from}, {@code totalCount}), so the provider
 * pages on deliberately — a level with thousands of units is never handed over
 * whole. The ordered id list comes from the per-version tree cache; only the
 * windowed levels are loaded (items, titles, issues in batch), so the cost is
 * proportional to the window, never to the branch.
 *
 * <p>Runs on the poller thread, outside the request security context, so it
 * enforces the conversation owner's fund read permission itself
 * ({@code FUND_RD}). A missing level and an unreadable one are answered with
 * the same error, so the model cannot probe which levels exist.
 */
@Component
public class GetArchivalBranchTool implements AiTool {

    /** Levels returned per call at most, regardless of the requested {@code maxNodes}. */
    static final int MAX_NODES = 200;

    private final UserService userService;
    private final NodeRepository nodeRepository;
    private final FundVersionRepository fundVersionRepository;
    private final LevelTreeCacheService levelTreeCacheService;
    private final AiContextResolver aiContextResolver;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public GetArchivalBranchTool(final UserService userService,
                                 final NodeRepository nodeRepository,
                                 final FundVersionRepository fundVersionRepository,
                                 final LevelTreeCacheService levelTreeCacheService,
                                 final AiContextResolver aiContextResolver,
                                 final TransactionTemplate transactionTemplate,
                                 final ObjectMapper objectMapper) {
        this.userService = userService;
        this.nodeRepository = nodeRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.levelTreeCacheService = levelTreeCacheService;
        this.aiContextResolver = aiContextResolver;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public StandardToolName name() {
        return StandardToolName.GET_ARCHIVAL_BRANCH;
    }

    @Override
    public Object execute(final Object arguments, final AiToolContext context) {
        GetArchivalBranchParams params = objectMapper.convertValue(arguments, GetArchivalBranchParams.class);
        if (params == null || params.getNodeId() == null) {
            throw new IllegalArgumentException("getArchivalBranch requires nodeId");
        }
        if (params.getView() == null) {
            throw new IllegalArgumentException("getArchivalBranch requires view (flat | deep)");
        }
        int from = params.getFrom() != null ? Math.max(0, params.getFrom()) : 0;
        int limit = params.getMaxNodes() != null && params.getMaxNodes() > 0
                ? Math.min(params.getMaxNodes(), MAX_NODES)
                : MAX_NODES;

        // The poller thread has no transaction; one transaction for the whole
        // window, so the id list and the loaded levels read the same version.
        return transactionTemplate.execute(status -> {
            ArrNode node = nodeRepository.findById(params.getNodeId()).orElse(null);
            ArrFundVersion version = node == null
                    ? null
                    : fundVersionRepository.findByFundIdAndLockChangeIsNull(node.getFundId());
            TreeNode treeNode = null;
            if (version != null && canReadFund(node.getFundId(), context)) {
                // deleted levels are absent from the open version's tree cache
                treeNode = levelTreeCacheService.getVersionTreeCache(version).get(node.getNodeId());
            }
            if (treeNode == null) {
                // one message for "missing" and "not readable" — existence must not leak
                throw new IllegalArgumentException("Description level not found: nodeId=" + params.getNodeId());
            }

            List<Integer> ordered = params.getView() == BranchView.FLAT
                    ? childIds(treeNode)
                    : subtreeIds(treeNode);
            int start = Math.min(from, ordered.size());
            int end = Math.min(start + limit, ordered.size());
            List<ArchivalDescription> nodes = aiContextResolver.buildArchivalDescriptions(
                    version, ordered.subList(start, end));
            return new GetArchivalBranchResult()
                    .fundId(node.getFundId())
                    .ruleSetCode(version.getRuleSet() != null ? version.getRuleSet().getCode() : null)
                    .nodes(nodes)
                    .from(start)
                    .totalCount(ordered.size());
        });
    }

    /** The direct children of the level, in arrangement order. */
    static List<Integer> childIds(final TreeNode level) {
        List<Integer> ids = new ArrayList<>();
        for (TreeNode child : children(level)) {
            ids.add(child.getId());
        }
        return ids;
    }

    /**
     * The whole subtree below the level, depth-first in arrangement order (the
     * level itself excluded). Iterative — a deep fund must not blow the stack.
     */
    static List<Integer> subtreeIds(final TreeNode level) {
        List<Integer> ids = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        pushReversed(stack, children(level));
        while (!stack.isEmpty()) {
            TreeNode current = stack.pop();
            ids.add(current.getId());
            pushReversed(stack, children(current));
        }
        return ids;
    }

    /** Pushes the children last-first so the first child is popped first (pre-order). */
    private static void pushReversed(final Deque<TreeNode> stack, final List<TreeNode> children) {
        for (int i = children.size() - 1; i >= 0; i--) {
            stack.push(children.get(i));
        }
    }

    private static List<TreeNode> children(final TreeNode level) {
        return level.getChildren() == null ? List.of() : level.getChildren();
    }

    /** True when the conversation owner may read the fund — the {@code searchNodes} rule. */
    private boolean canReadFund(final Integer fundId, final AiToolContext context) {
        if (context.userId() == null) {
            // the virtual admin account: no user row, full permissions
            return true;
        }
        Collection<UserPermission> permissions = userService.getUserPermissions(context.userId());
        return permissions.stream().anyMatch(p -> p.getPermission() == Permission.ADMIN
                || p.getPermission() == Permission.FUND_RD_ALL
                || (p.getPermission() == Permission.FUND_RD && p.getFundIds().contains(fundId)));
    }
}
