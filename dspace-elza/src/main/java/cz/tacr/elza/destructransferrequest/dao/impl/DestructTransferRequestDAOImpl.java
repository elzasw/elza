package cz.tacr.elza.destructransferrequest.dao.impl;

import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import cz.tacr.elza.destructransferrequest.service.DestructTransferRequest;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Query;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

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
        String sqlQuery = "FROM DestructionRequest WHERE identifier = :uuid";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("identifier", identifier);

        query.setCacheable(true);
        return singleResult(query);
    }

    @Override
    public DestructTransferRequest findByRequestId(Context context, Integer requestId) throws SQLException {
        String sqlQuery = "FROM DestructionRequest WHERE request_id = :requestId";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("requestId", requestId);

        query.setCacheable(true);
        return singleResult(query);
    }

    @Override
    public List<DestructTransferRequest> findByTypeAndStatus(Context context, DestructTransferRequest.Status status, DestructTransferRequest.RequestType requestType) throws SQLException {
        String sqlQuery = "FROM DestructionRequest WHERE request_type = :requestType AND status = :status";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("requestType", requestType);
        query.setParameter("status", status);

        return list(query);
    }

    @Override
    public void deleteByRequestId(Context context, Integer requestId) throws SQLException {
        String sqlQuery = "Delete from DestructionRequest where request_id = :requestId";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("requestId", requestId);
        query.executeUpdate();
    }

    @Override
    public Integer insertDestrucTransferRequest(Context context, DestructTransferRequest destructTransferRequest) throws SQLException {
        String sqlQuery = "INSERT INTO DestructionRequest (uuid, request_type, dao_identifiers, identifier, system_identifier, " +
                "description, user_name, target_fund, status, request_date, processing_date, rejected_message) " +
                "VALUES (:uuid, :requesttype, :daoIdentifiers, :identifier, :systemIdentifier, :description, :userName, :targetFund, " +
                ":status, :requestDate, :processingDate, :rejectedMessage)";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("uuid", UUID.randomUUID().toString());
        query.setParameter("requesttype", destructTransferRequest.getRequestType());
        query.setParameter("daoIdentifiers", destructTransferRequest.getDaoIdentifiers());
        query.setParameter("identifier", destructTransferRequest.getIdentifier());
        query.setParameter("systemIdentifier", destructTransferRequest.getSystemIdentifier());
        query.setParameter("description", destructTransferRequest.getDescription());
        query.setParameter("userName", destructTransferRequest.getUserName());
        query.setParameter("targetFund", destructTransferRequest.getTargetFund());
        query.setParameter("status", destructTransferRequest.getStatus());
        query.setParameter("requestDate", destructTransferRequest.getRequestDate());
        query.setParameter("processingDate", destructTransferRequest.getProcessingDate());
        query.setParameter("rejectedMessage", destructTransferRequest.getRejectedMessage());

        return query.executeUpdate();
    }

    @Override
    public void updateDestrucTransferRequest(Context context, DestructTransferRequest destructTransferRequest) throws SQLException {
        String sqlQuery = "UPDATE DestructionRequest SET status = :status, processing_date = :processingDate, " +
                "error_message = :errorMessage WHERE request_id = :requestId";
        Query query = createQuery(context, sqlQuery);
        query.setParameter("requestId", destructTransferRequest.getRequestId());
        query.setParameter("status", destructTransferRequest.getStatus());
        query.setParameter("processingDate", destructTransferRequest.getProcessingDate());
        query.setParameter("errorMessage", destructTransferRequest.getRejectedMessage());

        query.executeUpdate();
    }

}
