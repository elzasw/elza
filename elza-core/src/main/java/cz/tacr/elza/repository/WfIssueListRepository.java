package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssueList;

@Repository
public interface WfIssueListRepository extends ElzaJpaRepository<WfIssueList, Integer> {

    @Query("from wf_issue_list l where l.fund.id = :fundId order by l.open desc, l.name")
    List<WfIssueList> findByFundId(@Param(value = "fundId") Integer fundId);
}