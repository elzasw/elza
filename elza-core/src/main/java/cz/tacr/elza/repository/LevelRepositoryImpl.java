package cz.tacr.elza.repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.utils.ObjectListIterator;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.11.2015
 */
@Component
public class LevelRepositoryImpl implements LevelRepositoryCustom {

    private final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;


    @Override
    public List<ArrLevel> findByParentNode(final ArrNode nodeParent, @Nullable final ArrChange change) {
        if (change == null) {
            return levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(nodeParent);
        } else {
            return levelRepository.findByParentNodeOrderByPositionAsc(nodeParent, change);
        }
    }

    @Override
    public List<ArrLevel> findByNode(final ArrNode node, @Nullable final ArrChange change) {
        if (change == null) {
            return levelRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            return levelRepository.findByNodeOrderByPositionAsc(node, change);
        }
    }

    @Override
    public Integer countChildsByParent(final ArrNode node, @Nullable final ArrChange lockChange) {
        if(lockChange == null){
            return levelRepository.countChildsByParent(node);
        }else{
            return levelRepository.countChildsByParentAndChange(node, lockChange);
        }
    }

    @Override
    public List<ArrLevel> findAllParentsByNodeAndVersion(final ArrNode node, final ArrFundVersion version) {
        Assert.notNull(node);
        Assert.notNull(version);


        ArrChange lockChange = version.getLockChange();

        List<ArrLevel> parents = new LinkedList<>();

        boolean found = false;
        for (ArrLevel arrLevel : findByNode(node, lockChange)) {
            if (findParentNodesToRootByNodeId(parents, arrLevel, version.getRootNode(), lockChange)) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalStateException(
                    "Nebyl nalezen seznam rodičů pro node " + node.getNodeId() + " ve verzi " + version.toString());
        }


        return parents;
    }


    @Override
    public ArrLevel findOlderSibling(final ArrLevel level, @Nullable final ArrChange lockChange) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrLevel> query = builder.createQuery(ArrLevel.class);
        Root<ArrLevel> root = query.from(ArrLevel.class);


        Predicate nodeParent = builder.equal(root.get("nodeParent"), level.getNodeParent());
        Predicate position = builder.lt(root.get("position"), level.getPosition());

        Predicate condition;
        if (lockChange == null) {
            condition = builder.and(nodeParent, position, builder.isNull(root.get("deleteChange")));
        } else {
            Join<Object, Object> createChange = root.join("createChange", JoinType.INNER);
            Join<Object, Object> deleteChange = root.join("deleteChange", JoinType.LEFT);

            condition = builder.and(
                    nodeParent,
                    builder.lt(createChange.get("changeId"), lockChange.getChangeId()),
                    position, builder.or(
                            builder.isNull(deleteChange.get("changeId")),
                            builder.gt(deleteChange.get("changeId"), lockChange.getChangeId()))
            );
        }

        Order order = builder.desc(root.get("position"));
        query.select(root).where(condition).orderBy(order);

        List<ArrLevel> resultList = entityManager.createQuery(query).setFirstResult(0).setMaxResults(1).getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    /**
     * Rekurzivně prochází strom ke kořenu a pokud má uzel jako prarodiče daný kořenový uzel, je přidán do seznamu.
     *
     * @param toRootTree seznam rodičů hledaného uzlu
     * @param level      uzel, pro který hledáme rodiče
     * @param rootNode   kořenový uzel verze
     * @param lockChange čas uzamčení verze
     * @return true pokud má daný uzel za prarodiče kořenový uzel
     */
    private boolean findParentNodesToRootByNodeId(final List<ArrLevel> toRootTree,
                                                  final ArrLevel level,
                                                  final ArrNode rootNode,
                                                  @Nullable final ArrChange lockChange) {
        if (level.getNode().equals(rootNode)) {
            return true;
        }

        for (ArrLevel parentLevel : findByNode(level.getNodeParent(), lockChange)) {
            if (findParentNodesToRootByNodeId(toRootTree, parentLevel, rootNode, lockChange)) {
                toRootTree.add(0, parentLevel);
                return true;
            }
        }

        return false;
    }


    @Override
    public List<ArrLevel> findAllChildrenByNode(final ArrNode node, final ArrChange lockChange) {
        Assert.notNull(node);

        List<ArrLevel> children = findByParentNode(node, lockChange);

        List<ArrLevel> result = new LinkedList<ArrLevel>();
        while (!children.isEmpty()) {
            ArrLevel child = children.remove(0);
            result.add(child);

            if (lockChange == null) {
                children.addAll(
                        levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(child.getNode()));
            } else {
                children.addAll(levelRepository.findByParentNodeOrderByPositionAsc(child.getNode(), lockChange));
            }
        }

        return result;
    }


    @Override
    public boolean isLevelInRootTree(final ArrLevel level,
                                     final ArrNode rootNode,
                                     @Nullable final ArrChange lockChange) {
        if (level.getNode().equals(rootNode) || rootNode.equals(level.getNodeParent())) {
            return true;
        }


        boolean result = false;
        for (ArrLevel parentLevel : findByNode(level.getNodeParent(), lockChange)) {
            result = result || isLevelInRootTree(parentLevel, rootNode, lockChange);
        }

        return result;
    }


    @Override
    public ArrLevel findNodeInRootTreeByNodeId(final ArrNode node,
                                               final ArrNode rootNode,
                                               @Nullable final ArrChange lockChange) {
        List<ArrLevel> levelsByNode = findByNode(node, lockChange);

        if (levelsByNode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Entita byla změněna nebo odstraněna. Načtěte znovu entitu a opakujte akci.");
        } else if (levelsByNode.size() == 1) {
            return levelsByNode.iterator().next();
        }


        for (ArrLevel arrFaLevel : levelsByNode) {
            if (levelRepository.isLevelInRootTree(arrFaLevel, rootNode, lockChange)) {
                return arrFaLevel;
            }
        }

        return null;
    }


    @Override
    public List<ArrLevel> findLevelsByDirection(final ArrLevel level,
                                                final ArrFundVersion version,
                                                final RelatedNodeDirection direction) {
        Assert.notNull(level);
        Assert.notNull(version);
        Assert.notNull(direction);

        switch (direction) {
            case NODE:
                return Arrays.asList(level);
            case PARENT:

                // pokud je to root level, nemuze mit rodice
                if (level.getNode().equals(version.getRootNode())) {
                    return Arrays.asList();
                }

                return Arrays.asList(levelRepository
                        .findNodeInRootTreeByNodeId(level.getNodeParent(), version.getRootNode(),
                                version.getLockChange()));
            case ASCENDANTS:
                return levelRepository.findAllParentsByNodeAndVersion(level.getNode(), version);
            case CHILDREN:
                return levelRepository.findByParentNode(level.getNode(), version.getLockChange());
            case DESCENDANTS:
                return levelRepository.findAllChildrenByNode(level.getNode(), version.getLockChange());
            case SIBLINGS: {
                ArrayList<ArrLevel> siblings = new ArrayList<>(levelRepository
                        .findByParentNode(level.getNodeParent(), version.getLockChange()));

                //požadujeme pouze nejbližšího sourozence před a za objektem
                int nodeIndex = siblings.indexOf(level);
                List<ArrLevel> result = new ArrayList<>(2);
                if (nodeIndex >= 1) {
                    result.add(siblings.get(nodeIndex - 1));
                }
                if ((nodeIndex + 1) < siblings.size()) {
                    result.add(siblings.get(nodeIndex + 1));
                }

                return result;
            }
            case ALL_SIBLINGS: {
                ArrayList<ArrLevel> siblings = new ArrayList<>(levelRepository
                        .findByParentNode(level.getNodeParent(), version.getLockChange()));
                return siblings;
            }
            case ALL:
                return levelRepository.findAllChildrenByNode(version.getRootNode(), version.getLockChange());
            default:
                throw new NotImplementedException(
                        "Chybi implementace pro smer prohledavani stromu " + direction.name());
        }
    }


    @Override
    public List<LevelInfo> readTree(final ArrFundVersion version){
        Set<Integer> leaves = new HashSet<>();
        leaves.add(version.getRootNode().getNodeId());

        Set<Integer> allIds = new HashSet<>();
        while (!leaves.isEmpty()){

            List<Object[]> resultList = subTree(version, leaves);
            leaves.clear();

            for (Object[] row : (List<Object[]>) (List<?>) resultList) {
                allIds.add((Integer) row[0]);
                allIds.add((Integer) row[1]);
                allIds.add((Integer) row[2]);
                allIds.add((Integer) row[3]);

                leaves.add((Integer) row[4]);
            }
            //množina může obsahovat i null hodnoty, takže je vyhodíme
            leaves.remove(null);
        }

        allIds.remove(null);

        return findLevelInfoByIds(allIds);
    }


    /**
     * Načte potomky daných uzlů. Vždy načítá 4 generace uzlů.
     *
     * @param version verze stromu
     * @param rootIds seznam id uzlů, pro které se mají načíst potomci
     * @return 4 generace potomků
     */
    private List<Object[]> subTree(final ArrFundVersion version, final Set<Integer> rootIds) {

        List<Object[]> result = new LinkedList<>();

        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(rootIds);
        while (iterator.hasNext()) {
            List<Integer> partIds = iterator.next();

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT DISTINCT ");
            builder.append("a1.level_id as a1, ");
            builder.append("a2.level_id as a2, ");
            builder.append("a3.level_id as a3, ");
            builder.append("a4.level_id as a4, ");
            builder.append("a4.node_id as n4 ");

            if (version.getLockChange() == null) {
                builder.append("FROM arr_level a1 ");
                builder.append("LEFT JOIN arr_level a2 ON a2.node_id_parent = a1.node_id AND a2.delete_change_id IS NULL ");
                builder.append("LEFT JOIN arr_level a3 ON a3.node_id_parent = a2.node_id AND a3.delete_change_id IS NULL ");
                builder.append("LEFT JOIN arr_level a4 ON a4.node_id_parent = a3.node_id AND a4.delete_change_id IS NULL ");

                builder.append("WHERE a1.delete_change_id IS NULL AND ");
            } else {
                builder.append("FROM arr_level a1 ");
                builder.append("LEFT JOIN arr_level a2 ON a2.node_id_parent = a1.node_id AND a2.create_change_id < :closeDate AND (a2.delete_change_id IS NULL OR a2.delete_change_id > :closeDate) ");
                builder.append("LEFT JOIN arr_level a3 ON a3.node_id_parent = a2.node_id AND a3.create_change_id < :closeDate AND (a3.delete_change_id IS NULL OR a3.delete_change_id > :closeDate) ");
                builder.append("LEFT JOIN arr_level a4 ON a4.node_id_parent = a3.node_id AND a4.create_change_id < :closeDate AND (a4.delete_change_id IS NULL OR a4.delete_change_id > :closeDate) ");


                builder.append("WHERE a1.create_change_id < :closeDate AND (a1.delete_change_id IS NULL OR a1.delete_change_id > :closeDate) AND ");
            }


            builder.append("a1.node_id_parent IN (:ids)");

            Query query = entityManager.createNativeQuery(builder.toString());
            if (version.getLockChange() != null) {
                query.setParameter("closeDate", version.getLockChange().getChangeId());
            }

            query.setParameter("ids", partIds);

            result.addAll((List<Object[]>) query.getResultList());
        }

        return result;
    }


    /**
     * Najde uzly podle jejich primárního id.
     *
     * @param ids seznam uzlů
     * @return uzly s danými id
     */
    private List<LevelInfo> findLevelInfoByIds(final Collection<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.EMPTY_LIST;
        }

        List<LevelInfo> result = new ArrayList<>(ids.size());

        String hql = "SELECT l.node_id, l.position, l.node_id_parent FROM arr_level l WHERE l.level_id IN (:ids)";

        Query query = entityManager.createNativeQuery(hql);

        ObjectListIterator<Integer> iterator = new ObjectListIterator<Integer>(ids);
        while (iterator.hasNext()) {
            List<Integer> partIds = iterator.next();

            query.setParameter("ids", partIds);

            for (Object[] row : (List<Object[]>) query.getResultList()) {
                result.add(new LevelInfo((Integer) row[0], (Integer) row[1], (Integer) row[2]));
            }
        }

        return result;
    }

    @Override
    public List<Integer> findNodeIdsSubtree(final ArrNode node, final ArrChange change) {

        String sql = "WITH RECURSIVE treeData AS (SELECT t.* FROM arr_level t WHERE t.node_id = :nodeId UNION ALL SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) " +
                "SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL AND n.last_update > :date";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("nodeId", node.getNodeId());
        query.setParameter("date", Timestamp.valueOf(change.getChangeDate()));

        return (List<Integer>) query.getResultList();
    }

    @Override
    public List<Integer> findNodeIdsParent(final ArrNode node, final ArrChange change) {

        String sql = "WITH RECURSIVE treeData AS (SELECT t.* FROM arr_level t WHERE t.node_id = :nodeId UNION ALL SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id_parent = t.node_id) " +
                "SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL AND n.last_update > :date";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("nodeId", node.getNodeId());
        query.setParameter("date", Timestamp.valueOf(change.getChangeDate()));

        return (List<Integer>) query.getResultList();
    }

}
