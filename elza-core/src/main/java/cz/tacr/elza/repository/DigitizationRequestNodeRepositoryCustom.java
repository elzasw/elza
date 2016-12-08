package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;

import java.util.Map;


/**
 * @author Martin Šlapa
 * @since 08.12.2016
 */
public interface DigitizationRequestNodeRepositoryCustom {

    Map<ArrDigitizationRequest,Integer> countByRequests(Iterable<ArrDigitizationRequest> requestForNodes);

}
