package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;

/**
 * Implementace repository pro {@link WfIssue} - Custom.
 */
@Component
public class WfIssueRepositoryImpl implements WfIssueRepositoryCustom {

    // --- fields ---

    @PersistenceContext
    private EntityManager entityManager;

    // --- methods ---

    @Override
    public List<WfIssue> findByIssueListId(@NotNull Integer issueListId, @Nullable WfIssueState issueState, @Nullable WfIssueType issueType) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<WfIssue> query = builder.createQuery(WfIssue.class);
        Root<WfIssue> root = query.from(WfIssue.class);

        List<Predicate> where = new ArrayList<>();
        where.add(builder.equal(root.join("issueList").get("issueListId"), issueListId));

        if (issueState != null) {
            where.add(builder.equal(root.get("issueState"), issueState));
        }

        if (issueType != null) {
            where.add(builder.equal(root.get("issueType"), issueType));
        }

        query.where(where.toArray(new Predicate[0]));

        query.orderBy(builder.asc(root.get("number")));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<WfIssue> findOpenByFundIdAndNodeNull(@NotNull Integer fundId, @Nullable Integer userId) {

        StringBuilder hql = new StringBuilder(256);
        hql.append("select i from wf_issue i" +
                " where i.issueList.fund.fundId = :fundId");
        if (userId != null) {
            hql.append(" and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)");
        }
        hql.append(" and i.issueState.finalState = false" +
                " and i.issueList.open = true" +
                " and i.node = null" +
                " order by i.issueList.issueListId, i.number");

        Query query = entityManager.createQuery(hql.toString());

        query.setParameter("fundId", fundId);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        return query.getResultList();
    }

    @Override
    public List<WfIssue> findOpenByNodeId(@NotNull Integer nodeId, @Nullable Integer userId) {

        StringBuilder hql = new StringBuilder(256);
        hql.append("select i from wf_issue i" +
                " where i.node.nodeId = :nodeId");
        if (userId != null) {
            hql.append(" and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)");
        }
        hql.append(" and i.issueState.finalState = false" +
                " and i.issueList.open = true" +
                " order by i.issueList.issueListId, i.number");

        Query query = entityManager.createQuery(hql.toString());

        query.setParameter("fundId", nodeId);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        return query.getResultList();
    }
}
