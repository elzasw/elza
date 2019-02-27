package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssueType;

@Repository
public interface WfIssueTypeRepository extends ElzaJpaRepository<WfIssueType, Integer>, Packaging<WfIssueType> {

    @Query("select it from wf_issue_type it order by it.viewOrder")
    List<WfIssueType> findAllOrderByViewOrder();
}