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
import jakarta.persistence.Table;

/**
 * Vazební entita mezi dao a požadavkem.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_dao_request_dao")
@Table
public class ArrDaoRequestDao {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer daoRequestDaoId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrDaoRequest.class)
    @JoinColumn(name = "daoRequestId", nullable = false)
    private ArrDaoRequest daoRequest;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(name = "daoRequestId", insertable = false, updatable = false)
    private Integer daoRequestId;

    public Integer getDaoRequestDaoId() {
        return daoRequestDaoId;
    }

    public void setDaoRequestDaoId(final Integer daoRequestDaoId) {
        this.daoRequestDaoId = daoRequestDaoId;
    }

    public ArrDaoRequest getDaoRequest() {
        return daoRequest;
    }

    public void setDaoRequest(final ArrDaoRequest daoRequest) {
        this.daoRequest = daoRequest;
        this.daoRequestId = (daoRequest == null) ? null : daoRequest.getRequestId();
    }

    public ArrDao getDao() {
        return dao;
    }

    public void setDao(final ArrDao dao) {
        this.dao = dao;
    }

    public Integer getDaoRequestId() {
        return daoRequestId;
    }
}
