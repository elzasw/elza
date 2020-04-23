package cz.tacr.elza.dataexchange.input.sections;

import java.util.Collection;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.common.items.ImportableItemData;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.context.NodeContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.schema.v2.AccessPointRefs;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.Level;

/**
 * Process item(level) for given section
 *
 */
public class SectionLevelProcessor implements ItemProcessor {

    private final ImportContext context;

    // Current section context
    private final SectionContext section;

    public SectionLevelProcessor(ImportContext context) {
        this.context = context;
        this.section = context.getSections().getCurrentSection();
    }

    @Override
    public void process(Object item) {
        Level level = (Level) item;
        ArrNode node = createNode(level);
        NodeContext contextNode = createContextNode(level, node);
        processSubEntities(level, contextNode);
    }

    private ArrNode createNode(Level item) {
        ArrNode node = new ArrNode();
        node.setFund(section.getFund());
        node.setLastUpdate(section.getCreateChange().getChangeDate().toLocalDateTime());
        node.setUuid(section.generateNodeUuid());
        return node;
    }

    /**
     * Create context node and sets ArrLevel.
     */
    private NodeContext createContextNode(Level item, ArrNode node) {
        String importId = item.getId();
        if (StringUtils.isEmpty(importId)) {
            throw new DEImportException("Level id is not set");
        }
        String parentImportId = item.getPid();
        // root node needs special processing
        if (StringUtils.isEmpty(parentImportId)) {
            return section.setRootNode(node, importId);
        }
        // get parent context and append as child node
        NodeContext parentNode = section.getNode(parentImportId);
        if (parentNode == null) {
            throw new DEImportException("Parent for level not found, parentLevelId:" + parentImportId);
        }
        return parentNode.addChildNode(node, importId);
    }

    private void processSubEntities(Level item, NodeContext node) {
        try {
            processDescItems(item.getDdOrDoOrDp(), node);
        } catch (DEImportException e) {
            throw new DEImportException(
                    "Fund level cannot be processed, levelId:" + item.getId() + ", detail:" + e.getMessage(), e);
        }
    }

    private void processDescItems(Collection<DescriptionItem> items, NodeContext node) {
        StaticDataProvider ruleSystem = section.getStaticData();

        for (DescriptionItem item : items) {
            // resolve item type
            ItemType itemType = ruleSystem.getItemTypeByCode(item.getT());
            if (itemType == null) {
                throw new DEImportException("Description item type not found, code:" + item.getT());
            }
            // create data
            DataType dataType = itemType.getDataType();

            if(itemType.getDataType() == DataType.STRING && itemType.getEntity().getStringLengthLimit() != null) {
                if(((DescriptionItemString) item).getV().length() > itemType.getEntity().getStringLengthLimit()) {
                    throw new BusinessException("Délka řetězce : " + ((DescriptionItemString) item).getV()
                            + " je delší než maximální povolená : " +itemType.getEntity().getStringLengthLimit(), BaseCode.INVALID_LENGTH);
                }
            }

            ImportableItemData itemData = item.createData(context, dataType);
            ArrData data = itemData.getData();

            DescItemIndexData indexData = new DescItemIndexData(section.getFund().getFundId(), itemData.getFulltext(),
                    data);
            ArrDescItem descItem = createDescItem(section, itemType, item.getS(), indexData);

            node.addDescItem(descItem, data);
        }
    }

    private ArrDescItem createDescItem(SectionContext section,
                                       ItemType rsit,
                                       String specCode,
                                       ArrDescItemIndexData indexData) {
        ArrDescItem descItem = new ArrDescItem(indexData);

        // set common properties
        descItem.setCreateChange(section.getCreateChange());
        descItem.setDescItemObjectId(section.generateDescItemObjectId());
        descItem.setItemType(rsit.getEntity());
        descItem.setItemSpec(resolveItemSpec(rsit, specCode));

        return descItem;
    }

    public static RulItemSpec resolveItemSpec(ItemType rsit, String specCode) {
        boolean specCodeExists = StringUtils.isNotEmpty(specCode);
        String typeCode = rsit.getCode();

        if (rsit.hasSpecifications()) {
            if (specCodeExists) {
                RulItemSpec itemSpec = rsit.getItemSpecByCode(specCode);
                if (itemSpec == null) {
                    throw new DEImportException(
                            "Description item specification not found, typeCode:" + typeCode + ", specCode:"
                                    + specCode);
                }
                return itemSpec;
            } else {
                throw new DEImportException(
                        "Description item specification missing, typeCode:" + typeCode + ", specCode:" + specCode);
            }
        } else if (specCodeExists) {
            throw new DEImportException(
                    "Specification for description item not expected, typeCode:" + typeCode + ", specCode:" + specCode);
        }
        return null;
    }
}
