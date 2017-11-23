package cz.tacr.elza.controller.vo;

/**
 * ArrFile Value object
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public class ArrFileVO extends DmsFileVO {

    private Integer fundId;

    private Boolean editable;
    private Boolean generatePdf;

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(Integer fundId) {
        this.fundId = fundId;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Boolean getGeneratePdf() {
        return generatePdf;
    }

    public void setGeneratePdf(Boolean generatePdf) {
        this.generatePdf = generatePdf;
    }
}
