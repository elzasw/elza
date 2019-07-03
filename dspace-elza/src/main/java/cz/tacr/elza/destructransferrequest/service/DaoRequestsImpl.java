package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.util.List;
import java.util.UUID;

/**
 * Implementace webových služeb pro komunikaci se systému Elza (server)
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 21.06.2019.
 */
@Component
@javax.jws.WebService(
        serviceName = "CoreService",
        portName = "DaoCoreRequests",
        targetNamespace = "http://dspace.tacr.cz/ws/core/v1",
        endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoRequests")

public class DaoRequestsImpl implements DaoRequests{
    private String SEPARATOR = ";";

    @Autowired
    private DestructTransferRequestDAO destructionRequestDAO;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    @Override
    public String postDestructionRequest(DestructionRequest destructionRequest) throws DaoServiceException {
        log.info("Spuštěna metoda postDestructionRequest.");
        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
        } catch (Exception e) {
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }
        try {
            DestructTransferRequest destructRequest = destructionRequestDAO.findByIdentifier(context, destructionRequest.getIdentifier());
            if (destructRequest != null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + destructionRequest.getIdentifier() +
                        " již v systému existuje.");
            }

            DaoIdentifiers daoIdentifiers = destructionRequest.getDaoIdentifiers();
            if (daoIdentifiers == null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + destructionRequest.getIdentifier() +
                        " neobsahuje žádné identifikátory.");
            }
            log.info("Kontroluji identifikátory skartovaných digitalizátů.");
            List<String> identifierList = daoIdentifiers.getIdentifier();
            String identifierStr = null;
            for (String identifier : identifierList) {
                checkIdentifier(context, UUID.fromString(identifier));
                if (StringUtils.isNotEmpty(identifierStr)) {
                    identifierStr = identifierStr + SEPARATOR + identifier;
                } else {
                    identifierStr = identifierStr + identifier;
                }
            }

            log.info("Ukládám požadavek na skartaci digitalizátu.");
            destructRequest = new DestructTransferRequest();
            destructRequest.initDestructionRequest(destructionRequest);
            destructRequest.setDaoIdentifiers(identifierStr);
            destructionRequestDAO.insertDestrucTransferRequest(context, destructRequest);
            log.info("Ukončena metoda postDestructionRequest");
            return destructRequest.getUuid();

        } catch (Exception e) {
            throw new DaoServiceException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public String postTransferRequest(TransferRequest transferRequest) throws DaoServiceException {
        log.info("Spuštěna metoda postTransferRequest.");
        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
        } catch (Exception e) {
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }
        try {
            DestructTransferRequest destructRequest = destructionRequestDAO.findByIdentifier(context, transferRequest.getIdentifier());
            if (destructRequest != null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + transferRequest.getIdentifier() +
                        " již v systému existuje.");
            }

            DaoIdentifiers daoIdentifiers = transferRequest.getDaoIdentifiers();
            if (daoIdentifiers == null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + transferRequest.getIdentifier() +
                        " neobsahuje žádné identifikátory.");
            }
            log.info("Kontroluji identifikátory delimitovaných digitalizátů.");
            List<String> identifierList = daoIdentifiers.getIdentifier();
            String identifierStr = null;
            for (String identifier : identifierList) {
                checkIdentifier(context, UUID.fromString(identifier));
                if (StringUtils.isNotEmpty(identifierStr)) {
                    identifierStr = identifierStr + SEPARATOR + identifier;
                } else {
                    identifierStr = identifierStr + identifier;
                }
            }

            log.info("Ukládám požadavek na delimitaci digitalizátu.");
            destructRequest = new DestructTransferRequest();
            destructRequest.initTransferRequest(transferRequest);
            destructRequest.setDaoIdentifiers(identifierStr);
            destructionRequestDAO.insertDestrucTransferRequest(context, destructRequest);
            log.info("Ukončena metoda postTransferRequest");
            return destructRequest.getUuid();

        } catch (Exception e) {
            throw new DaoServiceException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public DaosSyncResponse syncDaos(DaosSyncRequest daosSyncRequest) throws DaoServiceException {
        throw new UnsupportedOperationException("Funkce syncDaos není implementována.");

    }

    private void checkIdentifier(Context context, UUID uuId) {
        Item item = null;
        try {
            item = itemService.find(context, uuId);
        } catch (Exception e) {
            throw new ProcessingException("Chyba při vyhledání položky digitalizátu (" + uuId + "): " + e.getMessage());
        }

        if (item == null) {
            throw new UnsupportedOperationException("Digitalizát s Uuid=" + uuId + " nebyl v tabulce Item nalezen.");
        }
    }

}
