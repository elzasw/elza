package cz.tacr.elza.repository;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.16
 */
public class DaoRepositoryImpl implements DaoRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDao> findByFundAndNodePaginating(ArrFundVersion fundVersion, @Nullable ArrNode node, Integer index, Integer maxResults) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");

        String hql = "SELECT d FROM arr_dao d "
                + "join d.daoPackage p "
                + "join p.fund f "
                + "WHERE f.fundId = :fundId and d.valid = true";

        if (node != null) {
            hql += " and exists (select l from arr_dao_link l  join l.node n "
                    + " where n.nodeId = :nodeId and l.dao.daoId = d.daoId and l.deleteChange IS NULL) ";
        } else {
            hql += " and not exists (select l from arr_dao_link l "
                    + " where l.dao.daoId = d.daoId and l.deleteChange IS NULL) ";
        }

        hql += " order by d.label ASC, d.code ASC ";

        Query query = entityManager.createQuery(hql);
        query.setFirstResult(index);
        query.setMaxResults(maxResults);

        query.setParameter("fundId", fundVersion.getFund().getFundId());
        if (node != null) {
            query.setParameter("nodeId", node.getNodeId());
        }

        //noinspection unchecked
        return query.getResultList();
    }

    @Override
    public List<ArrDao> findByFundAndPackagePaginating(Integer fundId, ArrDaoPackage daoPackage,
                                                       Integer index,
                                                       Integer maxResults, boolean unassigned) {
        Validate.notNull(fundId, "AS musí být vyplněn");
        Validate.notNull(daoPackage, "DAO obal musí být vyplněn");
        String hql = "SELECT d FROM arr_dao d "
                + "  join d.daoPackage p "
                + "  join p.fund f "
                + " WHERE f.fundId = :fundId and d.valid = true "
                + "   and p = :daoPackage ";


        if (unassigned) {
            hql += " AND NOT exists (SELECT dl FROM arr_dao_link dl "
                    + "               WHERE dl.dao = d "
                    + "                 AND (dl.deleteChange IS NULL))";
        }

        hql += " order by d.label ASC, d.code ASC ";

        Query query = entityManager.createQuery(hql);
        if (index != null && maxResults != null) {
            query.setFirstResult(index);
            query.setMaxResults(maxResults);
        }

        query.setParameter("fundId", fundId);
        query.setParameter("daoPackage", daoPackage);

        //noinspection unchecked
        return query.getResultList();
    }

}

