package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.aiprovider.client.vo.AiObject;
import cz.tacr.elza.aiprovider.client.vo.ArchivalDescription;
import cz.tacr.elza.aiprovider.client.vo.ArchivalDescriptionObject;
import cz.tacr.elza.aiprovider.client.vo.DataType;
import cz.tacr.elza.aiprovider.client.vo.DescriptionItem;
import cz.tacr.elza.aiprovider.client.vo.FundInfo;
import cz.tacr.elza.aiprovider.client.vo.FundInfoObject;
import cz.tacr.elza.aiprovider.client.vo.InstitutionInfo;
import cz.tacr.elza.aiprovider.client.vo.NodeIssue;
import cz.tacr.elza.aiprovider.client.vo.NodeIssueKind;
import cz.tacr.elza.aiprovider.client.vo.ObjectType;
import cz.tacr.elza.controller.vo.AiContextFundVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextObjectVO;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.UserService;

/**
 * Resolves the UI's typed context objects ({@code AiContextObject}) into the
 * provider's typed objects ({@code AiObject}) that travel with a task, reading
 * the referenced domain data from the DB.
 *
 * <ul>
 *   <li>{@code AiContextFund} → an {@code elza.fundInfo} (built from {@link ArrFund}
 *       plus the fund's rule-set code).</li>
 *   <li>{@code AiContextNode} → an {@code elza.archivalDescription} of the level
 *       (its items as stable codes + display text, reference mark, depth, parent,
 *       …) plus, for the {@code context} role, its ancestors up to the root and
 *       the fund's {@code elza.fundInfo}.</li>
 * </ul>
 *
 * <p>Two entry points serve the two roles: {@link #resolvePrimary} yields the one
 * object a task parameter expects, {@link #resolveAll} yields the primary object
 * plus supporting context (ancestors, fund), deduplicated. Read permission
 * ({@code FUND_RD}) on the target fund is enforced; anything the user cannot read
 * or that cannot be mapped is skipped (logged), and the request goes out with
 * whatever could be resolved.
 *
 * <p>v1 resolves against the fund's <b>open</b> version; {@code fundVersionId} on
 * a node context is reserved for later locked-version support.
 */
@Service
public class AiContextResolver {

    private static final Logger logger = LoggerFactory.getLogger(AiContextResolver.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeConformityRepository nodeConformityRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private UserService userService;

    /**
     * Resolves the context objects into provider objects for the {@code context}
     * role: each fund/node plus its supporting objects (a node contributes its
     * level, its ancestors, and its fund). Fund info is sent once per fund and a
     * level once per node.
     */
    public List<AiObject> resolveAll(final List<AiContextObjectVO> contextObjects) {
        List<AiObject> resolved = new ArrayList<>();
        if (contextObjects == null) {
            return resolved;
        }
        Set<Integer> fundInfoAdded = new HashSet<>();
        Set<Integer> levelAdded = new HashSet<>();
        for (AiContextObjectVO ctx : contextObjects) {
            if (ctx instanceof AiContextFundVO fund) {
                addFundInfo(resolved, fundInfoAdded, fund.getFundId());
            } else if (ctx instanceof AiContextNodeVO node) {
                addNode(resolved, fundInfoAdded, levelAdded, node);
            } else {
                logger.info("AI context object {} is not resolvable to a provider object yet",
                        ctx == null ? null : ctx.getClass().getSimpleName());
            }
        }
        return resolved;
    }

    /**
     * Resolves one context object to its single primary provider object — a fund
     * to its {@code elza.fundInfo}, a node to its own {@code elza.archivalDescription}
     * (no ancestors, no fund). Used to match a supplied parameter to the task's
     * declared parameter by object type.
     */
    public Optional<AiObject> resolvePrimary(final AiContextObjectVO ctx) {
        if (ctx instanceof AiContextFundVO fund) {
            return resolveFundInfo(fund.getFundId());
        }
        if (ctx instanceof AiContextNodeVO node) {
            ArrFundVersion version = resolveReadableOpenVersion(node.getFundId());
            if (version == null || node.getNodeId() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(buildArchivalDescription(version, node.getNodeId(), true));
        }
        logger.info("AI context object {} is not resolvable to a provider object yet",
                ctx == null ? null : ctx.getClass().getSimpleName());
        return Optional.empty();
    }

    /** Adds a node's level, its ancestors (root-ward) and its fund, deduplicated. */
    private void addNode(final List<AiObject> resolved, final Set<Integer> fundInfoAdded,
                         final Set<Integer> levelAdded, final AiContextNodeVO node) {
        ArrFundVersion version = resolveReadableOpenVersion(node.getFundId());
        if (version == null || node.getNodeId() == null) {
            return;
        }
        Map<Integer, TreeNode> treeMap = levelTreeCacheService.getVersionTreeCache(version);
        if (treeMap.get(node.getNodeId()) == null) {
            logger.info("AI context node {} not found in fund {} open version; skipped",
                    node.getNodeId(), node.getFundId());
            return;
        }
        // The active level plus its ancestors up to the root, skipping levels
        // already added by an earlier context object.
        List<Integer> chain = new ArrayList<>();
        chain.add(node.getNodeId());
        chain.addAll(levelTreeCacheService.getParentNodes(version, node.getNodeId()));
        List<Integer> toBuild = chain.stream().filter(levelAdded::add).toList();

        if (!toBuild.isEmpty()) {
            StaticDataProvider sdp = staticDataService.getData();
            Map<Integer, List<ArrDescItem>> itemsByNode = loadItems(version, toBuild);
            Map<Integer, ArrNode> nodesById = nodeRepository.findAllById(toBuild).stream()
                    .collect(Collectors.toMap(ArrNode::getNodeId, n -> n));
            Map<Integer, String> titlesByNode = loadTitles(version, toBuild);
            Map<Integer, List<NodeIssue>> issuesByNode = loadIssues(version, toBuild, sdp);
            for (Integer nodeId : toBuild) {
                AiObject level = buildArchivalDescription(nodeId, nodeId.equals(node.getNodeId()),
                        nodesById.get(nodeId), treeMap.get(nodeId),
                        itemsByNode.getOrDefault(nodeId, List.of()), sdp,
                        titlesByNode.get(nodeId), issuesByNode.getOrDefault(nodeId, List.of()));
                if (level != null) {
                    resolved.add(level);
                }
            }
        }
        addFundInfo(resolved, fundInfoAdded, node.getFundId());
    }

    /** Builds one level's {@code elza.archivalDescription}, loading its own pieces. */
    private AiObject buildArchivalDescription(final ArrFundVersion version, final Integer nodeId,
                                              final boolean focus) {
        TreeNode treeNode = levelTreeCacheService.getVersionTreeCache(version).get(nodeId);
        if (treeNode == null) {
            logger.info("AI context node {} not found in fund {} open version; skipped",
                    nodeId, version.getFundId());
            return null;
        }
        ArrNode node = nodeRepository.findById(nodeId).orElse(null);
        StaticDataProvider sdp = staticDataService.getData();
        List<ArrDescItem> items = loadItems(version, List.of(nodeId)).getOrDefault(nodeId, List.of());
        String title = loadTitles(version, List.of(nodeId)).get(nodeId);
        List<NodeIssue> issues = loadIssues(version, List.of(nodeId), sdp).getOrDefault(nodeId, List.of());
        return buildArchivalDescription(nodeId, focus, node, treeNode, items, sdp, title, issues);
    }

    /** Maps a level's already-loaded pieces to an {@code elza.archivalDescription} object. */
    private AiObject buildArchivalDescription(final Integer nodeId, final boolean focus,
                                              final ArrNode node, final TreeNode treeNode,
                                              final List<ArrDescItem> items, final StaticDataProvider sdp,
                                              final String title, final List<NodeIssue> issues) {
        ArchivalDescription data = new ArchivalDescription().nodeId(nodeId);
        if (focus) {
            data.focus(true);
        }
        if (node != null) {
            data.uuid(node.getUuid());
        }
        if (treeNode != null) {
            data.depth(treeNode.getDepth());
            if (treeNode.getParent() != null) {
                data.parentId(treeNode.getParent().getId());
            }
            data.hasChildren(!treeNode.getChildren().isEmpty());
            data.referenceMark(toReferenceMark(treeNode.getReferenceMark()));
        }
        if (title != null && !title.isEmpty()) {
            data.title(title);
        }
        if (issues != null && !issues.isEmpty()) {
            data.issues(issues);
        }
        data.items(items.stream()
                .map(item -> toDescriptionItem(item, sdp))
                .filter(Objects::nonNull)
                .toList());
        return new ArchivalDescriptionObject()
                .objectType(ObjectType.ELZA_ARCHIVAL_DESCRIPTION)
                .data(data);
    }

    /** Maps one description item to stable codes plus its display text. */
    private DescriptionItem toDescriptionItem(final ArrDescItem item, final StaticDataProvider sdp) {
        ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
        if (itemType == null) {
            return null;
        }
        DescriptionItem out = new DescriptionItem().type(itemType.getCode());
        if (itemType.getDataType() != null) {
            out.dataType(DataType.fromValue(itemType.getDataType().getCode()));
        }
        if (item.getItemSpecId() != null) {
            RulItemSpec spec = sdp.getItemSpecById(item.getItemSpecId());
            if (spec != null) {
                out.spec(spec.getCode());
            }
        }
        // Same renderer fulltext indexing uses: enum → spec name, record-ref →
        // entity name, structured → rendered value; null for coordinates/tables.
        String value = item.getFulltextValue();
        if (value != null && !value.isEmpty()) {
            out.value(value);
        }
        // Reference items carry a machine-readable id alongside the display value.
        ArrData data = item.getData();
        if (data instanceof ArrDataRecordRef recordRef) {
            out.accessPointId(recordRef.getRecordId());
        } else if (data instanceof ArrDataStructureRef structRef) {
            out.structuredObjectId(structRef.getStructuredObjectId());
            ArrStructuredObject structObj = structRef.getStructuredObject();
            if (structObj != null) {
                StructType structType = sdp.getStructuredTypeById(structObj.getStructuredTypeId());
                if (structType != null) {
                    out.structuredObjectType(structType.getCode());
                }
                String complement = structObj.getComplement();
                if (complement != null && !complement.isEmpty()) {
                    out.complement(complement);
                }
            }
        }
        return out;
    }

    private List<String> toReferenceMark(final Integer[] referenceMark) {
        if (referenceMark == null) {
            return null;
        }
        return Arrays.stream(referenceMark).map(String::valueOf).toList();
    }

    /** Loads the description items (with values) of the given nodes in the open version. */
    private Map<Integer, List<ArrDescItem>> loadItems(final ArrFundVersion version, final List<Integer> nodeIds) {
        return descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(nodeIds).stream()
                .collect(Collectors.groupingBy(ArrDescItem::getNodeId));
    }

    /**
     * Loads the tree display title of each node — the same value shown in the
     * arrangement tree (built from the fund's configured title items).
     */
    private Map<Integer, String> loadTitles(final ArrFundVersion version, final List<Integer> nodeIds) {
        Map<Integer, String> titles = new HashMap<>();
        for (TreeNodeVO treeNode : levelTreeCacheService.getNodesByIds(nodeIds, version)) {
            if (treeNode.getName() != null) {
                titles.put(treeNode.getId(), treeNode.getName());
            }
        }
        return titles;
    }

    /**
     * Loads the problems found by automatic checks (node conformity) for the given
     * nodes in the open version, grouped by node id. Only levels in the error state
     * contribute; a valid or not-yet-validated level has no entry.
     */
    private Map<Integer, List<NodeIssue>> loadIssues(final ArrFundVersion version, final List<Integer> nodeIds,
                                                     final StaticDataProvider sdp) {
        List<ArrNodeConformity> conformities = nodeConformityRepository.findByNodeIdsAndFundVersion(nodeIds, version);
        if (conformities.isEmpty()) {
            return Map.of();
        }
        conformities = nodeConformityRepository.fetchErrorAndMissingConformity(conformities, version,
                ArrNodeConformity.State.ERR);
        Map<Integer, List<NodeIssue>> result = new HashMap<>();
        for (ArrNodeConformity conformity : conformities) {
            List<NodeIssue> issues = toIssues(conformity, sdp);
            if (!issues.isEmpty()) {
                result.put(conformity.getNodeId(), issues);
            }
        }
        return result;
    }

    /** Maps one level's conformity errors and missing items to {@link NodeIssue}s. */
    private List<NodeIssue> toIssues(final ArrNodeConformity conformity, final StaticDataProvider sdp) {
        List<NodeIssue> issues = new ArrayList<>();
        for (ArrNodeConformityError error : conformity.getErrorConformity()) {
            NodeIssue issue = new NodeIssue().kind(NodeIssueKind.INVALID_VALUE)
                    .description(error.getDescription());
            if (error.getPolicyType() != null) {
                issue.policyType(error.getPolicyType().getCode());
            }
            // The item type/spec of the invalid value, so the AI can tie it to an item.
            ArrDescItem descItem = error.getDescItem();
            if (descItem != null) {
                ItemType itemType = sdp.getItemTypeById(descItem.getItemTypeId());
                if (itemType != null) {
                    issue.itemType(itemType.getCode());
                }
                setSpecCode(issue, descItem.getItemSpecId(), sdp);
            }
            issues.add(issue);
        }
        for (ArrNodeConformityMissing missing : conformity.getMissingConformity()) {
            NodeIssue issue = new NodeIssue().kind(NodeIssueKind.MISSING)
                    .description(missing.getDescription());
            if (missing.getPolicyType() != null) {
                issue.policyType(missing.getPolicyType().getCode());
            }
            ItemType itemType = sdp.getItemTypeById(missing.getItemTypeId());
            if (itemType != null) {
                issue.itemType(itemType.getCode());
            }
            setSpecCode(issue, missing.getItemSpecId(), sdp);
            issues.add(issue);
        }
        return issues;
    }

    /** Sets the issue's spec code from a spec id, when the id resolves to a spec. */
    private void setSpecCode(final NodeIssue issue, final Integer itemSpecId, final StaticDataProvider sdp) {
        if (itemSpecId == null) {
            return;
        }
        RulItemSpec spec = sdp.getItemSpecById(itemSpecId);
        if (spec != null) {
            issue.spec(spec.getCode());
        }
    }

    /**
     * Adds an {@code elza.fundInfo} object for the given fund, unless one for that
     * fund was already added. A missing, unreadable or unknown fund is skipped.
     */
    private void addFundInfo(final List<AiObject> resolved, final Set<Integer> fundInfoAdded,
                             final Integer fundId) {
        if (fundId == null || !fundInfoAdded.add(fundId)) {
            return;
        }
        resolveFundInfo(fundId).ifPresent(resolved::add);
    }

    /** Builds the {@code elza.fundInfo} for a fund, enforcing read permission. */
    private Optional<AiObject> resolveFundInfo(final Integer fundId) {
        if (fundId == null) {
            return Optional.empty();
        }
        ArrFund fund = fundRepository.findById(fundId).orElse(null);
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        if (fund == null || version == null) {
            logger.info("AI context fund {} not found; skipped", fundId);
            return Optional.empty();
        }
        if (!canRead(version)) {
            logger.info("AI context fund {} not readable by user; skipped", fundId);
            return Optional.empty();
        }
        FundInfo info = new FundInfo()
                .name(fund.getName())
                .internalCode(fund.getInternalCode())
                .ruleSetCode(version.getRuleSet() != null ? version.getRuleSet().getCode() : null)
                .fundNumber(fund.getFundNumber())
                .mark(fund.getMark())
                .unitDate(fund.getUnitdate());
        if (fund.getInstitution() != null) {
            info.institution(toInstitutionInfo(fund.getInstitution()));
        }
        return Optional.of(new FundInfoObject().objectType(ObjectType.ELZA_FUND_INFO).data(info));
    }

    /** Resolves a fund's open version, or {@code null} when missing or not readable. */
    private ArrFundVersion resolveReadableOpenVersion(final Integer fundId) {
        if (fundId == null) {
            return null;
        }
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        if (version == null) {
            logger.info("AI context fund {} has no open version; skipped", fundId);
            return null;
        }
        if (!canRead(version)) {
            logger.info("AI context fund {} not readable by user; skipped", fundId);
            return null;
        }
        return version;
    }

    private boolean canRead(final ArrFundVersion version) {
        return AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_RD_ALL)
                .or(Permission.FUND_RD, version)
                .matches(userService.getLoggedUserDetail());
    }

    private InstitutionInfo toInstitutionInfo(final ParInstitution institution) {
        InstitutionInfo info = new InstitutionInfo().code(institution.getInternalCode());
        if (institution.getAccessPointId() != null) {
            // The institution's name is the preferred (display) name of its access point.
            ApIndex preferredName = accessPointService.findPreferredPartIndex(institution.getAccessPointId());
            if (preferredName != null) {
                info.name(preferredName.getIndexValue());
            }
        }
        return info;
    }
}
