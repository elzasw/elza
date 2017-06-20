package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.utils.ObjectListIterator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Component
public class DescItemRepositoryImpl implements DescItemRepositoryCustom {

    private Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDescItem> findByNodes(final Collection<ArrNode> nodes, @Nullable final ArrChange lockChange) {
        Assert.notNull(nodes);

        if (nodes.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (lockChange == null) {
            return descItemRepository.findByNodesAndDeleteChangeIsNull(nodes);
        } else {
            return descItemRepository.findByNodesAndDeleteChange(nodes, lockChange);
        }
    }



    @Override
    public Map<Integer, DescItemTitleInfo> findDescItemTitleInfoByNodeId(final Set<Integer> nodeIds,
                                                                         final RulItemType titleType,
                                                                         @Nullable final ArrChange lockChange) {
        Assert.notNull(titleType);
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

            Join<Object, Object> nodeJoin = root.join(ArrDescItem.NODE, JoinType.INNER);
            root.fetch(ArrDescItem.NODE, JoinType.INNER);

            Predicate predicateNodeIds = nodeJoin.get(ArrNode.NODE_ID).in(subNodeIds);
            Predicate predicateDeleteChange = root.get(ArrDescItem.DELETE_CHANGE_ID).isNull();
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
}
