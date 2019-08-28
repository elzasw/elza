package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.destructransferrequest.DestructTransferRequest;
import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.types.v1.RequestRevoked;
import org.apache.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servisní třída pro notifikaci došlých požadavků ze systému Elza
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 25.06.2019.
 */
@Component
public class ProcessingRequestService {
    @Autowired
    private DestructTransferRequestDAO destructTransferRequestDAO;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    /**
     * Odešle notifikaci o zpracování požadavků na skartaci digitalizátů do systému ELZA.
     * Po odeslání notifikace je požadavek z databáze odstraněn.
     * Created by Marbes Consulting
     * ludek.cacha@marbes.cz / 25.06.2019.
     */
    @Scheduled(initialDelay = 5000, fixedDelay = 30000)
    public void processingRequestNotification() throws CoreServiceException {
        log.debug("Spuštěna metoda processingRequestNotification.");
        List<DestructTransferRequest> destructTransferRequest = new ArrayList<>();
        Context context;
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
            destructTransferRequest = destructTransferRequestDAO.findAll(context);

        } catch (SQLException e) {
            throw new ProcessingException("Chyba při vyhledání požadavků na skartaci/delimitaci: " + e.getMessage());
        }

        if (destructTransferRequest.size() == 0) {
            log.debug("V zásobníku požadavků na skartaci/delimitaci nebyl nalezen žádný záznam pro zpracování.");
            return;
        }

        DestructTransferRequest processingRequest = new DestructTransferRequest();
        try {
            log.debug("Odesílám informace o zpracování požadavků na skartaci/delimitaci do systému Elza.");
            for (DestructTransferRequest destructRequest : destructTransferRequest) {
                if (destructRequest.getStatus().equals(DestructTransferRequest.Status.PROCESSED)) {
                    log.debug("Odesílám informaci o úspěšném zpracování požadavku na skartaci/delimitaci položky identifier=" +
                            destructRequest.getIdentifier() + ".");
                    if (destructRequest.getType().equals(DestructTransferRequest.Type.DESTRUCTION)) {
                        WsClient.destructionRequestFinished(destructRequest.getIdentifier());
                    } else {
                        WsClient.transferRequestFinished(destructRequest.getIdentifier());
                    }

                } else {
                    log.debug("Odesílám informaci o chyném zpracování požadavku na skartaci/delimitaci položky identifier=" +
                            destructRequest.getIdentifier() + ".");
                    RequestRevoked requestRevoked = new RequestRevoked();
                    requestRevoked.setIdentifier(destructRequest.getIdentifier());
                    requestRevoked.setDescription(destructRequest.getRejectedMessage());
                    if (destructRequest.getType().equals(DestructTransferRequest.Type.DESTRUCTION)) {
                        WsClient.destructionRequestRevoked(requestRevoked);
                    } else {
                        WsClient.transferRequestRevoked(requestRevoked);
                    }
                }

                log.debug("Mažu požadavek na skartaci/delimitaci.");
                destructTransferRequestDAO.delete(context, destructRequest.getRequestId());
                context.commit();

                log.info("Informace o zpracování požadavku na skartaci/delimitaci requestID=" + processingRequest.getRequestId() +
                        "byla úspěšně odeslána do systému Elza.");
            }
            context.commit();
        } catch (Exception e) {
            context.abort();
            log.debug("Chyba při zpracovávání požadavku na skartaci/delimitaci requestID=" + processingRequest.getRequestId() + "ERROR: " +
                    e.getMessage() + ".");
        }

        log.debug("Ukončena metoda processingRequestNotification.");

    }
}
