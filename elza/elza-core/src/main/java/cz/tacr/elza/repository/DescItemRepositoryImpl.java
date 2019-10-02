package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;


/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 */
@Component
public class DescItemRepositoryImpl implements DescItemRepositoryCustom {

    private static final Logger logger = LoggerFactory.getLogger(DescItemRepositoryImpl.class);

    @Autowired
    private EntityManager entityManager;


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
    public List<ArrDescItem> findDescItemsByNodeIds(final Collection<Integer> nodeIds, final Collection<RulItemType> itemTypes, final Integer changeId) {
        String jpql = "SELECT di FROM arr_item di JOIN FETCH di.node n JOIN FETCH di.itemType dit LEFT JOIN FETCH di.itemSpec dis " +
                "LEFT JOIN FETCH di.data d " +
                "LEFT JOIN FETCH d.record drr " +
                "LEFT JOIN FETCH d.party dpr " +
                "LEFT JOIN FETCH d.structuredObject dso " +
                "WHERE ";
        if (changeId == null) {
            jpql += "di.deleteChange IS NULL ";
        } else {
            jpql += "di.createChange.changeId <= :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId >= :changeId) ";
        }

        jpql += "AND n.nodeId IN (:nodeIds)";

        if (CollectionUtils.isNotEmpty(itemTypes)) {
            jpql += " AND di.itemType IN (:itemTypes)";
        }

        List<ArrDescItem> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<>(nodeIds);
        while (nodeIdsIterator.hasNext()) {

            Query query = entityManager.createQuery(jpql);
            if (changeId != null) {
                query.setParameter("changeId", changeId);
            }
            if (CollectionUtils.isNotEmpty(itemTypes)) {
                query.setParameter("itemTypes", itemTypes);
            }
            query.setParameter("nodeIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }

    @Override
    public List<ArrDescItem> findByNodesContainingText(final Collection<ArrNode> nodes,
                                                       final RulItemType itemType,
                                                       final Set<RulItemSpec> specifications,
                                                       final String text) {

        if(StringUtils.isBlank(text)){
            throw new IllegalArgumentException("Parametr text nesmí mít prázdnou hodnotu.");
        }

        if(itemType.getUseSpecification() && CollectionUtils.isEmpty(specifications)){
            throw new IllegalArgumentException("Musí být zadána alespoň jedna filtrovaná specifikace.");
        }

        String searchText = "%" + text + "%";

        String hql = "SELECT di FROM arr_item di JOIN FETCH di.data d WHERE (di.data IN (SELECT ds FROM arr_data_string ds WHERE ds.value like :text)" +
                " OR di.data IN (SELECT ds FROM arr_data_text ds WHERE ds.value like :text)" +
                " OR di.data IN (SELECT ds FROM arr_data_unitid ds WHERE ds.unitId like :text))"
                + " AND di.itemType = :itemType";

        if(itemType.getUseSpecification()){
            hql+= " AND di.itemSpec IN (:specs)";
        }

        hql += " AND di.node IN (:nodes) AND di.deleteChange IS NULL";

        Query query = entityManager.createQuery(hql);
        query.setParameter("itemType", itemType);
        query.setParameter("nodes", nodes);
        query.setParameter("text", searchText);
        if (itemType.getUseSpecification()) {
            query.setParameter("specs", specifications);
        }

        return query.getResultList();
    }
}
