package cz.tacr.elza.controller.vo;

public class FundStatisticsVO implements Comparable<FundStatisticsVO> {

    private ArrFundVO fund;

    private int requestCount;

    private Integer fundVersionId;

    public FundStatisticsVO(Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
        this.requestCount = 0;
    }

    public ArrFundVO getFund() {
        return fund;
    }

    public void setFund(ArrFundVO fund) {
        this.fund = fund;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
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
