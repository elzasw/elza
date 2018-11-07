package cz.tacr.elza.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssue;

@Repository
public interface WfIssueRepository extends ElzaJpaRepository<WfIssue, Integer>, WfIssueRepositoryCustom {

    @Query("from wf_issue i where i.issueList.id = :issueListId order by number")
    Page<WfIssue> findByFundId(@Param(value = "issueListId") Integer issueListId, Pageable pageable);

    @Query("select max(i.number) from wf_issue i where i.issueList.id = :issueListId")
    Optional<Integer> getNumberMax(@Param(value = "issueListId") Integer issueListId);

}