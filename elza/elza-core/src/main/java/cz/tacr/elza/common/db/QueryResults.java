package cz.tacr.elza.common.db;

import java.util.List;

public class QueryResults<T> {

    /**
     * Total record count.
     * 
     * This might be greated then number of returned records
     */
    int recordCount;

    List<T> records;

    public QueryResults(final int recordCount, final List<T> records) {
        this.recordCount = recordCount;
        this.records = records;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public List<T> getRecords() {
        return records;
    }

}
