package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 05.12.2016
 */
@Repository
public interface DigitizationFrontdeskRepository extends ElzaJpaRepository<ArrDigitizationFrontdesk, Integer> {

}
