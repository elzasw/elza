package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfComment;

@Repository
public interface WfCommentRepository extends ElzaJpaRepository<WfComment, Integer> {

    @Query("from wf_comment c where c.issue.id = :issueId order by c.timeCreated")
    List<WfComment> findByIssueId(@Param(value = "issueId") Integer issueId);
}
