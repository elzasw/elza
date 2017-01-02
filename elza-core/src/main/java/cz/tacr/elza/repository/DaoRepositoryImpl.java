package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.16
 */
public class DaoRepositoryImpl implements DaoRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDao> findByFundAndNodePaginating(ArrFundVersion fundVersion, @Nullable ArrNode node, Integer index, Integer maxResults) {
        String hql = "SELECT d FROM arr_dao d "
                + "join d.daoPackage p "
                + "join p.fund f "
                + "WHERE f.fundId = :fundId ";

        if (node != null) {
            hql += " and exists (select l from arr_dao_link l  join l.node n "
                    + " where n.nodeId = :nodeId and l.dao.daoId = d.daoId and l.deleteChange IS NULL) ";
        } else {
            hql += " and not exists (select l from arr_dao_link l "
                    + " where l.dao.daoId = d.daoId and l.deleteChange IS NULL) ";
        }

        hql += " order by d.daoId desc ";

        Query query = entityManager.createQuery(hql);
        query.setMaxResults(maxResults);

        query.setParameter("fundId", fundVersion.getFund().getFundId());
        if (node != null) {
            query.setParameter("nodeId", node.getNodeId());
        }

        //noinspection unchecked
        return query.getResultList();
    }

}

