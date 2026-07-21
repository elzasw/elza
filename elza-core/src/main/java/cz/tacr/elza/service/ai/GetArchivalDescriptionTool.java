package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ArchivalDescription;
import cz.tacr.elza.aiprovider.client.vo.ArchivalDescriptionDetail;
import cz.tacr.elza.aiprovider.client.vo.GetArchivalDescriptionParams;
import cz.tacr.elza.aiprovider.client.vo.NodeHit;
import cz.tacr.elza.aiprovider.client.vo.NodeWindow;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;

/**
 * Standard {@code getArchivalDescription} tool — fetches one description level
 * in full and/or its surroundings in the arrangement tree, requested by the AI
 * model mid-turn, typically to read the items behind a search hit or to see what
 * a series contains. Argument/result shapes are defined by the AI provider
 * contract ({@code GetArchivalDescriptionParams} / {@code ArchivalDescriptionDetail});
 * design notes: {@code elza-development/typespec-ai/investigation-tools.md}.
 *
 * <p>The level always comes with its complete description items — the same
 * payload the client sends as {@code elza.archivalDescription} context, built by
 * {@link AiContextResolver}. The children/sibling listings are <b>windows</b>
 * over the ordered lists of the per-version tree cache: the window is cut from
 * the cached id list first and only then decorated with titles, so the cost is
 * proportional to the window, never to the level (levels may hold thousands of
 * units).
 *
 * <p>The tool runs on the poller thread, outside the request security context,
 * so it enforces the conversation owner's fund read permission itself
 * ({@code FUND_RD}). A missing level and an unreadable one are answered with the
 * same error, so the model cannot probe which levels exist.
 */
@Component
public class GetArchivalDescriptionTool implements AiTool {

    /** Nodes returned per window (children / siblings), regardless of the requested limit. */
    static final int MAX_WINDOW = 50;

    private final UserService userService;
    private final NodeRepository nodeRepository;
    private final FundVersionRepository fundVersionRepository;
    private final LevelTreeCacheService levelTreeCacheService;
    private final AiContextResolver aiContextResolver;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public GetArchivalDescriptionTool(final UserService userService,
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
        return StandardToolName.GET_ARCHIVAL_DESCRIPTION;
    }

    @Override
    public Object execute(final Object arguments, final AiToolContext context) {
        GetArchivalDescriptionParams params = objectMapper.convertValue(arguments, GetArchivalDescriptionParams.class);
        boolean hasId = params != null && params.getNodeId() != null;
        boolean hasUuid = params != null && StringUtils.isNotBlank(params.getUuid());
        if (hasId == hasUuid) {
            throw new IllegalArgumentException(
                    "getArchivalDescription requires exactly one of nodeId or uuid");
        }
        int windowLimit = params.getLimit() != null && params.getLimit() > 0
                ? Math.min(params.getLimit(), MAX_WINDOW)
                : MAX_WINDOW;

        // The poller thread has no transaction; one transaction for the whole
        // build, so the anchor and the windows read the same version consistently.
        return transactionTemplate.execute(status -> {
            ArrNode node = hasId
                    ? nodeRepository.findById(params.getNodeId()).orElse(null)
                    : nodeRepository.findOneByUuid(params.getUuid().trim());
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
                throw new IllegalArgumentException("Description level not found: "
                        + (hasId ? "nodeId=" + params.getNodeId()
                                 : "uuid=" + params.getUuid().trim()));
            }

            ArchivalDescription data = aiContextResolver.buildArchivalDescription(version, node.getNodeId());
            aiContextResolver.enrichEntityRefs(data);

            ArchivalDescriptionDetail detail = new ArchivalDescriptionDetail()
                    .node(data)
                    .fundId(node.getFundId())
                    .ruleSetCode(version.getRuleSet() != null ? version.getRuleSet().getCode() : null);
            if (Boolean.TRUE.equals(params.getWithParents())) {
                detail.parents(buildParents(treeNode, version));
            }
            if (Boolean.TRUE.equals(params.getWithSiblings())) {
                detail.siblings(buildSiblingsWindow(treeNode, version, params.getSiblingsFrom(), windowLimit));
            }
            if (Boolean.TRUE.equals(params.getWithChildren())) {
                detail.children(buildChildrenWindow(treeNode, version, params.getChildrenFrom(), windowLimit));
            }
            return detail;
        });
    }

    /** The parent chain, root first; never windowed — the depth is small. */
    private List<NodeHit> buildParents(final TreeNode treeNode, final ArrFundVersion version) {
        List<Integer> chain = new ArrayList<>();
        TreeNode parent = treeNode.getParent();
        while (parent != null) {
            chain.add(0, parent.getId());
            parent = parent.getParent();
        }
        return toNodeHits(chain, version);
    }

    /** A window of the direct children, in arrangement order; plain offset. */
    private NodeWindow buildChildrenWindow(final TreeNode treeNode, final ArrFundVersion version,
                                           final Integer childrenFrom, final int windowLimit) {
        List<TreeNode> children = treeNode.getChildren();
        int from = clamp(childrenFrom != null ? childrenFrom : 0, 0, children.size());
        return toWindow(children, from, windowLimit, version);
    }

    /**
     * A window of the siblings (the parent's ordered children; a fund root is its
     * own single sibling). Without an offset the window is centered on the level
     * itself — its neighborhood; the anchor's own index is always reported.
     */
    private NodeWindow buildSiblingsWindow(final TreeNode treeNode, final ArrFundVersion version,
                                           final Integer siblingsFrom, final int windowLimit) {
        TreeNode parent = treeNode.getParent();
        List<TreeNode> siblings = parent == null ? List.of(treeNode) : parent.getChildren();
        int anchorIndex = 0;
        for (int i = 0; i < siblings.size(); i++) {
            if (treeNode.getId().equals(siblings.get(i).getId())) {
                anchorIndex = i;
                break;
            }
        }
        int from = siblingsFrom != null
                ? clamp(siblingsFrom, 0, siblings.size())
                : clamp(anchorIndex - windowLimit / 2, 0, Math.max(0, siblings.size() - windowLimit));
        return toWindow(siblings, from, windowLimit, version).nodeIndex(anchorIndex);
    }

    /**
     * Cuts the window out of the whole ordered list and only then decorates the
     * few windowed nodes with titles — cost proportional to the window, never
     * the list.
     */
    private NodeWindow toWindow(final List<TreeNode> orderedNodes, final int from, final int windowLimit,
                                final ArrFundVersion version) {
        int to = Math.min(from + windowLimit, orderedNodes.size());
        List<Integer> windowIds = orderedNodes.subList(from, to).stream().map(TreeNode::getId).toList();
        return new NodeWindow()
                .nodes(toNodeHits(windowIds, version))
                .from(from)
                .totalCount(orderedNodes.size());
    }

    /** Decorates the given levels with the tree title and reference designation. */
    private List<NodeHit> toNodeHits(final List<Integer> nodeIds, final ArrFundVersion version) {
        if (nodeIds.isEmpty()) {
            return List.of();
        }
        List<NodeHit> hits = new ArrayList<>(nodeIds.size());
        for (TreeNodeVO treeNode : levelTreeCacheService.getNodesByIds(nodeIds, version)) {
            NodeHit hit = new NodeHit()
                    .nodeId(treeNode.getId())
                    .title(treeNode.getName());
            if (treeNode.getReferenceMark() != null) {
                hit.referenceMark(Arrays.asList(treeNode.getReferenceMark()));
            }
            hits.add(hit);
        }
        return hits;
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

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(value, max));
    }
}
