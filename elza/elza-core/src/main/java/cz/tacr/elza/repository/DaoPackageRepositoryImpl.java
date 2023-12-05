package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFundVersion;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 14.12.16
 */
public class DaoPackageRepositoryImpl implements DaoPackageRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDaoPackage> findDaoPackages(ArrFundVersion fundVersion, String search, Boolean unassigned, Integer maxResults) {
        String hql = "select dp from arr_dao_package dp "
                + " left join fetch dp.daoBatchInfo dbi "
                + " join dp.fund f "
                + " where f.fundId = :fundId ";

        if (StringUtils.isNotBlank(search)) {
            hql += " and ( dp.code like :search or dbi.code like :search or dbi.label like :search "
                    + " or exists (select d2 from arr_dao d2 where d2.daoPackage = dp and (d2.code like :search or d2.label like :search)))";
        }

        if (unassigned) {
            hql += "and exists (select d from arr_dao d\n"
                    + "          WHERE d.daoPackage = dp "
                    + "            AND d.valid = true "
                    + "            AND NOT exists (SELECT dl FROM arr_dao_link dl "
                    + "                             WHERE dl.dao = d "
                    + "                               AND (dl.deleteChange IS NULL)))";
        }

        hql += " order by dp.code ASC ";

        Query query = entityManager.createQuery(hql);
        query.setMaxResults(maxResults);

        query.setParameter("fundId", fundVersion.getFund().getFundId());
        if (StringUtils.isNotBlank(search)) {
            query.setParameter("search", "%" + search + "%");
        }

        //noinspection unchecked
        return query.getResultList();
    }

}
