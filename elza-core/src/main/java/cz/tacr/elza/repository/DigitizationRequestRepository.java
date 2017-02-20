package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DigitizationRequestRepository extends ElzaJpaRepository<ArrDigitizationRequest, Integer> {

    @Modifying
    void deleteByFund(ArrFund fund);
}
