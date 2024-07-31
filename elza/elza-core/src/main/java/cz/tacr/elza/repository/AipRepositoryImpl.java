package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.AipFilterGen;
import cz.tacr.elza.controller.vo.AipFilterVO;
import cz.tacr.elza.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class AipRepositoryImpl implements AipRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public FilteredResult<DaAip> findAipsByFilter(final List<AipFilterGen> filters, final Integer firstResult , final Integer maxResults) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DaAip> query = cb.createQuery(DaAip.class);
        CriteriaQuery<Long> queryCount = cb.createQuery(Long.class);

        Root<DaAip> root = query.from(DaAip.class);
        Root<DaAip> aipRootCount = queryCount.from(DaAip.class);

        Predicate condition = prepareFindAipByFilterCount(filters, cb, root);
        Predicate conditionCount = prepareFindAipByFilterCount(filters, cb, aipRootCount);

        query.select(root);
        queryCount.select(cb.countDistinct(aipRootCount));

        if (condition != null) {
            Order order = cb.asc(root.get(DaAip.FIELD_CODE));
            query.where(condition).orderBy(order);

            queryCount.where(conditionCount);
        }

        TypedQuery<DaAip> tq = entityManager.createQuery(query)
                .setFirstResult(firstResult);
        if (maxResults > 0) {
            tq.setMaxResults(maxResults);
        }
        List<DaAip> list = tq.getResultList();
        int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }

    private <T> Predicate prepareFindAipByFilterCount(final List<AipFilterGen> filters,
                                                    final CriteriaBuilder cb,
                                                    final Root<DaAip> aipRoot) {
        List<Predicate> predicates = new ArrayList<>();
        Join<DaAip, DaAipState> stateJoin = aipRoot.join("states", JoinType.LEFT);
        Join<DaAip, DaSyncQueueItem> syncJoin = aipRoot.join("daSyncQueueItem", JoinType.LEFT);
        Join<DaAipState, ApAccessPoint> oApJoin = stateJoin.join("originatorAccessPoint", JoinType.LEFT);
        Join<DaAipState, ParInstitution> instJoin = stateJoin.join("institution", JoinType.LEFT);
        Join<ParInstitution, ApAccessPoint> instApJoin = instJoin.join("accessPoint", JoinType.LEFT);
        Join<DaAipState, ArrFund> fundJoin = stateJoin.join("fund", JoinType.LEFT);

        for (AipFilterGen filter : filters) {
            Path<?> path;
            switch (filter.getPath()) {
                case "da_aip" -> path = aipRoot.get(filter.getAttr());
                case "da_aip_state" -> path = stateJoin.get(filter.getAttr());
                case "da_sync_queue_item" -> path = syncJoin.get(filter.getAttr());
                case "originator_access_point" -> path = oApJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID);
                case "institution_access_point" -> path = instApJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID);
                case "arr_fund" -> path = fundJoin.get(ArrFund.FIELD_FUND_ID);

                default -> throw new IllegalArgumentException("Invalid table name: " + filter.getPath());
            }

            switch (filter.getCriteria()) {
                case IS_NULL:
                    predicates.add(cb.isNull(path));
                    break;
                case IS_NOT_NULL:
                    predicates.add(cb.isNotNull(path));
                    break;
                case CONTAINS:
                    predicates.add(cb.like(path.as(String.class), "%" + filter.getValue() + "%"));
                    break;
                case DOES_NOT_CONTAIN:
                    predicates.add(cb.notLike(path.as(String.class), "%" + filter.getValue() + "%"));
                    break;
                case EQUALS:
                    predicates.add(cb.equal(path.as(String.class), filter.getValue()));
                    break;
                case BETWEEN:
                    if (filter.getAttr().equals("unitdateFrom")) {
                        predicates.add(cb.and(
                                cb.between(stateJoin.get("unitdateFrom").as(String.class), filter.getFrom(), filter.getTo()),
                                cb.between(stateJoin.get("unitdateTo").as(String.class), filter.getFrom(), filter.getTo())
                        ));
                    } else {
                        predicates.add(cb.between(path.as(Integer.class), Integer.parseInt(filter.getFrom()), Integer.parseInt(filter.getTo())));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid filter criteria: " + filter.getCriteria());
            }
        }

        if (predicates.isEmpty()) {
            return cb.conjunction();
        }

        return cb.or(predicates.toArray(new Predicate[0]));
    }
}
