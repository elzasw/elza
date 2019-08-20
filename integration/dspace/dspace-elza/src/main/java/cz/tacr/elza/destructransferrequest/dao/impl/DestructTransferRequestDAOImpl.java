package cz.tacr.elza.destructransferrequest.dao.impl;

import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.elza.DestructTransferRequest;
import org.hibernate.Query;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

/**
 * Implementace rozhraní pro objekt DestructTransferRequest.
 *
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 21.06.2019.
 */
@Component
public class DestructTransferRequestDAOImpl extends AbstractHibernateDAO<DestructTransferRequest> implements DestructTransferRequestDAO {
    protected DestructTransferRequestDAOImpl()
    {
        super();
    }

    @Override
    public DestructTransferRequest findByIdentifier(Context context, String identifier) throws SQLException {
        String tableName = DestructTransferRequest.class.getName();
        String sqlQuery = "SELECT dr FROM " + tableName + " dr WHERE dr.identifier = :identifier";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("identifier", identifier);

        query.setCacheable(true);
        return singleResult(query);
    }

    @Override
    public List<DestructTransferRequest> findByTypeAndStatus(Context context, DestructTransferRequest.Status status, DestructTransferRequest.RequestType requestType) throws SQLException {
        String tableName = DestructTransferRequest.class.getName();
        String sqlQuery = "SELECT dr FROM " + tableName + " dr WHERE dr.requestType = :requestType AND" +
                " dr.status = :status";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("requestType", requestType);
        query.setParameter("status", status);

        return list(query);
    }

    @Override
    public boolean uniqueIdetifier(Context context, Integer requestId, String identifier) throws SQLException {
        String tableName = DestructTransferRequest.class.getName();
        String sqlQuery = "SELECT dr FROM " + tableName + " dr WHERE dr.identifier = :identifier AND dr.requestId <> :requestId";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("identifier", identifier);
        query.setParameter("requestId", requestId);

        query.setCacheable(true);
        return singleResult(query) == null;
    }

}
