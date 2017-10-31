package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.sections.context.ContextNode;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;

public interface ImportableItem {

    /**
     * Creates new item data for import. Implementation shouldn't set item reference and data type.
     *
     * @return Importable item data, not-null.
     *
     * @see ContextNode#addDescItem(ArrDescItem, ArrData)
     */
    default ImportableItemData createData(ImportContext context, DataType dataType) {
        throw new UnsupportedOperationException();
    }

    public static class ImportableItemData {

        private final ArrData data;

        private final String fulltext;

        public ImportableItemData(ArrData data, String fulltext) {
            this.data = data;
            this.fulltext = fulltext;
        }

        public ImportableItemData(ArrData data) {
            this(data, data.getFulltextValue());
        }

        public ArrData getData() {
            return data;
        }

        public String getFulltext() {
            return fulltext;
        }
    }
}
