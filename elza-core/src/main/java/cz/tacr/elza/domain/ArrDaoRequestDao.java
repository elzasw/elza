package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Implementace {@link cz.tacr.elza.api.ArrRequestQueueItem}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_dao_request_dao")
@Table
public class ArrDaoRequestDao implements cz.tacr.elza.api.ArrDaoRequestDao<ArrDaoRequest, ArrDao> {

    @Id
    @GeneratedValue
    private Integer daoRequestDaoId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrDaoRequest.class)
    @JoinColumn(name = "daoRequestId", nullable = false)
    private ArrDaoRequest daoRequest;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Override
    public Integer getDaoRequestDaoId() {
        return daoRequestDaoId;
    }

    @Override
    public void setDaoRequestDaoId(final Integer daoRequestDaoId) {
        this.daoRequestDaoId = daoRequestDaoId;
    }

    @Override
    public ArrDaoRequest getDaoRequest() {
        return daoRequest;
    }

    @Override
    public void setDaoRequest(final ArrDaoRequest daoRequest) {
        this.daoRequest = daoRequest;
    }

    @Override
    public ArrDao getDao() {
        return dao;
    }

    @Override
    public void setDao(final ArrDao dao) {
        this.dao = dao;
    }
}
