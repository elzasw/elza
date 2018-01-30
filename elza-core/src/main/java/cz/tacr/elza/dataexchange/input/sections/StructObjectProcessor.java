package cz.tacr.elza.dataexchange.input.sections;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.dataexchange.common.items.ImportableItem.ImportableItemData;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.context.ContextSection;
import cz.tacr.elza.dataexchange.input.sections.context.ContextStructObject;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureData.State;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.StructuredObject;

public class StructObjectProcessor implements ItemProcessor {

    private final ImportContext context;

    // current section context
    private final ContextSection section;

    public StructObjectProcessor(ImportContext context) {
        this.context = context;
        this.section = context.getSections().getCurrentSection();
    }

    @Override
    public void process(Object item) {
        StructuredObject object = (StructuredObject) item;
        validateObject(object);
        ContextStructObject cso = processObject(object);
        processItems(object.getDeOrDiOrDd(), cso);
    }

    private void validateObject(StructuredObject item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Structured object id is not set");
        }
    }

    private ContextStructObject processObject(StructuredObject item) {
        ArrStructureData so = new ArrStructureData();
        so.setCreateChange(section.getCreateChange());
        so.setFund(section.getFund());
        so.setState(State.OK);
        so.setAssignable(Boolean.TRUE);

        return section.addStructObject(so, item.getId());
    }

    private void processItems(Collection<DescriptionItem> descItems, ContextStructObject cso) {
        RuleSystem rs = section.getRuleSystem();

        for (DescriptionItem descItem : descItems) {
            // resolve item type
            RuleSystemItemType rsit = rs.getItemTypeByCode(descItem.getT());
            if (rsit == null) {
                throw new DEImportException("Description item type not found, code:" + descItem.getT());
            }
            // check if structured object reference
            // dataType check is insufficient, item can be DescriptionItemUndefined
            if (descItem instanceof DescriptionItemStructObjectRef) {
                DescriptionItemStructObjectRef refItem = (DescriptionItemStructObjectRef) descItem;
                // check if referenced structured object already processed
                ContextStructObject refCso = section.getContextStructObject(refItem.getSoid());
                if (refCso == null) {
                    // not yet processed (cannot call processData directly)
                    ArrStructureItem structItem = createStructItem(rsit, descItem.getS());
                    cso.addStructItem(structItem, refItem.getSoid());
                    continue;
                }
            }
            processData(descItem, rsit, cso);
        }
    }

    private void processData(DescriptionItem descItem, RuleSystemItemType rsit, ContextStructObject cso) {
        // create data
        DataType dataType = rsit.getDataType();
        ImportableItemData itemData = descItem.createData(context, dataType);

        // update data type reference
        ArrData data = itemData.getData();
        if (data != null) {
            data.setDataType(dataType.getEntity());
        }

        // add structured item
        ArrStructureItem structItem = createStructItem(rsit, descItem.getS());
        cso.addStructItem(structItem, data);
    }

    private ArrStructureItem createStructItem(RuleSystemItemType rsit, String specCode) {
        ArrStructureItem structItem = new ArrStructureItem();
        structItem.setCreateChange(section.getCreateChange());
        structItem.setDescItemObjectId(section.generateDescItemObjectId());
        structItem.setItemType(rsit.getEntity());
        structItem.setItemSpec(SectionLevelProcessor.resolveItemSpec(rsit, specCode));
        // structItem.setData(...) - updates internally
        // structItem.setPosition(...) - updates internally
        // structItem.setStructureData(...) - updates internally
        return structItem;
    }
}
