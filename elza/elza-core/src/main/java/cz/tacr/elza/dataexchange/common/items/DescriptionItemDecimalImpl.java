package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.schema.v2.DescriptionItemDecimal;

public class DescriptionItemDecimalImpl extends DescriptionItemDecimal {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.DECIMAL) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        if (getV().precision() > dataType.getValueMaxSize()) {
            throw new DEImportException("Value exceeded max size, data type:" + dataType + ", maxSize:" + dataType.getValueMaxSize()
                    + ", value:" + getV());
        }
        ArrDataDecimal data = new ArrDataDecimal();
        data.setValue(getV());
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data);
    }
}
