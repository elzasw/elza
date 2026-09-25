package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;
import jakarta.transaction.Transactional;

/**
 * Carries out a {@link DaImportPlan} below a unit of description: finds or creates the planned
 * levels, gives the created ones their items and attaches the digital entities of the package.
 *
 * A planned level is, below the level planned above it:
 * <ol>
 * <li>the child whose UUID is the ID of the div - a UUID match always wins, and the level is
 * taken as it is, without items from the package;</li>
 * <li>otherwise the child with the same values of the items the script matches by (e.g. level
 * type and name of a group of the file plan) - such a level is shared with other packages; it
 * gets the items it has none of yet, a different value of an item it has is kept and reported;</li>
 * <li>otherwise a new last child, with the UUID of the div - so that the next version of the
 * package is matched to it - unless that UUID is taken elsewhere in the fund.</li>
 * </ol>
 *
 * Permissions are not checked here: the automatic processing has no user, and the import
 * started by a user checks the permission to the fund before it gets here.
 */
@Service
public class DaImportBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DaImportBuilder.class);

    /** What carrying out a plan did. */
    public record Outcome(int created, int matched, int attached, List<String> conflicts) {
    }

    private final DaService daService;
    private final LevelRepository levelRepository;
    private final NodeRepository nodeRepository;
    private final DaDaoRepository daoRepository;
    private final FundVersionRepository fundVersionRepository;
    private final DescriptionItemService descriptionItemService;
    private final ArrangementInternalService arrangementInternalService;
    private final RuleService ruleService;

    public DaImportBuilder(DaService daService, LevelRepository levelRepository, NodeRepository nodeRepository,
                           DaDaoRepository daoRepository, FundVersionRepository fundVersionRepository,
                           DescriptionItemService descriptionItemService,
                           ArrangementInternalService arrangementInternalService, RuleService ruleService) {
        this.daService = daService;
        this.levelRepository = levelRepository;
        this.nodeRepository = nodeRepository;
        this.daoRepository = daoRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.descriptionItemService = descriptionItemService;
        this.arrangementInternalService = arrangementInternalService;
        this.ruleService = ruleService;
    }

    /**
     * The data of the planned items become the data of the created items, so a plan can be
     * carried out once.
     *
     * @param target the unit of description the package is imported to, in the fund of the package
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public Outcome build(DaAip aip, ArrNode target, DaImportPlan plan) {
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(target.getFund().getFundId());
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_LEVEL, target);
        Map<String, DaDao> logicalDaos = daoRepository
                .findByAipAndTypeAndDeleteChangeIsNull(aip, DaDao.DaoType.LOGICAL).stream()
                .collect(Collectors.toMap(DaDao::getCode, Function.identity(), (a, b) -> a));

        Run run = new Run(aip, version, change, logicalDaos);
        for (DaImportPlan.Node node : plan.getRoots()) {
            run.place(node, target);
        }
        run.finish();
        return new Outcome(run.created.size(), run.matched, run.attached, run.conflicts);
    }

    /** A child of a level, with its open items. */
    private record Child(ArrNode node, List<ArrDescItem> items) {
    }

    /** One carrying out of a plan. */
    private class Run {

        private final DaAip aip;
        private final ArrFundVersion version;
        private final ArrChange change;
        private final Map<String, DaDao> logicalDaos;
        private final MultipleItemChangeContext changeContext;

        /** Children of the levels looked into, by node id; the created ones are added. */
        private final Map<Integer, List<Child>> children = new HashMap<>();
        private final List<Integer> created = new ArrayList<>();
        private final List<Integer> enriched = new ArrayList<>();
        private final List<String> conflicts = new ArrayList<>();
        private int matched;
        private int attached;

        Run(DaAip aip, ArrFundVersion version, ArrChange change, Map<String, DaDao> logicalDaos) {
            this.aip = aip;
            this.version = version;
            this.change = change;
            this.logicalDaos = logicalDaos;
            this.changeContext = descriptionItemService.createChangeContext(version.getFundVersionId());
        }

        void place(DaImportPlan.Node node, ArrNode parent) {
            if (node.getDecision() == DaImportResult.Decision.ATTACH) {
                attach(node, parent);
                return;
            }
            ArrNode level = findOrCreate(node, parent);
            if (node.isAttachOwnEntity()) {
                attach(node, level);
            }
            for (DaImportPlan.Node child : node.getChildren()) {
                place(child, level);
            }
        }

        private void attach(DaImportPlan.Node node, ArrNode parent) {
            DaDao dao = logicalDaos.get(node.getDivId());
            if (dao == null) {
                throw new SystemException("AIP " + aip.getCode() + " nemá digitální entitu pro div " + node.getDivId()
                        + "; je třeba jej znovu sestavit z metadat", BaseCode.INVALID_STATE);
            }
            daService.linkToNode(aip, dao, parent, ArrDaoLink.LinkType.PART_AIP, change);
            attached++;
        }

        private ArrNode findOrCreate(DaImportPlan.Node node, ArrNode parent) {
            List<Child> siblings = childrenOf(parent);
            String uuid = AipNodeUuids.normalize(node.getDivId());

            if (uuid != null) {
                for (Child sibling : siblings) {
                    if (uuid.equals(sibling.node().getUuid())) {
                        matched++;
                        return sibling.node();
                    }
                }
            }
            if (!node.getMatchBy().isEmpty()) {
                for (Child sibling : siblings) {
                    if (sameValues(node, sibling)) {
                        matched++;
                        enrich(node, sibling);
                        return sibling.node();
                    }
                }
            }
            return create(node, parent, uuid, siblings);
        }

        private ArrNode create(DaImportPlan.Node node, ArrNode parent, @Nullable String uuid, List<Child> siblings) {
            if (uuid == null || !nodeRepository.findByFundAndUuidIn(parent.getFund(), List.of(uuid)).isEmpty()) {
                if (uuid != null) {
                    logger.info("UUID {} divu AIP {} už ve fondu patří jiné úrovni, nová úroveň dostane jiné",
                                uuid, aip.getCode());
                }
                uuid = daService.generateUuid();
            }
            Integer maxPosition = levelRepository.findMaxPositionUnderParent(parent);
            ArrNode level = daService.createChildNode(parent, change, maxPosition == null ? 1 : maxPosition + 1, uuid);

            List<ArrDescItem> items = new ArrayList<>(node.getItems().size());
            for (DaImportResult.Item item : node.getItems()) {
                items.add(createItem(level, item));
            }
            siblings.add(new Child(level, items));
            children.put(level.getNodeId(), new ArrayList<>());
            created.add(level.getNodeId());
            return level;
        }

        /**
         * A level shared with other packages gets the items it has none of yet. A different value
         * of an item it has is kept - which value is right is for the user to decide - and
         * reported.
         */
        private void enrich(DaImportPlan.Node node, Child sibling) {
            Map<Integer, List<ArrDescItem>> existing = sibling.items().stream()
                    .collect(Collectors.groupingBy(ArrDescItem::getItemTypeId));
            Map<Integer, List<DaImportResult.Item>> planned = node.getItems().stream()
                    .collect(Collectors.groupingBy(i -> i.itemType().getItemTypeId()));
            boolean added = false;
            for (Map.Entry<Integer, List<DaImportResult.Item>> entry : planned.entrySet()) {
                List<ArrDescItem> present = existing.get(entry.getKey());
                if (CollectionUtils.isEmpty(present)) {
                    for (DaImportResult.Item item : entry.getValue()) {
                        sibling.items().add(createItem(sibling.node(), item));
                    }
                    added = true;
                } else if (!values(present).equals(plannedValues(entry.getValue()))) {
                    RulItemType itemType = entry.getValue().get(0).itemType();
                    conflicts.add("Úroveň '" + StringUtils.defaultString(node.getLabel(), node.getDivId())
                            + "': prvek popisu " + itemType.getCode() + " má v AIP " + aip.getCode()
                            + " jinou hodnotu, ponechána stávající");
                }
            }
            if (added) {
                enriched.add(sibling.node().getNodeId());
            }
        }

        private ArrDescItem createItem(ArrNode node, DaImportResult.Item item) {
            ArrDescItem descItem = new ArrDescItem();
            descItem.setItemType(item.itemType());
            descItem.setItemSpec(item.itemSpec());
            descItem.setData(item.data());
            return descriptionItemService.createDescriptionItemInBatch(descItem, node, version, change, changeContext);
        }

        private boolean sameValues(DaImportPlan.Node node, Child sibling) {
            for (RulItemType itemType : node.getMatchBy()) {
                List<String> planned = plannedValues(node.getItems().stream()
                        .filter(i -> i.itemType().getItemTypeId().equals(itemType.getItemTypeId()))
                        .collect(Collectors.toList()));
                List<String> present = values(sibling.items().stream()
                        .filter(i -> i.getItemTypeId().equals(itemType.getItemTypeId()))
                        .collect(Collectors.toList()));
                if (planned.isEmpty() || !planned.equals(present)) {
                    return false;
                }
            }
            return true;
        }

        private List<Child> childrenOf(ArrNode parent) {
            return children.computeIfAbsent(parent.getNodeId(), id -> {
                List<ArrNode> nodes = levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(parent)
                        .stream().map(ArrLevel::getNode).collect(Collectors.toList());
                if (nodes.isEmpty()) {
                    return new ArrayList<>();
                }
                Map<Integer, List<ArrDescItem>> itemsByNode = descriptionItemService
                        .findByNodeIdsAndDeleteChangeIsNull(nodes.stream().map(ArrNode::getNodeId).toList())
                        .stream().collect(Collectors.groupingBy(ArrDescItem::getNodeId));
                List<Child> result = new ArrayList<>(nodes.size());
                for (ArrNode node : nodes) {
                    result.add(new Child(node, new ArrayList<>(itemsByNode.getOrDefault(node.getNodeId(), List.of()))));
                }
                return result;
            });
        }

        void finish() {
            changeContext.flush();
            if (!created.isEmpty()) {
                ruleService.conformityInfo(version.getFundVersionId(), created, NodeTypeOperation.CREATE_NODE,
                                           null, null, null);
            }
            if (!enriched.isEmpty()) {
                ruleService.conformityInfo(version.getFundVersionId(), enriched, NodeTypeOperation.SAVE_DESC_ITEM,
                                           null, null, null);
            }
            logger.info("Import AIP {}: vytvořeno úrovní {}, nalezeno existujících {}, připojeno příloh {}, rozporů {}",
                        aip.getCode(), created.size(), matched, attached, conflicts.size());
        }
    }

    /** Comparable values of items of one type: specification and value, in a stable order. */
    private static List<String> values(List<ArrDescItem> items) {
        return items.stream().map(i -> value(i.getItemSpecId(), i.getData())).sorted().toList();
    }

    private static List<String> plannedValues(List<DaImportResult.Item> items) {
        return items.stream()
                .map(i -> value(i.itemSpec() == null ? null : i.itemSpec().getItemSpecId(), i.data()))
                .sorted().toList();
    }

    private static String value(@Nullable Integer itemSpecId, @Nullable ArrData data) {
        String text = data == null ? null : data.getFulltextValue();
        return Objects.toString(itemSpecId, "") + "|" + StringUtils.normalizeSpace(StringUtils.defaultString(text));
    }
}
