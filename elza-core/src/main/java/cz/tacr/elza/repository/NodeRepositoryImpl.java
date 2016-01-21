package cz.tacr.elza.repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
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
     * Vrátí id nodů které mají danou hodnotu v dané verzi.
     *
     * @param text The query text.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List findByFulltextAndVersionLockChangeId(String text, Integer lockChangeId) {
        List<String> descItemIds = findDescItemIdsByData(text);
        if (descItemIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        String descItemIdsString = StringUtils.join(descItemIds, " ");
        List<Integer> descItemNodeIds = findNodeIdsByValidDescItems(lockChangeId, descItemIdsString);

        return descItemNodeIds;
    }

    /**
     * Vyhledá id atributů podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     *
     * @param text hodnota podle které se hledá
     *
     * @return id atributů které mají danou hodnotu
     */
    @SuppressWarnings("unchecked")
    private List<String> findDescItemIdsByData(String text) {
        String searchValue;
        if (StringUtils.isBlank(text)) {
            return Collections.EMPTY_LIST;
        }

        searchValue = "*" + text + "*";

        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder()
                .forEntity(ArrData.class).get();

        Query query = queryBuilder.keyword().wildcard().onField("fulltextValue").matching(searchValue).createQuery();

        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, ArrData.class);
        jpaQuery.setProjection("descItemId");

        List<String> result = (List<String>) jpaQuery.getResultList().stream().map(row -> {
            return ((Object[]) row)[0];
        }).collect(Collectors.toList());

        return result;
    }

    /**
     * Vyhledá id nodů podle platných atributů. Hledá napříč archivními pomůckami.
     *
     * @param lockChangeId id změny uzavření verze archivní pomůcky, může být null
     * @param descItemIdsString id atributů pro které se mají hledat nody
     *
     * @return id nodů které mají před danou změnou nějaký atribut
     */
    @SuppressWarnings("unchecked")
    private List<Integer> findNodeIdsByValidDescItems(Integer lockChangeId, String descItemIdsString) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder()
                .forEntity(ArrDescItem.class).get();

        Filter changeFilter;
        if (lockChangeId == null) {
            // deleteChange is null
            changeFilter = NumericRangeFilter.newIntRange("deleteChangeId", Integer.MAX_VALUE, Integer.MAX_VALUE, true,
                    true);
        } else {
            // createChangeId < lockChangeId
            NumericRangeFilter<Integer> createChangeFilter = NumericRangeFilter.newIntRange("createChangeId", null,
                    lockChangeId, false, false);
            // and (deleteChange is null or deleteChange < lockChangeId)
            NumericRangeFilter<Integer> nullDeleteChangeFilter = NumericRangeFilter.newIntRange("deleteChangeId", Integer.MAX_VALUE,
                    Integer.MAX_VALUE, true, true);
            NumericRangeFilter<Integer> deleteChangeFilter = NumericRangeFilter.newIntRange("deleteChangeId", null,
                    lockChangeId, false, false);

            Filter[] deleteChangeFilters = {nullDeleteChangeFilter, deleteChangeFilter};
            ChainedFilter orFilter = new ChainedFilter(deleteChangeFilters, ChainedFilter.OR);

            Filter[] andFilters = {createChangeFilter, orFilter};
            changeFilter = new ChainedFilter(andFilters, ChainedFilter.AND);
        }



        Query descItemIdsQuery = queryBuilder.keyword().onField("descItemIdString").matching(descItemIdsString).createQuery();
        Query validDescItemInVersionQuery = queryBuilder.all().filteredBy(changeFilter).createQuery();
        Query query = queryBuilder.bool().must(descItemIdsQuery).must(validDescItemInVersionQuery).createQuery();

        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, ArrDescItem.class);
        jpaQuery.setProjection("nodeId");

        List<Integer> result = jpaQuery.getResultList().stream().mapToInt(row -> {
            return (int) ((Object[]) row)[0];
        }).boxed().collect(Collectors.toList());

        return result;
    }
}
