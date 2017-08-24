package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.schema.v2.DescriptionItemDecimal;

public class DescriptionItemDecimalImpl extends DescriptionItemDecimal {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.DECIMAL;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        ArrDataDecimal data = new ArrDataDecimal();
        data.setValue(getV());
        return data;
    }
}
