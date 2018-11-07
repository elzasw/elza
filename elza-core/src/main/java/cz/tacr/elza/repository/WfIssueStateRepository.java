package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.WfIssueState;

@Repository
public interface WfIssueStateRepository extends ElzaJpaRepository<WfIssueState, Integer> {

    @Query("from wf_issue_state s where s.startState = true")
    WfIssueState getStartState();
}