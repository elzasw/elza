package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoRequest;

import java.util.Collection;
import java.util.Map;


/**
 * @author Martin Šlapa
 * @since 11.01.2017
 */
public interface DaoRequestDaoRepositoryCustom {

    Map<ArrDaoRequest, Integer> countByRequests(Collection<ArrDaoRequest> requestForDaos);

}
