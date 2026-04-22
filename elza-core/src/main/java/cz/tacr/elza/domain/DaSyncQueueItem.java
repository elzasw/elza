package cz.tacr.elza.domain;

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

@Entity(name = "da_sync_queue_item")
public class DaSyncQueueItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer syncQueueItemId;

    @Column(name = "code", length = StringLength.LENGTH_250, nullable = false)
    private String code;

    @Column(name = "aip_version", length = StringLength.LENGTH_250)
    private String aipVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = StringLength.LENGTH_ENUM, nullable = false)
    private QueueItemState state;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digital_repository_id", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAip.class)
    @JoinColumn(name = "aip_id")
    private DaAip aip;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private AipType aipType;

    @Column
    private Boolean active;


    public Integer getSyncQueueItemId() {
        return syncQueueItemId;
    }

    public void setSyncQueueItemId(Integer syncQueueItemId) {
        this.syncQueueItemId = syncQueueItemId;
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

    public QueueItemState getState() {
        return state;
    }

    public void setState(QueueItemState state) {
        this.state = state;
    }

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    public DaAip getAip() {
        return aip;
    }

    public void setAip(DaAip aip) {
        this.aip = aip;
    }

    public AipType getAipType() {
        return aipType;
    }

    public void setAipType(AipType aipType) {
        this.aipType = aipType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public enum QueueItemState {

        UPDATE("K aktualizaci"),

        IMPORT_NEW("Ke stažení"),

        IMPORT_OK("Aktualizováno/Staženo"), // předchozí OK

        IMPORT_ERROR("Chyba při importu"),

        EXPORT_NEW("K exportu"),

        EXPORT_OK("Exportováno"),

        EXPORT_ERROR("Chyba při exportu");

        private String value;

        QueueItemState(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static QueueItemState fromValue(String v) {
            return valueOf(v);
        }


    }
}
