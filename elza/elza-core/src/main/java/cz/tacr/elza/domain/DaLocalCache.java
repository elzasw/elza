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

@Entity(name = "da_local_cache")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaLocalCache {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer localCacheId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaSyncQueueItem.class)
    @JoinColumn(name = "sync_queue_item_id", nullable = false)
    private DaSyncQueueItem syncQueueItem;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAipState.class)
    @JoinColumn(name = "aip_state_id")
    private DaAipState aipState;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = false)
    private AipType aipType;

    @Column(length = StringLength.LENGTH_1000, nullable = false)
    private String filePath;

    @Column(length = StringLength.LENGTH_1000)
    private String filePathMetadata;

    public Integer getLocalCacheId() {
        return localCacheId;
    }

    public void setLocalCacheId(Integer localCacheId) {
        this.localCacheId = localCacheId;
    }

    public DaSyncQueueItem getSyncQueueItem() {
        return syncQueueItem;
    }

    public void setSyncQueueItem(DaSyncQueueItem syncQueueItem) {
        this.syncQueueItem = syncQueueItem;
    }

    public AipType getAipType() {
        return aipType;
    }

    public void setAipType(AipType aipType) {
        this.aipType = aipType;
    }

    public DaAipState getAipState() {
        return aipState;
    }

    public void setAipState(DaAipState aipState) {
        this.aipState = aipState;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePathMetadata() {
        return filePathMetadata;
    }

    public void setFilePathMetadata(String filePathMetadata) {
        this.filePathMetadata = filePathMetadata;
    }
}
