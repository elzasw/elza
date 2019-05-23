package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;


/**
 * Repository for ArrDigitizationRequest
 * 
 * See {@link ArrDigitizationRequest}
 */
@Repository
public interface DigitizationRequestRepository extends ElzaJpaRepository<ArrDigitizationRequest, Integer> {

}
