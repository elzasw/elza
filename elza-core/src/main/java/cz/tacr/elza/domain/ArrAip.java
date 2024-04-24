package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.api.DipType;
import cz.tacr.elza.api.ProcessState;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;



import java.time.LocalDateTime;

@Entity(name = "arr_aip")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class ArrAip {

    public static final String FIELD_EXT_AIP_ID = "extAipId";
    public static final String FIELD_NAME = "name";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer aipId;

    @Column(name = "ext_aip_id", nullable = false)
    private Integer extAipId;

    @Column(name = "name", length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(name = "aip_version")
    private Integer aipVersion;

    @Column(name = "aip_size")
    private Integer aipSize;

    @Column(name = "fund_id", nullable = false)
    private Integer fundId;

    @Column(name = "institution_id", nullable = false)
    private Integer institutionId;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "dip_type", length = StringLength.LENGTH_ENUM, nullable = false)
    private DipType dipType;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_state", length = StringLength.LENGTH_ENUM, nullable = false)
    private ProcessState processState;

    @Column(name = "sync_date")
    private LocalDateTime syncDate;


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

    public Integer getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Integer institutionId) {
        this.institutionId = institutionId;
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
}
