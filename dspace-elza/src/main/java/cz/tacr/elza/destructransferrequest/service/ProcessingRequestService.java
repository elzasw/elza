package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.metadataconstants.MetadataConstantService;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.RequestRevoked;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.elza.DestructTransferRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Servisní třída pro zpracování požadavků ze systému Elza
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 25.06.2019.
 */
@Component
public class ProcessingRequestService {
    private String SEPARATOR = ";";

    @Autowired
    private DescructTransferRequestService descructTransferRequestService;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);
    private MetadataConstantService metadata;

    public final static String METADATA_ISELZA = "ISELZA";

    /**
     * Zpracuje požadavky na skartaci digitalizátů, které jsou ve stavu QUEUED a odešle informaci do systému Elza
     * Created by Marbes Consulting
     * ludek.cacha@marbes.cz / 25.06.2019.
     */
    public void processingDestructionRequest() throws CoreServiceException {
        log.info("Spuštěna metoda processingDestructionRequest.");
        List<DestructTransferRequest> destructTransferRequest = new ArrayList<>();
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            destructTransferRequest = descructTransferRequestService.findByTypeAndStatus(context,
                DestructTransferRequest.Status.QUEUED, DestructTransferRequest.RequestType.DESTRUCTION);
        } catch (SQLException e) {
            throw new ProcessingException("Chyba při vyhledání požadavků na skartaci: " + e.getMessage());
        }

        if (destructTransferRequest.size() == 0) {
            log.info("V zásobníku požadavků na skartaci nebyl vybrán žádný záznam ke zpracování.");
            return;
        }

        DestructTransferRequest processingRequest = new DestructTransferRequest();
        try {
            for (DestructTransferRequest destructRequest : destructTransferRequest) {
                log.info("Zpracovávám požadavek na skartaci requestID=" + destructRequest.getRequestId() + ".");
                processingRequest = destructRequest;
                String[] daoIdentifiers = StringUtils.split(destructRequest.getDaoIdentifiers(), SEPARATOR);
                for (String identifier : daoIdentifiers) {
                    UUID uuId = UUID.fromString(identifier);
                    log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
                    Item item = getItem(context, uuId);
                    ItemService itemService = item.getItemService();

                    log.info("Skartuji (ruším) metadata položky digitalizátu Uuid=" + uuId + ".");
                    List<MetadataValue> metadataValueList = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                    itemService.removeMetadataValues(context, item, metadataValueList);
                    itemService.update(context, item);
                    log.info("Skartuji (ruším) položku digitalizátu Uuid=" + uuId + ".");
                    itemService.delete(context, item);
                }

                log.info("Aktualizuji stav požadavku na skartaci.");
                processingRequest.setStatus(DestructTransferRequest.Status.PROCESSED);
                processingRequest.setProcessingDate(new Date());
                processingRequest.setRejectedMessage(null);
                descructTransferRequestService.update(context, processingRequest);
                context.complete();

                log.info("Odesílám informaci o zpracování požadavku na skartaci do systému Elza.");
                WsClient.destructionRequestFinished(destructRequest.getUuid());
                log.info("Informaci o zpracování požadavku na skartaci byla úspěšně odeslána do systému Elza.");
            }
        } catch (Exception e) {
            log.info("Chyba při zpracovávání požadavku na skartaci requestID=" + processingRequest.getRequestId() + "ERROR: " +
                    e.getLocalizedMessage() + ".");

            log.info("Aktualizuji stav požadavku na skartaci.");
            try {
                processingRequest.setStatus(DestructTransferRequest.Status.REJECTED);
                processingRequest.setProcessingDate(new Date());
                processingRequest.setRejectedMessage(e.getMessage());
                descructTransferRequestService.update(context, processingRequest);
                context.complete();

                log.info("Odesílám chybovou zprávu do systému Elza.");
                RequestRevoked requestRevoked = new RequestRevoked();
                requestRevoked.setIdentifier(processingRequest.getUuid());
                requestRevoked.setDescription(e.getMessage());
                WsClient.destructionRequestRevoked(requestRevoked);
            } catch (Exception e1) {
                throw new ProcessingException("Chyba při zpracování chybového stavu požadavku na skartaci: " + e1.getMessage());
            }
        }
        log.info("Ukončena metoda processingDestructionRequest.");

    }

    /**
     * Zpracuje požadavky na delimitaci digitalizátů, které jsou ve stavu QUEUED a odešle informaci do systému Elza
     * Created by Marbes Consulting
     * ludek.cacha@marbes.cz / 25.06.2019.
     */
    public void processingTransferRequest() throws CoreServiceException {
        log.info("Spuštěna metoda processingTransferRequest.");
        List<DestructTransferRequest> destructTransferRequest = new ArrayList<>();
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem(); //TODO:cacha - testování
            destructTransferRequest = descructTransferRequestService.findByTypeAndStatus(context,
                    DestructTransferRequest.Status.QUEUED, DestructTransferRequest.RequestType.TRANSFER);
        } catch (SQLException e) {
            context.abort();
            throw new ProcessingException("Chyba při vyhledání požadavků na delimitaci: " + e.getMessage());
        }

        if (destructTransferRequest.size() == 0) {
            log.info("V zásobníku požadavků na delimitaci nebyl vybrán žádný záznam ke zpracování.");
            return;
        }

        DestructTransferRequest processingRequest = new DestructTransferRequest();
        try {
            for (DestructTransferRequest destructRequest : destructTransferRequest) {
                log.info("Zpracovávám požadavek na delimitaci requestID=" + destructRequest.getRequestId() + ".");
                processingRequest = destructRequest;
                String[] daoIdentifiers = StringUtils.split(destructRequest.getDaoIdentifiers(), SEPARATOR);
                for (String identifier : daoIdentifiers) {
                    UUID uuId = UUID.fromString(identifier);
                    log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
                    Item item = getItem(context, uuId);
                    ItemService itemService = item.getItemService();

                    log.info("Vyhledávám cílovou kolekci " + destructRequest.getTargetFund() + "pro položku digitalizátu.");
                    Collection targetCollection = collectionService.find(context, UUID.fromString(destructRequest.getTargetFund()));
                    if (targetCollection == null) {
                        throw new ProcessingException("Cílová kolekce Uuid=" + UUID.fromString(destructRequest.getTargetFund()) +
                                " nebyla nalezena.");
                    }

                    log.info("Odstraňuji propojení digitalizátu s aktuálními kolekcemi.");
                    List<Collection> collectionList = item.getCollections();
                    for (Collection collection : collectionList) {
                        log.info("Odstraňuji propojení digitalizátu s kolekcí " + collection.getID() + ".");
                        collectionService.removeItem(context, collection, item);
                        collectionService.update(context, collection);
                    }

                    log.info("Odstraňuji propojení digitalizátu s kolekcí vlastnika.");
                    Collection owningCollection = item.getOwningCollection();
                    if (owningCollection != null) {
                        collectionService.removeItem(context, owningCollection, item);
                        collectionService.update(context, owningCollection);
                    }

                    log.info("Zakládám propojení digitalizátu Uuid=" + uuId + "na cílovou (archivní) kolekci " + targetCollection.getID() + ".");
                    collectionService.addItem(context, targetCollection, item);
                    collectionService.update(context, targetCollection);

                    final String[] mt = metadata.getMetaData(METADATA_ISELZA);
                    log.info("Aktualizuji hodnotu metadat pro element=" + mt[1] + " na FALSE.");
                    log.info("Vyhledávám metadata pro položku digitalizátu Uuid=" + uuId + " schema=" + mt[0] +
                            " element=" + mt[1] + ".");
                    List<MetadataValue> metadataList = itemService.getMetadata(item, mt[0], mt[1], mt[2], Item.ANY);
                    setMetadataValue(context, item, metadataList, Boolean.FALSE.toString(), METADATA_ISELZA);
                }

                log.info("Aktualizuji stav požadavku na delimitaci.");
                processingRequest.setStatus(DestructTransferRequest.Status.PROCESSED);
                processingRequest.setProcessingDate(new Date());
                processingRequest.setRejectedMessage(null);
                descructTransferRequestService.update(context, processingRequest);
                context.complete();

                log.info("Odesílám informaci o zpracování požadavku na delimitaci do systému Elza.");
                WsClient.transferRequestFinished(destructRequest.getUuid());
                log.info("Informaci o zpracování požadavku na delimitaci byla úspěšně odeslána do systému Elza.");
            }
        } catch (Exception e) {
            log.info("Chyba při zpracovávání požadavku na delimitaci requestID=" + processingRequest.getRequestId() + "ERROR: " +
                    e.getLocalizedMessage() + ".");

            log.info("Aktualizuji stav požadavku na delimitaci.");
            try {
                processingRequest.setStatus(DestructTransferRequest.Status.REJECTED);
                processingRequest.setProcessingDate(new Date());
                processingRequest.setRejectedMessage(e.getMessage());
                descructTransferRequestService.update(context, processingRequest);
                context.complete();

                log.info("Odesílám chybovou zprávu do systému Elza.");
                RequestRevoked requestRevoked = new RequestRevoked();
                requestRevoked.setIdentifier(processingRequest.getUuid());
                requestRevoked.setDescription(e.getMessage());
                WsClient.transferRequestRevoked(requestRevoked);
            } catch (Exception e1) {
                context.abort();
                throw new ProcessingException("Chyba při zpracování chybového stavu požadavku na delimitaci: " + e1.getMessage());
            }
        }
        log.info("Ukončena metoda processingTransferRequest.");

    }

    /**
     * Vyhledá digitalizát dle zadaného UUID
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

        if (item == null) {
            throw new UnsupportedOperationException("Digitalizát s Uuid=" + uuId + " nebyl v tabulce Item nalezen.");
        }
        return item;
    }

    /**
     * Provede nastavení metadat pro zadané pole
     * @param context
     * @param item
     * @param metadataList
     * @param mdValue
     */
    private void setMetadataValue(Context context, Item item, List<MetadataValue> metadataList, String mdValue, String mdField) {
        final String[] mt = MetadataConstantService.getMetaData(mdField);
        try {
            ItemService itemService = item.getItemService();
            if (metadataList.size() == 0) {
                itemService.addMetadata(context, item, mt[0], mt[1], mt[2], null, mdValue);
            } else if (metadataList.size() == 1) {
                MetadataValue metadataValue = metadataList.get(0);
                metadataValue.setValue(mdValue);
            } else {
                itemService.removeMetadataValues(context, item, metadataList);
                itemService.addMetadata(context, item, mt[0], mt[1], mt[2], null, mdValue);
            }
            itemService.update(context, item);
        }
        catch (SQLException es) {
            throw new DaoServiceException("Chyba při ukládání položky " + item.getID() + " do databáze: " + es);
        }
        catch (Exception e) {
            throw new ProcessingException("Chyba při nastavení metadat  pro položku digitalizátu Uuid=" + item.getID() +
                    " schema=" + mt[0] + " element=" + mt[1] + ": " + e.getMessage());
        }
    }
}
