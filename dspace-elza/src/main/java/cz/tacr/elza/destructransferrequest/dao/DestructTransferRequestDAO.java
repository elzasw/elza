package cz.tacr.elza.destructransferrequest.dao;

import cz.tacr.elza.destructransferrequest.service.DestructTransferRequest;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface rozhraní pro objekt DestructTransferRequest.
 *
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 21.06.2019.
 */
public interface DestructTransferRequestDAO extends GenericDAO<DestructTransferRequest> {

    public DestructTransferRequest findByIdentifier(Context context, String identifier) throws SQLException;

    public DestructTransferRequest findByRequestId(Context context, Integer requestId) throws SQLException;

    public List<DestructTransferRequest> findByTypeAndStatus(Context context, DestructTransferRequest.Status status,
                                                             DestructTransferRequest.RequestType requestType) throws SQLException;

    public void deleteByRequestId(Context context, Integer requestId) throws SQLException;

    public Integer insertDestrucTransferRequest(Context context, DestructTransferRequest destructTransferRequest) throws SQLException;

    public void updateDestrucTransferRequest(Context context, DestructTransferRequest destructTransferRequest) throws SQLException;

}
