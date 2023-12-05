package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DmsFile;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Implementace repository pro DmsFile - Custom
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.16
 */
@Component
public class FileRepositoryImpl extends AbstractFileRepository<DmsFile> implements FileRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public FilteredResult<DmsFile> findByText(String search, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DmsFile> query = builder.createQuery(DmsFile.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<DmsFile> file = query.from(DmsFile.class);
        Root<DmsFile> fileCount = queryCount.from(DmsFile.class);

        Predicate predicate = prepareFileSearchPredicate(search, file);
        Predicate predicateCount = prepareFileSearchPredicate(search, fileCount);

        if (predicate != null) {
            query.where(builder.and(predicate));
            queryCount.where(builder.and(predicateCount));
        }

        return getFilteredResult(query, queryCount, file, fileCount, firstResult, maxResults);
    }
}
