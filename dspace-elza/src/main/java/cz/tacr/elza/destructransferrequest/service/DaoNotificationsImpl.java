package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
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
        endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoRequests")

public class DaoNotificationsImpl implements DaoNotifications {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    private final String ELEMENT = "isElza";
    private final String METADATASCHEMA = "tacr";

    @Override
    public void onDaoLinked(OnDaoLinked onDaoLinked) throws DaoServiceException {
        log.info("Spuštěna metoda onDaoLinked.");
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
        } catch (Exception e) {
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        String daoIdentifier = onDaoLinked.getDaoIdentifier();
        if (StringUtils.isEmpty(daoIdentifier )) {
            throw new ProcessingException("Identifikátor digitalizátu nesmí být prázdný.");
        }
        UUID uuId = UUID.fromString(daoIdentifier);
        log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
        Item item = getItem(context, uuId);
        ItemService itemService = item.getItemService();

        log.info("Vyhledávám metadata element=" + ELEMENT + " pro položku digitalizátu Uuid=" + uuId + " schema=" +
                METADATASCHEMA + ".");
        List<MetadataValue> metadataList = itemService.getMetadata(item, METADATASCHEMA, ELEMENT, null, Item.ANY);
        try {
            if (!metadataList.isEmpty()) {
                itemService.removeMetadataValues(context, item, metadataList);
            }
            itemService.addMetadata(context, item, METADATASCHEMA, ELEMENT, null, null, Boolean.TRUE.toString());
        } catch (Exception e) {
            throw new ProcessingException("Chyba při nastavení metadat  pro položku digitalizátu Uuid=" + uuId + " schema=" +
                    METADATASCHEMA + ".");
        }

        log.info("Ukončena metoda onDaoLinked");
    }

    @Override
    public void onDaoUnlinked(OnDaoUnlinked onDaoUnlinked) throws DaoServiceException {
        log.info("Spuštěna metoda onDaoUnlinked.");
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
        } catch (Exception e) {
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

        log.info("Vyhledávám metadata element=" + ELEMENT + " pro položku digitalizátu Uuid=" + uuId + " schema=" +
                METADATASCHEMA + ".");
        List<MetadataValue> metadataList = itemService.getMetadata(item, METADATASCHEMA, ELEMENT, null, Item.ANY);
        try {
            if (!metadataList.isEmpty()) {
                itemService.removeMetadataValues(context, item, metadataList);
            }
            itemService.addMetadata(context, item, METADATASCHEMA, ELEMENT, null, null, Boolean.FALSE.toString());
        } catch (Exception e) {
            throw new ProcessingException("Chyba při nastavení metadat  pro položku digitalizátu Uuid=" + uuId + " schema=" +
                    METADATASCHEMA + ".");
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
}
