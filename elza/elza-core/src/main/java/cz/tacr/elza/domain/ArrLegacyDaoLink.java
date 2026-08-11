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
 * Vazba na digitalizát evidovaný jako {@link ArrDao} — původní (legacy)
 * podoba vazby. Dnes ji vytváří výhradně SOAP/WSDL tok; zaniká ve fázi 5
 * plánu da-migration.md spolu s rodinou arr_dao.
 */
@Table
@Entity(name = "arr_legacy_dao_link")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrLegacyDaoLink extends ArrDaoLink {

    public static final String TABLE_NAME = "arr_legacy_dao_link";

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(name = "daoId", updatable = false, insertable = false)
    private Integer daoId;

    @Column(length = StringLength.LENGTH_250)
    private String scenario;

    public ArrDao getDao() {
        return dao;
    }

    public void setDao(final ArrDao dao) {
        this.dao = dao;
        this.daoId = dao == null ? null : dao.getDaoId();
    }

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(final Integer daoId) {
        this.daoId = daoId;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }
}
