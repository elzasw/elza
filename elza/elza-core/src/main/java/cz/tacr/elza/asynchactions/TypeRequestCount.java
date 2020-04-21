package cz.tacr.elza.asynchactions;

public class TypeRequestCount {

    private int nodeRequestCount;
    private int bulkRequestCount;
    private int outputRequestCount;

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
