package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssueList;

@Repository
public interface WfIssueListRepository extends ElzaJpaRepository<WfIssueList, Integer>, WfIssueListRepositoryCustom {

    @Query("select il" +
            " from wf_issue_list il" +
            " where il.fund.fundId = :fundId")
    List<WfIssueList> findByFundId(@Param(value = "fundId") Integer fundId);

    @Query("select i.issueList.issueListId" +
            " from wf_issue i" +
            " where i.issueId = :issueId")
    Integer findIdByIssueId(@Param(value = "issueId") Integer issueId);

    @Query("select c.issue.issueList.issueListId" +
            " from wf_comment c" +
            " where c.commentId = :commentId")
    Integer findIdByCommentId(@Param(value = "commentId") Integer commentId);

    @Query("select il.fund.fundId" +
            " from wf_issue_list il" +
            " where il.issueListId = :issueListId")
    Integer findFundIdByIssueListId(@Param(value = "issueListId") Integer issueListId);

    @Query("select i.issueList.fund.fundId" +
            " from wf_issue i" +
            " where i.issueId = :issueId")
    Integer findFundIdByIssueId(@Param(value = "issueId") Integer issueId);

    @Query("select c.issue.issueList.fund.fundId" +
            " from wf_comment c" +
            " where c.commentId = :commentId")
    Integer findFundIdByCommentId(@Param(value = "commentId") Integer commentId);

    @Modifying
    @Query("delete from wf_issue_list il where il.fund.fundId = :fundId")
    void deleteByFundId(@Param(value = "fundId") Integer fundId);

}