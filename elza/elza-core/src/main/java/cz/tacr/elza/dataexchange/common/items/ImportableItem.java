package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;

public interface ImportableItem {

    /**
     * Creates new item data for import.
     *
     * @return Importable item data, not-null.
     *
     * @see ContextNode#addDescItem(ArrDescItem, ArrData)
     */
    default ImportableItemData createData(ImportContext context, DataType dataType) {
        throw new UnsupportedOperationException();
    }
}
