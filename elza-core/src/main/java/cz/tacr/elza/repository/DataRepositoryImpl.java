package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.utils.ObjectListIterator;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
public class DataRepositoryImpl implements DataRepositoryCustom {

    @Autowired
    private EntityManager entityManager;


    @Override
    public List<ArrData> findDescItemsByNodeIds(final Set<Integer> nodeIds,
                                                final Set<RulDescItemType> descItemTypes,
                                                final ArrFundVersion version) {


        String hql = "SELECT d FROM arr_data d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.descItemType dit JOIN FETCH d.dataType dt WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.descItemType IN (:descItemTypes) AND n.nodeId IN (:nodeIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("descItemTypes", descItemTypes);

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(nodeIds);
        while (nodeIdsIterator.hasNext()) {
            query.setParameter("nodeIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }


    @Override
    public List<ArrData> findByDataIdsAndVersionFetchSpecification(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFundVersion version) {
        String hql = "SELECT d FROM arr_data d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.descItemType dit JOIN FETCH di.descItemSpec dis JOIN FETCH d.dataType dt WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.descItemType IN (:descItemTypes) AND d.dataId IN (:dataIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("descItemTypes", descItemTypes);

        List<ArrData> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(dataIds);
        while (nodeIdsIterator.hasNext()) {
            query.setParameter("dataIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }


    @Override
    public <T extends ArrData> List<T> findByNodesContainingText(final Collection<ArrNode> nodes,
                                                                 final RulDescItemType descItemType,
                                                                 final String text) {

        if(StringUtils.isBlank(text)){
            throw new IllegalArgumentException("Parametr text nesmí mít prázdnou hodnotu.");
        }


        String searchText = "%" + text + "%";

        String tableName;
        switch (descItemType.getDataType().getCode()){
            case "STRING":
                tableName = descItemType.getDataType().getStorageTable();
                break;
            case "TEXT":
                tableName = descItemType.getDataType().getStorageTable();
                break;
            default:
                throw new IllegalStateException(
                        "Není zatím implementováno pro typ " + descItemType.getDataType().getCode());
        }

        String hql = "SELECT d FROM " + tableName +" d"
                + " JOIN FETCH d.descItem di "
                + " JOIN FETCH di.node n WHERE di.descItemType = :descItemType AND di.node IN (:nodes) AND d.value like :text";

        Query query = entityManager.createQuery(hql);
        query.setParameter("descItemType", descItemType);
        query.setParameter("nodes", nodes);
        query.setParameter("text", searchText);

        return query.getResultList();
    }
}
