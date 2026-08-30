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
import cz.tacr.elza.api.AipLinkState;
import cz.tacr.elza.api.AipProblemType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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

    @Column
    private Boolean completeAipLoad;

    /**
     * What is wrong with the AIP - the most severe problem detected, null when there is none.
     * The three columns below describe the problem this one names; they are empty together
     * with it.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AipProblemType problemType;

    /**
     * The problem in the words the user reads.
     */
    @Column
    private String problemDescription;

    /**
     * Technical detail of the problem - the chain of causes from the failure towards its root.
     * Not shown to the user, it serves the diagnostics; empty for a problem derived from the
     * state of the AIP, which has no failure behind it.
     */
    @Column
    private String problemDetail;

    /**
     * Path of the file inside the package the problem is about, so the user can go straight to
     * it in the package browser; empty when the problem is not about a single file.
     */
    @Column
    private String problemFile;

    @Column(length = 250)
    private String aipVersionMetadata;

    /**
     * How much of the AIP hangs on the archival description.
     *
     * Worked out when the links or the content of the package change and kept here, because it
     * depends on what the links reach and cannot be read off the link rows alone. It describes the
     * AIP for the user; nothing decides anything by it - whether a link may be created is answered
     * against the live links every time.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AipLinkState linkState = AipLinkState.NOT_LINKED;

    public AipLinkState getLinkState() {
        return linkState;
    }

    public void setLinkState(AipLinkState linkState) {
        this.linkState = linkState;
    }

    public AipProblemType getProblemType() {
        return problemType;
    }

    public void setProblemType(AipProblemType problemType) {
        this.problemType = problemType;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

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

    public Boolean getCompleteAipLoad() {
        return completeAipLoad;
    }

    public void setCompleteAipLoad(Boolean completeAipLoad) {
        this.completeAipLoad = completeAipLoad;
    }

    public String getProblemDetail() {
        return problemDetail;
    }

    public void setProblemDetail(String problemDetail) {
        this.problemDetail = problemDetail;
    }

    public String getProblemFile() {
        return problemFile;
    }

    public void setProblemFile(String problemFile) {
        this.problemFile = problemFile;
    }

    public String getAipVersionMetadata() {
        return aipVersionMetadata;
    }

    public void setAipVersionMetadata(String aipVersionMetadata) {
        this.aipVersionMetadata = aipVersionMetadata;
    }
}
