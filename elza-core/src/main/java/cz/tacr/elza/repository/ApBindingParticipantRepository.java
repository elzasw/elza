package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ApBindingParticipant;

public interface ApBindingParticipantRepository extends ElzaJpaRepository<ApBindingParticipant, Integer> {

    /**
     * Participants for the given binding-state ids. Ordered first by
     * bindingStateId (stable grouping), then by lastChange ascending so the
     * UI receives the per-revision list already in the required order.
     */
    @Query("SELECT p FROM ap_binding_participant p " +
           "WHERE p.bindingStateId IN :bindingStateIds " +
           "ORDER BY p.bindingStateId, p.lastChange ASC")
    List<ApBindingParticipant> findByBindingStateIdInOrderByLastChange(
            @Param("bindingStateIds") Collection<Integer> bindingStateIds);
}
