package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import cz.tacr.elza.domain.UsrUser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
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
    public List<ArrFundOpenVersion> findByFulltext(final String fulltext, final int max, final boolean readAllFunds, final UsrUser user) {

        String hql = "SELECT f, max(v) FROM arr_fund f JOIN f.versions v "
                + createFulltextWhereClause(fulltext, readAllFunds, user);
        hql += " GROUP BY f ORDER BY f.name";

        Query query = entityManager.createQuery(hql);
        if (StringUtils.isNotBlank(fulltext)) {
            String text = "%" + fulltext + "%";
            query.setParameter("text", text.toLowerCase());
        }

        if (!readAllFunds && user != null) {
            query.setParameter("user", user);
        }

        query.setMaxResults(max);
        List<Object[]> arrayList = query.getResultList();
        List<ArrFundOpenVersion> result = new ArrayList<>(arrayList.size());
        arrayList.forEach(array ->
                        result.add(new ArrFundOpenVersion((ArrFund) array[0], (ArrFundVersion) array[1]))
        );

        return result;
    }

    @Override
    public Integer findCountByFulltext(final String fulltext, final boolean readAllFunds, final UsrUser user) {
        String hql = "SELECT count(f) FROM arr_fund f " + createFulltextWhereClause(fulltext, readAllFunds, user);

        Query query = entityManager.createQuery(hql);

        if (StringUtils.isNotBlank(fulltext)) {
            String text = "%" + fulltext + "%";
            query.setParameter("text", text.toLowerCase());
        }

        if (!readAllFunds && user != null) {
            query.setParameter("user", user);
        }

        return Math.toIntExact((long) query.getSingleResult());

    }

    /**
     * Vytvoří WHERE podmínky pro dotazy vyhledávání podle fulltextu.
     *
     * @param fulltext fulltext
     * @param readAllFunds
     * @param user
     * @return WHERE podmínka (pouze pokud je nastaven fulltext)
     */
    private String createFulltextWhereClause(final String fulltext, final boolean readAllFunds, final UsrUser user) {
        String result = "";
        if (StringUtils.isNotBlank(fulltext)) {
            result += " WHERE LOWER(f.name) LIKE :text OR LOWER(f.internalCode) LIKE :text";
        }

        if (!readAllFunds && user != null) {
            if (StringUtils.isBlank(result)) {
                result += " WHERE ";
            } else {
                result += " AND ";
            }
            result += " f IN (SELECT pv.fund FROM usr_permission_view pv WHERE user = :user)";
        }

        return result;
    }


}
