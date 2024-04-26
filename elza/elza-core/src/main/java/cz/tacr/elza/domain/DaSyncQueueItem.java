package cz.tacr.elza.domain;

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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaRemoteRepository.class)
    @JoinColumn(name = "remote_repository_id", nullable = false)
    private DaRemoteRepository remoteRepository;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaRemoteAip.class)
    @JoinColumn(name = "remote_aip_id")
    private DaRemoteAip remoteAip;


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

    public DaRemoteRepository getRemoteRepository() {
        return remoteRepository;
    }

    public void setRemoteRepository(DaRemoteRepository remoteRepository) {
        this.remoteRepository = remoteRepository;
    }

    public DaRemoteAip getRemoteAip() {
        return remoteAip;
    }

    public void setRemoteAip(DaRemoteAip remoteAip) {
        this.remoteAip = remoteAip;
    }

    public enum QueueItemState {

        UPDATE("K aktualizaci"),

        IMPORT_NEW("Ke stažení"),

        IMPORT_OK("Aktualizováno/Staženo"), // předchozí OK

        EXPORT_NEW("K odeslání"),

        EXPORT_START("Odesílání"),

        EXPORT_OK("Odesláno"),

        ERROR("Chyba");

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
