package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import cz.tacr.elza.domain.RulItemType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.utils.ObjectListIterator;


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

        Query query = entityManager.createNativeQuery(hqlBuilder.toString());
        query.setParameter("descItemTypeId", titleType.getItemTypeId());

        if (lockChange != null) {
            query.setParameter("lockChange", lockChange.getChangeId());
        }


        Map<Integer, DescItemTitleInfo> result = new HashMap<>(nodeIds.size());

        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        while (iterator.hasNext()) {
            List<Integer> partIds = iterator.next();
            query.setParameter("ids", partIds);

            for (Object[] row : (List<Object[]>) query.getResultList()) {
                Integer nodeId = (Integer) row[0];
                Integer nodeVersion = (Integer) row[1];
                result.put(nodeId, new DescItemTitleInfo(nodeId, (String) row[2], nodeVersion));
            }
        }

        return result;
    }
}
