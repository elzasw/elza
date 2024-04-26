package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


import java.time.LocalDateTime;

@Entity(name = "da_aip")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaAip {

    public static final String FIELD_CODE = "code";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer aipId;

    @Column(name = "code", length = StringLength.LENGTH_250, nullable = false)
    private String code;

    @Column(name = "aip_version", length = StringLength.LENGTH_250)
    private String aipVersion;

    @Column(name = "aip_size")
    private Long aipSize;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digital_repository_id", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @Enumerated(EnumType.STRING)
    @Column(name = "aip_type", length = StringLength.LENGTH_ENUM, nullable = false)
    private AipType aipType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaRemoteAip.class)
    @JoinColumn(name = "remote_aip_id")
    private DaRemoteAip remoteAip;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fund_id", nullable = false)
    private ArrFund fund;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "last_change", nullable = false)
    private LocalDateTime lastChange;

    @Column(name = "unitdate_from")
    private LocalDateTime unitdateFrom;

    @Column(name = "unitdate_to")
    private LocalDateTime unitdateTo;

    @Column(name = "originator_code", length = StringLength.LENGTH_250)
    private String originatorCode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "originator_id")
    private ApAccessPoint originator;

    @Column(name = "ingestion_code", length = StringLength.LENGTH_250)
    private String ingestionCode;

    @Column(name = "ref_no", length = StringLength.LENGTH_250)
    private String refNo;

    @Column(name = "change_code", length = StringLength.LENGTH_250)
    private String changeCode;

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

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    public AipType getAipType() {
        return aipType;
    }

    public void setAipType(AipType aipType) {
        this.aipType = aipType;
    }

    public DaRemoteAip getRemoteAip() {
        return remoteAip;
    }

    public void setRemoteAip(DaRemoteAip remoteAip) {
        this.remoteAip = remoteAip;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
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

    public ApAccessPoint getOriginator() {
        return originator;
    }

    public void setOriginator(ApAccessPoint originator) {
        this.originator = originator;
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
}
