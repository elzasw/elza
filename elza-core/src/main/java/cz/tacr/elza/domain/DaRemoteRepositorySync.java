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

@Entity(name = "da_remote_repository_sync")
public class DaRemoteRepositorySync {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer remoteRepositorySyncId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digital_repository_id", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @Column(name = "next_query",  length = StringLength.LENGTH_250)
    private String nextQuery;

    public Integer getRemoteRepositorySyncId() {
        return remoteRepositorySyncId;
    }

    public void setRemoteRepositorySyncId(Integer remoteRepositorySyncId) {
        this.remoteRepositorySyncId = remoteRepositorySyncId;
    }

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    public String getNextQuery() {
        return nextQuery;
    }

    public void setNextQuery(String nextQuery) {
        this.nextQuery = nextQuery;
    }
}
