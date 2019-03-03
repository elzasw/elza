package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfComment;

@Repository
public interface WfCommentRepository extends ElzaJpaRepository<WfComment, Integer> {

    @Query("select c" +
            " from wf_comment c" +
            " where c.issue.issueId = :issueId" +
            " order by c.timeCreated desc")
    List<WfComment> findLastByIssueId(@Param(value = "issueId") Integer issueId, Pageable pageable);

    @Query("select c" +
            " from wf_comment c" +
            " where c.issue.issueId = :issueId" +
            " order by c.timeCreated")
    List<WfComment> findByIssueId(@Param(value = "issueId") Integer issueId);

    @Query("select c" +
            " from wf_comment c" +
            " where c.issue.issueId in :issueIds" +
            " order by c.timeCreated")
    List<WfComment> findByIssueIds(@Param(value = "issueIds") List<Integer> issueIds);

    @Modifying
    @Query("delete from wf_comment c" +
            " where c.issue.issueId in (select i.issueId from wf_issue i where i.issueList.fund.fundId = :fundId)")
    void deleteByFundId(@Param(value = "fundId") Integer fundId);

}
