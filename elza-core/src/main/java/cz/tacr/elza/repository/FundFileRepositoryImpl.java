package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

/**
 * Implementace repository pro ArrFile - Custom
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.16
 */
@Component
public class FundFileRepositoryImpl extends AbstractFileRepository<ArrFile> implements FundFileRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public FilteredResult<ArrFile> findByTextAndFund(String search, ArrFund fund, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrFile> query = builder.createQuery(ArrFile.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<ArrFile> file = query.from(ArrFile.class);
        Root<ArrFile> fileCount = queryCount.from(ArrFile.class);

        Predicate predicate = prepareFileSearchPredicate(search, file);
        Predicate predicateCount = prepareFileSearchPredicate(search, fileCount);

        Predicate equal = builder.equal(file.get(ArrFile.FUND), fund);
        Predicate equalCount = builder.equal(fileCount.get(ArrFile.FUND), fund);

        query.where(predicate != null ? builder.and(equal,predicate):builder.and(equal));
        queryCount.where(predicateCount != null ? builder.and(equalCount,predicateCount):builder.and(equal));

        return getFilteredResult(query, queryCount, file, fileCount, firstResult, maxResults);
    }

    @Override
    public List<ArrFile> findFilesBySubtreeNodeIds(final Collection<Integer> nodeIds, final boolean ignoreRootNode) {
        Assert.notEmpty(nodeIds, "Identifikátor JP musí být vyplněn");

        String sql_nodes = "WITH " + levelRepository.getRecursivePart() + " treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS (SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) UNION ALL SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) " +
                "SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL";

        if (ignoreRootNode) {
            sql_nodes += " AND n.node_id NOT IN (:nodeIds)";
        }

        String sql = "SELECT f.*, af.* FROM dms_file f JOIN arr_file af ON f.file_id = af.file_id WHERE f.file_id IN" +
                " (" +
                "  SELECT dfr.file_id FROM arr_data_file_ref dfr JOIN dms_file df ON df.file_id = dfr.file_id WHERE dfr.data_id IN (SELECT d.data_id FROM arr_data d JOIN arr_item i ON d.item_id = i.item_id JOIN arr_desc_item di ON di.item_id = i.item_id WHERE i.delete_change_id IS NULL AND d.data_type_id = 13 AND di.node_id IN (" + sql_nodes + "))" +
                " )";

        Query query = entityManager.createNativeQuery(sql, ArrFile.class);
        query.setParameter("nodeIds", nodeIds);

        return query.getResultList();
    }
}
