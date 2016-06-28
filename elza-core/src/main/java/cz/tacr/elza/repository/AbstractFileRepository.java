package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DmsFile;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

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

    public Predicate prepareFileSearchPredicate(String searchText, Root file) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        if (StringUtils.isNotBlank(searchText)) {
            final String searchValue = "%" + searchText.toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(file.get(T.NAME)), searchValue),
                    builder.like(builder.lower(file.get(T.FILE_NAME)), searchValue)
            );
        }
        return null;
    }

    public FilteredResult<T> getFilteredResult(CriteriaQuery query, CriteriaQuery<Long> queryCount, Root file, Root fileCount, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        query.select(file);
        queryCount.select(builder.countDistinct(fileCount));

        List<T> list = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
        long count = entityManager.createQuery(queryCount).getSingleResult();

        return new FilteredResult<>(firstResult, maxResults, count, list);
    }


    Class getGenericClass() {
        try {
            // Načtení názvu entity z generic parametru rozhraní repository
            Class<?> aClass = Class.forName(this.getClass().getTypeName());
            //ParameterizedType pt = (ParameterizedType) aClass.getGenericInterfaces()[0];
            return (Class) Class.forName(aClass.getSuperclass().getTypeParameters()[0].getClass().getTypeName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Nelze automaticky zjistit název datového objektu, je nutné překrýt metodu getGenericClass!", e);
        }
    }
}
