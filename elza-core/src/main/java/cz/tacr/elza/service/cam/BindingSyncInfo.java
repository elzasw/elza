package cz.tacr.elza.service.cam;

public class BindingSyncInfo {

    private final int id;

    private final int externalSystemId;

    private final String lastTransaction;

    private final String toTransaction;

    private final Integer page;

    private final Integer count;

    public BindingSyncInfo(int id, int externalSystemId, String lastTransaction, String toTransaction, Integer page, Integer count) {
        this.id = id;
        this.externalSystemId = externalSystemId;
        this.lastTransaction = lastTransaction;
        this.toTransaction = toTransaction;
        this.page = page;
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public int getExternalSystemId() {
        return externalSystemId;
    }

    public String getLastTransaction() {
        return lastTransaction;
    }

    public String getToTransaction() {
        return toTransaction;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getCount() {
        return count;
    }
}