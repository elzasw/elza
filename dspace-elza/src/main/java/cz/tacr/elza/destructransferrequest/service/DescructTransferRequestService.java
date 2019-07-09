/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.tacr.elza.destructransferrequest.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the MetadataSchema object.
 * The implementation of this class is responsible for all business logic calls for the MetadataSchema object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface DescructTransferRequestService {

    /**
     * Creates a new metadata schema in the database, using the name and namespace.
     *
     * @param context DSpace context object
     * @param destructTransferRequest
     * @return new MetadataSchema
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws NonUniqueMetadataException
     */
    public DestructTransferRequest create(Context context, DestructTransferRequest destructTransferRequest) throws SQLException, NonUniqueMetadataException;

    /**
     * Update the destruct transfer request in the database.
     * @param context DSpace context
     * @param destructTransferRequest destruct transfer request
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws NonUniqueMetadataException
     */
    public void update(Context context, DestructTransferRequest destructTransferRequest) throws SQLException, NonUniqueMetadataException;

    /**
     * Delete the destruct transfer request.
     * @param context DSpace context
     * @param destructTransferRequest destruct transfer request
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void delete(Context context, DestructTransferRequest destructTransferRequest) throws SQLException;

    /**
     * Return all destruct transfer request.
     * @param context DSpace context
     * @return array of metadata schemas
     * @throws SQLException if database error
     */
    public List<DestructTransferRequest> findAll(Context context) throws SQLException;

    /**
     * Get the destruct transfer request corresponding with this numeric ID.
     * The ID is a database key internal to DSpace.
     * @param context
     *            context, in case we need to read it in from DB
     * @param id
     *            the schema ID
     * @return the metadata schema object
     * @throws SQLException if database error
     */
    public DestructTransferRequest findById(Context context, int id) throws SQLException;

    /**
     * Get the destruct transfer request corresponding with this short name.
     * @param context DSpace context
     * @param identifier identifier destruct transfer request
     * @return destruct transfer request object or null if none found.
     * @throws SQLException if database error
     */
    public DestructTransferRequest findByIdentifier(Context context, String identifier) throws SQLException;

    /**
     * Get the destruct transfer request corresponding with this short name.
     * @param context DSpace context
     * @param status status destruct transfer request
     * @param requestType request type destruct transfer request
     * @return destruct transfer request object or null if none found.
     * @throws SQLException if database error
     */
    public List<DestructTransferRequest> findByTypeAndStatus(Context context, DestructTransferRequest.Status status,
                                                             DestructTransferRequest.RequestType requestType) throws SQLException;

    /**
     * Return true if and only if the passed name is unique.
     *
     * @param context DSpace context
     * @param destructTransferRequestId metadata schema id
     * @param identifier  short name of schema
     * @return true of false
     * @throws SQLException if database error
     */
    public boolean uniqueIdetifier(Context context, int destructTransferRequestId, String identifier) throws SQLException;

}
