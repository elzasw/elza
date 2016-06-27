package cz.tacr.elza.controller.vo;

/**
 * ArrFile Value object
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public class ArrFileVO extends DmsFileVO {

    private Integer fundId;

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(Integer fundId) {
        this.fundId = fundId;
    }
}
