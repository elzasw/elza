package cz.tacr.elza.dataexchange.input.sections;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.common.items.ImportableItemData;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.dataexchange.input.sections.context.StructObjContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.StructuredObject;

public class StructObjProcessor implements ItemProcessor {

    private final ImportContext context;

    // current section context
    private final SectionContext section;

    public StructObjProcessor(ImportContext context) {
        this.context = context;
        this.section = context.getSections().getCurrentSection();
    }

    @Override
    public void process(Object item) {
        StructuredObject object = (StructuredObject) item;
        StructObjContext objectCtx = processObject(object);
        processItems(object.getDdOrDoOrDp(), objectCtx);
    }

    private StructObjContext processObject(StructuredObject item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Structured object id is not set");
        }
        ArrStructuredObject entity = new ArrStructuredObject();
        entity.setCreateChange(section.getCreateChange());
        entity.setFund(section.getFund());
        entity.setState(State.OK);
        entity.setAssignable(Boolean.TRUE);

        return section.addStructObject(entity, item.getId());
    }

    private void processItems(Collection<DescriptionItem> items, StructObjContext structObjCtx) {
        StaticDataProvider staticData = section.getStaticData();
        // process structured object items
        for (DescriptionItem item : items) {
            ItemType rsit = staticData.getItemTypeByCode(item.getT());
            if (rsit == null) {
                throw new DEImportException("Description item type not found, code:" + item.getT());
            }
            processData(item, rsit, structObjCtx);
        }
    }

    private void processData(DescriptionItem item, ItemType rsit, StructObjContext structObjCtx) {
        // check if structured object reference
        // - dataType check is insufficient, item can be DescriptionItemUndefined
        if (item instanceof DescriptionItemStructObjectRef) {
            DescriptionItemStructObjectRef refItem = (DescriptionItemStructObjectRef) item;
            // check if referenced structured object already processed
            StructObjContext refObj = section.getStructObject(refItem.getSoid());
            if (refObj == null) {
                // not yet processed -> cannot create data immediately
                ArrStructuredItem structItem = createStructItem(rsit, item.getS());
                structObjCtx.addStructObjRef(structItem, refItem.getSoid());
                return;
            }
        }
        // create data
        DataType dataType = rsit.getDataType();
        ImportableItemData itemData = item.createData(context, dataType);
        ArrData data = itemData.getData();
        // add structured item
        ArrStructuredItem structItem = createStructItem(rsit, item.getS());
        structObjCtx.addStructItem(structItem, data);
    }

    private ArrStructuredItem createStructItem(ItemType rsit, String specCode) {
        ArrStructuredItem structItem = new ArrStructuredItem();
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
