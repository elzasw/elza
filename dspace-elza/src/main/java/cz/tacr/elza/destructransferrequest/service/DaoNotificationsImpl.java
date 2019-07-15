package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Implementace webových služeb pro komunikaci se systému Elza (server)
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 28.06.2019.
 */

@Component
@javax.jws.WebService(
        serviceName = "CoreService",
        portName = "DaoCoreRequests",
        targetNamespace = "http://dspace.tacr.cz/ws/core/v1",
        endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoNotifications")

public class DaoNotificationsImpl implements DaoNotifications {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    @Override
    public void onDaoLinked(OnDaoLinked onDaoLinked) throws DaoServiceException {
        log.info("Spuštěna metoda onDaoLinked.");
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem(); //TODO:cacha - zrušit
        } catch (SQLException e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        String daoIdentifier = onDaoLinked.getDaoIdentifier();
        if (StringUtils.isEmpty(daoIdentifier )) {
            throw new ProcessingException("Identifikátor digitalizátu nesmí být prázdný.");
        }
        UUID uuId = UUID.fromString(daoIdentifier);
        log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
        Item item = getItem(context, uuId);
        log.info("Aktualizuji metadata položky digitalizátu Uuid=" + uuId + ".");
        //TODO:cacha doplnit po upřesnění vstupních dat

        MetadataEnum mt = MetadataEnum.ISELZA;
        log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu Uuid=" + uuId + " schema=" +
                mt.getSchema() + ".");
        List<MetadataValue> metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
        setMetadataValue(context, item, metadataList, Boolean.TRUE.toString(), mt);

        Did did = onDaoLinked.getDid();
        if (did != null) {
            if (StringUtils.isNotBlank(did.getIdentifier())) {
                mt = MetadataEnum.ELZADIDID;
                log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu Uuid=" + uuId + " schema=" +
                        mt.getSchema() + ".");
                metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
                setMetadataValue(context, item, metadataList, did.getIdentifier(), mt);
            }
        }

        try {
            context.complete();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při ukončení contextu: " + e.getMessage());
        }

        log.info("Ukončena metoda onDaoLinked");
    }

    @Override
    public void onDaoUnlinked(OnDaoUnlinked onDaoUnlinked) throws DaoServiceException {
        log.info("Spuštěna metoda onDaoUnlinked.");
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem(); //TODO:cacha - zrušit
        } catch (SQLException e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        String daoIdentifier = onDaoUnlinked.getDaoIdentifier();
        if (StringUtils.isEmpty(daoIdentifier )) {
            throw new ProcessingException("Identifikátor digitalizátu nesmí být prázdný.");
        }
        UUID uuId = UUID.fromString(daoIdentifier);
        log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
        Item item = getItem(context, uuId);
        ItemService itemService = item.getItemService();
        log.info("Odstraňuji metadata položky digitalizátu Uuid=" + uuId + ".");

        MetadataEnum mt = MetadataEnum.ELZADIDID;
        log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu Uuid=" + uuId + " schema=" +
                mt.getSchema() + ".");
        try {
            List<MetadataValue> metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
            itemService.removeMetadataValues(context, item, metadataList);
            itemService.update(context, item);

            mt = MetadataEnum.ISELZA;
            log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu Uuid=" + uuId + " schema=" +
                    mt.getSchema() + ".");
            metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
            setMetadataValue(context, item, metadataList, Boolean.FALSE.toString(), mt);


            context.complete();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při ukončení contextu: " + e.getMessage());
        }

        log.info("Ukončena metoda onDaoUnlinked");
    }

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
     * @param mt
     */
    private void setMetadataValue(Context context, Item item, List<MetadataValue> metadataList, String mdValue, MetadataEnum mt) {
        try {
            if (metadataList.size() == 0) {
                itemService.addMetadata(context, item, mt.getSchema(), mt.getElement(), mt.getQualifier(), null, mdValue);
            } else if (metadataList.size() == 1) {
                MetadataValue metadataValue = metadataList.get(0);
                metadataValue.setValue(mdValue);
            } else {
                itemService.removeMetadataValues(context, item, metadataList);
                itemService.addMetadata(context, item, mt.getSchema(), mt.getElement(), mt.getQualifier(), null, mdValue);
            }
            itemService.update(context, item);
        }
        catch (SQLException es) {
                throw new DaoServiceException("Chyba při ukládání položky " + item.getID() + " do databáze: " + es);
        }
        catch (Exception e) {
            throw new ProcessingException("Chyba při nastavení metadat pro položku digitalizátu Uuid=" + item.getID() +
                    " schema=" + mt.getSchema() + " element=" + mt.getElement() + ": " + e.getMessage());
        }
    }
}
