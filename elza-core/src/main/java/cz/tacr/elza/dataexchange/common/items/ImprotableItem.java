package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.sections.context.ContextNode;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;

public interface ImprotableItem {

    /**
     * Creates new data for import. Implementation shouldn't set item reference and data type.
     *
     * @see ContextNode#addDescItem(ArrDescItem, ArrData)
     */
    default ArrData createData(ImportContext context, DataType dataType) {
        throw new UnsupportedOperationException();
    }
}
