package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.vo.ArrFundOpenVersion;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    @Autowired
    private FundRepository fundRepository;

    @Override
    public List<ArrFundOpenVersion> findByFulltext(final String fulltext, final int max, final boolean readAllFunds, final UsrUser user) {

        String hql = "SELECT f.fundId, max(v) FROM arr_fund f JOIN f.versions v "
                + createFulltextWhereClause(fulltext, readAllFunds, user);
        hql += " GROUP BY f.fundId, f.name ORDER BY f.name";

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
        for (Object[] array : arrayList) {
            final int fundId = (int) array[0];
            final ArrFund fund = fundRepository.getOneCheckExist(fundId);
            result.add(new ArrFundOpenVersion(fund, (ArrFundVersion) array[1]));
        }
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

    @Override
    public FilteredResult<ArrFund> findFundsWithPermissions(final String search, final Integer firstResult, final Integer maxResults, final Integer userId) {
        TypedQuery<ArrFund> data = buildFundFindQuery(true, search, firstResult, maxResults, userId);
        TypedQuery<Number> count = buildFundFindQuery(false, search, firstResult, maxResults, userId);
        return new FilteredResult<>(firstResult, maxResults, count.getSingleResult().longValue(), data.getResultList());
    }

    private <T> TypedQuery<T> buildFundFindQuery(final boolean dataQuery,
                                                 final String search,
                                                 final Integer firstResult,
                                                 final Integer maxResults,
                                                 final Integer userId) {
        StringBuilder conds = new StringBuilder();

        StringBuilder query = new StringBuilder();
        query.append("FROM usr_permission fu" +
                " JOIN arr_fund f on fu.fund = f"
        );

        // Podmínky hledání
        Map<String, Object> parameters = new HashMap<>();
        if (!StringUtils.isEmpty(search)) {
            conds.append(" LOWER(f.name) LIKE :search OR LOWER(f.internalCode) LIKE :search");
            parameters.put("search", "%" + search.toLowerCase() + "%");
        }

        if (userId != null) {
            if (conds.length() != 0) {
                conds.append(" AND ");
            }
            conds.append(" fu.userId IN (SELECT p.userControlId FROM usr_permission p WHERE p.userId = :userId OR p.groupId IN (SELECT gu.groupId FROM usr_group_user gu WHERE gu.userId = :userId))");
            parameters.put("userId", userId);
        }

        // Připojení podmínek ke query
        if (conds.length() > 0) {
            query.append(" WHERE " + conds.toString());
        }

        TypedQuery q;
        if (dataQuery) {
            String dataQueryStr = "select distinct f " + query.toString() + " order by f.name";
            q = entityManager.createQuery(dataQueryStr, ArrFund.class);
        } else {
            String countQueryStr = "select count(distinct f) " + query.toString();
            q = entityManager.createQuery(countQueryStr, Number.class);
        }

        parameters.forEach(q::setParameter);

        if (dataQuery) {
            q.setFirstResult(firstResult);
            if (maxResults >= 0) {
                q.setMaxResults(maxResults);
            }
        }

        return q;
    }

    /**
     * Vytvoří WHERE podmínky pro dotazy vyhledávání podle fulltextu.
     *
     * @param fulltext     fulltext
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
