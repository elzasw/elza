package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.WfIssueState;
import cz.tacr.elza.domain.WfIssueType;

/**
 * Implementace repository pro {@link WfIssue} - Custom.
 */
@Component
public class WfIssueRepositoryImpl implements WfIssueRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<WfIssue> findByIssueListId(Integer issueListId, @Nullable WfIssueState issueState, @Nullable WfIssueType issueType) {

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
}
