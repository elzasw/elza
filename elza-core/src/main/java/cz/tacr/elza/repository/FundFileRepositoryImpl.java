package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;

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

    @Override
    public FilteredResult<ArrFile> findByTextAndFund(String search, ArrFund fund, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrFile> query = builder.createQuery(ArrFile.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<ArrFile> file = query.from(ArrFile.class);
        Root<ArrFile> fileCount = queryCount.from(ArrFile.class);

        Predicate predicate = prepareFileSearchPredicate(search, file);
        Predicate predicateCount = prepareFileSearchPredicate(search, fileCount);

        Predicate equal = builder.equal(file.get(ArrFile.FIELD_FUND), fund);
        Predicate equalCount = builder.equal(fileCount.get(ArrFile.FIELD_FUND), fund);

        query.where(predicate != null ? builder.and(equal,predicate):builder.and(equal));
        queryCount.where(predicateCount != null ? builder.and(equalCount,predicateCount):builder.and(equal));

        return getFilteredResult(query, queryCount, file, fileCount, firstResult, maxResults);
    }

    @Override
    public List<ArrFile> findFilesBySubtreeNodeIds(final Collection<Integer> nodeIds, final boolean ignoreRootNode) {
        Validate.notEmpty(nodeIds);

        RecursiveQueryBuilder<ArrFile> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(ArrFile.class);

        rqBuilder.addSqlPart("SELECT f.*, af.* FROM dms_file f JOIN arr_file af ON f.file_id = af.file_id WHERE f.file_id IN (")

        .addSqlPart("SELECT dfr.file_id FROM arr_data_file_ref dfr JOIN dms_file df ON df.file_id = dfr.file_id WHERE dfr.data_id IN (")

        .addSqlPart("SELECT d.data_id FROM arr_item i JOIN arr_data d ON d.data_id = i.data_id ")
        .addSqlPart("JOIN arr_desc_item di ON di.item_id = i.item_id WHERE i.delete_change_id IS NULL AND d.data_type_id = 13 AND di.node_id IN (")

        .addSqlPart("WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS (")
        .addSqlPart("SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) ")
        .addSqlPart("UNION ALL ")
        .addSqlPart("SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent)")

        .addSqlPart("SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL");
        if (ignoreRootNode) {
            rqBuilder.addSqlPart(" AND n.node_id NOT IN (:nodeIds)");
        }

        rqBuilder.addSqlPart(")))");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeIds", nodeIds);
        return rqBuilder.getQuery().getResultList();
    }
}
