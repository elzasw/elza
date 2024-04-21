package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.DipType;
import cz.tacr.elza.api.ProcessState;
import cz.tacr.elza.domain.ArrAip;

import java.time.LocalDateTime;

public class ArrAipVO {

    private Integer id;

    private Integer aipId;

    private Integer extAipId;

    private String name;

    private Integer aipVersion;

    private Integer aipSize;

    private Integer fundId;

    private String fundName;

    private Integer institutionId;

    private String institutionName;

    private LocalDateTime createDate;

    private DipType dipType;

    private ProcessState processState;

    private LocalDateTime syncDate;

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

    public Integer getExtAipId() {
        return extAipId;
    }

    public void setExtAipId(Integer extAipId) {
        this.extAipId = extAipId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAipVersion() {
        return aipVersion;
    }

    public void setAipVersion(Integer aipVersion) {
        this.aipVersion = aipVersion;
    }

    public Integer getAipSize() {
        return aipSize;
    }

    public void setAipSize(Integer aipSize) {
        this.aipSize = aipSize;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(Integer fundId) {
        this.fundId = fundId;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public Integer getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Integer institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public DipType getDipType() {
        return dipType;
    }

    public void setDipType(DipType dipType) {
        this.dipType = dipType;
    }

    public ProcessState getProcessState() {
        return processState;
    }

    public void setProcessState(ProcessState processState) {
        this.processState = processState;
    }

    public LocalDateTime getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(LocalDateTime syncDate) {
        this.syncDate = syncDate;
    }

    public static ArrAipVO newInstance(ArrAip src) {
        ArrAipVO vo = new ArrAipVO();
        vo.setId(src.getAipId());
        vo.setAipId(src.getAipId());
        vo.setExtAipId(src.getExtAipId());
        vo.setName(src.getName());
        vo.setAipVersion(src.getAipVersion());
        vo.setAipSize(src.getAipSize());
        vo.setFundId(src.getFundId());
        vo.setInstitutionId(src.getInstitutionId());
        vo.setCreateDate(src.getCreateDate());
        vo.setDipType(src.getDipType());
        vo.setProcessState(src.getProcessState());
        vo.setSyncDate(src.getSyncDate());
        return vo;
    }
}
