package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DigitizationRequestNodeRepository extends ElzaJpaRepository<ArrDigitizationRequestNode, Integer>, DigitizationRequestNodeRepositoryCustom {

    @Query("SELECT p FROM arr_digitization_request_node p WHERE p.digitizationRequest = ?1 AND p.node IN (?2)")
    List<ArrDigitizationRequestNode> findByDigitizationRequestAndNode(ArrDigitizationRequest digitizationRequest, List<ArrNode> nodes);

    @Query("SELECT p FROM arr_digitization_request_node p WHERE p.digitizationRequest IN (?1)")
    List<ArrDigitizationRequestNode> findByDigitizationRequest(Collection<ArrDigitizationRequest> digitizationRequest);

    @Query("SELECT p.node FROM arr_digitization_request_node p WHERE p.digitizationRequest = :digitizationRequest")
    List<ArrNode> findNodesByDigitizationRequest(@Param("digitizationRequest") ArrDigitizationRequest digitizationRequest);

    @Query("SELECT p FROM arr_digitization_request_node p JOIN p.node n JOIN p.digitizationRequest dr WHERE n.nodeId IN (?1) AND dr.state = ?2")
    List<ArrDigitizationRequestNode> findByNodeIds(Collection<Integer> nodeIds, final ArrRequest.State state);

    @Modifying
    void deleteByDigitizationRequest(ArrDigitizationRequest digitizationRequest);

    @Modifying
    @Query("DELETE FROM arr_digitization_request_node dd WHERE dd.digitizationRequestId IN (SELECT d.requestId FROM arr_digitization_request d WHERE d.fund = ?1)")
    void deleteByFund(ArrFund fund);
}
