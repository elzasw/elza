package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "da_remote_aip")
public class DaRemoteAip {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer remoteAipId;

    @Column(name = "code", length = StringLength.LENGTH_250, nullable = false)
    private String code;

    @Column(name = "aip_version", length = StringLength.LENGTH_250)
    private String aipVersion;

    @Column(name = "fund_code", length = StringLength.LENGTH_250, nullable = false)
    private String fundCode;

    @Column(name = "institution_code", length = StringLength.LENGTH_250, nullable = false)
    private String institutionCode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaRemoteRepository.class)
    @JoinColumn(name = "remote_repository_id", nullable = false)
    private DaRemoteRepository remoteRepository;

    public Integer getRemoteAipId() {
        return remoteAipId;
    }

    public void setRemoteAipId(Integer remoteAipId) {
        this.remoteAipId = remoteAipId;
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

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public DaRemoteRepository getRemoteRepository() {
        return remoteRepository;
    }

    public void setRemoteRepository(DaRemoteRepository remoteRepository) {
        this.remoteRepository = remoteRepository;
    }
}
