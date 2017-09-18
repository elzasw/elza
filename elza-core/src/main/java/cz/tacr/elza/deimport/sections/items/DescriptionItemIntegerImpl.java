package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

public class DescriptionItemIntegerImpl extends DescriptionItemInteger {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.INT;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        ArrDataInteger data = new ArrDataInteger();
        data.setValue(getV().intValueExact());
        return data;
    }
}
