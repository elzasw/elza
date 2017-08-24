package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;

public class DescriptionItemUndefinedImpl extends DescriptionItemUndefined {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType != null;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        return null;
    }
}
