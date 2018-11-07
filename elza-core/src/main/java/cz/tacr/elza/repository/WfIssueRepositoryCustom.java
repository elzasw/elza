package cz.tacr.elza.repository;

import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;

/**
 * Metody pro samostatnou implementaci repository {@link WfIssueRepository}.
 */
public interface WfIssueRepositoryCustom {
    List<WfIssue> findByIssueListId(Integer issueListId, @Nullable WfIssueState issueState, @Nullable WfIssueType issueType);
}
