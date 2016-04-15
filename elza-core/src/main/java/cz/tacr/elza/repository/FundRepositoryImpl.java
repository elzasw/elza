package cz.tacr.elza.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.vo.ArrFundOpenVersion;


/**
 * Rozšiřující rozhraní pro archivní fondy.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
@Component
public class FundRepositoryImpl implements FundRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrFundOpenVersion> findByFulltext(final String fulltext, final int max) {


        String hql = "SELECT NEW cz.tacr.elza.domain.vo.ArrFundOpenVersion(f, max(v)) FROM arr_fund f JOIN f.versions v"
                + createFulltextWhereClause(fulltext);
        hql += " GROUP BY f ORDER BY f.name";

        Query query = entityManager.createQuery(hql);
        if (StringUtils.isNotBlank(fulltext)) {
            String text = "%" + fulltext + "%";
            query.setParameter("text", text.toLowerCase());
        }
        query.setMaxResults(max);

        return query.getResultList();
    }

    @Override
    public Integer findCountByFulltext(final String fulltext) {
        String hql = "SELECT count(f) FROM arr_fund f" + createFulltextWhereClause(fulltext);

        Query query = entityManager.createQuery(hql);

        if (StringUtils.isNotBlank(fulltext)) {
            String text = "%" + fulltext + "%";
            query.setParameter("text", text.toLowerCase());
        }

        return Math.toIntExact((long) query.getSingleResult());

    }

    /**
     * Vytvoří WHERE podmínky pro dotazy vyhledávání podle fulltextu.
     *
     * @param fulltext fulltext
     * @return WHERE podmínka (pouze pokud je nastaven fulltext)
     */
    private String createFulltextWhereClause(final String fulltext) {
        String result = "";
        if (StringUtils.isNotBlank(fulltext)) {
            result += " WHERE LOWER(f.name) LIKE :text OR LOWER(f.internalCode) LIKE :text";
        }

        return result;
    }


}
