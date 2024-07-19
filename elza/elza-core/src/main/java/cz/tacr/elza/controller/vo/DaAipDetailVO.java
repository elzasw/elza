package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.*;
import java.time.LocalDateTime;

public class DaAipDetailVO {

    private Integer aipId;

    private String code;

    private Integer digitalRepositoryId;

    private String aipVersion;

    private ArrFundVO fund;

    private ParInstitutionVO institution;

    private ParInstitutionVO originatorInstitution;

    private LocalDateTime unitdateFrom;

    private LocalDateTime unitdateTo;

    private String originator;

    private String institutionCode;

    private String ingestionCode;

    private String referenceNumber;

    private String nadChangeCode;

    private Long aipSize;

    private Boolean metadataLoad;

    private DaSyncQueueItem.QueueItemState state;


    public Integer getAipId() {
        return aipId;
    }

    public void setAipId(Integer aipId) {
        this.aipId = aipId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public void setDigitalRepositoryId(Integer digitalRepositoryId) {
        this.digitalRepositoryId = digitalRepositoryId;
    }

    public String getAipVersion() {
        return aipVersion;
    }

    public void setAipVersion(String aipVersion) {
        this.aipVersion = aipVersion;
    }

    public ParInstitutionVO getInstitution() {
        return institution;
    }

    public void setInstitution(ParInstitutionVO institution) {
        this.institution = institution;
    }

    public ParInstitutionVO getOriginatorInstitution() {
        return originatorInstitution;
    }

    public void setOriginatorInstitution(ParInstitutionVO originatorInstitution) {
        this.originatorInstitution = originatorInstitution;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public String getOriginator() {
        return originator;
    }

    public ArrFundVO getFund() {
        return fund;
    }

    public void setFund(ArrFundVO fund) {
        this.fund = fund;
    }
//
//    public String getInstApName() {
//        return instApName;
//    }
//
//    public void setInstApName(String instApName) {
//        this.instApName = instApName;
//    }
//
//    public String getInstitutionCode() {
//        return institutionCode;
//    }
//
//    public void setInstitutionCode(String institutionCode) {
//        this.institutionCode = institutionCode;
//    }

    public LocalDateTime getUnitdateFrom() {
        return unitdateFrom;
    }

    public void setUnitdateFrom(LocalDateTime unitdateFrom) {
        this.unitdateFrom = unitdateFrom;
    }

    public LocalDateTime getUnitdateTo() {
        return unitdateTo;
    }

    public void setUnitdateTo(LocalDateTime unitdateTo) {
        this.unitdateTo = unitdateTo;
    }

//    public String getOriginApName() {
//        return originApName;
//    }
//
//    public void setOriginApName(String originApName) {
//        this.originApName = originApName;
//    }

    public String getIngestionCode() {
        return ingestionCode;
    }

    public void setIngestionCode(String ingestionCode) {
        this.ingestionCode = ingestionCode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNadChangeCode() {
        return nadChangeCode;
    }

    public void setNadChangeCode(String nadChangeCode) {
        this.nadChangeCode = nadChangeCode;
    }

    public Long getAipSize() {
        return aipSize;
    }

    public void setAipSize(Long aipSize) {
        this.aipSize = aipSize;
    }

    public Boolean getMetadataLoad() {
        return metadataLoad;
    }

    public void setMetadataLoad(Boolean metadataLoad) {
        this.metadataLoad = metadataLoad;
    }

    public DaSyncQueueItem.QueueItemState getState() {
        return state;
    }

    public void setState(DaSyncQueueItem.QueueItemState state) {
        this.state = state;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }
}
