package cz.tacr.elza.repository;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.WfIssueList;

/**
 * Implementace repository pro {@link WfIssueList} - Custom.
 */
@Component
public class WfIssueListRepositoryImpl implements WfIssueListRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<WfIssueList> findByFundIdWithPermission(@NotNull Integer fundId, @Nullable Boolean open, @Nullable Integer userId) {

        StringBuilder hql = new StringBuilder();

        hql.append("select il" +
                " from wf_issue_list il" +
                " where il.fund.fundId = :fundId");
        if (open != null) {
            hql.append(" and il.open = :open");
        }
        if (userId != null) {
            hql.append(" and il.issueListId in (select pv.issueList.issueListId from usr_permission_view pv where pv.user.userId = :userId)");
        }
        hql.append(" order by il.open desc, il.name");

        Query query = entityManager.createQuery(hql.toString());

        query.setParameter("fundId", fundId);
        if (open != null) {
            query.setParameter("open", open);
        }
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        return query.getResultList();
    }
}
