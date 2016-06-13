package cz.tacr.elza.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import cz.tacr.elza.domain.RulItemType;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.filter.condition.DescItemCondition;
import cz.tacr.elza.filter.condition.HibernateDescItemCondition;
import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
import cz.tacr.elza.filter.condition.SelectsNothingCondition;

/**
 * Skupina filtrů pro typ atributu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 4. 2016
 */
public class DescItemTypeFilter {

    /** Typ hodnoty na který se má filtr aplikovat. */
    private RulItemType descItemType;

    /** Třída na kterou se bude aplikovat filtr. */
    private Class<?> cls;

    /** Seznam podmínek. */
    private List<DescItemCondition> conditions;

    /**
     * Konstruktor pro podmínky.
     *
     * @param descItemType typ atributu
     * @param cls třída na kterou se mají podmínky aplikovat
     * @param conditions podmínky
     */
    public DescItemTypeFilter(final RulItemType descItemType, final Class<?> cls, final List<DescItemCondition> conditions) {
        Assert.notNull(descItemType);
        Assert.notNull(cls);
        Assert.notEmpty(conditions);

        this.descItemType = descItemType;
        this.cls = cls;
        this.conditions = conditions;
    }

    public Map<Integer, List<String>> resolveConditions(final FullTextEntityManager fullTextEntityManager, final QueryBuilder queryBuilder, final Integer fundId, final EntityManager entityManager, final Integer lockChangeId) {
        List<Query> luceneQueries = new LinkedList<>();
        List<javax.persistence.Query> hibernateQueries = new LinkedList<>();
        for (DescItemCondition condition : conditions) {
            if (condition instanceof LuceneDescItemCondition) {
                LuceneDescItemCondition lucene = (LuceneDescItemCondition) condition;
                luceneQueries.add(lucene.createLuceneQuery(queryBuilder));
            } else if (condition instanceof SelectsNothingCondition) {
                return new HashMap<>(0);
            } else {
                HibernateDescItemCondition hibernate = (HibernateDescItemCondition) condition;
                hibernateQueries.add(hibernate.createHibernateQuery(entityManager, fundId, descItemType.getItemTypeId(), lockChangeId));
            }
        }

        Map<Integer, List<String>> nodeIdToDescItemIds = null;
        if (!luceneQueries.isEmpty()) {
            BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();
            booleanJunction.must(createDescItemTypeQuery(queryBuilder));
            booleanJunction.must(createFundIdQuery(queryBuilder, fundId));

            luceneQueries.forEach(q -> {
                booleanJunction.must(q);
            });

            List<Object> rows = createFullTextQuery(fullTextEntityManager, booleanJunction.createQuery(), ArrData.class).setProjection("nodeId", "descItemId").getResultList();

            nodeIdToDescItemIds = new HashMap<>(rows.size());
            for (Object row: rows) {
                Object[] rowArray = (Object[]) row;
                Integer nodeId = (Integer) rowArray[0];
                String descItemId = (String) rowArray[1];
                List<String> descItemIds = nodeIdToDescItemIds.get(nodeId);
                if (descItemIds == null) {
                    descItemIds = new LinkedList<>();
                    nodeIdToDescItemIds.put(nodeId, descItemIds);
                }
                descItemIds.add(descItemId);
            }
        }

        Set<Integer> nodeIds = null;
        if (!hibernateQueries.isEmpty()) {
            for (javax.persistence.Query q : hibernateQueries){
                List<Integer> resultList = q.getResultList();

                if (nodeIds == null) {
                    nodeIds = new HashSet<>(resultList);
                } else {
                    nodeIds.retainAll(resultList);
                }
            }
        }

        if (nodeIdToDescItemIds == null) {
            nodeIdToDescItemIds = new HashMap<>();
            for (Integer nodeId : nodeIds) {
                nodeIdToDescItemIds.put(nodeId, new LinkedList<>());
            }
        } else if (nodeIds != null) {
            for (Integer nodeId : nodeIds) {
                if (!nodeIdToDescItemIds.containsKey(nodeId)) {
                    nodeIdToDescItemIds.put(nodeId, new LinkedList<>());
                }
            }
        }

        return nodeIdToDescItemIds;
    }

    /**
     * Vytvoří hibernate jpa query z lucene query.
     *
     * @param query lucene qery
     * @param entityClass třída pro kterou je dotaz
     *
     * @return hibernate jpa query
     */
    private FullTextQuery createFullTextQuery(final FullTextEntityManager fullTextEntityManager, final Query query, final Class<?> entityClass) {
        return fullTextEntityManager.createFullTextQuery(query, entityClass);
    }

//
//    private Query createLuceneQuery(final QueryBuilder queryBuilder, final Integer fundId) {
//        Assert.notNull(queryBuilder);
//        Assert.notNull(fundId);
//
//        BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();
//        booleanJunction.must(createDescItemTypeQuery(queryBuilder));
//        booleanJunction.must(createFundIdQuery(queryBuilder, fundId));
//
//        conditions.forEach(c -> {
//            booleanJunction.must(c.createLuceneQuery(queryBuilder));
//        });
//
//        return booleanJunction.createQuery();
//    }

    private Query createDescItemTypeQuery(final QueryBuilder queryBuilder) {
        Integer descItemTypeId = descItemType.getItemTypeId();
        return queryBuilder.range().onField(ArrData.LUCENE_DESC_ITEM_TYPE_ID).from(descItemTypeId).to(descItemTypeId).
                createQuery();
    }

    private Query createFundIdQuery(final QueryBuilder queryBuilder, final Integer fundId) {
        return queryBuilder.keyword().onField("fundId").matching(fundId).createQuery();
    }


    public Class<?> getCls() {
        return cls;
    }

    public RulItemType getDescItemType() {
        return descItemType;
    }
}
