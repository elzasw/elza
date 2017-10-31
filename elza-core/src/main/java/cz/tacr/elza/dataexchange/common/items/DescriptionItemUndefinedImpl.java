package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;

public class DescriptionItemUndefinedImpl extends DescriptionItemUndefined {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType == null) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        return new ImportableItemData(null, null);
    }
}
