package cz.tacr.elza.destructransferrequest.dao;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.elza.DestructTransferRequest;

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

    public List<DestructTransferRequest> findByTypeAndStatus(Context context, DestructTransferRequest.Status status,
                                                             DestructTransferRequest.RequestType requestType) throws SQLException;

    public boolean uniqueIdetifier(Context context, Integer requestId, String identifier) throws SQLException;
}
