package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoDigitizationRequestNodeRepository extends ElzaJpaRepository<ArrDigitizationRequestNode, Integer> {

    List<ArrDigitizationRequestNode> findByDigitizationRequest(ArrDigitizationRequest arrDigitizationRequest);

}
