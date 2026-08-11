package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Vazba na obsah souborového repozitáře: kontejnerem je
 * {@link ArrDigitalRepository} typu FILESYSTEM, volitelným členem cesta
 * relativní ke kořeni repozitáře v kanonickém tvaru s '/' (NULL = kořen
 * repozitáře). Soubor ani složka nemají v databázi žádnou entitu — obsah se
 * čte živě z disku.
 */
@Table
@Entity(name = "arr_fs_link")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFsLink extends ArrDaoLink {

    public static final String TABLE_NAME = "arr_fs_link";

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digitalRepositoryId", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @Column(name = "digitalRepositoryId", updatable = false, insertable = false)
    private Integer digitalRepositoryId;

    @Column(length = StringLength.LENGTH_1000)
    private String path;

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
        this.digitalRepositoryId = digitalRepository == null ? null
                : digitalRepository.getExternalSystemId();
    }

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public void setDigitalRepositoryId(Integer digitalRepositoryId) {
        this.digitalRepositoryId = digitalRepositoryId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
