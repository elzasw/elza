package cz.tacr.elza.destructransferrequest.service;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.ProcessingException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.daoimport.service.DaoImportService;
import cz.tacr.elza.destructransferrequest.DestructTransferRequest;
import cz.tacr.elza.destructransferrequest.dao.DestructTransferRequestDAO;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.ChecksumType;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoIdentifiers;
import cz.tacr.elza.ws.types.v1.DaoSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncResponse;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.DestructionRequest;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.Dids;
import cz.tacr.elza.ws.types.v1.File;
import cz.tacr.elza.ws.types.v1.FileGroup;
import cz.tacr.elza.ws.types.v1.NonexistingDaos;
import cz.tacr.elza.ws.types.v1.TransferRequest;
import cz.tacr.elza.ws.types.v1.UnitOfMeasure;

/**
 * Implementace webových služeb pro komunikaci se systému Elza (server)
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 21.06.2019.
 */
@Component
@javax.jws.WebService(
        serviceName = "DaoRequests",
        targetNamespace = "http://dspace.tacr.cz/ws/core/v1/DaoRequests",
        endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoRequests")
public class DaoRequestsImpl implements DaoRequests{

    @Autowired
    private DestructTransferRequestDAO destructTransferRequestDAO;

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();;
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    /**
     * Zpracování požadavku na skartaci ze systému ELZA
     * @param destructionRequest
     * @return
     * @throws DaoServiceException
     */
    @Override
    public String postDestructionRequest(DestructionRequest destructionRequest) throws DaoServiceException {
        log.debug("Spuštěna metoda postDestructionRequest (skartace).");
        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        DestructTransferRequest processingRequest = new DestructTransferRequest();
        initDestructionRequest(destructionRequest, processingRequest);
        try {
            DaoIdentifiers daoIdentifiers = destructionRequest.getDaoIdentifiers();
            if (daoIdentifiers == null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + destructionRequest.getIdentifier() +
                        " neobsahuje žádné identifikátory položek.");
            }

            // Zpracování skartace jednotlivých položek požadavku
            List<String> identifierList = daoIdentifiers.getIdentifier();
            for (String identifier : identifierList) {
                log.debug("Kontroluji digitalizát identifier=" + identifier + ".");
                checkItemIdentifier(context, UUID.fromString(identifier));

                log.debug("Zpracovávám požadavek na skartaci položky identifier=" + identifier + ".");
                UUID uuId = UUID.fromString(identifier);
                log.debug("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
                Item item = getItem(context, uuId);

                log.debug("Skartuji (ruším) metadata položky digitalizátu Uuid=" + uuId + ".");
                List<MetadataValue> metadataValueList = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                itemService.removeMetadataValues(context, item, metadataValueList);
                itemService.update(context, item);
                log.debug("Skartuji (ruším) položku digitalizátu Uuid=" + uuId + ".");
                itemService.delete(context, item);
            }

            log.debug("Ukládám zpracování požadavku na skartaci digitalizátu (PROCESSED).");
            processingRequest.setStatus(DestructTransferRequest.Status.PROCESSED);
            processingRequest.setRejectedMessage(null);
            destructTransferRequestDAO.insert(context, processingRequest);
            context.commit();
            log.info("Požadavek na skartaci Identifier=" + destructionRequest.getIdentifier() + " byl úspěšně zpracován.");

        }
        catch (Exception e) {
            context.abort();
            log.info("Chyba při zpracování požadavku na skartaci Identifier=" + destructionRequest.getIdentifier() + ".", e);
            log.debug("Ukládám zpracování požadavku na delimitaci digitalizátu (REJECTED).");
            processingRequest.setStatus(DestructTransferRequest.Status.REJECTED);
            processingRequest.setRejectedMessage(e.getMessage());
            try {
                context = ContextUtils.createContext();
                destructTransferRequestDAO.insert(context, processingRequest);
                context.commit();
            } catch (Exception ex) {
                throw new ProcessingException("Chyba při ukládání požadavku " + destructionRequest.getIdentifier() + " do databáze: " + ex);
            }
        }

        log.debug("Ukončena metoda postDestructionRequest");
        return Integer.toString(processingRequest.getRequestId());
    }

    /**
     * Zpracování požadavku na delimitaci ze systému ELZA
     * Po přijetí požadavku se provede delimitace a do tabulky destruc_transfer_request se uloží výsledek pro odeslání notifikace
     * @param transferRequest
     * @return
     * @throws DaoServiceException
     */
    @Override
    public String postTransferRequest(TransferRequest transferRequest) throws DaoServiceException {
        log.debug("Spuštěna metoda postTransferRequest.");

        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        DestructTransferRequest processingRequest = new DestructTransferRequest();
        initTransferRequest(transferRequest, processingRequest);
        try {
            DaoIdentifiers daoIdentifiers = transferRequest.getDaoIdentifiers();
            if (daoIdentifiers == null) {
                throw new ProcessingException("Požadavek TransferRequest Identifier=" + transferRequest.getIdentifier() +
                        " neobsahuje žádné identifikátory položek.");
            }

            if (StringUtils.isBlank(transferRequest.getDescription())) {
                throw new ProcessingException("Požadavek TransferRequest Identifier=" + transferRequest.getIdentifier() +
                        " neobsahuje popis s kódem komunity a kódem kolekce.");
            }

            // Vyhledání cílové kolekce dle informace v poli Description (Community/Collection)
            String[] description = parseDescription(transferRequest.getDescription());
            if (StringUtils.isBlank(description[0])) {
                throw new IllegalStateException("Pole popis " + transferRequest.getDescription() + " neobsahuje identifikátor komunity.");
            }
            if (StringUtils.isBlank(description[1])) {
                throw new IllegalStateException("Pole popis " + transferRequest.getDescription() + " neobsahuje identifikátor kolekce.");
            }
            log.debug("Vyhledávám cílovou komunitu " + description[0] + "pro položku digitalizátu.");
            Community community = null;
            List<Community> communities = communityService.findAll(context);
            for (Community c : communities) {
                String desc = communityService.getMetadataFirstValue(c, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY);
                if (description[0].equalsIgnoreCase(desc)) {
                    community = c;
                    break;
                }
            }
            if (community == null) {
                throw new IllegalStateException("Komunita(archiv) s názvem " + description[0] + " neexistuje.");
            }

            log.debug("Vyhledávám cílovou kolekci " + description[1] + "pro položku digitalizátu.");
            Collection targetCollection = null;
            List<Collection> collections = communityService.getAllCollections(context, community);
            for (Collection c : collections) {
                String desc = collectionService.getMetadataFirstValue(c, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY);
                if (description[1].equalsIgnoreCase(desc)) {
                    targetCollection = c;
                    break;
                }
            }
            if (targetCollection == null) {
                throw new IllegalStateException("Kolekce(archivní soubor) s názvem " + description[1] + " neexistuje.");
            }

            // Zpracování delimitace jednotlivých položek požadavku
            List<String> identifierList = daoIdentifiers.getIdentifier();
            for (String identifier : identifierList) {
                log.debug("Kontroluji digitalizát identifier=" + identifier + ".");
                checkItemIdentifier(context, UUID.fromString(identifier));

                log.debug("Zpracovávám požadavek na delimitaci položky identifier=" + identifier + ".");
                UUID uuId = UUID.fromString(identifier);
                log.debug("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
                Item item = getItem(context, uuId);

                Collection owningCollection = item.getOwningCollection();
                log.debug("Přesouvám item " + item.getName() + " z kolekce " + owningCollection.getName()
                        + " do archivní kolekce " + targetCollection.getName());
                itemService.move(context, item, owningCollection, targetCollection);

                final MetadataEnum mt = MetadataEnum.ISELZA;
                log.debug("Aktualizuji hodnotu metadat pro element=" + mt.getElement() + " na FALSE.");
                log.debug("Vyhledávám metadata pro položku digitalizátu Uuid=" + uuId + " schema=" + mt.getSchema() +
                        " element=" + mt.getElement() + ".");
                List<MetadataValue> metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
                setMetadataValue(context, item, metadataList, Boolean.FALSE.toString(), mt);
            }

            log.debug("Ukládám zpracování požadavku na delimitaci digitalizátu (PROCESSED).");
            processingRequest.setStatus(DestructTransferRequest.Status.PROCESSED);
            processingRequest.setRejectedMessage(null);
            destructTransferRequestDAO.insert(context, processingRequest);
            context.commit();
            log.info("Požadavek na skartaci Identifier=" + transferRequest.getIdentifier() + " byl úspěšně zpracován.");

        } catch (Exception e) {
            context.abort();
            log.info("Chyba při zpracování požadavku na delimitaci Identifier=" + transferRequest.getIdentifier() + ".", e);
            log.debug("Ukládám zpracování požadavku na delimitaci digitalizátu (REJECTED).");
            processingRequest.setStatus(DestructTransferRequest.Status.REJECTED);
            processingRequest.setRejectedMessage(e.getMessage());
            try {
                context = ContextUtils.createContext();
                destructTransferRequestDAO.insert(context, processingRequest);
                context.commit();
            } catch (Exception ex) {
                throw new DaoServiceException("Chyba při ukládání požadavku " + transferRequest.getIdentifier() + " do databáze: " + ex);
            }
        }

        log.debug("Ukončena metoda postTransferRequest");
        return Integer.toString(processingRequest.getRequestId());
    }

    /**
     * Zpracování požadavku synchronizace
     * @param daosSyncRequest
     * @return
     * @throws DaoServiceException
     */
    @Override
    public DaosSyncResponse syncDaos(DaosSyncRequest daosSyncRequest) throws DaoServiceException {
        log.debug("Spuštěna metoda DaosSyncResponse.");
        DaosSyncResponse daosSyncResponse = new DaosSyncResponse();

        List<DaoSyncRequest> daoSyncRequestList = daosSyncRequest.getDaoSyncRequest();
        if (daoSyncRequestList.isEmpty()) {
            return daosSyncResponse;
        }

        Dids dids = daosSyncRequest.getDids();
        Map<String, Did> didMap = new HashMap<>();
        if (dids != null) {
            for (Did did : dids.getDid()) {
                didMap.put(did.getIdentifier(), did);
            }
        }

        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        Daoset daoset = new Daoset();
        List<Dao> daoList = daoset.getDao();
        NonexistingDaos nonexistingDaos = new NonexistingDaos();
        List<String> nonexistingDaoId = nonexistingDaos.getDaoId();
        for (DaoSyncRequest daoSyncRequest : daoSyncRequestList) {
            UUID uuId = UUID.fromString(daoSyncRequest.getDaoId());
            log.debug("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
            Item item = getItem(context, uuId);
            if (item != null) {
                log.debug("Aktualizuji metadata položky digitalizátu Uuid=" + uuId + ".");

                String didId = daoSyncRequest.getDidId();
                if (didId != null) {
                    Did did = didMap.get(didId);
                    if (did != null) {
                        WsClient.updateItem(context, item, did);
                    }
                }

                String handle = item.getHandle();
                log.debug("Zapisuji technická metadata položky digitalizátu Uuid=" + uuId + ".");
                List<Bundle> bundleList = item.getBundles();
                for (Bundle bundle : bundleList) {
                    if (bundle.getName().contains(DaoImportService.CONTENT_BUNDLE)) {
                        Dao dao = WsClient.createDao(item);
                        FileGroup fileGroup = new FileGroup();
                        List<File> fileList = fileGroup.getFile();

                        List<Bitstream> bitstreamList = bundle.getBitstreams();
                        for (Bitstream bitstream : bitstreamList) {
                            try {
                                File file = WsClient.createFile(handle, bitstream, context);
                                fileList.add(file);
                            } catch (Exception e) {
                                context.abort();
                                throw new ProcessingException("Chyba při vytváření souboru z bitstreamu: " + e.getMessage());
                            }
                        }
                        dao.setFileGroup(fileGroup);
                        daoList.add(dao);
                    }
                }
            } else {
                log.debug("Neexistující položka digitalizátu Uuid=" + uuId + ".");
                nonexistingDaoId.add(daoSyncRequest.getDaoId());
            }
        }

        try {
            context.commit();
        } catch (SQLException e) {
            throw new ProcessingException("Chyba při synchronizaci dao.", e);
        }

        daosSyncResponse.setDaoset(daoset);
        daosSyncResponse.setNonexistingDaos(nonexistingDaos);

        log.info("Ukončena metoda DaosSyncResponse");
        return daosSyncResponse;
    }

    /**
     * Vyhledá položku digitalizátu
     * @param context
     * @param uuId
     * @return
     */
    private Item getItem(Context context, UUID uuId) {
        Item item = null;
        try {
            item = itemService.find(context, uuId);
        } catch (Exception e) {
            throw new ProcessingException("Chyba při vyhledání položky digitalizátu (" + uuId + "): " + e.getMessage());
        }
        return item;
    }

    /**
     * Kontroluje existenci položky digitalizátu v systému
     * @param context
     * @param uuId
     */
    private void checkItemIdentifier(Context context, UUID uuId) {
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

    /**
     * Konvertuje hodnotu typu String na ChecksumType
     * @param checksumAlgorithm
     * @return
     */
    private static ChecksumType convertStringToChecksumType (String checksumAlgorithm) {
        if (StringUtils.isBlank(checksumAlgorithm)) {
            return null;
        }
        switch (checksumAlgorithm) {
            case "MD5":
                return ChecksumType.MD_5;
            case "SHA1":
                return ChecksumType.SHA_1;
            case "SHA256":
                return ChecksumType.SHA_256;
            case "SHA384":
                return ChecksumType.SHA_384;
            case "SHA512":
                return ChecksumType.SHA_512;
        }
        throw new ProcessingException("Pole ChecksumType " + checksumAlgorithm + " není podporováno.");
    }

    /**
     * Konvertuje hodnotu typu String na BigInteger
     * @param bigNumber
     * @return
     */
    private static BigInteger convertStringToBigInteger (String bigNumber) {
        if (StringUtils.isBlank(bigNumber)) {
            return null;
        }
        return BigInteger.valueOf(Long.parseLong(bigNumber));
    }

    /**
     * Konvertuje kód měrné jednotky
     * @param uomCode
     * @return
     */
    private static UnitOfMeasure convertStringToUnitOfMeasure (String uomCode) {
    switch (StringUtils.upperCase(uomCode)) {
        case "IN":
            return UnitOfMeasure.IN;
        case "MM":
            return UnitOfMeasure.MM;
    }
    throw new ProcessingException("Kód měrné jednotky " + uomCode + " není podporován.");
    }

    /**
     * Inicializuje data DestructTransferRequest pro požadavek typu DESTRUCTION
     * @param destructRequest
     * @param destTransfRequest
     */
    private void initDestructionRequest(DestructionRequest destructRequest, DestructTransferRequest destTransfRequest) {
        destTransfRequest.setType(DestructTransferRequest.Type.DESTRUCTION);
        destTransfRequest.setIdentifier(destructRequest.getIdentifier());
        destTransfRequest.setRequestDate(new Date());
    }

    /**
     * Inicializuje data DestructTransferRequest pro požadavek typu TRANSFER
     * @param transferRequest
     * @param destTransfRequest
     */
    private void initTransferRequest(TransferRequest transferRequest,DestructTransferRequest destTransfRequest) {
        destTransfRequest.setType(DestructTransferRequest.Type.TRANSFER);
        destTransfRequest.setIdentifier(transferRequest.getIdentifier());
        destTransfRequest.setRequestDate(new Date());
    }

    /**
     * Nastaví metadata pro zadanou položku digitalizátu
     * @param context
     * @param item
     * @param metadataList
     * @param mdValue
     * @param mdField
     */
    private void setMetadataValue(Context context, Item item, List<MetadataValue> metadataList, String mdValue, MetadataEnum mdField) {
        try {
            if (metadataList.size() == 0) {
                itemService.addMetadata(context, item, mdField.getSchema(), mdField.getElement(), mdField.getQualifier(), null, mdValue);
            } else if (metadataList.size() == 1) {
                MetadataValue metadataValue = metadataList.get(0);
                metadataValue.setValue(mdValue);
            } else {
                itemService.removeMetadataValues(context, item, metadataList);
                itemService.addMetadata(context, item, mdField.getSchema(), mdField.getElement(), mdField.getQualifier(), null, mdValue);
            }
            itemService.update(context, item);
        }
        catch (SQLException es) {
            throw new DaoServiceException("Chyba při ukládání položky " + item.getID() + " do databáze: " + es);
        }
        catch (Exception e) {
            throw new ProcessingException("Chyba při nastavení metadat  pro položku digitalizátu Uuid=" + item.getID() +
                    " schema=" + mdField.getSchema() + " element=" + mdField.getElement() + ": " + e.getMessage());
        }
    }

    /**
     * Rozdělí hodnotu v poli Description ne hodnoty komunita a kolekce (u delimitace)
     * @param description
     * @return
     */
    private String[] parseDescription(String description) {
        if(StringUtils.isBlank(description)) {
            return null;
        }
        String[] pole = new String[2];

        pole[0] = StringUtils.substringBefore(description, "/");
        pole[1] = StringUtils.substringAfter(description, "/");
        return pole;
    }

}
