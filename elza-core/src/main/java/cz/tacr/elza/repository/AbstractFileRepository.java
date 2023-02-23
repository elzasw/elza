package cz.tacr.elza.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.DmsFile;

/**
 * Implementace repository pro DmsFile - Custom
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.16
 */
@Component
abstract public class AbstractFileRepository<T extends DmsFile> {

    @PersistenceContext
    private EntityManager entityManager;

    public Predicate prepareFileSearchPredicate(final String searchText, final Root<T> file) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        if (StringUtils.isNotBlank(searchText)) {
            final String searchValue = "%" + searchText.toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(file.get(T.FIELD_NAME)), searchValue),
                    builder.like(builder.lower(file.get(T.FIELD_FILE_NAME)), searchValue)
            );
        }
        return null;
    }

	public FilteredResult<T> getFilteredResult(final CriteriaQuery<T> query, final CriteriaQuery<Long> queryCount,
	        final Root<T> file,
            final Root<T> fileCount, final Integer firstResult, final Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        query.select(file);
        queryCount.select(builder.countDistinct(fileCount));

        List<T> list = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
		int count = entityManager.createQuery(queryCount).getSingleResult().intValue();

        return new FilteredResult<T>(firstResult, maxResults, count, list);
    }
}
