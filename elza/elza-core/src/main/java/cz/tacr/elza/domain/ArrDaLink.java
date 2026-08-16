package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vazba na obsah digitálního archivu: kontejnerem je {@link DaAip}, volitelným
 * členem {@link DaDao} (NULL = celý AIP, viz {@link ArrDaoLink.LinkType#AIP}).
 */
@Table
@Entity(name = "arr_da_link")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDaLink extends ArrDaoLink {

    public static final String TABLE_NAME = "arr_da_link";

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAip.class)
    @JoinColumn(name = "aip_id", nullable = false)
    private DaAip aip;

    @Column(name = "aip_id", updatable = false, insertable = false)
    private Integer aipId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "da_dao_id")
    private DaDao daDao;

    @Column(name = "da_dao_id", updatable = false, insertable = false)
    private Integer daDaoId;

    public DaAip getAip() {
        return aip;
    }

    public void setAip(DaAip aip) {
        this.aip = aip;
        this.aipId = aip == null ? null : aip.getAipId();
    }

    public Integer getAipId() {
        return aipId;
    }

    public void setAipId(Integer aipId) {
        this.aipId = aipId;
    }

    public DaDao getDaDao() {
        return daDao;
    }

    public void setDaDao(DaDao daDao) {
        this.daDao = daDao;
        this.daDaoId = daDao == null ? null : daDao.getDaoId();
    }

    public Integer getDaDaoId() {
        return daDaoId;
    }

    public void setDaDaoId(Integer daDaoId) {
        this.daDaoId = daDaoId;
    }
}
