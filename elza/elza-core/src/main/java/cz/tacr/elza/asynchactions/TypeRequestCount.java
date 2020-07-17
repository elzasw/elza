package cz.tacr.elza.asynchactions;

public class TypeRequestCount {

    private final int nodeRequestCount;
    private final int bulkRequestCount;
    private final int outputRequestCount;

    public TypeRequestCount(int nodeRequestCount, int bulkRequestCount, int outputRequestCount) {
        this.nodeRequestCount = nodeRequestCount;
        this.bulkRequestCount = bulkRequestCount;
        this.outputRequestCount = outputRequestCount;
    }

    public int getNodeRequestCount() {
        return nodeRequestCount;
    }

    public int getBulkRequestCount() {
        return bulkRequestCount;
    }

    public int getOutputRequestCount() {
        return outputRequestCount;
    }
}
