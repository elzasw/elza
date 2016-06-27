package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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

        Predicate equal = builder.equal(file.get(ArrFile.FUND), fund);
        Predicate equalCount = builder.equal(fileCount.get(ArrFile.FUND), fund);

        query.where(predicate != null ? builder.and(equal,predicate):builder.and(equal));
        queryCount.where(predicateCount != null ? builder.and(equalCount,predicateCount):builder.and(equal));

        return getFilteredResult(query, queryCount, file, fileCount, firstResult, maxResults);
    }
}
