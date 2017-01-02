package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Vazební entita mezi dao a požadavkem.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrDaoRequestDao<DR, D> extends Serializable {
    Integer getDaoRequestDaoId();

    void setDaoRequestDaoId(Integer daoRequestDaoId);

    DR getDaoRequest();

    void setDaoRequest(DR daoRequest);

    D getDao();

    void setDao(D dao);
}
