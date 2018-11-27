package cz.tacr.elza.repository;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;

/**
 * Metody pro samostatnou implementaci repository {@link WfIssueRepository}.
 */
public interface WfIssueRepositoryCustom {

    List<WfIssue> findByIssueListId(@NotNull Integer issueListId, @Nullable WfIssueState issueState, @Nullable WfIssueType issueType);

    List<WfIssue> findOpenByFundIdAndNodeNull(@NotNull Integer fundId, @Nullable Integer userId);

    List<WfIssue> findOpenByNodeId(@NotNull Integer nodeId, @Nullable Integer userId);
}
