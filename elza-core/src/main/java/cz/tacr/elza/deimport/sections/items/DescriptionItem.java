package cz.tacr.elza.deimport.sections.items;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.sections.context.ContextNode;
import cz.tacr.elza.deimport.sections.context.ContextSection;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.schema.v2.AbstractDescriptionItem;

/**
 * Implements general layer for description item import. Manually declared class between concrete
 * description item and abstract class from specification.
 */
public class DescriptionItem extends AbstractDescriptionItem {

    @Override
    public final void importItem(ContextNode contextNode, ImportContext context) {
        ContextSection section = contextNode.getSection();
        RuleSystemItemType itemType = resolveItemType(section.getRuleSystem());
        ArrDescItem descItem = createDescItem(section, itemType);
        DataType dataType = itemType.getDataType();
        ArrData data = createData(context, dataType);
        if (data != null) {
            data.setDataType(dataType.getEntity());
            descItem.setUndefined(false);
        }
        contextNode.addDescItem(descItem, data);
    }

    protected boolean isDataTypeSupported(DataType dataType) {
        return false;
    }

    /**
     * Creates new data. Implementation shouldn't set item reference and data type.
     *
     * @see ContextNode#addDescItem(ArrDescItem, ArrData)
     */
    protected ArrData createData(ImportContext context, DataType dataType) {
        return null;
    }

    private RuleSystemItemType resolveItemType(RuleSystem ruleSystem) {
        RuleSystemItemType itemType = ruleSystem.getItemTypeByCode(getT());
        if (itemType == null) {
            throw new DEImportException("Description item type not found, code:" + getT());
        }
        if (isDataTypeSupported(itemType.getDataType())) {
            return itemType;
        }
        throw new DEImportException(
                "Item type not supported, data type:" + itemType.getDataType() + ", xml type:" + getClass().getSimpleName());
    }

    private ArrDescItem createDescItem(ContextSection section, RuleSystemItemType itemType) {
        ArrDescItem descItem = new ArrDescItem(section.getFund().getFundId());
        descItem.setCreateChange(section.getCreateChange());
        descItem.setDescItemObjectId(section.generateDescItemObjectId());
        descItem.setItemType(itemType.getEntity());
        descItem.setUndefined(true);

        // resolve item spec
        boolean specCodeExists = StringUtils.isNotEmpty(getS());
        if (itemType.hasSpecifications()) {
            if (specCodeExists) {
                RulItemSpec itemSpec = itemType.getItemSpecByCode(getS());
                if (itemSpec == null) {
                    throw new DEImportException(
                            "Description item specification not found, typeCode:" + getT() + ", specCode:" + getS());
                }
                descItem.setItemSpec(itemSpec);
            } else {
                throw new DEImportException("Description item specification missing, typeCode:" + getT() + ", specCode:" + getS());
            }
        } else if (specCodeExists) {
            throw new DEImportException(
                    "Specification for description item not expected, typeCode:" + getT() + ", specCode:" + getS());
        }
        return descItem;
    }
}
