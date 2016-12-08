package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRequest;

import java.util.List;

/**
 * @author Martin Šlapa
 * @since 08.12.2016
 */
public interface RequestRepositoryCustom {

    List<ArrRequest> findRequests(ArrFund fund, ArrRequest.State state, ArrRequest.ClassType type);

}
