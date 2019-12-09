package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.domain.ArrData;

public class ImportableItemData {

    private final ArrData data;

    private final String fulltext;

    public ImportableItemData(ArrData data, String fulltext) {
        this.data = data;
        this.fulltext = fulltext;
    }

    public ImportableItemData(ArrData data) {
        this(data, data != null ? data.getFulltextValue() : null);
    }

    public ArrData getData() {
        return data;
    }

    public String getFulltext() {
        return fulltext;
    }
}