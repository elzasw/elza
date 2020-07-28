package cz.tacr.elza.repository;

import java.sql.Timestamp;
import java.util.ArrayList;
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
import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.11.2015
 */
@Component
public class LevelRepositoryImpl implements LevelRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public List<ArrLevel> findByParentNode(final ArrNode nodeParent, @Nullable final ArrChange lockChange) {
        if (lockChange == null) {
            return levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(nodeParent);
        } else {
            return levelRepository.findByParentNodeOrderByPositionAsc(nodeParent, lockChange);
        }
    }

    @Override
    public ArrLevel findByNode(final ArrNode node, @Nullable final ArrChange lockChange) {
        if (lockChange == null) {
            return levelRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            return levelRepository.findByNodeAndNotNullLockChange(node, lockChange);
        }
    }

    @Override
    public ArrLevel findByNodeId(final Integer nodeId, @Nullable final ArrChange lockChange) {
        if (lockChange == null) {
            return levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
        } else {
            return levelRepository.findByNodeIdAndNotNullLockChange(nodeId, lockChange);
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
    public List<ArrLevel> findAllParentsByNodeId(final Integer nodeId,
                                                 @Nullable final ArrChange lockChange,
                                                 boolean orderFromRoot) {
        Validate.notNull(nodeId);

        RecursiveQueryBuilder<ArrLevel> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(ArrLevel.class);

        String specifiedVerCond = "l.create_change_id < :lockChangeId AND (l.delete_change_id IS NULL OR l.delete_change_id > :lockChangeId)";
        String currentVerCond = "l.delete_change_id IS NULL";
        String verCond = lockChange != null ? specifiedVerCond : currentVerCond;

        rqBuilder.addSqlPart("WITH RECURSIVE parentPath(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position, path) AS (")
                .addSqlPart("SELECT l.*, 1 FROM arr_level l WHERE l.node_id = :nodeId AND ")
                .addSqlPart(verCond)
                .addSqlPart(" UNION ALL ")
                .addSqlPart("SELECT l.*, pp.path + 1 FROM arr_level l JOIN parentPath pp ON l.node_id=pp.node_id_parent WHERE ")
                .addSqlPart(verCond)
                .addSqlPart(") SELECT * FROM parentPath WHERE node_id <> :nodeId ORDER BY path ")
                .addSqlPart(orderFromRoot ? "DESC" : "ASC");

        rqBuilder.prepareQuery(entityManager);

        rqBuilder.setParameter("nodeId", nodeId);
        if (lockChange != null) {
            Integer lockChangeId = lockChange.getChangeId();
            rqBuilder.setParameter("lockChangeId", Validate.notNull(lockChangeId));
        }

        return rqBuilder.getQuery().getResultList();
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
        final ArrLevel arrLevel = resultList.isEmpty() ? null : resultList.get(0);
        return arrLevel;
    }

    // TODO: Rewrite this query with recursive query
    @Override
    public List<ArrLevel> findAllChildrenByNode(final ArrNode node, final ArrChange lockChange) {
        Assert.notNull(node, "JP musí být vyplněna");

        List<ArrLevel> children = findByParentNode(node, lockChange);

        List<ArrLevel> result = new LinkedList<>();
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
    public List<ArrLevel> findLevelsByDirection(final ArrLevel level,
                                                final ArrFundVersion version,
                                                final RelatedNodeDirection direction) {
        Assert.notNull(level, "Level musí být vyplněn");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(direction, "Směr musí být vyplněn");

        switch (direction) {
            case NODE:
                return Collections.singletonList(level);
            case PARENT:

                // pokud je to root level, nemuze mit rodice
                if (level.getNode().equals(version.getRootNode())) {
                    return Collections.emptyList();
                }

                return Collections.singletonList(levelRepository.findByNode(level.getNodeParent(), version.getLockChange()));
            case ASCENDANTS:
                return levelRepository.findAllParentsByNodeId(level.getNodeId(), version.getLockChange(), false);
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
    public List<LevelInfo> readTree(final ArrChange change, final Integer rootNodeId) {
        Set<Integer> leaves = new HashSet<>();
        leaves.add(rootNodeId);

        Set<Integer> allIds = new HashSet<>();
        while (!leaves.isEmpty()){

            List<Object[]> resultList = subTree(change, leaves);
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
     * @param change
     *            do které změny se má načítat, null načte poslední verzi
     * @param rootIds
     *            seznam id uzlů, pro které se mají načíst potomci
     * @return 4 generace potomků
     */
    private List<Object[]> subTree(final ArrChange change, final Set<Integer> rootIds) {

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

            if (change == null) {
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
            if (change != null) {
                query.setParameter("closeDate", change.getChangeId());
            }

            query.setParameter("ids", partIds);

            result.addAll(query.getResultList());
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
            return Collections.emptyList();
        }

        List<LevelInfo> result = new ArrayList<>(ids.size());

        String hql = "SELECT l.node_id, l.position, l.node_id_parent FROM arr_level l WHERE l.level_id IN (:ids)";


        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(ids);
        while (iterator.hasNext()) {
            List<Integer> partIds = iterator.next();

            //TODO Change to use projections!
            // Note: Hibernate 5.2.8 requires same number of in parameteres for prepared
            // native query, thus same query cannot be used multiple times
            Query query = entityManager.createNativeQuery(hql);
            query.setParameter("ids", partIds);

            @SuppressWarnings("unchecked")
			List<Object[]> queryResult = query.getResultList();
            for (Object[] row : queryResult) {
                result.add(new LevelInfo((Integer) row[0], (Integer) row[1], (Integer) row[2]));
            }
        }

        return result;
    }

    @Override
    public List<Integer> findNewerNodeIdsInSubtree(final int nodeId, final Timestamp lastUpdate) {
        RecursiveQueryBuilder<Integer> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(Integer.class);

        rqBuilder.addSqlPart("WITH RECURSIVE treedata(node_id, node_id_parent) AS (" +
                "SELECT t.node_id, t.node_id_parent FROM arr_level t WHERE t.node_id = :nodeId AND t.delete_change_id IS NULL " +
                "UNION ALL " +
                "SELECT t.node_id, t.node_id_parent FROM arr_level t JOIN treedata td ON td.node_id = t.node_id_parent AND t.delete_change_id IS NULL " +
                ") " +
                "SELECT DISTINCT n.node_id FROM treedata t JOIN arr_node n ON n.node_id = t.node_id " +
                "WHERE n.last_update > :lastUpdate");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeId", nodeId);
        rqBuilder.setParameter("lastUpdate", lastUpdate);
        return rqBuilder.getQuery().getResultList();
    }

    @Override
    public List<Integer> findNewerNodeIdsInParents(final int nodeId, final Timestamp lastUpdate) {
        RecursiveQueryBuilder<Integer> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(Integer.class);

        rqBuilder.addSqlPart("WITH RECURSIVE treedata(node_id, node_id_parent) AS (" +
                "SELECT t.node_id, t.node_id_parent FROM arr_level t WHERE t.node_id = :nodeId AND t.delete_change_id IS NULL " +
                "UNION ALL " +
                "SELECT t.node_id, t.node_id_parent FROM arr_level t JOIN treedata td ON td.node_id_parent = t.node_id AND t.delete_change_id IS NULL " +
                ") " +
                "SELECT DISTINCT n.node_id FROM treedata t JOIN arr_node n ON n.node_id = t.node_id " +
                "WHERE n.last_update > :lastUpdate");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeId", nodeId);
        rqBuilder.setParameter("lastUpdate", lastUpdate);
        return rqBuilder.getQuery().getResultList();
    }

    @Override
    public List<ArrLevel> findLevelsSubtree(final Integer nodeId, final int skip, final int max, final boolean ignoreRootNodes) {
        RecursiveQueryBuilder<ArrLevel> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(ArrLevel.class);

        rqBuilder.addSqlPart("WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position, path) AS (")
        .addSqlPart("SELECT t.*, '000001' AS path FROM arr_level t WHERE t.node_id = :nodeId AND t.delete_change_id IS NULL ")
        .addSqlPart("UNION ALL ")
        .addSqlPart("SELECT t.*, CONCAT(td.path, '.', RIGHT(CONCAT('000000', t.position), 6)) AS deep ")
        .addSqlPart("FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent ")
        .addSqlPart("WHERE t.delete_change_id IS NULL) ")

        .addSqlPart("SELECT t.* FROM treeData t JOIN arr_node n ON n.node_id = t.node_id ")
        .addSqlPart("WHERE t.delete_change_id IS NULL ");
        if (ignoreRootNodes) {
            rqBuilder.addSqlPart("AND n.node_id <> :nodeId ");
        }

        rqBuilder.addSqlPart("ORDER BY t.path");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeId", nodeId);
        NativeQuery<ArrLevel> query = rqBuilder.getQuery();
        query.setFirstResult(skip);
        if (max > 0) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    @Override
    public long readLevelTree(Integer nodeId, ArrChange change, boolean excludeRoot, TreeLevelConsumer treeLevelConsumer) {
        Validate.notNull(nodeId);
        Validate.isTrue(change == null, "Not implemented"); // TODO: implement condition for closed versions

        RecursiveQueryBuilder<ArrLevel> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(ArrLevel.class);

        rqBuilder.addSqlPart("WITH RECURSIVE fundTree(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position, depth) AS (")
        .addSqlPart("SELECT l.*, 0 FROM arr_level l WHERE l.node_id = :nodeId AND l.delete_change_id IS NULL ")
        .addSqlPart("UNION ALL ")
        .addSqlPart("SELECT l.*, ft.depth + 1 FROM arr_level l JOIN fundTree ft ON ft.node_id=l.node_id_parent WHERE l.delete_change_id IS NULL) ")

        .addSqlPart("SELECT * FROM fundTree ft ");
        if (excludeRoot) {
            rqBuilder.addSqlPart("WHERE node_id <> :nodeId ");
        }
        rqBuilder.addSqlPart("ORDER BY depth, node_id_parent, position");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeId", nodeId);

        NativeQuery<ArrLevel> query = rqBuilder.getQuery();

        // probably false positive due to SQLQuery -> NativeQuery migration (should be fixed in HB 6.0)
        query.addScalar("depth", StandardBasicTypes.INTEGER);
        query.setCacheMode(CacheMode.IGNORE);

        long count = 0;
        try (ScrollableResults scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY)) {
            while (scrollableResults.next()) {
                ArrLevel level = (ArrLevel) scrollableResults.get(0);
                int depth = scrollableResults.getInteger(1).intValue();
                treeLevelConsumer.accept(level, depth);
                count++;
            }
        }
        if (!excludeRoot && count == 0) {
            throw new IllegalArgumentException("Root node not found, nodeId:" + nodeId);
        }
        return count;
    }
}
