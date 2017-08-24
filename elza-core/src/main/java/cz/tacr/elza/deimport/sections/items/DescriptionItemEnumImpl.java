package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;

public class DescriptionItemEnumImpl extends DescriptionItemEnum {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.ENUM;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        return new ArrDataNull();
    }
}
