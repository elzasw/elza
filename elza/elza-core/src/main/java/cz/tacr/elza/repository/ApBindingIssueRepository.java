package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ApBindingIssue;

public interface ApBindingIssueRepository extends ElzaJpaRepository<ApBindingIssue, Integer> {

    List<ApBindingIssue> findByBindingIdIn(Collection<Integer> bindingIds);

    /**
     * Issues of a binding for the client. Errors come first, then a stable order
     * by id (creation order). The order is stable across refreshes and across
     * syncs — the uuid-merge keeps each retained issue's id, so a kept issue does
     * not change position when a new one appears.
     */
    @Query("SELECT bi FROM ap_binding_issue bi " +
           "LEFT JOIN FETCH bi.relatedBinding " +
           "WHERE bi.bindingId = :bindingId " +
           "ORDER BY CASE WHEN bi.severity = cz.tacr.elza.domain.ApBindingIssue.Severity.ERROR THEN 0 ELSE 1 END, " +
           "bi.bindingIssueId")
    List<ApBindingIssue> findByBindingIdFetchRelated(@Param("bindingId") Integer bindingId);

    List<ApBindingIssue> findByBindingId(Integer bindingId);
}
