package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.dataexchange.input.sections.context.StructObjContext;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;

public class DescriptionItemStructObjectRefImpl extends DescriptionItemStructObjectRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.STRUCTURED) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }

        SectionContext section = context.getSections().getCurrentSection();
        StructObjContext cso = section.getStructObject(getSoid());
        if (cso == null) {
            throw new DEImportException("Referenced structured object not found, soId:" + getSoid());
        }
        ArrDataStructureRef data = new ArrDataStructureRef();
        data.setStructuredObject(cso.getIdHolder().getEntityRef(context.getSession()));
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data, null);
    }

}
