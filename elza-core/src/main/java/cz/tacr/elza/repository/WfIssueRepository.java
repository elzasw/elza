package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssue;

@Repository
public interface WfIssueRepository extends ElzaJpaRepository<WfIssue, Integer>, WfIssueRepositoryCustom {

    @Query("select i" +
            " from wf_issue i" +
            " where i.issueList.issueListId = :issueListId" +
            " order by i.number")
    Page<WfIssue> findByFundId(@Param(value = "issueListId") Integer issueListId, Pageable pageable);

    @Query("select max(i.number)" +
            " from wf_issue i" +
            " where i.issueList.issueListId = :issueListId")
    Optional<Integer> getNumberMax(@Param(value = "issueListId") Integer issueListId);

    @Modifying
    @Query("delete from wf_issue i" +
            " where i.issueList.issueListId in (select il.issueListId from wf_issue_list il where il.fund.fundId = :fundId)")
    void deleteByFundId(@Param(value = "fundId") Integer fundId);

    @Modifying
    @Query("update wf_issue i set i.node = null where i.node.id in :nodeIds")
    void resetNodes(@Param(value = "nodeIds") Collection<Integer> nodeIds);
}