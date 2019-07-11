/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.tacr.elza.destructransferrequest.service;


import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import org.apache.log4j.Logger;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.elza.DestructTransferRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Service implementation for the DescructTransferRequest object.
 * This class is responsible for all business logic calls for the DescructTransferRequest object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
@Service
public class DescructTransferRequestServiceImpl implements DescructTransferRequestService {

    private static Logger log = Logger.getLogger(DescructTransferRequestServiceImpl.class);

    @Autowired(required = true)
    protected DestructTransferRequestDAO destructTransferRequestDAO;

    @Override
    public List<DestructTransferRequest> findAll(Context context) throws SQLException {
        return destructTransferRequestDAO.findAll(context, DestructTransferRequest.class);
    }

    @Override
    public void create(Context context, DestructTransferRequest destructTransferRequest) throws SQLException, NonUniqueMetadataException {
        // Kontrola unikátnosti pole identifier
        if (!uniqueIdetifier(context, destructTransferRequest.getRequestId(), destructTransferRequest.getIdentifier()))
        {
            throw new NonUniqueMetadataException("Hodnota v poli identifier: " + destructTransferRequest.getIdentifier()
                    + " musí být unikátní");
        }

        // Create a table row and update it with the values
        DestructTransferRequest destructTransfRequest = destructTransferRequestDAO.create(context, new DestructTransferRequest());
        destructTransfRequest.setUuid(destructTransferRequest.getUuid());
        destructTransfRequest.setDaoIdentifiers(destructTransferRequest.getDaoIdentifiers());
        destructTransfRequest.setDescription(destructTransferRequest.getDescription());
        destructTransfRequest.setIdentifier(destructTransferRequest.getIdentifier());
        destructTransfRequest.setProcessingDate(destructTransferRequest.getProcessingDate());
        destructTransfRequest.setRejectedMessage(destructTransferRequest.getRejectedMessage());
        destructTransfRequest.setRequestDate(destructTransferRequest.getRequestDate());
        destructTransfRequest.setRequestType(destructTransferRequest.getRequestType());
        destructTransfRequest.setStatus(destructTransferRequest.getStatus());
        destructTransfRequest.setSystemIdentifier(destructTransferRequest.getSystemIdentifier());
        destructTransfRequest.setTargetFund(destructTransferRequest.getTargetFund());
        destructTransfRequest.setUserName(destructTransferRequest.getUserName());

        destructTransferRequestDAO.save(context, destructTransfRequest);
        log.info(LogManager.getHeader(context, "create_destruct_transfer_request",
                "destruct_transfer_request_id=" + destructTransfRequest.getRequestId()));

    }

    @Override
    public void update(Context context, DestructTransferRequest destructTransferRequest) throws SQLException, NonUniqueMetadataException {
        // Kontrola unikátnosti pole identifier
        if (!uniqueIdetifier(context, destructTransferRequest.getRequestId(), destructTransferRequest.getIdentifier()))
        {
            throw new NonUniqueMetadataException("Hodnota v poli identifier: " + destructTransferRequest.getIdentifier()
                    + " musí být unikátní");
        }

        destructTransferRequestDAO.save(context, destructTransferRequest);
        log.debug(LogManager.getHeader(context, "update_destruct_transfer_request",
                "destruct_transfer_request_id=" + destructTransferRequest.getRequestId() + "Uuid="
                        + destructTransferRequest.getUuid() + "identifier=" + destructTransferRequest.getIdentifier()));
    }


    @Override
    public void delete(Context context, DestructTransferRequest destructTransferRequest) throws SQLException {

        log.info(LogManager.getHeader(context, "delete_metadata_schema",
                "destruct_transfer_request_id=" + destructTransferRequest.getIdentifier()));

        destructTransferRequestDAO.delete(context, destructTransferRequest);
    }

    @Override
    public DestructTransferRequest findById(Context context, int id) throws SQLException {
        return destructTransferRequestDAO.findByID(context, DestructTransferRequest.class, id);
    }

    @Override
    public DestructTransferRequest findByIdentifier(Context context, String identifier) throws SQLException {
        if (identifier == null)
        {
            return null;
        }
        return destructTransferRequestDAO.findByIdentifier(context, identifier);
    }

    @Override
    public List<DestructTransferRequest> findByTypeAndStatus(Context context, DestructTransferRequest.Status status, DestructTransferRequest.RequestType requestType) throws SQLException {
        return destructTransferRequestDAO.findByTypeAndStatus(context, status, requestType);
    }

    @Override
    public boolean uniqueIdetifier(Context context, int destructTransferRequestId, String identifier) throws SQLException
    {
        return destructTransferRequestDAO.uniqueIdetifier(context, destructTransferRequestId, identifier);
    }

}
