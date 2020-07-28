package cz.tacr.elza.controller.vo;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.service.attachment.AttachmentService;

import static cz.tacr.elza.repository.ExceptionThrow.fund;

/**
 * ArrFile Value object
 *
 *
 * @since 13.3.2016
 */
public class ArrFileVO extends DmsFileVO {

    private Integer fundId;

    private Boolean editable;
    private Boolean generatePdf;

    public ArrFileVO() {

    }

    public ArrFileVO(ArrFile srcFile) {
        super(srcFile);
        this.fundId = srcFile.getFund().getFundId();
        //this.editable = srcFile.get
    }

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

    static public ArrFileVO newInstance(ArrFile srcFile, AttachmentService attachmentService) {
        ArrFileVO result = new ArrFileVO(srcFile);
        result.setEditable(attachmentService.isEditable(srcFile.getMimeType()));
        result.setGeneratePdf(attachmentService.supportGenerateTo(srcFile, "application/pdf"));
        return result;
    }

    public ArrFile createEntity(FundRepository fundRepository) {
        ArrFile result = new ArrFile();
        if (fundId != null) {
            ArrFund fund = fundRepository.findById(fundId)
                    .orElseThrow(fund(fundId));
            result.setFund(fund);
        }
        copyTo(result);
        return result;
    }
}
