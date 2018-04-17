package cz.tacr.elza.bulkaction.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelService;

/**
 * @author <a href="mailto:jiri.vanek@marbes.cz">Jiří Vaněk</a>
 */
public class PersistentSortBulkAction extends BulkAction {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    private PersistentSortConfig config;

    private PersistentSortRunConfig runConfig;

    private Queue<ArrLevel> queue = new LinkedList<>();

    private RulItemType itemType;

    private RulItemSpec itemSpec;

    public PersistentSortBulkAction(PersistentSortConfig persistentSortConfig) {
        Validate.notNull(persistentSortConfig);

        this.config = persistentSortConfig;
    }

    @Override
    protected void init(ArrBulkActionRun bulkActionRun) {
        super.init(bulkActionRun);

        String jsonConfig = bulkActionRun.getConfig();

        if (jsonConfig == null) {
            throw new SystemException("Chybí nastavení hromadné akce " + getName(), BaseCode.SYSTEM_ERROR);
        }
        try {
            runConfig = objectMapper.readValue(jsonConfig, PersistentSortRunConfig.class);
        } catch (IOException e) {
            throw new SystemException("Problém při parsování JSON", e, BaseCode.JSON_PARSE);
        }

        itemType = itemTypeRepository.findOneByCode(runConfig.getItemTypeCode());
        if (itemType == null) {
            throw new SystemException(
                    "Hromadná akce " + getName() + " je nakonfigurována pro neexistující typ atributu:",
                    BaseCode.SYSTEM_ERROR).set("itemTypeCode", runConfig.getItemTypeCode());
        }

        String dataTypeCode = itemType.getDataType().getCode();
        List<String> allowedDataTypes = Arrays.asList("INT", "DECIMAL", "STRING", "TEXT", "FORMATTED_TEXT", "UNITDATE");
        if (!allowedDataTypes.contains(dataTypeCode)) {
            throw new SystemException(
                    "Hromadná akce " + getName() + " je nakonfigurována pro nepodporovaný datový typ:",
                    BaseCode.SYSTEM_ERROR).set("dataTypeCode", dataTypeCode);
        }

        if (itemType.getUseSpecification()) {
            if (StringUtils.isBlank(runConfig.getItemSpecCode())) {
                throw new SystemException(
                        "Hromadná akce " + getName() + " musí mít nastavenu specifikaci pro typ atributu:",
                        BaseCode.SYSTEM_ERROR).set("itemTypeCode", runConfig.getItemTypeCode());
            }

            itemSpec = itemSpecRepository.findOneByCode(runConfig.getItemSpecCode());
            if (itemSpec == null) {
                throw new SystemException(
                        "Hromadná akce " + getName() + " je nakonfigurována pro neexistující specifikaci:",
                        BaseCode.SYSTEM_ERROR).set("itemSpecCode", runConfig.getItemSpecCode());
            }
        }
    }

    @Override
    public void run(ActionRunContext runContext) {
        for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrNode nodeRef = nodeRepository.getOne(nodeId);
            ArrLevel level = levelRepository.findByNodeAndDeleteChangeIsNull(nodeRef);
            Validate.notNull(level);

            queue.add(level);
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.BULK_ACTION);
        while (!queue.isEmpty()) {
            sort(queue.poll(), change);
        }
    }

    private void sort(ArrLevel parent, ArrChange change) {
        List<ArrLevel> children = levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(parent.getNode());
        if (children.isEmpty()) {
            return;
        }

        List<ArrNode> nodes = new ArrayList<>(children.size());
        for (ArrLevel level : children) {
            nodes.add(level.getNode());
            if (runConfig.isSortChildren() && !queue.contains(level)) {
                queue.add(level);
            }
        }

        Map<Integer, Comparable> nodesValues = getNodeValues(nodes);

        Comparator<ArrLevel> comparator = new PersistentSortComparator(nodesValues);

        if (runConfig.isAsc()) {
            comparator = comparator.reversed();
        }
        children.sort(comparator);

        for (int i = 1; i <= children.size(); i++) {
            children.get(i - 1).setPosition(i);
        }

        fundLevelService.shiftNodes(children, change, 1);
    }

    private Map<Integer, Comparable> getNodeValues(List<ArrNode> nodes) {
        List<ArrDescItem> descItems;
        if (itemSpec == null) {
            descItems = descItemRepository.findOpenByNodesAndType(nodes, itemType);
        } else {
            descItems = descItemRepository.findOpenByNodesAndTypeAndSpec(nodes, itemType, Collections.singletonList(itemSpec));
        }

        Map<Integer, List<ArrDescItem>> nodeIdToDescItemList = descItems.stream().collect(Collectors.groupingBy(ArrDescItem::getNodeId));

        Map<Integer, Comparable> nodeValues = new HashMap<>();
        String dataTypeCode = itemType.getDataType().getCode();
        for (Integer nodeId : nodeIdToDescItemList.keySet()) {
            List<ArrDescItem> arrDescItems = nodeIdToDescItemList.get(nodeId);

            Comparable value;
            switch (dataTypeCode) {
                case "STRING":
                case "TEXT":
                case "FORMATTED_TEXT":
                    value = getNodeValue(arrDescItems, ArrDescItem::getFulltextValue);
                    break;
                case "INT":
                    value = getNodeValue(arrDescItems, ArrDescItem::getValueInt);
                    break;
                case "DECIMAL":
                    value = getNodeValue(arrDescItems, ArrDescItem::getValueDouble);
                    break;
                case "UNITDATE":
                    value = getNodeValue(arrDescItems, ArrDescItem::getNormalizedFrom);
                    break;
                default:
                    throw new SystemException("Hromadná akce " + getName() + " nepodporuje datový typ:", BaseCode.SYSTEM_ERROR).
                            set("dataTypeCode", dataTypeCode);
            }
            if (value != null) {
                nodeValues.put(nodeId, value);
            }
        }
        return nodeValues;
    }

    private Comparable getNodeValue(List<ArrDescItem> descItems, Function<ArrDescItem, Comparable> valueFunction) {
        Comparable value;
        if (CollectionUtils.isEmpty(descItems)) {
            value = null;
        } else if (descItems.size() == 1) {
            value = valueFunction.apply(descItems.iterator().next());
        } else {
            List<Comparable> values =  descItems.stream().
                    map(i -> valueFunction.apply(i)).
                    sorted().
                    collect(Collectors.toList());

            if (runConfig.isAsc()) {
                value = values.iterator().next();
            } else {
                value = Iterables.getLast(values);
            }
        }
        return value;
    }


    @Override
    public String getName() {
        return "PersistentSortBulkAction";
    }

    private class PersistentSortComparator implements Comparator<ArrLevel> {

        private Map<Integer, Comparable> nodeValues;

        public PersistentSortComparator(Map<Integer, Comparable> nodeValues) {
            this.nodeValues = nodeValues;
        }

        @Override
        public int compare(ArrLevel o1, ArrLevel o2) {
            Comparable v1 = nodeValues.get(o1.getNodeId());
            Comparable v2 = nodeValues.get(o2.getNodeId());

            if (v1 == null && v2 == null) {
                return 0;
            } else if (v1 == null) {
                return 1;
            } else if (v2 == null) {
                return -1;
            } else {
                return v2.compareTo(v1);
            }
        }
    }
}
