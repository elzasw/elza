package cz.tacr.elza.dataexchange.input.sections;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.dataexchange.common.items.ImportableItem.ImportableItemData;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.context.ContextNode;
import cz.tacr.elza.dataexchange.input.sections.context.ContextSection;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemIndexData;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.schema.v2.AccessPointRefs;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.Level;

public class SectionLevelProcessor implements ItemProcessor {

    private final ImportContext context;

    private final ContextSection section;

    public SectionLevelProcessor(ImportContext context) {
        this.context = context;
        this.section = context.getSections().getCurrentSection();
    }

    @Override
    public void process(Object item) {
        Level level = (Level) item;
        validateLevel(level);
        ArrNode node = createNode(level);
        ContextNode contextNode = createContextNode(level, node);
        processSubEntities(level, contextNode);
    }

    private void validateLevel(Level item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Fund level id is not set");
        }
    }

    private ArrNode createNode(Level item) {
        ArrNode node = new ArrNode();
        node.setFund(section.getFund());
        node.setLastUpdate(section.getCreateChange().getChangeDate());
        node.setUuid(section.generateNodeUuid());
        return node;
    }

    /**
     * Create context node and sets ArrLevel.
     */
    private ContextNode createContextNode(Level item, ArrNode node) {
        String importId = item.getId();
        String parentImportId = item.getPid();

        if (StringUtils.isEmpty(parentImportId)) {
            return section.setRootNode(node, importId);
        }
        ContextNode parentNode = section.getContextNode(parentImportId);
        if (parentNode == null) {
            throw new DEImportException("Parent for level not found, parentLevelId:" + parentImportId);
        }
        return parentNode.addChildNode(node, importId);
    }

    private void processSubEntities(Level item, ContextNode node) {
        try {
            processAccessPointRefs(item.getAprs(), node);
            processDescItems(item.getDeOrDiOrDd(), node);
        } catch (DEImportException e) {
            throw new DEImportException("Fund level cannot be processed, levelId:" + item.getId() + ", detail:" + e.getMessage(), e);
        }
    }

    private void processAccessPointRefs(AccessPointRefs references, ContextNode node) {
        if (references == null) {
            return;
        }
        for (String apEntryId : references.getApid()) {
            AccessPointInfo apInfo = context.getAccessPoints().getAccessPointInfo(apEntryId);
            if (apInfo == null) {
                throw new DEImportException("Referenced access point not found, apeId:" + apEntryId);
            }
            ArrNodeRegister nodeRegister = new ArrNodeRegister();
            nodeRegister.setRecord(apInfo.getEntityReference(context.getSession()));
            nodeRegister.setCreateChange(section.getCreateChange());
            node.addNodeRegister(nodeRegister);
        }
    }

    private void processDescItems(Collection<DescriptionItem> items, ContextNode node) {
        RuleSystem ruleSystem = section.getRuleSystem();

        for (DescriptionItem item : items) {
            // resolve item type
            RuleSystemItemType itemType = ruleSystem.getItemTypeByCode(item.getT());
            if (itemType == null) {
                throw new DEImportException("Description item type not found, code:" + item.getT());
            }
            // create data
            DataType dataType = itemType.getDataType();
            ImportableItemData itemData = item.createData(context, dataType);

            // update data type reference
            ArrData data = itemData.getData();
            if (data != null) {
                data.setDataType(dataType.getEntity());
            }

            ImportIndexData indexData = new ImportIndexData(section.getFund().getFundId(), itemData.getFulltext(), data);
            ArrDescItem descItem = createDescItem(section, itemType, item.getS(), indexData);

            node.addDescItem(descItem, data);
        }
    }

    private ArrDescItem createDescItem(ContextSection section,
                                       RuleSystemItemType itemType,
                                       String specCode,
                                       ArrDescItemIndexData indexData) {
        ArrDescItem descItem = new ArrDescItem(indexData);

        // set common properties
        descItem.setCreateChange(section.getCreateChange());
        descItem.setDescItemObjectId(section.generateDescItemObjectId());
        descItem.setItemType(itemType.getEntity());

        // resolve item spec
        String typeCode = itemType.getCode();
        boolean specCodeExists = StringUtils.isNotEmpty(specCode);
        if (itemType.hasSpecifications()) {
            if (specCodeExists) {
                RulItemSpec itemSpec = itemType.getItemSpecByCode(specCode);
                if (itemSpec == null) {
                    throw new DEImportException(
                            "Description item specification not found, typeCode:" + typeCode + ", specCode:" + specCode);
                }
                descItem.setItemSpec(itemSpec);
            } else {
                throw new DEImportException(
                        "Description item specification missing, typeCode:" + typeCode + ", specCode:" + specCode);
            }
        } else if (specCodeExists) {
            throw new DEImportException(
                    "Specification for description item not expected, typeCode:" + typeCode + ", specCode:" + specCode);
        }

        return descItem;
    }

    private static class ImportIndexData implements ArrDescItemIndexData {

        private final Integer fundId;

        private final String fulltext;

        private final Integer valueInt;

        private final Double valueDouble;

        private final Long normalizedFrom;

        private final Long normalizedTo;

        public ImportIndexData(Integer fundId, String fulltext, ArrData data) {
            this.fundId = fundId;
            this.fulltext = fulltext;
            this.valueInt = data.getValueInt();
            this.valueDouble = data.getValueDouble();
            this.normalizedFrom = data.getNormalizedFrom();
            this.normalizedTo = data.getNormalizedTo();
        }

        @Override
        public Integer getFundId() {
            return fundId;
        }

        @Override
        public String getFulltextValue() {
            return fulltext;
        }

        @Override
        public Integer getValueInt() {
            return valueInt;
        }

        @Override
        public Double getValueDouble() {
            return valueDouble;
        }

        @Override
        public Long getNormalizedFrom() {
            return normalizedFrom;
        }

        @Override
        public Long getNormalizedTo() {
            return normalizedTo;
        }
    }
}