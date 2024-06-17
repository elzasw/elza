package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digital_repository_id", nullable = false)
    private ArrDigitalRepository digitalRepository;

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

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }
}
