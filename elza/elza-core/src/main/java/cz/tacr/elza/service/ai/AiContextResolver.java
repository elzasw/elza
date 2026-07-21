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
import cz.tacr.elza.aiprovider.client.vo.ArchivalOutline;
import cz.tacr.elza.aiprovider.client.vo.ArchivalOutlineObject;
import cz.tacr.elza.aiprovider.client.vo.OutlineRow;
import cz.tacr.elza.aiprovider.client.vo.ArchivalEntity;
import cz.tacr.elza.aiprovider.client.vo.ArchivalEntityInfo;
import cz.tacr.elza.aiprovider.client.vo.ArchivalEntityObject;
import cz.tacr.elza.aiprovider.client.vo.DataType;
import cz.tacr.elza.aiprovider.client.vo.DescriptionItem;
import cz.tacr.elza.aiprovider.client.vo.EntityPart;
import cz.tacr.elza.aiprovider.client.vo.FundInfo;
import cz.tacr.elza.aiprovider.client.vo.FundInfoObject;
import cz.tacr.elza.aiprovider.client.vo.InstitutionInfo;
import cz.tacr.elza.aiprovider.client.vo.NodeIssue;
import cz.tacr.elza.aiprovider.client.vo.NodeIssueKind;
import cz.tacr.elza.aiprovider.client.vo.ObjectType;
import cz.tacr.elza.aiprovider.client.vo.StructuredObjectInfo;
import cz.tacr.elza.controller.vo.AiContextAccesspointVO;
import cz.tacr.elza.controller.vo.AiContextFundVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextObjectVO;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
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
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedBinding;
import cz.tacr.elza.service.cache.CachedPart;

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
 *   <li>{@code AiContextAccesspoint} → an {@code elza.archivalEntity} (built from
 *       the access-point cache): identity, classification (its {@link ApType} as
 *       subclass, the type-hierarchy root as class), external-system identity, and
 *       the tree of parts — each part's display text and items.</li>
 * </ul>
 *
 * <p>Two entry points serve the two roles: {@link #resolvePrimary} yields the one
 * object a task parameter expects, {@link #resolveAll} yields the primary object
 * plus supporting context (ancestors, fund), deduplicated. Read permission is
 * enforced — {@code FUND_RD} on a target fund, {@code AP_SCOPE_RD} on an access
 * point's scope; anything the user cannot read or that cannot be mapped is skipped
 * (logged), and the request goes out with whatever could be resolved.
 *
 * <p>v1 resolves against the fund's <b>open</b> version; {@code fundVersionId} on
 * a node context is reserved for later locked-version support.
 */
@Service
public class AiContextResolver {

    private static final Logger logger = LoggerFactory.getLogger(AiContextResolver.class);

    /** Max sibling rows in a level's outline (window centered on the level). */
    private static final int OUTLINE_SIBLINGS_MAX = 50;

    /** Max child rows in a level's outline (the first children, arrangement order). */
    private static final int OUTLINE_CHILDREN_MAX = 200;

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
    private RuleService ruleService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

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
            } else if (ctx instanceof AiContextAccesspointVO accessPoint) {
                resolveArchivalEntity(accessPoint.getAccessPointId()).ifPresent(resolved::add);
            } else {
                logger.info("AI context object {} is not resolvable to a provider object yet",
                        ctx == null ? null : ctx.getClass().getSimpleName());
            }
        }
        enrichEntityRefs(resolved);
        return resolved;
    }

    /**
     * Resolves one context object to its single primary provider object — a fund
     * to its {@code elza.fundInfo}, a node to its own {@code elza.archivalDescription}
     * (no ancestors, no fund). Used to match a supplied parameter to the task's
     * declared parameter by object type.
     */
    public Optional<AiObject> resolvePrimary(final AiContextObjectVO ctx) {
        Optional<AiObject> resolved = resolvePrimaryObject(ctx);
        resolved.ifPresent(object -> enrichEntityRefs(List.of(object)));
        return resolved;
    }

    private Optional<AiObject> resolvePrimaryObject(final AiContextObjectVO ctx) {
        if (ctx instanceof AiContextFundVO fund) {
            return resolveFundInfo(fund.getFundId());
        }
        if (ctx instanceof AiContextNodeVO node) {
            ArrFundVersion version = resolveReadableOpenVersion(node.getFundId());
            if (version == null || node.getNodeId() == null) {
                return Optional.empty();
            }
            AiObject object = buildArchivalDescription(version, node.getNodeId(), true);
            if (object instanceof ArchivalDescriptionObject description && description.getData() != null) {
                // A primary node is the reviewed *subject* of the task (e.g.
                // elza.revision) — pin the element catalog its suggestions must
                // stay within (RevisionFinding.targetItemType).
                description.getData().setAllowedItemTypes(allowedItemTypes(version, node.getNodeId()));
            }
            return Optional.ofNullable(object);
        }
        if (ctx instanceof AiContextAccesspointVO accessPoint) {
            return resolveArchivalEntity(accessPoint.getAccessPointId());
        }
        logger.info("AI context object {} is not resolvable to a provider object yet",
                ctx == null ? null : ctx.getClass().getSimpleName());
        return Optional.empty();
    }

    /**
     * Item-type codes the rule package permits on the level — the REQUIRED,
     * RECOMMENDED and POSSIBLE types Elza's rules compute for the node
     * ({@link RuleService#getDescriptionItemTypes}); IMPOSSIBLE types are
     * excluded. {@code null} when the computation fails (the payload field is
     * optional; a task then simply gets no catalog to bound suggestions with).
     */
    private List<String> allowedItemTypes(final ArrFundVersion version, final Integer nodeId) {
        try {
            ArrNode node = nodeRepository.findById(nodeId).orElse(null);
            if (node == null) {
                return null;
            }
            return ruleService.getDescriptionItemTypes(version, node).stream()
                    .filter(type -> type.getType() != RulItemType.Type.IMPOSSIBLE)
                    .map(RulItemTypeExt::getCode)
                    .toList();
        } catch (Exception e) {
            logger.warn("Allowed item types for node {} could not be computed: {}", nodeId, e.getMessage());
            return null;
        }
    }

    /**
     * The surroundings of a level as a compact {@code elza.archivalOutline}:
     * its nearest siblings (a window centered on the level, the level itself
     * included so its position is visible) followed by its first children, in
     * arrangement order. Deterministic and cheap (~10–20 tokens per row, no
     * items loaded) — the hierarchical context of {@code elza.revision}
     * (tasks/elza-revision.md §2). Rows carry no {@code unitDate} in v1 (it
     * would cost an item load per row); the field is optional in the contract.
     * Empty when the fund/level is missing or not readable.
     */
    public Optional<AiObject> resolveOutline(final AiContextNodeVO nodeCtx) {
        ArrFundVersion version = resolveReadableOpenVersion(nodeCtx.getFundId());
        if (version == null || nodeCtx.getNodeId() == null) {
            return Optional.empty();
        }
        TreeNode treeNode = levelTreeCacheService.getVersionTreeCache(version).get(nodeCtx.getNodeId());
        if (treeNode == null) {
            return Optional.empty();
        }

        List<Integer> ids = new ArrayList<>();
        if (treeNode.getParent() != null) {
            List<TreeNode> siblings = treeNode.getParent().getChildren();
            int index = siblings.indexOf(treeNode);
            int from = Math.max(0, index - OUTLINE_SIBLINGS_MAX / 2);
            int to = Math.min(siblings.size(), from + OUTLINE_SIBLINGS_MAX);
            for (TreeNode sibling : siblings.subList(from, to)) {
                ids.add(sibling.getId());
            }
        }
        List<TreeNode> children = treeNode.getChildren();
        if (children != null) {
            for (TreeNode child : children.subList(0, Math.min(children.size(), OUTLINE_CHILDREN_MAX))) {
                ids.add(child.getId());
            }
        }
        if (ids.isEmpty()) {
            return Optional.empty();
        }

        // Decorate only the outline window: titles come from the same tree-cache
        // batch call the search hits use.
        Map<Integer, String> titles = loadTitles(version, ids);
        Map<Integer, TreeNode> treeMap = levelTreeCacheService.getVersionTreeCache(version);
        List<OutlineRow> rows = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            TreeNode row = treeMap.get(id);
            if (row == null) {
                continue;
            }
            rows.add(new OutlineRow()
                    .nodeId(id)
                    .depth(row.getDepth())
                    .referenceMark(toReferenceMark(row.getReferenceMark()))
                    .title(titles.get(id)));
        }
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        ArchivalOutline outline = new ArchivalOutline()
                .fundId(version.getFundId())
                .rows(rows);
        return Optional.of(new ArchivalOutlineObject()
                .objectType(ObjectType.ELZA_ARCHIVAL_OUTLINE)
                .data(outline));
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
                ArchivalDescription level = buildArchivalDescription(nodeId, nodeId.equals(node.getNodeId()),
                        nodesById.get(nodeId), treeMap.get(nodeId),
                        itemsByNode.getOrDefault(nodeId, List.of()), sdp,
                        titlesByNode.get(nodeId), issuesByNode.getOrDefault(nodeId, List.of()));
                if (level != null) {
                    resolved.add(new ArchivalDescriptionObject()
                            .objectType(ObjectType.ELZA_ARCHIVAL_DESCRIPTION)
                            .data(level));
                }
            }
        }
        addFundInfo(resolved, fundInfoAdded, node.getFundId());
    }

    /** Builds one level's {@code elza.archivalDescription}, loading its own pieces. */
    private AiObject buildArchivalDescription(final ArrFundVersion version, final Integer nodeId,
                                              final boolean focus) {
        ArchivalDescription data = buildArchivalDescriptionPayload(version, nodeId, focus);
        if (data == null) {
            return null;
        }
        return new ArchivalDescriptionObject()
                .objectType(ObjectType.ELZA_ARCHIVAL_DESCRIPTION)
                .data(data);
    }

    /**
     * Builds one level's {@code ArchivalDescription} payload — the same data a
     * context level carries, without the focus flag. Pure mapping: no permission
     * check (the caller enforces fund read permission), and the entity references
     * carried by the items stay bare — enrich them via
     * {@link #enrichEntityRefs(ArchivalDescription)}. Serves the
     * {@code getArchivalDescription} tool; {@code null} when the level is not in
     * the version's tree (deleted or foreign).
     */
    public ArchivalDescription buildArchivalDescription(final ArrFundVersion version, final Integer nodeId) {
        return buildArchivalDescriptionPayload(version, nodeId, false);
    }

    /** Builds one level's {@code ArchivalDescription} payload, loading its own pieces. */
    private ArchivalDescription buildArchivalDescriptionPayload(final ArrFundVersion version, final Integer nodeId,
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

    /** Maps a level's already-loaded pieces to the {@code ArchivalDescription} payload. */
    private ArchivalDescription buildArchivalDescription(final Integer nodeId, final boolean focus,
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
        return data;
    }

    /** Maps a level's description item to stable codes plus its display text. */
    private DescriptionItem toDescriptionItem(final ArrDescItem item, final StaticDataProvider sdp) {
        return buildItem(item.getItemTypeId(), item.getItemSpecId(), item.getData(),
                item.getFulltextValue(), sdp);
    }

    /** Maps an access-point item (a part's item) to the same {@link DescriptionItem} shape. */
    private DescriptionItem toDescriptionItem(final ApItem item, final StaticDataProvider sdp) {
        ArrData data = item.getData();
        // Cached item data carries scalar values inline, but a reference item's
        // target (access point / structured object) is not loaded here — only its
        // id — so its display text is resolved elsewhere, not via getFulltextValue.
        String value = data == null || data instanceof ArrDataRecordRef || data instanceof ArrDataStructureRef
                ? null
                : data.getFulltextValue();
        return buildItem(item.getItemTypeId(), item.getItemSpecId(), data, value, sdp);
    }

    /**
     * Maps one description/access-point item to stable codes plus its display text.
     * A reference item also carries its target as a nested object: an access-point
     * reference ({@code RECORD_REF}) fills {@link DescriptionItem#getEntity()}, a
     * structured-object reference ({@code STRUCTURED}) fills
     * {@link DescriptionItem#getStructuredObject()}.
     */
    private DescriptionItem buildItem(final Integer itemTypeId, final Integer itemSpecId,
                                      final ArrData data, final String value, final StaticDataProvider sdp) {
        ItemType itemType = sdp.getItemTypeById(itemTypeId);
        if (itemType == null) {
            return null;
        }
        DescriptionItem out = new DescriptionItem().type(itemType.getCode());
        if (itemType.getDataType() != null) {
            out.dataType(DataType.fromValue(itemType.getDataType().getCode()));
        }
        if (itemSpecId != null) {
            RulItemSpec spec = sdp.getItemSpecById(itemSpecId);
            if (spec != null) {
                out.spec(spec.getCode());
            }
        }
        // Same renderer fulltext indexing uses: enum → spec name, record-ref →
        // entity name, structured → rendered value; null for coordinates/tables.
        if (value != null && !value.isEmpty()) {
            out.value(value);
        }
        // Reference items carry a machine-readable reference alongside the display value.
        if (data instanceof ArrDataRecordRef recordRef && recordRef.getRecordId() != null) {
            // The referenced entity as a lightweight info; value already holds its
            // preferred name. Classification and external identity are left to the
            // entity resolver (filled in one batch to avoid a per-reference lookup).
            ArchivalEntityInfo entity = new ArchivalEntityInfo().accessPointId(recordRef.getRecordId());
            if (value != null && !value.isEmpty()) {
                entity.preferredName(value);
            }
            out.entity(entity);
        } else if (data instanceof ArrDataStructureRef structRef) {
            StructuredObjectInfo structured = new StructuredObjectInfo()
                    .structuredObjectId(structRef.getStructuredObjectId());
            ArrStructuredObject structObj = structRef.getStructuredObject();
            if (structObj != null) {
                StructType structType = sdp.getStructuredTypeById(structObj.getStructuredTypeId());
                if (structType != null) {
                    structured.structuredType(structType.getCode());
                }
                String complement = structObj.getComplement();
                if (complement != null && !complement.isEmpty()) {
                    structured.complement(complement);
                }
            }
            out.structuredObject(structured);
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Access point → elza.archivalEntity
    // -----------------------------------------------------------------------

    /**
     * Resolves an access point to its full {@code elza.archivalEntity} — identity,
     * classification and the tree of parts (each part's items + its display text),
     * read from the access-point cache. Enforces scope read permission; a missing,
     * unreadable or state-less access point is skipped (logged).
     */
    private Optional<AiObject> resolveArchivalEntity(final Integer accessPointId) {
        if (accessPointId == null) {
            return Optional.empty();
        }
        CachedAccessPoint cap = accessPointCacheService.findCachedAccessPoint(accessPointId);
        if (cap == null || cap.getApState() == null) {
            logger.info("AI context access point {} not found; skipped", accessPointId);
            return Optional.empty();
        }
        if (!canReadScope(cap.getApState().getScopeId())) {
            logger.info("AI context access point {} not readable by user; skipped", accessPointId);
            return Optional.empty();
        }
        return Optional.of(new ArchivalEntityObject()
                .objectType(ObjectType.ELZA_ARCHIVAL_ENTITY)
                .data(buildArchivalEntity(cap)));
    }

    /**
     * Maps a cached access point to the full {@code ArchivalEntity} payload —
     * identity, classification, {@code ruleSetCode} and the tree of parts (each
     * part's items + its display text). Pure mapping: no permission check (the
     * caller enforces scope read permission) and the entity references carried by
     * the parts' items stay bare — enrich them via
     * {@link #enrichEntityRefs(ArchivalEntity)}, unless the caller batches the
     * enrichment over a larger object set. Also serves the {@code getArchivalEntity}
     * tool, whose result is this same payload.
     */
    public ArchivalEntity buildArchivalEntity(final CachedAccessPoint cap) {
        ApState apState = cap.getApState();
        StaticDataProvider sdp = staticDataService.getData();
        ArchivalEntity entity = new ArchivalEntity().accessPointId(cap.getAccessPointId());
        entity.uuid(cap.getUuid());
        Classification classification = classify(apState.getApTypeId(), sdp);
        if (classification != null) {
            entity.classCode(classification.classCode()).className(classification.className())
                    .typeCode(classification.typeCode()).typeName(classification.typeName());
        }
        applyExternalId(entity, cap.getBindings());
        entity.ruleSetCode(scopeRuleSetCode(apState, sdp));

        List<CachedPart> parts = cap.getParts();
        if (parts != null && !parts.isEmpty()) {
            Map<Integer, List<CachedPart>> childrenByParent = parts.stream()
                    .filter(p -> p.getParentPartId() != null)
                    .collect(Collectors.groupingBy(CachedPart::getParentPartId));
            List<EntityPart> topParts = parts.stream()
                    .filter(p -> p.getParentPartId() == null)
                    .map(p -> buildEntityPart(p, childrenByParent, cap.getPreferredPartId(), sdp))
                    .toList();
            if (!topParts.isEmpty()) {
                entity.parts(topParts);
            }
        }
        return entity;
    }

    /**
     * Maps a cached access point to the lightweight {@code ArchivalEntityInfo} —
     * identity, classification, external-system identity and the preferred
     * (display) name, without the parts. Pure mapping: no permission check (the
     * caller enforces scope read permission). Serves the {@code searchEntities}
     * tool, whose hits are this payload.
     */
    public ArchivalEntityInfo buildArchivalEntityInfo(final CachedAccessPoint cap) {
        StaticDataProvider sdp = staticDataService.getData();
        ArchivalEntityInfo info = new ArchivalEntityInfo().accessPointId(cap.getAccessPointId());
        info.uuid(cap.getUuid());
        ApState apState = cap.getApState();
        if (apState != null) {
            Classification classification = classify(apState.getApTypeId(), sdp);
            if (classification != null) {
                info.classCode(classification.classCode()).className(classification.className())
                        .typeCode(classification.typeCode()).typeName(classification.typeName());
            }
        }
        List<CachedBinding> bindings = cap.getBindings();
        if (bindings != null && !bindings.isEmpty()) {
            CachedBinding binding = bindings.get(0);
            info.externalSystemCode(binding.getExternalSystemCode()).externalId(binding.getValue());
        }
        String preferredName = findPreferredName(cap);
        if (preferredName != null && !preferredName.isEmpty()) {
            info.preferredName(preferredName);
        }
        return info;
    }

    /** The display name of the entity's preferred part, or {@code null}. */
    private String findPreferredName(final CachedAccessPoint cap) {
        if (cap.getParts() == null || cap.getPreferredPartId() == null) {
            return null;
        }
        for (CachedPart part : cap.getParts()) {
            if (cap.getPreferredPartId().equals(part.getPartId())) {
                return findDisplayName(part.getIndices());
            }
        }
        return null;
    }

    /** Builds one part (recursively, with its sub-parts) from a cached part. */
    private EntityPart buildEntityPart(final CachedPart part,
                                       final Map<Integer, List<CachedPart>> childrenByParent,
                                       final Integer preferredPartId, final StaticDataProvider sdp) {
        EntityPart out = new EntityPart().partType(part.getPartTypeCode());
        out.partId(part.getPartId());
        if (part.getPartTypeCode() != null) {
            RulPartType partType = sdp.getPartTypeByCode(part.getPartTypeCode());
            if (partType != null) {
                out.partTypeName(partType.getName());
            }
        }
        if (part.getPartId() != null && part.getPartId().equals(preferredPartId)) {
            out.preferred(true);
        }
        String display = findDisplayName(part.getIndices());
        if (display != null && !display.isEmpty()) {
            out.value(display);
        }
        if (part.getItems() != null && !part.getItems().isEmpty()) {
            List<DescriptionItem> items = part.getItems().stream()
                    .map(item -> toDescriptionItem(item, sdp))
                    .filter(Objects::nonNull)
                    .toList();
            if (!items.isEmpty()) {
                out.items(items);
            }
        }
        List<CachedPart> children = childrenByParent.getOrDefault(part.getPartId(), List.of());
        if (!children.isEmpty()) {
            out.parts(children.stream()
                    .map(child -> buildEntityPart(child, childrenByParent, preferredPartId, sdp))
                    .toList());
        }
        return out;
    }

    /** The part's display-name index value (its text representation), or {@code null}. */
    private String findDisplayName(final List<ApIndex> indices) {
        if (indices == null) {
            return null;
        }
        for (ApIndex index : indices) {
            if (GroovyResult.DISPLAY_NAME.equals(index.getIndexType())) {
                return index.getIndexValue();
            }
        }
        return null;
    }

    /**
     * Classification of an entity from its {@link ApType}: the type itself is the
     * subclass, the root of the type hierarchy is the class — both by code and by
     * (localized) name. Resolved from the in-memory type cache (no query);
     * {@code null} when the type is unknown.
     */
    private Classification classify(final Integer apTypeId, final StaticDataProvider sdp) {
        if (apTypeId == null) {
            return null;
        }
        ApType type = sdp.getApTypeById(apTypeId);
        if (type == null) {
            return null;
        }
        ApType root = type;
        while (root.getParentApTypeId() != null) {
            ApType parent = sdp.getApTypeById(root.getParentApTypeId());
            if (parent == null) {
                break;
            }
            root = parent;
        }
        return new Classification(root.getCode(), root.getName(), type.getCode(), type.getName());
    }

    /** An entity's class (type-hierarchy root) and type (leaf / subclass), by code and name. */
    private record Classification(String classCode, String className, String typeCode, String typeName) {
    }

    /** Fills an entity's external-system identity from its single binding, if any. */
    private void applyExternalId(final ArchivalEntity entity, final List<CachedBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        CachedBinding binding = bindings.get(0);
        entity.externalSystemCode(binding.getExternalSystemCode()).externalId(binding.getValue());
    }

    /**
     * The code of the rule set governing the entity's description, taken from the
     * entity's scope ({@link ApScope#getRulRuleSet()}); {@code null} when the scope
     * has no rule set. Selects the dictionary that resolves the item-type/spec codes
     * carried by the parts' items — the same dictionary funds select via their own
     * rule set.
     */
    private String scopeRuleSetCode(final ApState apState, final StaticDataProvider sdp) {
        ApScope scope = apState.getScope();
        if (scope == null || scope.getRuleSetId() == null) {
            return null;
        }
        RuleSet ruleSet = sdp.getRuleSetById(scope.getRuleSetId());
        return ruleSet != null ? ruleSet.getCode() : null;
    }

    /** True when the logged user may read access points in the given scope. */
    private boolean canReadScope(final Integer scopeId) {
        return AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.AP_SCOPE_RD_ALL)
                .or(Permission.AP_SCOPE_RD, scopeId)
                .matches(userService.getLoggedUserDetail());
    }

    /**
     * Enriches the entity references carried by description items across the whole
     * resolved set ({@code RECORD_REF} items point at other access points).
     */
    private void enrichEntityRefs(final List<AiObject> objects) {
        List<ArchivalEntityInfo> refs = new ArrayList<>();
        for (AiObject object : objects) {
            collectEntityRefs(object, refs);
        }
        enrichRefs(refs);
    }

    /**
     * Enriches the entity references carried by one entity's parts — the
     * single-object variant used by the {@code getArchivalEntity} tool, which
     * resolves an entity outside the context flow.
     */
    public void enrichEntityRefs(final ArchivalEntity entity) {
        List<ArchivalEntityInfo> refs = new ArrayList<>();
        collectFromParts(entity.getParts(), refs);
        enrichRefs(refs);
    }

    /**
     * Enriches the entity references carried by one level's description items —
     * the single-level variant used by the {@code getArchivalDescription} tool,
     * which resolves a level outside the context flow.
     */
    public void enrichEntityRefs(final ArchivalDescription description) {
        List<ArchivalEntityInfo> refs = new ArrayList<>();
        collectFromItems(description.getItems(), refs);
        enrichRefs(refs);
    }

    /**
     * Enriches collected entity references in one batch — a single state load and
     * a single preferred-name load for all referenced ids — filling each
     * reference's classification, and its preferred name when the item did not
     * already carry it (the access-point-cache path does not). The referenced
     * entity's name is already exposed as the item value, so this adds no more
     * than the arrangement/registry UI already shows.
     */
    private void enrichRefs(final List<ArchivalEntityInfo> refs) {
        Set<Integer> ids = refs.stream()
                .map(ArchivalEntityInfo::getAccessPointId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Integer, ApState> stateById = accessPointService.groupStateByAccessPointId(new ArrayList<>(ids));
        Set<Integer> missingName = refs.stream()
                .filter(ref -> ref.getAccessPointId() != null
                        && (ref.getPreferredName() == null || ref.getPreferredName().isEmpty()))
                .map(ArchivalEntityInfo::getAccessPointId)
                .collect(Collectors.toSet());
        Map<Integer, ApIndex> nameById = missingName.isEmpty()
                ? Map.of()
                : accessPointService.findPreferredPartIndexMapByIds(missingName);

        StaticDataProvider sdp = staticDataService.getData();
        for (ArchivalEntityInfo ref : refs) {
            Integer id = ref.getAccessPointId();
            if (id == null) {
                continue;
            }
            ApState state = stateById.get(id);
            if (state != null) {
                Classification classification = classify(state.getApTypeId(), sdp);
                if (classification != null) {
                    ref.classCode(classification.classCode()).className(classification.className())
                            .typeCode(classification.typeCode()).typeName(classification.typeName());
                }
            }
            if (ref.getPreferredName() == null || ref.getPreferredName().isEmpty()) {
                ApIndex index = nameById.get(id);
                if (index != null) {
                    ref.preferredName(index.getIndexValue());
                }
            }
        }
    }

    /** Collects the entity references nested in an object's description items. */
    private void collectEntityRefs(final AiObject object, final List<ArchivalEntityInfo> acc) {
        if (object instanceof ArchivalDescriptionObject description && description.getData() != null) {
            collectFromItems(description.getData().getItems(), acc);
        } else if (object instanceof ArchivalEntityObject entity && entity.getData() != null) {
            collectFromParts(entity.getData().getParts(), acc);
        }
    }

    /** Collects entity references from a tree of parts (items + nested sub-parts). */
    private void collectFromParts(final List<EntityPart> parts, final List<ArchivalEntityInfo> acc) {
        if (parts == null) {
            return;
        }
        for (EntityPart part : parts) {
            collectFromItems(part.getItems(), acc);
            collectFromParts(part.getParts(), acc);
        }
    }

    /** Collects the {@code entity} reference of each access-point item. */
    private void collectFromItems(final List<DescriptionItem> items, final List<ArchivalEntityInfo> acc) {
        if (items == null) {
            return;
        }
        for (DescriptionItem item : items) {
            if (item.getEntity() != null) {
                acc.add(item.getEntity());
            }
        }
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
        return Optional.of(new FundInfoObject().objectType(ObjectType.ELZA_FUND_INFO)
                .data(buildFundInfo(fund, version)));
    }

    /**
     * Maps a fund to the {@code FundInfo} payload — identity, rule set, holding
     * institution and the open version's root node (the entry point for
     * {@code getArchivalDescription} browsing). Pure mapping: no permission check
     * (the caller enforces fund read permission). Also serves the
     * {@code searchFunds} tool, whose hits are this payload; {@code version} may
     * be {@code null} (a fund without an open version) — the version-bound fields
     * stay absent then.
     */
    public FundInfo buildFundInfo(final ArrFund fund, final ArrFundVersion version) {
        FundInfo info = new FundInfo()
                // lets the model reference the fund in tools, e.g. searchNodes.fundIds
                .fundId(fund.getFundId())
                .name(fund.getName())
                .internalCode(fund.getInternalCode())
                .fundNumber(fund.getFundNumber())
                .mark(fund.getMark())
                .unitDate(fund.getUnitdate());
        if (version != null) {
            if (version.getRuleSet() != null) {
                info.ruleSetCode(version.getRuleSet().getCode());
            }
            if (version.getRootNode() != null) {
                // lets the model enter the fund: getArchivalDescription from here
                info.rootNodeId(version.getRootNode().getNodeId());
            }
        }
        if (fund.getInstitution() != null) {
            info.institution(toInstitutionInfo(fund.getInstitution()));
        }
        return info;
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
