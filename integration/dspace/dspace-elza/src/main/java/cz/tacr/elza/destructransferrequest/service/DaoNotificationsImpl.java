package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.ws.WsClient;
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
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
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
        serviceName = "DaoNotifications",
        targetNamespace = "http://dspace.tacr.cz/ws/core/v1/DaoNotifications",
        endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoNotifications")
public class DaoNotificationsImpl implements DaoNotifications {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    @Override
    public void onDaoLinked(OnDaoLinked onDaoLinked) throws DaoServiceException {
        log.info("Spuštěna metoda onDaoLinked.");
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();
        } catch (SQLException e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        String handle = onDaoLinked.getDaoIdentifier();
        if (StringUtils.isEmpty(handle )) {
            throw new ProcessingException("Handle digitalizátu nesmí být prázdný.");
        }
        log.info("Vyhledávám položku digitalizátu s handle=" + handle + ".");
        Item item = getItem(context, handle);
        log.info("Aktualizuji metadata položky digitalizátu Uuid=" + handle + ".");

        MetadataEnum mt = MetadataEnum.ISELZA;
        log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu s handle=" + handle + " schema=" +
                mt.getSchema() + ".");
        List<MetadataValue> metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
        setMetadataValue(context, item, metadataList, Boolean.TRUE.toString(), mt);

        Did did = onDaoLinked.getDid();
        if (did != null) {
            if (StringUtils.isNotBlank(did.getIdentifier())) {
                mt = MetadataEnum.ELZADIDID;
                log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu s handle=" + handle + " schema=" +
                        mt.getSchema() + ".");
                metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
                setMetadataValue(context, item, metadataList, did.getIdentifier(), mt);
            }
            WsClient.updateItem(context, item, did);
        }

        try {
            context.commit();
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
            context.turnOffAuthorisationSystem();
        } catch (SQLException e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        String handle = onDaoUnlinked.getDaoIdentifier();
        if (StringUtils.isEmpty(handle )) {
            throw new ProcessingException("Handle digitalizátu nesmí být prázdný.");
        }
        log.info("Vyhledávám položku digitalizátu s handle=" + handle + ".");
        Item item = getItem(context, handle);
        ItemService itemService = item.getItemService();
        log.info("Odstraňuji metadata položky digitalizátu s handle=" + handle + ".");

        try {
            MetadataEnum mt = MetadataEnum.ELZADIDID;
            log.info("Aktualizuji metadata element=" + mt.getElement() + " pro položku digitalizátu s handle=" + handle + " schema=" +
                    mt.getSchema() + ".");
            List<MetadataValue> metadataList = itemService.getMetadata(item, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
            itemService.removeMetadataValues(context, item, metadataList);
            itemService.update(context, item);

            context.commit();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při ukončení contextu: " + e.getMessage());
        }

        log.info("Ukončena metoda onDaoUnlinked");
    }

    private Item getItem(Context context, String handle) {
        Item item;
        try {
            item = (Item) handleService.resolveToObject(context, handle);
        } catch (Exception e) {
            throw new ProcessingException("Chyba při vyhledání položky digitalizátu (" + handle + "): " + e.getMessage());
        }

        if (item == null) {
            throw new UnsupportedOperationException("Digitalizát s handle=" + handle + " nebyl v nalezen.");
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
