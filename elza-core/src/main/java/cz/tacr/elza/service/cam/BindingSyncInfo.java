package cz.tacr.elza.service.cam;

public class BindingSyncInfo {

    private final int id;

    private final int externalSystemId;

    private final String lastTransaction;

    private final Integer page;

    public BindingSyncInfo(int id, int externalSystemId, String lastTransaction, Integer page) {
        this.id = id;
        this.externalSystemId = externalSystemId;
        this.lastTransaction = lastTransaction;
        this.page = page;
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

    public Integer getPage() {
        return page;
    }
}
