package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateOptionsStep;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.vo.NodeIdChangeId;
import cz.tacr.elza.service.vo.NodeIdChangeIdDescItem;

/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 */
@Component
public class DescItemRepositoryImpl implements DescItemRepositoryCustom {

    private static final Logger logger = LoggerFactory.getLogger(DescItemRepositoryImpl.class);

    @Autowired
    private EntityManager entityManager;
    
    @Autowired
	private DataService dataService;

    private SearchSession searchSession = null;

    private SearchSession getSearchSession() {
		if (searchSession == null) {
			searchSession = Search.session(entityManager);
    	}
    	return searchSession;
    }

    @Override
    public List<NodeIdChangeIdDescItem> findDescItemsByNodeChangePairs(final Collection<NodeIdChangeId> pairs,
                                                                       final Collection<Integer> itemTypeIds) {

        if (pairs.isEmpty()) {
            return Collections.emptyList();
        }

        // dedupe defensively: identical (nodeId, changeId) doesn't need to be joined twice
        List<NodeIdChangeId> uniquePairs = new ArrayList<>(new LinkedHashSet<>(pairs));

        // stay well below the JDBC 65 535 parameter cap (each pair uses 2 params + itemTypeIds)
        final int CHUNK = 500;

        // pair-to-itemId mapping collected across chunks: [nodeId, changeId, itemId]
        List<Object[]> mapping = new ArrayList<>();
        for (int from = 0; from < uniquePairs.size(); from += CHUNK) {
            List<NodeIdChangeId> chunk = uniquePairs.subList(from, Math.min(from + CHUNK, uniquePairs.size()));
            mapping.addAll(runPairsChunk(chunk, itemTypeIds));
        }

        if (mapping.isEmpty()) {
            return Collections.emptyList();
        }

        // load managed ArrDescItem entities with the same fetch-joins as findDescItemsByNodeIds
        Set<Integer> itemIds = new HashSet<>();
        for (Object[] row : mapping) {
            itemIds.add(((Number) row[2]).intValue());
        }

        Map<Integer, ArrDescItem> byId = new HashMap<>();
        for (List<Integer> partition : Lists.partition(new ArrayList<>(itemIds), ObjectListIterator.getMaxBatchSize())) {
            TypedQuery<ArrDescItem> q = entityManager.createQuery(
                    "SELECT di FROM arr_desc_item di" +
                            " JOIN FETCH di.node n" +
                            " JOIN FETCH di.itemType dit" +
                            " LEFT JOIN FETCH di.itemSpec dis" +
                            " WHERE di.itemId IN :ids",
                    ArrDescItem.class);
            q.setParameter("ids", partition);
            for (ArrDescItem item : q.getResultList()) {
                byId.put(item.getItemId(), item);
            }
        }

        // reconstruct pair → item associations in the original row order
        List<NodeIdChangeIdDescItem> result = new ArrayList<>(mapping.size());
        for (Object[] row : mapping) {
            Integer nodeId = ((Number) row[0]).intValue();
            Integer changeId = ((Number) row[1]).intValue();
            Integer itemId = ((Number) row[2]).intValue();
            ArrDescItem item = byId.get(itemId);
            if (item != null) {
                result.add(new NodeIdChangeIdDescItem(new NodeIdChangeId(nodeId, changeId), item));
            }
        }
        return result;
    }

    private List<Object[]> runPairsChunk(final List<NodeIdChangeId> pairs,
                                         final Collection<Integer> itemTypeIds) {

        StringBuilder valuesClause = new StringBuilder(pairs.size() * 8);
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0) {
                valuesClause.append(", ");
            }
            valuesClause.append("(?, ?)");
        }

        StringBuilder sql = new StringBuilder(512);
        sql.append("SELECT p.node_id, p.change_id, di.item_id ")
           .append("FROM arr_desc_item di ")
           .append("JOIN arr_item i ON i.item_id = di.item_id ")
           .append("JOIN (VALUES ").append(valuesClause)
           .append(") AS p(node_id, change_id) ON di.node_id = p.node_id ")
           .append("WHERE i.create_change_id <= p.change_id ")
           .append("  AND (i.delete_change_id IS NULL OR i.delete_change_id >= p.change_id)");

        if (CollectionUtils.isNotEmpty(itemTypeIds)) {
            sql.append(" AND i.item_type_id IN (");
            for (int i = 0; i < itemTypeIds.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
            }
            sql.append(")");
        }

        Query q = entityManager.createNativeQuery(sql.toString());

        int idx = 1;
        for (NodeIdChangeId pair : pairs) {
            q.setParameter(idx++, pair.nodeId());
            q.setParameter(idx++, pair.changeId());
        }
        if (CollectionUtils.isNotEmpty(itemTypeIds)) {
            for (Integer typeId : itemTypeIds) {
                q.setParameter(idx++, typeId);
            }
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
    }    

    @Override
    public Map<Integer, DescItemTitleInfo> findDescItemTitleInfoByNodeId(final Set<Integer> nodeIds,
                                                                         final RulItemType titleType,
                                                                         @Nullable final ArrChange lockChange) {
        Assert.notNull(titleType, "Typ musí být vyplněn");
        if (CollectionUtils.isEmpty(nodeIds)) {
            return new HashMap<>();
        }

        if (titleType != null) {
            if (!titleType.getDataType().getCode().equalsIgnoreCase("STRING")) {
                logger.warn("Title uzlu musí být datového typu STRING, jinak nebude nalezen.");
            }
        }

        StringBuilder hqlBuilder = new StringBuilder();
        hqlBuilder.append("SELECT DISTINCT n.node_id, n.version, ds.value ");
        hqlBuilder.append("FROM arr_node n ");
        hqlBuilder.append("LEFT JOIN arr_desc_item a ON n.node_id = a.node_id AND a.desc_item_type_id = :descItemTypeId ");

        if (lockChange == null) {
            hqlBuilder.append("AND a.delete_change_id IS NULL ");
        } else {
            hqlBuilder
                    .append("AND a.create_change_id < :lockChange AND (a.delete_change_id IS NULL OR a.delete_change_id > :lockChange) ");
        }

        hqlBuilder.append("LEFT JOIN arr_data d ON d.desc_item_id = a.desc_item_id ");
        hqlBuilder.append("LEFT JOIN arr_data_string ds ON d.data_id = ds.data_id ");
        hqlBuilder.append("WHERE n.node_id IN (:ids) ");

        Map<Integer, DescItemTitleInfo> result = new HashMap<>(nodeIds.size());

        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        while (iterator.hasNext()) {
            List<Integer> partIds = iterator.next();

            Query query = entityManager.createNativeQuery(hqlBuilder.toString());
            query.setParameter("descItemTypeId", titleType.getItemTypeId());

            if (lockChange != null) {
                query.setParameter("lockChange", lockChange.getChangeId());
            }
            query.setParameter("ids", partIds);

            for (Object[] row : (List<Object[]>) query.getResultList()) {
                Integer nodeId = (Integer) row[0];
                Integer nodeVersion = (Integer) row[1];
                result.put(nodeId, new DescItemTitleInfo(nodeId, (String) row[2], nodeVersion));
            }
        }

        return result;
    }

    @Override
    public Map<Integer, List<ArrDescItem>> findByNodes(final Collection<Integer> nodeIds) {
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        Map<Integer, List<ArrDescItem>> result = new HashMap<>();
        while (iterator.hasNext()) {
            List<Integer> subNodeIds = iterator.next();

            // SELECT i FROM arr_desc_item i WHERE i.node in (?1) AND i.deleteChange IS NULL

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<ArrDescItem> query = builder.createQuery(ArrDescItem.class);
            Root<ArrDescItem> root = query.from(ArrDescItem.class);

            Join<Object, Object> nodeJoin = root.join(ArrDescItem.FIELD_NODE, JoinType.INNER);
            root.fetch(ArrDescItem.FIELD_NODE, JoinType.INNER);

            Predicate predicateNodeIds = nodeJoin.get(ArrNode.FIELD_NODE_ID).in(subNodeIds);
            Predicate predicateDeleteChange = root.get(ArrDescItem.FIELD_DELETE_CHANGE_ID).isNull();
            query.where(predicateNodeIds, predicateDeleteChange);

            List<ArrDescItem> resultList = entityManager.createQuery(query).getResultList();

            for (ArrDescItem descItem : resultList) {
                Integer nodeId = descItem.getNodeId();
                List<ArrDescItem> descItems = result.get(nodeId);
                if (descItems == null) {
                    descItems = new ArrayList<>();
                    result.put(nodeId, descItems);
                }
                descItems.add(descItem);
            }

        }
        return result;
    }

    @Override
    public List<ArrDescItem> findDescItemsByNodeIds(final Collection<Integer> nodeIds, 
    		final Collection<Integer> itemTypeIds, final Integer changeId) {
        String jpql = "SELECT di FROM arr_desc_item di JOIN FETCH di.node n JOIN FETCH di.itemType dit LEFT JOIN FETCH di.itemSpec dis " +
                //"LEFT JOIN FETCH di.data d " +
                //"LEFT JOIN FETCH d.structuredObject dso " +
                "WHERE ";
        if (changeId == null) {
            jpql += "di.deleteChange IS NULL ";
        } else {
            jpql += "di.createChange.changeId <= :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId >= :changeId) ";
        }

        jpql += "AND n.nodeId IN (:nodeIds)";

        if (CollectionUtils.isNotEmpty(itemTypeIds)) {
            jpql += " AND di.itemTypeId IN (:itemTypeIds)";
        }

        List<ArrDescItem> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<>(nodeIds);
        while (nodeIdsIterator.hasNext()) {

            Query query = entityManager.createQuery(jpql);
            if (changeId != null) {
                query.setParameter("changeId", changeId);
            }
            if (CollectionUtils.isNotEmpty(itemTypeIds)) {
                query.setParameter("itemTypeIds", itemTypeIds);
            }
            query.setParameter("nodeIds", nodeIdsIterator.next());
            
            // fetch data
            List<ArrDescItem> fetchedData = query.getResultList();
            dataService.findItemsWithData(fetchedData);

            result.addAll(fetchedData);
        }

        return result;
    }

    @Override
    public List<ArrDescItem> findByNodesContainingText(final Collection<ArrNode> nodes,
                                                       final RulItemType itemType,
                                                       final Set<RulItemSpec> specifications,
                                                       final String text) {
        if (StringUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Parametr text nesmí mít prázdnou hodnotu.");
        }

        if (itemType.getUseSpecification() && CollectionUtils.isEmpty(specifications)){
            throw new IllegalArgumentException("Musí být zadána alespoň jedna filtrovaná specifikace.");
        }

        SearchPredicateFactory factory = getSearchSession().scope(ArrDescItem.class).predicate();
        BooleanPredicateClausesStep<?> finalPredicate = factory.bool();

        // by nodes
        BooleanPredicateClausesStep<?> nodeItems = factory.bool();
        nodes.forEach(node -> {
        	nodeItems.should(factory.match().field(ArrDescItem.FIELD_NODE_ID).matching(node.getNodeId()));
        });
        finalPredicate.must(nodeItems);

        // deleteChange is null
    	BooleanPredicateClausesStep<?> nullDeleteChange = factory.bool().mustNot(factory.exists().field(ArrDescItem.FIELD_DELETE_CHANGE_ID));
    	finalPredicate.must(nullDeleteChange);

        // by itemType
        MatchPredicateOptionsStep<?> itemTypeId = factory.match().field(ArrDescItem.FIELD_DESC_ITEM_TYPE_ID).matching(itemType.getItemTypeId());
        finalPredicate.must(itemTypeId);

        // by itemSpecs
        if (itemType.getUseSpecification()) {
            BooleanPredicateClausesStep<?> itemSpecs = factory.bool();
        	specifications.forEach(spec -> {
        		itemSpecs.should(factory.match().field(ArrDescItem.FIELD_ITEM_SPEC_ID).matching(spec.getItemSpecId()));
        	});
        	finalPredicate.must(itemSpecs);
        }

        // by text
        String searchValue = '*' + text + '*';
        WildcardPredicateOptionsStep<?> textPredicate = factory.wildcard().field(ArrDescItem.FULLTEXT_ATT).matching(searchValue);
        finalPredicate.must(textPredicate);

        List<ArrDescItem> fetchedData = getSearchSession().search(ArrDescItem.class)
							        		.where(finalPredicate.toPredicate())
							        		.fetchAll()
							        		.hits();
        dataService.findItemsWithData(fetchedData);

		return fetchedData;
    }

    @Override
    public List<ArrDescItem> findByNodesContainingStructureObjectIds(final Collection<ArrNode> nodes,
                                                                     final RulItemType itemType,
                                                                     final Set<RulItemSpec> specifications,
                                                                     final Collection<Integer> stuctureObjectIds) {

        if (itemType.getUseSpecification() && CollectionUtils.isEmpty(specifications)) {
            throw new IllegalArgumentException("Musí být zadána alespoň jedna filtrovaná specifikace.");
        }

        String hql = "SELECT i FROM arr_desc_item i WHERE i.itemType = :itemType"
                + " AND i.node IN (:nodes) AND i.deleteChange IS NULL";

        if (!CollectionUtils.isEmpty(stuctureObjectIds)) {
            if (stuctureObjectIds.iterator().next() == -1) { // vybrat pouze prázdné hodnoty
                hql += " AND i.data IS NULL";
            } else {
                hql += " AND i.data IN (SELECT ds FROM arr_data_structure_ref ds WHERE ds.structuredObjectId IN :stuctureObjectIds)";
            }
        }
        
        if (itemType.getUseSpecification()) {
            hql += " AND i.itemSpec IN (:specs)";
        }

        Query query = entityManager.createQuery(hql);
        query.setParameter("itemType", itemType);
        query.setParameter("nodes", nodes);
        if (!CollectionUtils.isEmpty(stuctureObjectIds) && stuctureObjectIds.iterator().next() != -1) {
            query.setParameter("stuctureObjectIds", stuctureObjectIds);
        }
        if (itemType.getUseSpecification()) {
            query.setParameter("specs", specifications);
        }

        List<ArrDescItem> fetchedData = query.getResultList();
        dataService.findItemsWithData(fetchedData);

		return fetchedData;
    }
}
