package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ApBindingIssue;

public interface ApBindingIssueRepository extends ElzaJpaRepository<ApBindingIssue, Integer> {

    @Query("SELECT bi FROM ap_binding_issue bi " +
           "LEFT JOIN FETCH bi.relatedBinding " +
           "WHERE bi.bindingId = :bindingId")
    List<ApBindingIssue> findByBindingIdFetchRelated(@Param("bindingId") Integer bindingId);

    List<ApBindingIssue> findByBindingId(Integer bindingId);
}
