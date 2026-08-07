package cz.tacr.elza.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ItemOperation;
import cz.tacr.elza.aiprovider.client.vo.ItemOperationAdd;
import cz.tacr.elza.aiprovider.client.vo.ItemOperationDelete;
import cz.tacr.elza.aiprovider.client.vo.ItemOperationUpdate;
import cz.tacr.elza.aiprovider.client.vo.NodeUpdateProposal;
import cz.tacr.elza.aiprovider.client.vo.NodeUpdateProposals;
import cz.tacr.elza.aiprovider.client.vo.ProposedChange;
import cz.tacr.elza.aiprovider.client.vo.ProposedItemValue;
import cz.tacr.elza.controller.vo.AiActivityLinkVO;
import cz.tacr.elza.controller.vo.AiContextAccesspointVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextTypeVO;
import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;
import cz.tacr.elza.controller.vo.AiNodeUpdateProposalsBlockVO;
import cz.tacr.elza.controller.vo.AiProposalChangeVO;
import cz.tacr.elza.controller.vo.AiProposalNodeVO;
import cz.tacr.elza.controller.vo.AiProposalOperationVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiProposalDecision;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiProposalDecisionRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.websocket.UserEventPushService;

/**
 * Server side of the AI node-update proposals ({@code elza.nodeUpdateProposals}
 * result blocks, tasks/elza-enhance-description.md): renders a stored proposals
 * block as the decision-aware {@link AiNodeUpdateProposalsBlockVO} (validation
 * against the level's <em>current</em> state, persisted decisions merged), and
 * executes the user's apply / reject decisions. Strictly propose-only pipeline:
 * an apply re-validates the change and writes all its item operations in ONE
 * versioned {@link ArrChange} through {@link DescriptionItemService}; nothing
 * is ever written without the explicit apply call. Decisions are persisted as
 * {@link AiProposalDecision} — the panel card states and the evaluation signal
 * (acceptance rate per {@code promptVersion}).
 */
@Service
public class AiProposalService {

    private static final Logger logger = LoggerFactory.getLogger(AiProposalService.class);

    /** Object type of the proposals result block this service understands. */
    public static final String OBJECT_TYPE = "elza.nodeUpdateProposals";

    static final String STATE_PROPOSED = "PROPOSED";
    static final String STATE_BLOCKED = "BLOCKED";

    static final String KIND_ADD = "ADD";
    static final String KIND_UPDATE = "UPDATE";
    static final String KIND_DELETE = "DELETE";

    /**
     * Data types an operation may target (the v1 scope of the task contract);
     * operations on the reference/composite kinds are blocked.
     */
    private static final Set<DataType> PROPOSABLE_TYPES = EnumSet.of(
            DataType.STRING, DataType.TEXT, DataType.FORMATTED_TEXT, DataType.INT,
            DataType.DECIMAL, DataType.DATE, DataType.UNITDATE, DataType.UNITID,
            DataType.ENUM, DataType.RECORD_REF);

    @Autowired
    private AiRequestRepository aiRequestRepository;

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private AiProposalDecisionRepository decisionRepository;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private UserService userService;

    @Autowired
    private AiRequestViewMapper viewMapper;

    @Autowired
    private UserEventPushService pushService;

    @Autowired
    private ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Rendering (used by NodeUpdateProposalsBlockMapper)
    // -----------------------------------------------------------------------

    /**
     * Renders one stored {@code elza.nodeUpdateProposals} result block as the
     * decision-aware display block: operation rows with old→new values, each
     * change's state from its persisted decision or from validation against the
     * level's current state (blocked changes carry the user-facing reason).
     */
    public AiDisplayBlockVO toBlock(final JsonNode data, final AiBlockContext context) {
        NodeUpdateProposals parsed;
        try {
            parsed = objectMapper.treeToValue(data, NodeUpdateProposals.class);
        } catch (Exception e) {
            logger.warn("AI proposals block cannot be parsed: {}", e.getMessage());
            return fenced(data);
        }
        Map<String, AiProposalDecision> decisions = loadDecisions(context);
        StaticDataProvider sdp = staticDataService.getData();
        AiNodeUpdateProposalsBlockVO block = new AiNodeUpdateProposalsBlockVO();
        block.setNodes(new ArrayList<>());
        List<NodeUpdateProposal> proposals = parsed.getProposals() == null ? List.of() : parsed.getProposals();
        for (int p = 0; p < proposals.size(); p++) {
            NodeUpdateProposal proposal = proposals.get(p);
            NodeContext nodeCtx = loadNodeContext(proposal.getNodeId());
            AiProposalNodeVO nodeVO = new AiProposalNodeVO().node(nodeLink(nodeCtx));
            List<ProposedChange> changes = proposal.getChanges() == null ? List.of() : proposal.getChanges();
            for (int c = 0; c < changes.size(); c++) {
                nodeVO.addChangesItem(toChangeVO(nodeCtx, changes.get(c),
                        changeKey(context, p, c), decisions, sdp));
            }
            block.addNodesItem(nodeVO);
        }
        return block;
    }

    private AiProposalChangeVO toChangeVO(final NodeContext nodeCtx, final ProposedChange change,
                                          final String changeKey,
                                          final Map<String, AiProposalDecision> decisions,
                                          final StaticDataProvider sdp) {
        AiProposalChangeVO vo = new AiProposalChangeVO()
                .changeKey(changeKey)
                .reason(change.getReason())
                .confidence(change.getConfidence());
        for (ItemOperation op : operations(change)) {
            vo.addOperationsItem(toOperationRow(nodeCtx, op, sdp));
        }
        AiProposalDecision decision = decisions.get(changeKey);
        if (decision != null) {
            // A decided change keeps its state regardless of the level's current
            // reality (the apply itself changed the anchors).
            vo.state(decision.getState());
            vo.decideDate(toOffset(decision.getDecideDate()));
            return vo;
        }
        try {
            prepare(nodeCtx, change, sdp);
            vo.state(STATE_PROPOSED);
        } catch (BlockedException e) {
            vo.state(STATE_BLOCKED);
            vo.blockedReason(e.getMessage());
        }
        return vo;
    }

    /**
     * One operation as a display diff row. Best-effort — rendered even for a
     * blocked or already decided change, so the user always sees what was (or
     * would be) done: the old value comes from the proposal's own verbatim
     * anchor, falling back to the live item.
     */
    private AiProposalOperationVO toOperationRow(final NodeContext nodeCtx, final ItemOperation op,
                                                 final StaticDataProvider sdp) {
        AiProposalOperationVO row = new AiProposalOperationVO();
        ProposedItemValue newItem = null;
        Integer anchorId = null;
        String currentValue = null;
        if (op instanceof ItemOperationAdd add) {
            row.kind(KIND_ADD);
            newItem = add.getNewItem();
        } else if (op instanceof ItemOperationUpdate update) {
            row.kind(KIND_UPDATE);
            newItem = update.getNewItem();
            anchorId = update.getItemObjectId();
            currentValue = update.getCurrentValue();
        } else if (op instanceof ItemOperationDelete delete) {
            row.kind(KIND_DELETE);
            anchorId = delete.getItemObjectId();
            currentValue = delete.getCurrentValue();
        } else {
            row.kind(op.getKind() == null ? "?" : op.getKind().getValue());
        }

        ArrDescItem target = anchorId == null ? null : nodeCtx.openByObjectId.get(anchorId);
        ItemType itemType = null;
        if (target != null) {
            itemType = sdp.getItemTypeById(target.getItemTypeId());
        } else if (newItem != null && newItem.getType() != null) {
            itemType = sdp.getItemTypeByCode(newItem.getType());
        }
        row.itemTypeName(itemType != null ? displayName(itemType)
                : newItem != null && newItem.getType() != null ? newItem.getType() : "?");

        String specName = null;
        if (newItem != null && newItem.getSpec() != null && itemType != null) {
            RulItemSpec spec = itemType.getItemSpecByCode(newItem.getSpec());
            specName = spec != null ? spec.getName() : newItem.getSpec();
        } else if (newItem == null && target != null && target.getItemSpecId() != null) {
            RulItemSpec spec = sdp.getItemSpecById(target.getItemSpecId());
            specName = spec == null ? null : spec.getName();
        }
        if (specName != null) {
            row.specName(specName);
        }

        if (anchorId != null) {
            String oldValue = currentValue != null ? currentValue
                    : target != null ? target.getFulltextValue() : null;
            if (StringUtils.isNotBlank(oldValue)) {
                row.oldValue(oldValue);
            }
        }
        if (newItem != null) {
            if (newItem.getAccessPointId() != null) {
                String name = preferredName(newItem.getAccessPointId());
                row.newValue(name != null ? name : "#" + newItem.getAccessPointId());
                row.entity(new AiActivityLinkVO()
                        .label(name)
                        .target(new AiContextAccesspointVO()
                                .accessPointId(newItem.getAccessPointId())
                                .type(AiContextTypeVO.ACCESSPOINT)));
            } else if (StringUtils.isNotBlank(newItem.getValue())) {
                row.newValue(newItem.getValue());
            } else if (specName != null) {
                // A purely spec-carried value (ENUM) reads as the spec's name.
                row.newValue(specName);
            }
        }
        return row;
    }

    // -----------------------------------------------------------------------
    // Decisions (used by AiProviderController)
    // -----------------------------------------------------------------------

    /**
     * Applies one proposed change: re-validates it against the level's current
     * state and writes all its operations in one versioned change; the decision
     * is recorded. When validation fails, nothing is written — the returned
     * refreshed exchange renders the change blocked with the reason.
     */
    @Transactional
    public AiRequestVO applyChange(final Integer requestId, final String changeKey) {
        RequestAccess access = loadOwnRequest(requestId);
        LocatedChange located = locateChange(access.request(), changeKey);
        if (decisionRepository.findByAiRequestIdAndChangeKey(requestId, changeKey).isPresent()) {
            return viewMapper.loadVO(access.request());
        }
        NodeContext nodeCtx = loadNodeContext(located.proposal().getNodeId());
        StaticDataProvider sdp = staticDataService.getData();
        List<PreparedOp> prepared;
        try {
            prepared = prepare(nodeCtx, located.change(), sdp);
        } catch (BlockedException e) {
            // Nothing is written; the refreshed view shows the change blocked.
            return viewMapper.loadVO(access.request());
        }
        checkCanArrange(nodeCtx.version);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.BATCH_CHANGE_DESC_ITEM,
                nodeCtx.node);
        applyOps(nodeCtx, prepared, change);
        saveDecision(access, changeKey, AiProposalDecision.STATE_APPLIED, change.getChangeId());

        pushService.push(access.conversation().getUserId(), viewMapper.buildUpdateMessage(access.request()));
        return viewMapper.loadVO(access.request());
    }

    /** Rejects one proposed change (records the decision; the description is not touched). */
    @Transactional
    public AiRequestVO rejectChange(final Integer requestId, final String changeKey) {
        RequestAccess access = loadOwnRequest(requestId);
        locateChange(access.request(), changeKey);
        if (decisionRepository.findByAiRequestIdAndChangeKey(requestId, changeKey).isEmpty()) {
            saveDecision(access, changeKey, AiProposalDecision.STATE_REJECTED, null);
            pushService.push(access.conversation().getUserId(), viewMapper.buildUpdateMessage(access.request()));
        }
        return viewMapper.loadVO(access.request());
    }

    private void applyOps(final NodeContext nodeCtx, final List<PreparedOp> prepared, final ArrChange change) {
        List<ArrDescItem> toCreate = new ArrayList<>();
        List<ArrDescItem> toUpdate = new ArrayList<>();
        List<ArrDescItem> toDelete = new ArrayList<>();
        for (PreparedOp op : prepared) {
            switch (op.kind) {
                case KIND_ADD -> {
                    ArrDescItem item = new ArrDescItem();
                    item.setItemType(op.itemType.getEntity());
                    item.setItemSpec(op.itemSpec);
                    item.setData(op.data);
                    item.setNode(nodeCtx.node);
                    toCreate.add(item);
                }
                case KIND_UPDATE -> {
                    // Carrier for updateValueAsNewVersion: the anchor's identity
                    // and position, the proposed spec and data.
                    ArrDescItem item = new ArrDescItem();
                    item.setDescItemObjectId(op.target.getDescItemObjectId());
                    item.setPosition(op.target.getPosition());
                    item.setItemType(op.target.getItemType());
                    item.setItemSpec(op.itemSpec);
                    item.setData(op.data);
                    item.setNode(op.target.getNode());
                    toUpdate.add(item);
                }
                case KIND_DELETE -> toDelete.add(op.target);
                default -> throw new IllegalStateException("Unexpected operation kind: " + op.kind);
            }
        }
        if (!toCreate.isEmpty()) {
            descriptionItemService.createDescriptionItems(toCreate, nodeCtx.node, nodeCtx.version, change);
        }
        if (!toUpdate.isEmpty()) {
            descriptionItemService.updateDescriptionItems(toUpdate, nodeCtx.version, change, false);
        }
        if (!toDelete.isEmpty()) {
            descriptionItemService.deleteDescriptionItems(toDelete, nodeCtx.version, change, true, false);
        }
    }

    private void saveDecision(final RequestAccess access, final String changeKey, final String state,
                              final Integer changeId) {
        AiProposalDecision decision = new AiProposalDecision();
        decision.setAiRequestId(access.request().getAiRequestId());
        decision.setChangeKey(changeKey);
        decision.setState(state);
        decision.setChangeId(changeId);
        decision.setUserId(loggedUserId());
        decision.setDecideDate(new Date());
        decisionRepository.save(decision);
    }

    // -----------------------------------------------------------------------
    // Validation & preparation (shared by rendering and apply)
    // -----------------------------------------------------------------------

    /** An operation cannot be applied; the message is user-facing, in Czech. */
    private static class BlockedException extends RuntimeException {
        BlockedException(final String message) {
            super(message);
        }
    }

    /** One operation resolved against the level's current state, ready to apply. */
    private static class PreparedOp {
        String kind;
        ArrDescItem target;
        ItemType itemType;
        RulItemSpec itemSpec;
        ArrData data;
    }

    /**
     * Validates a change against the level's current state and resolves its
     * operations to appliable form. Throws {@link BlockedException} with the
     * user-facing reason on the first violation — the whole change stands or
     * falls together.
     */
    private List<PreparedOp> prepare(final NodeContext nodeCtx, final ProposedChange change,
                                     final StaticDataProvider sdp) {
        if (nodeCtx.unavailableReason != null) {
            throw new BlockedException(nodeCtx.unavailableReason);
        }
        if (nodeCtx.extByTypeId == null) {
            throw new BlockedException("Nepodařilo se vyhodnotit pravidla pro jednotku popisu.");
        }
        List<ItemOperation> ops = operations(change);
        if (ops.isEmpty()) {
            throw new BlockedException("Návrh neobsahuje žádnou operaci.");
        }
        List<PreparedOp> prepared = new ArrayList<>(ops.size());
        for (ItemOperation op : ops) {
            if (op instanceof ItemOperationAdd add) {
                prepared.add(prepareAdd(nodeCtx, add, sdp));
            } else if (op instanceof ItemOperationUpdate update) {
                prepared.add(prepareUpdate(nodeCtx, update, sdp));
            } else if (op instanceof ItemOperationDelete delete) {
                prepared.add(prepareDelete(nodeCtx, delete, sdp));
            } else {
                throw new BlockedException("Neznámý druh operace návrhu.");
            }
        }
        return prepared;
    }

    private PreparedOp prepareAdd(final NodeContext nodeCtx, final ItemOperationAdd add,
                                  final StaticDataProvider sdp) {
        ProposedItemValue newItem = add.getNewItem();
        if (newItem == null || StringUtils.isBlank(newItem.getType())) {
            throw new BlockedException("Neúplná operace návrhu.");
        }
        ItemType itemType = sdp.getItemTypeByCode(newItem.getType());
        if (itemType == null) {
            throw new BlockedException("Neznámý typ prvku popisu: " + newItem.getType() + ".");
        }
        RulItemTypeExt ext = nodeCtx.extByTypeId.get(itemType.getItemTypeId());
        if (ext == null || ext.getType() == RulItemType.Type.IMPOSSIBLE) {
            throw new BlockedException("Prvek „" + displayName(itemType)
                    + "“ nelze na této jednotce popisu použít.");
        }
        checkProposableDataType(itemType);
        boolean hasValue = nodeCtx.openItems.stream()
                .anyMatch(item -> itemType.getItemTypeId().equals(item.getItemTypeId()));
        if (hasValue && Boolean.FALSE.equals(ext.getRepeatable())) {
            throw new BlockedException("Prvek „" + displayName(itemType)
                    + "“ není opakovatelný a jednotka popisu už jeho hodnotu obsahuje.");
        }
        PreparedOp op = new PreparedOp();
        op.kind = KIND_ADD;
        op.itemType = itemType;
        op.itemSpec = resolveSpec(itemType, newItem);
        op.data = buildData(itemType, op.itemSpec, newItem);
        return op;
    }

    private PreparedOp prepareUpdate(final NodeContext nodeCtx, final ItemOperationUpdate update,
                                     final StaticDataProvider sdp) {
        ArrDescItem target = resolveAnchor(nodeCtx, update.getItemObjectId(), update.getCurrentValue());
        ProposedItemValue newItem = update.getNewItem();
        if (newItem == null) {
            throw new BlockedException("Neúplná operace návrhu.");
        }
        ItemType itemType = sdp.getItemTypeById(target.getItemTypeId());
        if (newItem.getType() != null && itemType != null && !newItem.getType().equals(itemType.getCode())) {
            throw new BlockedException("Návrh mění typ prvku popisu – takovou změnu nelze provést.");
        }
        if (itemType == null) {
            throw new BlockedException("Neznámý typ měněného prvku popisu.");
        }
        checkProposableDataType(itemType);
        PreparedOp op = new PreparedOp();
        op.kind = KIND_UPDATE;
        op.target = target;
        op.itemType = itemType;
        op.itemSpec = resolveSpec(itemType, newItem);
        op.data = buildData(itemType, op.itemSpec, newItem);
        return op;
    }

    private PreparedOp prepareDelete(final NodeContext nodeCtx, final ItemOperationDelete delete,
                                     final StaticDataProvider sdp) {
        ArrDescItem target = resolveAnchor(nodeCtx, delete.getItemObjectId(), delete.getCurrentValue());
        ItemType itemType = sdp.getItemTypeById(target.getItemTypeId());
        if (itemType != null) {
            checkProposableDataType(itemType);
        }
        PreparedOp op = new PreparedOp();
        op.kind = KIND_DELETE;
        op.target = target;
        op.itemType = itemType;
        return op;
    }

    /** Resolves an update/delete anchor: the open item, its verbatim value, mutability. */
    private ArrDescItem resolveAnchor(final NodeContext nodeCtx, final Integer itemObjectId,
                                      final String currentValue) {
        if (itemObjectId == null) {
            throw new BlockedException("Neúplná operace návrhu.");
        }
        ArrDescItem target = nodeCtx.openByObjectId.get(itemObjectId);
        if (target == null) {
            throw new BlockedException("Měněný prvek popisu už na jednotce popisu neexistuje.");
        }
        if (currentValue != null) {
            String live = target.getFulltextValue();
            if (live == null || !live.strip().equals(currentValue.strip())) {
                throw new BlockedException(
                        "Prvek popisu byl mezitím změněn – návrh neodpovídá jeho aktuální hodnotě.");
            }
        }
        if (Boolean.TRUE.equals(target.getReadOnly())) {
            throw new BlockedException("Prvek popisu je pouze pro čtení.");
        }
        return target;
    }

    private void checkProposableDataType(final ItemType itemType) {
        if (!PROPOSABLE_TYPES.contains(itemType.getDataType())) {
            throw new BlockedException("Prvek „" + displayName(itemType)
                    + "“ tohoto datového typu nelze návrhem upravovat.");
        }
    }

    /** Resolves the spec for a type that uses specifications; ignores a spec supplied for one that doesn't. */
    private RulItemSpec resolveSpec(final ItemType itemType, final ProposedItemValue newItem) {
        if (!Boolean.TRUE.equals(itemType.getEntity().getUseSpecification())) {
            return null;
        }
        if (StringUtils.isBlank(newItem.getSpec())) {
            throw new BlockedException("Návrh neuvádí specifikaci prvku „" + displayName(itemType) + "“.");
        }
        RulItemSpec spec = itemType.getItemSpecByCode(newItem.getSpec());
        if (spec == null) {
            throw new BlockedException("Neplatná specifikace „" + newItem.getSpec()
                    + "“ prvku „" + displayName(itemType) + "“.");
        }
        return spec;
    }

    /** Builds the item's new value for the type's data kind (the v1 proposable scope). */
    private ArrData buildData(final ItemType itemType, final RulItemSpec spec, final ProposedItemValue newItem) {
        DataType dataType = itemType.getDataType();
        String value = newItem.getValue();
        ArrData data;
        try {
            switch (dataType) {
                case STRING -> {
                    ArrDataString d = new ArrDataString();
                    d.setStringValue(requireValue(value, itemType));
                    data = d;
                }
                case TEXT, FORMATTED_TEXT -> {
                    ArrDataText d = new ArrDataText();
                    d.setTextValue(requireValue(value, itemType));
                    data = d;
                }
                case INT -> {
                    ArrDataInteger d = new ArrDataInteger();
                    d.setIntegerValue(Integer.valueOf(requireValue(value, itemType).strip()));
                    data = d;
                }
                case DECIMAL -> {
                    ArrDataDecimal d = new ArrDataDecimal();
                    d.setValue(new BigDecimal(requireValue(value, itemType).strip().replace(',', '.')));
                    data = d;
                }
                case DATE -> {
                    ArrDataDate d = new ArrDataDate();
                    d.setValue(LocalDate.parse(requireValue(value, itemType).strip()));
                    data = d;
                }
                case UNITID -> {
                    ArrDataUnitid d = new ArrDataUnitid();
                    d.setUnitId(requireValue(value, itemType));
                    data = d;
                }
                case UNITDATE -> data = ArrDataUnitdate.valueOf(requireValue(value, itemType));
                case ENUM -> data = new ArrDataNull();
                case RECORD_REF -> {
                    if (newItem.getAccessPointId() == null) {
                        throw new BlockedException("Návrh neuvádí odkazovanou entitu prvku „"
                                + displayName(itemType) + "“.");
                    }
                    ApAccessPoint accessPoint = apAccessPointRepository
                            .findById(newItem.getAccessPointId()).orElse(null);
                    if (accessPoint == null) {
                        throw new BlockedException("Odkazovaná entita nebyla nalezena.");
                    }
                    ArrDataRecordRef d = new ArrDataRecordRef();
                    d.setRecord(accessPoint);
                    data = d;
                }
                default -> throw new BlockedException("Prvek „" + displayName(itemType)
                        + "“ tohoto datového typu nelze návrhem upravovat.");
            }
        } catch (BlockedException e) {
            throw e;
        } catch (Exception e) {
            throw new BlockedException("Hodnotu „" + value + "“ nelze uložit do prvku „"
                    + displayName(itemType) + "“.");
        }
        data.setDataType(dataType.getEntity());
        return data;
    }

    private String requireValue(final String value, final ItemType itemType) {
        if (StringUtils.isBlank(value)) {
            throw new BlockedException("Návrh neobsahuje hodnotu prvku „" + displayName(itemType) + "“.");
        }
        return value;
    }

    // -----------------------------------------------------------------------
    // Node context
    // -----------------------------------------------------------------------

    /** The level's loaded current state, shared by rendering and validation. */
    private static class NodeContext {
        Integer nodeId;
        ArrNode node;
        ArrFundVersion version;
        String title;
        List<ArrDescItem> openItems = List.of();
        Map<Integer, ArrDescItem> openByObjectId = Map.of();
        /** Rule-computed types for the node; {@code null} when the evaluation failed. */
        Map<Integer, RulItemTypeExt> extByTypeId;
        /** User-facing reason when the level cannot be worked with at all. */
        String unavailableReason;
    }

    private NodeContext loadNodeContext(final Integer nodeId) {
        NodeContext ctx = new NodeContext();
        ctx.nodeId = nodeId;
        if (nodeId == null) {
            ctx.unavailableReason = "Návrh neuvádí jednotku popisu.";
            return ctx;
        }
        ctx.node = nodeRepository.findById(nodeId).orElse(null);
        if (ctx.node == null) {
            ctx.unavailableReason = "Jednotka popisu nebyla nalezena.";
            return ctx;
        }
        ctx.version = fundVersionRepository.findByFundIdAndLockChangeIsNull(ctx.node.getFundId());
        if (ctx.version == null || !canRead(ctx.version)) {
            ctx.unavailableReason = "Jednotka popisu není dostupná.";
            return ctx;
        }
        ctx.openItems = descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(List.of(nodeId));
        ctx.openByObjectId = ctx.openItems.stream()
                .filter(item -> item.getDescItemObjectId() != null)
                .collect(Collectors.toMap(ArrDescItem::getDescItemObjectId, Function.identity(), (a, b) -> a));
        for (TreeNodeVO treeNode : levelTreeCacheService.getNodesByIds(List.of(nodeId), ctx.version)) {
            if (treeNode.getName() != null) {
                ctx.title = treeNode.getName();
            }
        }
        try {
            ctx.extByTypeId = ruleService.getDescriptionItemTypes(ctx.version, ctx.node).stream()
                    .collect(Collectors.toMap(RulItemType::getItemTypeId, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            logger.warn("Item types for node {} could not be computed: {}", nodeId, e.getMessage());
            ctx.extByTypeId = null;
        }
        return ctx;
    }

    private AiActivityLinkVO nodeLink(final NodeContext nodeCtx) {
        AiContextNodeVO target = new AiContextNodeVO().type(AiContextTypeVO.NODE).nodeId(nodeCtx.nodeId);
        if (nodeCtx.node != null) {
            target.fundId(nodeCtx.node.getFundId());
        }
        if (nodeCtx.version != null) {
            target.fundVersionId(nodeCtx.version.getFundVersionId());
        }
        return new AiActivityLinkVO().label(nodeCtx.title).target(target);
    }

    // -----------------------------------------------------------------------
    // Locating a change in the stored output
    // -----------------------------------------------------------------------

    /** One proposed change located in a request's stored output by its change key. */
    private record LocatedChange(NodeUpdateProposal proposal, ProposedChange change) {
    }

    private record RequestAccess(AiRequest request, AiConversation conversation) {
    }

    /**
     * Locates a change by its key {@code blockIndex/proposalIndex/changeIndex} —
     * the same addressing {@link #toBlock} renders (block index counts every
     * output block, matching the registry's iteration).
     */
    private LocatedChange locateChange(final AiRequest request, final String changeKey) {
        int[] key = parseChangeKey(changeKey);
        if (key != null && request.getOutput() != null && "done".equals(request.getState())) {
            try {
                JsonNode root = objectMapper.readTree(request.getOutput());
                if (root.isArray() && key[0] < root.size()) {
                    JsonNode block = root.get(key[0]);
                    if (OBJECT_TYPE.equals(block.path("objectType").asText(null))) {
                        NodeUpdateProposals proposals = objectMapper.treeToValue(block.path("data"),
                                NodeUpdateProposals.class);
                        if (proposals.getProposals() != null && key[1] < proposals.getProposals().size()) {
                            NodeUpdateProposal proposal = proposals.getProposals().get(key[1]);
                            if (proposal.getChanges() != null && key[2] < proposal.getChanges().size()) {
                                return new LocatedChange(proposal, proposal.getChanges().get(key[2]));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("AI proposal change {} of request {} cannot be read: {}",
                        changeKey, request.getAiRequestId(), e.getMessage());
            }
        }
        throw notFound("AI proposal change not found: " + changeKey, changeKey);
    }

    private int[] parseChangeKey(final String changeKey) {
        if (changeKey == null) {
            return null;
        }
        String[] parts = changeKey.split("/");
        if (parts.length != 3) {
            return null;
        }
        try {
            int[] key = new int[3];
            for (int i = 0; i < 3; i++) {
                key[i] = Integer.parseInt(parts[i]);
                if (key[i] < 0) {
                    return null;
                }
            }
            return key;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String changeKey(final AiBlockContext context, final int proposalIndex, final int changeIndex) {
        return (context == null ? 0 : context.blockIndex()) + "/" + proposalIndex + "/" + changeIndex;
    }

    private Map<String, AiProposalDecision> loadDecisions(final AiBlockContext context) {
        if (context == null || context.aiRequestId() == null) {
            return Map.of();
        }
        return decisionRepository.findByAiRequestId(context.aiRequestId()).stream()
                .collect(Collectors.toMap(AiProposalDecision::getChangeKey, Function.identity(), (a, b) -> a));
    }

    // -----------------------------------------------------------------------
    // Access & helpers
    // -----------------------------------------------------------------------

    private RequestAccess loadOwnRequest(final Integer requestId) {
        AiRequest request = aiRequestRepository.findById(requestId)
                .orElseThrow(() -> notFound("AI request not found: " + requestId, requestId));
        AiConversation conversation = aiConversationRepository.findById(request.getAiConversationId())
                .filter(c -> loggedUserId().equals(c.getUserId()))
                .orElseThrow(() -> notFound("AI request not found: " + requestId, requestId));
        return new RequestAccess(request, conversation);
    }

    private boolean canRead(final ArrFundVersion version) {
        return AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_RD_ALL)
                .or(Permission.FUND_RD, version)
                .matches(userService.getLoggedUserDetail());
    }

    /** Arranging (write) permission on the fund — panel visibility alone is not enough to apply. */
    private void checkCanArrange(final ArrFundVersion version) {
        boolean allowed = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_ARR_ALL)
                .or(Permission.FUND_ARR, version)
                .matches(userService.getLoggedUserDetail());
        if (!allowed) {
            throw new AccessDeniedException("User is not authorized to modify the archival description.",
                    Collections.emptyList());
        }
    }

    private Integer loggedUserId() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null || userDetail.getId() == null) {
            throw new AccessDeniedException("User not authorized.", Collections.emptyList());
        }
        return userDetail.getId();
    }

    private String preferredName(final Integer accessPointId) {
        try {
            ApIndex index = accessPointService.findPreferredPartIndex(accessPointId);
            return index == null ? null : index.getIndexValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static String displayName(final ItemType itemType) {
        String name = itemType.getEntity().getName();
        return StringUtils.isNotBlank(name) ? name : itemType.getCode();
    }

    private static List<ItemOperation> operations(final ProposedChange change) {
        return change.getOperations() == null ? List.of() : change.getOperations();
    }

    /** Fallback rendering when the stored block cannot be parsed (mirrors the registry rule). */
    private AiDisplayBlockVO fenced(final JsonNode value) {
        String pretty;
        try {
            pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            pretty = String.valueOf(value);
        }
        return new AiMarkdownBlockVO().content("```json\n" + pretty + "\n```");
    }

    private static ObjectNotFoundException notFound(final String message, final Object id) {
        return new ObjectNotFoundException(message, BaseCode.ID_NOT_EXIST).setId(id);
    }

    private static OffsetDateTime toOffset(final Date date) {
        return date == null ? null
                : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
