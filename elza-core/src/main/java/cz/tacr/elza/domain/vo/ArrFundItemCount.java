package cz.tacr.elza.domain.vo;

public class ArrFundItemCount {

    // --- fields ---

    private final Integer fundId;
    private final int itemCount;

    // --- getters/setters ---

    public Integer getFundId() {
        return fundId;
    }

    public int getItemCount() {
        return itemCount;
    }

    // --- constructor ---

    public ArrFundItemCount(Integer fundId, int itemCount) {
        this.fundId = fundId;
        this.itemCount = itemCount;
    }
}
