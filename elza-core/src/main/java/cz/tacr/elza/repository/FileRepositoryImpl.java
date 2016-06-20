package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DmsFile;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Implementace repository pro DmsFile - Custom
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.16
 */
@Component
public class FileRepositoryImpl implements FileRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DmsFile> findByText(final @Nullable String searchText, final Integer firstResult, final Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DmsFile> query = builder.createQuery(DmsFile.class);
        Root<DmsFile> file = query.from(DmsFile.class);

        if (StringUtils.isNotBlank(searchText)) {
            final String searchValue = "%" + searchText.toLowerCase() + "%";
            query.where(builder.or(
                    builder.like(builder.lower(file.get(DmsFile.NAME)), searchValue),
                    builder.like(builder.lower(file.get(DmsFile.FILE_NAME)), searchValue)
            ));
        }
        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findByTextCount(final String searchText) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<DmsFile> file = query.from(DmsFile.class);
        query.select(builder.countDistinct(file));

        if (StringUtils.isNotBlank(searchText)) {
            final String searchValue = "%" + searchText.toLowerCase() + "%";
            query.where(builder.or(
                    builder.like(builder.lower(file.get(DmsFile.NAME)), searchValue),
                    builder.like(builder.lower(file.get(DmsFile.FILE_NAME)), searchValue)
            ));
        }
        return entityManager.createQuery(query).getSingleResult();
    }
}
