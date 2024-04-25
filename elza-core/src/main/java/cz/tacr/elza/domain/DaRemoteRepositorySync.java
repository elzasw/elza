package cz.tacr.elza.domain;

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

@Entity(name = "da_remote_repository_sync")
public class DaRemoteRepositorySync {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer remoteRepositorySyncId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaRemoteRepository.class)
    @JoinColumn(name = "remoteRepositoryId", nullable = false)
    private DaRemoteRepository remoteRepository;

    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    public Integer getRemoteRepositorySyncId() {
        return remoteRepositorySyncId;
    }

    public void setRemoteRepositorySyncId(Integer remoteRepositorySyncId) {
        this.remoteRepositorySyncId = remoteRepositorySyncId;
    }

    public DaRemoteRepository getRemoteRepository() {
        return remoteRepository;
    }

    public void setRemoteRepository(DaRemoteRepository remoteRepository) {
        this.remoteRepository = remoteRepository;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
