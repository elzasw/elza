package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrNode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DigitizationRequestNodeRepository extends ElzaJpaRepository<ArrDigitizationRequestNode, Integer>, DigitizationRequestNodeRepositoryCustom {

    List<ArrDigitizationRequestNode> findByDigitizationRequestAndNode(ArrDigitizationRequest digitizationRequest, List<ArrNode> nodes);

    @Query("SELECT p FROM arr_digitization_request_node p WHERE p.digitizationRequest IN (?1)")
    List<ArrDigitizationRequestNode> findByDigitizationRequest(Collection<ArrDigitizationRequest> digitizationRequest);
}
