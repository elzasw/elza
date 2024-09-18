package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.*;

import java.util.List;


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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "daAip")
    private List<DaAipState> states;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aip")
    private List<DaSyncQueueItem> syncQueueItems;

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

    public List<DaSyncQueueItem> getSyncQueueItems() {
        return syncQueueItems;
    }

    public void setSyncQueueItems(List<DaSyncQueueItem> syncQueueItems) {
        this.syncQueueItems = syncQueueItems;
    }

    public List<DaAipState> getStates() {
        return states;
    }

    public void setStates(List<DaAipState> states) {
        this.states = states;
    }
}
