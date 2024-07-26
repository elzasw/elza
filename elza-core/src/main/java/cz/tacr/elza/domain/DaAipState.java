package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity(name = "da_aip_state")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaAipState {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer aipStateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAip.class)
    @JoinColumn(name = "aip_id", nullable = false)
    private DaAip daAip;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @Column(length = 250, nullable = false)
    private String aipVersion;

    @Column
    private Long aipSize;

    @Column(length = 250)
    private String fundCode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fund_id")
    private ArrFund fund;

    @Column(length = 250)
    private String institutionCode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParInstitution.class)
    @JoinColumn(name = "institution_id")
    private ParInstitution institution;

    @Column
    private LocalDateTime unitdateFrom;

    @Column
    private LocalDateTime unitdateTo;

    @Column(length = 250)
    private String originator;

    @Column(length = 50)
    private String originatorCamId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "access_point_id")
    private ApAccessPoint originatorAccessPoint;

    @Column(length = 250)
    private String ingestionCode;

    @Column(length = 250)
    private String referenceNumber;

    @Column(length = 250)
    private String nadChangeCode;

    @Column
    private Boolean metadataLoad;

    public Integer getAipStateId() {
        return aipStateId;
    }

    public void setAipStateId(Integer aipStateId) {
        this.aipStateId = aipStateId;
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

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public DaAip getDaAip() {
        return daAip;
    }

    public void setDaAip(DaAip daAip) {
        this.daAip = daAip;
    }

    public DaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(DaChange createChange) {
        this.createChange = createChange;
    }

    public DaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(DaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public ParInstitution getInstitution() {
        return institution;
    }

    public void setInstitution(ParInstitution institution) {
        this.institution = institution;
    }

    public String getOriginator() {
        return originator;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public String getOriginatorCamId() {
        return originatorCamId;
    }

    public void setOriginatorCamId(String originatorCamId) {
        this.originatorCamId = originatorCamId;
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

    public ApAccessPoint getOriginatorAccessPoint() {
        return originatorAccessPoint;
    }

    public void setOriginatorAccessPoint(ApAccessPoint originatorAccessPoint) {
        this.originatorAccessPoint = originatorAccessPoint;
    }

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

    public Boolean getMetadataLoad() {
        return metadataLoad;
    }

    public void setMetadataLoad(Boolean metadataLoad) {
        this.metadataLoad = metadataLoad;
    }
}
