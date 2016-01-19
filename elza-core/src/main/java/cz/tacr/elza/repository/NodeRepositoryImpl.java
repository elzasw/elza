package cz.tacr.elza.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.utils.NodeUtils;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Component
public class NodeRepositoryImpl implements NodeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public List<ArrNode> findNodesByDirection(final ArrNode node,
                                              final ArrFindingAidVersion version,
                                              final RelatedNodeDirection direction) {
        Assert.notNull(node);
        Assert.notNull(version);
        Assert.notNull(direction);


        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootLevel().getNode(),
                version.getLockChange());
        List<ArrLevel> levels = levelRepository.findLevelsByDirection(level, version, direction);

        return NodeUtils.createNodeList(levels);
    }

    /**
     * A basic search for the entity User. The search is done by exact match per
     * keywords on fields name, city and email.
     *
     * @param text The query text.
     */
    @Override
    public List search(String text, Integer lockChangeId) {
        text = "38 39";
        lockChangeId = 84;
        lockChangeId = null;
        // get the full text entity manager
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        // create the query using Hibernate Search query DSL
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder()
                .forEntity(ArrData.class).get();

//        Filter changeFilter;
//        if (lockChangeId == null) {
//            // deleteChange is null
//            changeFilter = NumericRangeFilter.newIntRange("deleteChangeId", Integer.MIN_VALUE, Integer.MIN_VALUE, true,
//                    true);
//        } else {
//            // createChangeId < lockChangeId
//            NumericRangeFilter<Integer> createChangeFilter = NumericRangeFilter.newIntRange("createChangeId", null,
//                    lockChangeId, false, false);
//            // and (deleteChange is null or deleteChange < lockChangeId)
//            NumericRangeFilter<Integer> nullDeleteChangeFilter = NumericRangeFilter.newIntRange("deleteChangeId", Integer.MIN_VALUE,
//                    Integer.MIN_VALUE, true, true);
//            NumericRangeFilter<Integer> deleteChangeFilter = NumericRangeFilter.newIntRange("deleteChangeId", lockChangeId,
//                    null, false, false);
//
//            Filter[] deleteChangeFilters = {nullDeleteChangeFilter, deleteChangeFilter};
//            ChainedFilter orFilter = new ChainedFilter(deleteChangeFilters, ChainedFilter.OR);
//
//            Filter[] andFilters = {createChangeFilter, orFilter};
//            changeFilter = new ChainedFilter(andFilters, ChainedFilter.AND);
//        }
        // a very basic query by keywords
        org.apache.lucene.search.Query query = queryBuilder.keyword()//.filteredBy(changeFilter)
                .onField("nodeId").matching(text).createQuery();

        // wrap Lucene query in an Hibernate Query object
        org.hibernate.search.jpa.FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query,
                ArrData.class);

        jpaQuery.setProjection(FullTextQuery.ID, "nodeId");

        // execute search and return results (sorted by relevance as default)
        @SuppressWarnings("unchecked")
        List results = jpaQuery.getResultList();

        System.out.println(StringUtils.join(results, "|"));
        return results;
    } // method search
}
