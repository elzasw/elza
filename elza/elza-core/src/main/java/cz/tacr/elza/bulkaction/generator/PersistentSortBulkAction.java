package cz.tacr.elza.bulkaction.generator;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.core.ElzaLocale;
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
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.FundLevelService;

/**
 * Funkce řadící uzly archivního souboru podle hodnot jednotek popisu.
 */
public class PersistentSortBulkAction extends BulkAction {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ElzaLocale elzaLocale;

    private PersistentSortRunConfig runConfig;

    private Queue<ArrLevel> queue = new LinkedList<>();

    private RulItemType itemType;

    private RulItemSpec itemSpec;

    private Comparator<ArrDescItem> valueComparator;

    public PersistentSortBulkAction(PersistentSortConfig persistentSortConfig) {
        Validate.notNull(persistentSortConfig);
    }

    @Override
    protected void init(ArrBulkActionRun bulkActionRun) {
        super.init(bulkActionRun);

        String jsonConfig = bulkActionRun.getConfig();

        if (jsonConfig == null) {
            throw createConfigException("Missing runtime configuration");
        }
        try {
            runConfig = objectMapper.readValue(jsonConfig, PersistentSortRunConfig.class);
        } catch (IOException e) {
            throw createConfigException("Failed to parse runtime configuration")
                    .set("jsonConfig", jsonConfig);
        }

        itemType = itemTypeRepository.findOneByCode(runConfig.getItemTypeCode());
        if (itemType == null) {
            throw createConfigException("Item type not found")
                    .set("itemTypeCode", runConfig.getItemTypeCode());
        }

        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        List<DataType> allowedDataTypes = Arrays.asList(DataType.INT, DataType.DECIMAL, DataType.STRING, DataType.TEXT,
                DataType.FORMATTED_TEXT, DataType.UNITDATE);
        if (!allowedDataTypes.contains(dataType)) {
            throw createConfigException("Unsupported data type").set("dataTypeCode", dataType.getCode());
        }

        if (itemType.getUseSpecification()) {
            if (StringUtils.isBlank(runConfig.getItemSpecCode())) {
                throw createConfigException("Missing specification.")
                        .set("itemTypeCode", runConfig.getItemTypeCode());
            }

            itemSpec = itemSpecRepository.findOneByCode(runConfig.getItemSpecCode());
            if (itemSpec == null) {
                throw createConfigException("Specification not found.")
                        .set("itemSpecCode", runConfig.getItemSpecCode());
            }
        }

        valueComparator = getValueComparator(dataType);
    }

    @Override
    public void run(ActionRunContext runContext) {
        for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrNode nodeRef = nodeRepository.getOne(nodeId);
            ArrLevel level = levelRepository.findByNodeAndDeleteChangeIsNull(nodeRef);
            Validate.notNull(level);

            queue.add(level);
        }

        ArrChange change = runContext.getChange();
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

        Map<Integer, ArrDescItem> nodesValues = getNodeValues(nodes);

        Comparator<ArrLevel> comparator = new PersistentSortComparator(nodesValues);

        if (!runConfig.isAsc()) {
            comparator = comparator.reversed();
        }
        children.sort(comparator);

        fundLevelService.changeLevelsPosition(children, change);
    }

    /**
     * @return mapa id node -> hodnota, pokud uzel hodnotu nemá tak v mapě není
     */
    private Map<Integer, ArrDescItem> getNodeValues(List<ArrNode> nodes) {
        List<ArrDescItem> descItems;
        if (itemSpec == null) {
            descItems = descItemRepository.findOpenByNodesAndType(nodes, itemType);
        } else {
            descItems = descItemRepository.findOpenByNodesAndTypeAndSpec(nodes, itemType, Collections.singletonList(itemSpec));
        }

        Map<Integer, ArrDescItem> nodeValues = new HashMap<>();
        for (ArrDescItem arrDescItem : descItems) {
            ArrDescItem currentValue = nodeValues.get(arrDescItem.getNodeId());
            if (currentValue != null) {
                // hodnota je v mapě
                // zjistíme, zda neexistuje lepší
                int result = this.valueComparator.compare(currentValue, arrDescItem);
                // podle způsobu řazení zjistíme jestli je nová hodnota
                // lepší než ta co už máme(pro typy atributů s více hodnotami)
                if (runConfig.isAsc() && result < 0) {
                    nodeValues.put(arrDescItem.getNodeId(), arrDescItem);
                } else if (!runConfig.isAsc() && result > 0) {
                    nodeValues.put(arrDescItem.getNodeId(), arrDescItem);
                }
            } else {
                // vložíme vždy
                nodeValues.put(arrDescItem.getNodeId(), arrDescItem);
            }
        }

        return nodeValues;
    }

    /**
     * @return vrátí metodu pro získání hodnoty z {@link ArrDescItem}
     */
    private Comparator<ArrDescItem> getValueComparator(DataType dataType) {
        switch (dataType) {
        case STRING:
        case URI_REF:
        case TEXT:
        case FORMATTED_TEXT:
            return (o1, o2) -> {
                Collator collator = elzaLocale.getCollator();
                String v1 = o1.getFulltextValue();
                String v2 = o2.getFulltextValue();
                int result = collator.compare(v1, v2);
                return result;
            };
        case INT:
            return (arg0, arg1) ->
                arg0.getValueInt().compareTo(arg1.getValueInt());
        case DECIMAL:
            return (arg0, arg1) ->
                arg0.getValueDouble().compareTo(arg1.getValueDouble());
        case UNITDATE:
            return (arg0, arg1) -> arg0.getNormalizedFrom().compareTo(arg1.getNormalizedFrom());
        case DATE:
            return (arg0, arg1) -> arg0.getValueDate().compareTo(arg1.getValueDate());
        case BIT:
            return (arg0, arg1) -> arg0.isValue().compareTo(arg1.isValue());
        default:
            throw new SystemException("Hromadná akce " + getName() + " nepodporuje datový typ:", BaseCode.SYSTEM_ERROR)
                    .set("dataTypeCode", dataType.getCode());
        }
    }

    @Override
    public String getName() {
        return "PersistentSortBulkAction";
    }

    private class PersistentSortComparator implements Comparator<ArrLevel> {

        private Map<Integer, ArrDescItem> nodeValues;

        public PersistentSortComparator(Map<Integer, ArrDescItem> nodesValues) {
            this.nodeValues = nodesValues;
        }

        @Override
        public int compare(ArrLevel o1, ArrLevel o2) {
            ArrDescItem v1 = nodeValues.get(o1.getNodeId());
            ArrDescItem v2 = nodeValues.get(o2.getNodeId());

            if (v1 == null && v2 == null) {
                return 0;
            } else if (v1 == null) {
                return 1;
            } else if (v2 == null) {
                return -1;
            } else {
                return valueComparator.compare(v1, v2);
            }
        }
    }
}
