package cz.tacr.elza.destructransferrequest.dao;

import cz.tacr.elza.destructransferrequest.DestructTransferRequest;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface rozhraní pro objekt DestructTransferRequest.
 *
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 21.06.2019.
 */
public interface DestructTransferRequestDAO {

    public List<DestructTransferRequest> findAll(Context context) throws SQLException;

    public List<DestructTransferRequest> findByIdentifier(Context context, String identifier) throws SQLException;

    public void insert(Context context, DestructTransferRequest destructTransferRequest) throws SQLException;

    public void update(Context context, DestructTransferRequest destructTransferRequest) throws SQLException;

    public void delete(Context context, Integer requestId) throws SQLException;
}
