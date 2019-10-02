package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

public class DescriptionItemIntegerImpl extends DescriptionItemInteger {

    /**
     * @return Value or null when BigInteger is out of the range of the int type.
     */
    public Integer toInteger() {
        try {
            return getV().intValueExact();
        } catch (ArithmeticException e) {
            return null;
        }
    }

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.INT) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        Integer value = toInteger();
        if (value == null || value.compareTo(dataType.getValueMaxSize()) > 0) {
            throw new DEImportException("Value exceeded max size, data type:" + dataType + ", maxSize:" + dataType.getValueMaxSize()
                    + ", value:" + getV());
        }
        ArrDataInteger data = new ArrDataInteger();
        data.setValue(value);
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data);
    }
}
