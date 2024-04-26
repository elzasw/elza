package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.DaAip;

import java.time.LocalDateTime;

public class DaAipVO {

    private Integer id;

    private Integer aipId;

    private String code;

    private String aipVersion;

    private Long aipSize;

    private Integer digitalRepositoryId;

    private AipType aipType;

    private Integer remoteAipId;

    private Integer fundId;

    private LocalDateTime createDate;

    private LocalDateTime lastChange;

    private LocalDateTime unitdateFrom;

    private LocalDateTime unitdateTo;

    private String originatorCode;

    private Integer originatorId;

    private String ingestionCode;

    private String refNo;

    private String changeCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getAipVersion() {
        return aipVersion;
    }

    public void setAipVersion(String aipVersion) {
        this.aipVersion = aipVersion;
    }

    public Long getAipSize() {
        return aipSize;
    }

    public void setAipSize(Long aipSize) {
        this.aipSize = aipSize;
    }

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public void setDigitalRepositoryId(Integer digitalRepositoryId) {
        this.digitalRepositoryId = digitalRepositoryId;
    }

    public AipType getAipType() {
        return aipType;
    }

    public void setAipType(AipType aipType) {
        this.aipType = aipType;
    }

    public Integer getRemoteAipId() {
        return remoteAipId;
    }

    public void setRemoteAipId(Integer remoteAipId) {
        this.remoteAipId = remoteAipId;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(Integer fundId) {
        this.fundId = fundId;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getLastChange() {
        return lastChange;
    }

    public void setLastChange(LocalDateTime lastChange) {
        this.lastChange = lastChange;
    }

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

    public String getOriginatorCode() {
        return originatorCode;
    }

    public void setOriginatorCode(String originatorCode) {
        this.originatorCode = originatorCode;
    }

    public Integer getOriginatorId() {
        return originatorId;
    }

    public void setOriginatorId(Integer originatorId) {
        this.originatorId = originatorId;
    }

    public String getIngestionCode() {
        return ingestionCode;
    }

    public void setIngestionCode(String ingestionCode) {
        this.ingestionCode = ingestionCode;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getChangeCode() {
        return changeCode;
    }

    public void setChangeCode(String changeCode) {
        this.changeCode = changeCode;
    }

    public static DaAipVO newInstance(DaAip src) {
        DaAipVO vo = new DaAipVO();
        vo.setId(src.getAipId());
        vo.setAipId(src.getAipId());
        vo.setCode(src.getCode());
        vo.setAipVersion(src.getAipVersion());
        vo.setAipSize(src.getAipSize());
        vo.setDigitalRepositoryId(src.getDigitalRepository().getExternalSystemId());
        vo.setAipType(src.getAipType());
        vo.setRemoteAipId(src.getRemoteAip() != null ? src.getRemoteAip().getRemoteAipId() : null);
        vo.setFundId(src.getFund().getFundId());
        vo.setCreateDate(src.getCreateDate());
        vo.setLastChange(src.getLastChange());
        vo.setUnitdateFrom(src.getUnitdateFrom());
        vo.setUnitdateTo(src.getUnitdateTo());
        vo.setOriginatorCode(src.getOriginatorCode());
        vo.setOriginatorId(src.getOriginator() != null ? src.getOriginator().getAccessPointId() : null);
        vo.setIngestionCode(src.getIngestionCode());
        vo.setRefNo(src.getRefNo());
        vo.setChangeCode(src.getChangeCode());
        return vo;
    }
}
