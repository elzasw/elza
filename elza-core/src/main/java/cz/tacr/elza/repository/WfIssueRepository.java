package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssue;

@Repository
public interface WfIssueRepository extends ElzaJpaRepository<WfIssue, Integer>, WfIssueRepositoryCustom {

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

    /*
    @Query("select i" +
            " from wf_issue i" +
            " where i.node.nodeId = :nodeId" +
            " and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)" +
            " and i.issueState.finalState = false" +
            " order by i.issueList.issueListId, i.number")
    List<WfIssue> findOpenByNodeId(@Param(value = "nodeId") Integer nodeId, @Param(value = "userId") Integer userId);
    */

    /*
    @Query("select i" +
            " from wf_issue i" +
            " where i.issueList.fund.fundId = :fundId" +
            " and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)" +
            " and i.issueState.finalState = false" +
            " and i.issueList.open = true" +
            " and i.node = null" +
            " order by i.issueList.issueListId, i.number")
    List<WfIssue> findOpenByFundIdAndNodeNull(@Param(value = "fundId") Integer fundId, @Param(value = "userId") Integer userId);
    */
}