package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Implementace repository pro ArrOutputFile - Custom
 *
 */
@Component
public class OutputFileRepositoryImpl extends AbstractFileRepository<ArrOutputFile> implements OutputFileRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FilteredResult<ArrOutputFile> findByTextAndResult(String search, ArrOutputResult result, Integer firstResult, Integer maxResults) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrOutputFile> query = builder.createQuery(ArrOutputFile.class);
        CriteriaQuery<Long> queryCount = builder.createQuery(Long.class);

        Root<ArrOutputFile> file = query.from(ArrOutputFile.class);        
        Root<ArrOutputFile> fileCount = queryCount.from(ArrOutputFile.class);

        Predicate predicate = prepareFileSearchPredicate(search, file);
        Predicate predicateCount = prepareFileSearchPredicate(search, fileCount);

        Predicate equal = builder.equal(file.get(ArrOutputFile.OUTPUT_RESULT), result);
        Predicate equalCount = builder.equal(fileCount.get(ArrOutputFile.OUTPUT_RESULT), result);

        query.where(predicate != null ? builder.and(equal,predicate):builder.and(equal));
        queryCount.where(predicateCount != null ? builder.and(equalCount,predicateCount):builder.and(equal));

        return getFilteredResult(query, queryCount, file, fileCount, firstResult, maxResults);
    }
}
