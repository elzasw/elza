package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

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

    	//TODO hibernate search 6
    	return Collections.EMPTY_LIST;
// org.springframework.dao.InvalidDataAccessApiUsageException: Unsupported tuple comparison combination. LHS is neither a tuple nor a tuple subquery but RHS is a tuple: org.hibernate.sql.ast.tree.predicate.ComparisonPredicate@2b8529bc
//        StringBuilder hql = new StringBuilder(256);
//        hql.append("select i from wf_issue i" +
//                " where i.issueList.fundId = :fundId");
//        if (userId != null) {
//            hql.append(" and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)");
//        }
//        hql.append(" and i.issueState.finalState = false" +
//                " and i.issueList.open = true" +
//                " and i.node = null" +
//                " order by i.issueList.issueListId, i.number");
//
//        TypedQuery<WfIssue> query = entityManager.createQuery(hql.toString(), WfIssue.class);
//
//        query.setParameter("fundId", fundId);
//        if (userId != null) {
//            query.setParameter("userId", userId);
//        }
//
//        return query.getResultList();
    }

    @Override
    public List<WfIssue> findOpenByNodeId(@NotNull Collection<Integer> nodeIds, @Nullable Integer userId) {

        StringBuilder hql = new StringBuilder(256);
        hql.append("select i from wf_issue i" +
                " where i.node.nodeId in :nodeIds");
        if (userId != null) {
            hql.append(" and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)");
        }
        hql.append(" and i.issueState.finalState = false" +
                " and i.issueList.open = true" +
                " order by i.issueList.issueListId, i.number");

        Query query = entityManager.createQuery(hql.toString());

        query.setParameter("nodeIds", nodeIds);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        return query.getResultList();
    }

    @Override
    public List<Integer> findNodeIdWithOpenIssueByFundId(@NotNull Integer fundId, @Nullable Integer userId) {

        StringBuilder hql = new StringBuilder(256);
        hql.append("select distinct i.node.nodeId from wf_issue i" +
                " where i.issueList.fund.fundId = :fundId");
        if (userId != null) {
            hql.append(" and i.issueList in (select pv.issueList from usr_permission_view pv where pv.user.userId = :userId)");
        }
        hql.append(" and i.issueState.finalState = false" +
                " and i.issueList.open = true" +
                " and i.node is not null");

        Query query = entityManager.createQuery(hql.toString());

        query.setParameter("fundId", fundId);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        return query.getResultList();
    }
}
