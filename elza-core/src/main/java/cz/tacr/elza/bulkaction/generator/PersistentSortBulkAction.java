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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.core.data.DataType;
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
 * Funkce řadící uzly archivního souboru podle hodnot jednotek popisu.
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

    private Function<ArrDescItem, Comparable> valueFunction;

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

        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        List<DataType> allowedDataTypes = Arrays.asList(DataType.INT, DataType.DECIMAL, DataType.STRING, DataType.TEXT,
                DataType.FORMATTED_TEXT, DataType.UNITDATE);
        if (!allowedDataTypes.contains(dataType)) {
            throw new SystemException(
                    "Hromadná akce " + getName() + " je nakonfigurována pro nepodporovaný datový typ:",
                    BaseCode.SYSTEM_ERROR).set("dataTypeCode", dataType.getCode());
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

        valueFunction = getValueFunction(dataType);
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

    /**
     * Seřazení potomků předané úrovně.
     * @param parent úroveň jejíž potomci se mají seřadit
     * @param change změna v rámci které se řazení děje
     */
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

    /**
     * @return mapa id node -> hodnota, pokud uzel hodnotu nemá tak v mapě není
     */
    private Map<Integer, Comparable> getNodeValues(List<ArrNode> nodes) {
        List<ArrDescItem> descItems;
        if (itemSpec == null) {
            descItems = descItemRepository.findOpenByNodesAndType(nodes, itemType);
        } else {
            descItems = descItemRepository.findOpenByNodesAndTypeAndSpec(nodes, itemType, Collections.singletonList(itemSpec));
        }

        Map<Integer, Comparable> nodeValues = new HashMap<>();
        for (ArrDescItem arrDescItem : descItems) {
            Comparable newValue = valueFunction.apply(arrDescItem);
            Comparable currentValue = nodeValues.get(arrDescItem.getNodeId());
            if (currentValue == null) { // v mapě ještě hodnota není
                if (newValue != null) { // a nová hodnota není null tak si ji uložíme
                    nodeValues.put(arrDescItem.getNodeId(), newValue);
                }
            } else if (newValue != null) {
                int compareTo = currentValue.compareTo(newValue);
                if (runConfig.isAsc() && compareTo == -1) { // podle způsobu řazení zjistíme jestli je nová hodnota lepší než ta co už máme(pro typy atributů s více hodnotami)
                    nodeValues.put(arrDescItem.getNodeId(), newValue);
                } else if (!runConfig.isAsc() && compareTo == 1) {
                    nodeValues.put(arrDescItem.getNodeId(), newValue);
                }
            }
        }

        return nodeValues;
    }

    /**
     * @return vrátí metodu pro získání hodnoty z {@link ArrDescItem}
     */
    private Function<ArrDescItem,Comparable> getValueFunction(DataType dataType) {
        Function<ArrDescItem,Comparable> valueFuncion;
        switch (dataType) {
            case STRING:
            case TEXT:
            case FORMATTED_TEXT:
                valueFuncion = ArrDescItem::getFulltextValue;
                break;
            case INT:
                valueFuncion = ArrDescItem::getValueInt;
                break;
            case DECIMAL:
                valueFuncion = ArrDescItem::getValueDouble;
                break;
            case UNITDATE:
                valueFuncion = ArrDescItem::getNormalizedFrom;
                break;
            default:
                throw new SystemException("Hromadná akce " + getName() + " nepodporuje datový typ:", BaseCode.SYSTEM_ERROR).
                        set("dataTypeCode", dataType.getCode());
        }

        return valueFuncion;
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
