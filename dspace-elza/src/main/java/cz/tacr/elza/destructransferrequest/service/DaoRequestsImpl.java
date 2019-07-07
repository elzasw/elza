package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.metadataconstants.MetadataConstantService;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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

//    @Autowired
    private DescructTransferRequestService descructTransferRequestService;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    //private DescructTransferRequestService descructTransferRequestService = cz.tacr.elza.factory.ContentServiceFactory.getInstance().getDescructTransferRequestService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);
    private MetadataConstantService metadata;

    @Override
    public String postDestructionRequest(DestructionRequest destructionRequest) throws DaoServiceException {
        log.info("Spuštěna metoda postDestructionRequest.");
        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }
        try {
            DestructTransferRequest destructRequest = descructTransferRequestService.findByIdentifier(context, destructionRequest.getIdentifier());
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
            descructTransferRequestService.create(context, destructRequest);
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
            DestructTransferRequest destructRequest = descructTransferRequestService.findByIdentifier(context, transferRequest.getIdentifier());
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
            descructTransferRequestService.create(context, destructRequest);
            log.info("Ukončena metoda postTransferRequest");
            return destructRequest.getUuid();

        } catch (Exception e) {
            throw new DaoServiceException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public DaosSyncResponse syncDaos(DaosSyncRequest daosSyncRequest) throws DaoServiceException {
        log.info("Spuštěna metoda DaosSyncResponse.");
        DaosSyncResponse daosSyncResponse = new DaosSyncResponse();

        List<DaoSyncRequest> daoSyncRequestList = daosSyncRequest.getDaoSyncRequest();
        if (daoSyncRequestList.isEmpty()) {
            return daosSyncResponse;
        }

        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        Daoset daoset = new Daoset();
        List<Dao> daoList = daoset.getDao();

        NonexistingDaos nonexistingDaos = new NonexistingDaos();
        List<String> nonexistingDaoId = nonexistingDaos.getDaoId();
        for (DaoSyncRequest daoSyncRequest : daoSyncRequestList) {
            UUID uuId = UUID.fromString(daoSyncRequest.getDaoId());
            log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
            Item item = getItem(context, uuId);
            if (item != null) {
                log.info("Aktualizuji metadata položky digitalizátu Uuid=" + uuId + ".");
                //TODO:cacha doplnit po upřesnění vstupních dat

                log.info("Zapisuji technická metadata položky digitalizátu Uuid=" + uuId + ".");
                ItemService itemService = item.getItemService();
                String[] techMataData = metadata.getTechMetaData();
                for (String mataDataCode : techMataData) {
                    final String[] mt = metadata.getMetaData(mataDataCode);
                    List<MetadataValue> metadataList = itemService.getMetadata(item, mt[0], mt[1], mt[2], Item.ANY);
                    for (MetadataValue metadataValue : metadataList) {
                        Dao dao = new Dao();
//                        dao.setFileGroup();
                        dao.setIdentifier(Integer.toString(metadataValue.getID()));
                        dao.setLabel(metadataValue.getValue());

                        List<RelatedFileGroup> relatedFileGroupList = dao.getRelatedFileGroup();
                        RelatedFileGroup relatedFileGroup = new RelatedFileGroup();
//                        relatedFileGroup.setFileGroup();
//                        relatedFileGroup.setIdentifier();
//                        relatedFileGroup.setLabel();

                        relatedFileGroupList.add(relatedFileGroup);
                        daoList.add(dao);
                    }
                }
            } else {
                log.info("Neexistující položka digitalizátu Uuid=" + uuId + ".");
                nonexistingDaoId.add(daoSyncRequest.getDaoId());
            }

        }

        daosSyncResponse.setDaoset(daoset);
        daosSyncResponse.setNonexistingDaos(nonexistingDaos);

        log.info("Ukončena metoda DaosSyncResponse");
        return daosSyncResponse;
    }

    private Item getItem(Context context, UUID uuId) {
        Item item = null;
        try {
            item = itemService.find(context, uuId);
        } catch (Exception e) {
            throw new ProcessingException("Chyba při vyhledání položky digitalizátu (" + uuId + "): " + e.getMessage());
        }
        return item;
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
