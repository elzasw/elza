package cz.tacr.elza.repository;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import cz.tacr.elza.domain.WfIssueList;

/**
 * Metody pro samostatnou implementaci repository {@link WfIssueListRepository}.
 */
public interface WfIssueListRepositoryCustom {
    List<WfIssueList> findByFundIdWithPermission(@NotNull Integer fundId, @Nullable Boolean open, @Nullable Integer userId);
}