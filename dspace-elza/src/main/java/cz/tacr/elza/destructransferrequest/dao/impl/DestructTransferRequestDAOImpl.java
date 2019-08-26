package cz.tacr.elza.destructransferrequest.dao.impl;

import cz.tacr.elza.destructransferrequest.DestructTransferRequest;
import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.hibernate.Query;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private static Logger log = Logger.getLogger(DestructTransferRequestDAOImpl.class);

    private final String TABLENAME = "destruc_transfer_request";

    @Override
    public List<DestructTransferRequest> findAll(Context context) throws SQLException {
        String sqlQuery = "SELECT request_id, type, identifier, status, rejected_message, request_date FROM " + TABLENAME;
        Query query = getHibernateSession(context).createSQLQuery(sqlQuery);

        List<DestructTransferRequest> result = new ArrayList<>();
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            result.add(setResultFromRow(row));
        }

        log.debug(LogManager.getHeader(context, "findAll_destruct_transfer_request",
                "query=" + sqlQuery));

        return result;
    }

    @Override
    public List<DestructTransferRequest> findByIdentifier(Context context, String identifier) throws SQLException {
        String sqlQuery = "SELECT request_id, type, identifier, status, rejected_message, request_date FROM " + TABLENAME +
                " WHERE identifier=:identifier";
        Query query = getHibernateSession(context).createSQLQuery(sqlQuery);
        query.setString("identifier", identifier);

        List<DestructTransferRequest> result = new ArrayList<>();
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            result.add(setResultFromRow(row));
        }

        log.debug(LogManager.getHeader(context, "findAll_destruct_transfer_request",
                "query=" + sqlQuery));

        return result;
    }

    @Override
    public void insert(Context context, DestructTransferRequest destructTransferRequest) throws SQLException {
        String sqlQuery = "INSERT INTO " + TABLENAME + " (type,identifier,status,rejected_message,request_date) VALUES " +
                "(:type,:identifier,:status,:rejectedMessage,:requestDate)";
        Query query = getHibernateSession(context).createSQLQuery(sqlQuery);
        query.setString("type", destructTransferRequest.getType().name());
        query.setString("identifier", destructTransferRequest.getIdentifier());
        query.setString("status", destructTransferRequest.getStatus().name());
        query.setString("rejectedMessage", destructTransferRequest.getRejectedMessage());
        query.setTimestamp("requestDate", destructTransferRequest.getRequestDate());
        query.executeUpdate();

        log.debug(LogManager.getHeader(context, "create_destruct_transfer_request",
                "destruct_transfer_request_id=" + destructTransferRequest.getRequestId()));
    }

    @Override
    public void update(Context context, DestructTransferRequest destructTransferRequest) throws SQLException {
        String sqlQuery = "UPDATE " + TABLENAME +
                " SET type=:type,identifier=:identifier,status=:status,rejected_message=:rejectedMessage,request_date=:requestDate" +
                " WHERE request_id=:requestId";
        Query query = getHibernateSession(context).createSQLQuery(sqlQuery);
        query.setInteger("requestId", destructTransferRequest.getRequestId());
        query.setString("type", destructTransferRequest.getType().name());
        query.setString("identifier", destructTransferRequest.getIdentifier());
        query.setString("status", destructTransferRequest.getStatus().name());
        query.setString("rejectedMessage", destructTransferRequest.getRejectedMessage());
        query.setTimestamp("requestDate", destructTransferRequest.getRequestDate());
        query.executeUpdate();

        log.debug(LogManager.getHeader(context, "update_destruct_transfer_request",
                "destruct_transfer_request_id=" + destructTransferRequest.getRequestId()));
    }

    @Override
    public void delete(Context context, Integer requestId) throws SQLException {
        String sqlQuery = "DELETE FROM " + TABLENAME + " WHERE request_id=:requestId";
        Query query = getHibernateSession(context).createSQLQuery(sqlQuery);
        query.setInteger("requestId", requestId);
        query.executeUpdate();

        log.debug(LogManager.getHeader(context, "delete_destruct_transfer_request",
                "destruct_transfer_request_id=" + requestId));
    }

    /**
     * Uloží výsledek dotazu (SELECT) do objektu DestructTransferRequest
     * @param row
     * @return
     */
    private DestructTransferRequest setResultFromRow(Object[] row) {
        DestructTransferRequest destructTransferRequest = new DestructTransferRequest();
        destructTransferRequest.setRequestId(Integer.valueOf(row[0].toString()));
        destructTransferRequest.setType(DestructTransferRequest.typeFromString(row[1].toString()));
        destructTransferRequest.setIdentifier(row[2].toString());
        destructTransferRequest.setStatus(DestructTransferRequest.statusFromString(row[3].toString()));
        destructTransferRequest.setRejectedMessage(row[4] != null ? row[4].toString() : null);
        destructTransferRequest.setRequestDate(parseDateFromString(row[5].toString()));

        return destructTransferRequest;
    }

    /**
     * Konvertuje hodnotu typu String na hodnotu typu Date
     * @param dateInString
     * @return
     */
    private static Date parseDateFromString(String dateInString) {
        if (StringUtils.isEmpty(dateInString))
            return null;
        Date date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            date = formatter.parse(dateInString);
        } catch (ParseException e) {
            throw new ProcessingException("Chyba při konverzi datumu: " + e.getMessage());
        }
        return date;
    }

}
