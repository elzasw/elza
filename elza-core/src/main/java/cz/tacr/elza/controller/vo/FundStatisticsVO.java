package cz.tacr.elza.controller.vo;

public class FundStatisticsVO implements Comparable<FundStatisticsVO> {

    private final ArrFundVO fund;

    private int requestCount;

    private final Integer fundVersionId;

    public FundStatisticsVO(Integer fundVersionId, final ArrFundVO fund) {
        this.fundVersionId = fundVersionId;
        this.fund = fund;
        this.requestCount = 0;
    }

    public ArrFundVO getFund() {
        return fund;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void addCount() {
        this.requestCount++;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    public int compareTo(FundStatisticsVO o) {
        if (this.getRequestCount() > o.getRequestCount()) {
            return 1;
        } else if (this.getRequestCount() < o.getRequestCount()) {
            return -1;
        } else {
            return 0;
        }
    }
}
