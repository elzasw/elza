package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import cz.tacr.cam.schema.cam.ItemsXml;
import cz.tacr.cam.schema.cam.NewItemsXml;
import cz.tacr.cam.schema.cam.PartTypeXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.PartsXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.cam.CamXmlFactory.EntityRefHandler;

/**
 * Builder for CAM XML
 *
 * This builder will create XML for one binding
 */
abstract public class CamXmlBuilder {

    protected final StaticDataProvider sdp;
    protected final ApAccessPoint accessPoint;
    protected final ApScope scope;

    protected final EntityRefHandler entityRefHandler;
    protected final GroovyService groovyService;

    protected final Map<Integer, String> partUuidMap = new HashMap<>();

    CamXmlBuilder(final StaticDataProvider sdp,
                  final ApAccessPoint accessPoint,
                  final EntityRefHandler entityRefHandler,
                  final GroovyService groovyService,
                  final ApScope scope) {
        this.sdp = sdp;
        this.accessPoint = accessPoint;
        this.entityRefHandler = entityRefHandler;
        this.groovyService = groovyService;
        this.scope = scope;
    }

    protected NewItemsXml createNewItems(ApBindingItem changedPart, Collection<ApItem> itemList, String externalSystemTypeCode) {
        NewItemsXml newItems = new NewItemsXml();
        newItems.setPid(new UuidXml(changedPart.getValue()));
        newItems.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));

        createXmlItems(itemList, newItems.getItems(), externalSystemTypeCode);

        return newItems;
    }

    protected PartsXml createParts(Collection<ApPart> partList,
                                   Map<Integer, List<ApItem>> itemMap,
                                   String externalSystemTypeCode) {
        // if no parts available -> return null
        if (CollectionUtils.isEmpty(partList)) {
            // schema allows empty element prts
            return null;
        }        

        Collection<ApPart> adjustedPartList = partList;
        // check if prefered part is first
        ApPart preferedPart = accessPoint.getPreferredPart();
        if (preferedPart != null) {
            // prepare list with first pref.part
            adjustedPartList = new ArrayList<>(partList.size());
            adjustedPartList.add(preferedPart);
            for (ApPart part : partList) {
                if (!part.getPartId().equals(preferedPart.getPartId())) {
                    adjustedPartList.add(part);
                }
            }
        }
        
        // if no parts available -> create item without parts
        List<PartXml> partxmlList = createPartList(adjustedPartList, itemMap, externalSystemTypeCode);
        // if no parts available -> return null
        if (CollectionUtils.isEmpty(partxmlList)) {
            // schema allows empty element prts
            return null;
        }

        PartsXml parts = new PartsXml();
        for (PartXml part : partxmlList) {
            parts.getList().add(part);
        }
        return parts;
    }

    protected List<PartXml> createPartList(Collection<ApPart> partList,
                                           Map<Integer, List<ApItem>> itemMap,
                                           String externalSystemTypeCode) {
        List<PartXml> partXmlList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                List<ApItem> partItems = itemMap.get(part.getPartId());
                if (CollectionUtils.isEmpty(partItems)) {
                    continue;
                }
                partXmlList.add(createPart(part, partItems, externalSystemTypeCode));
            }
        }
        return partXmlList;
    }

    private PartXml createPart(ApPart apPart, List<ApItem> partItems, String externalSystemTypeCode) {
        Validate.isTrue(partItems.size() > 0, "Empty part list, entityId: {}", apPart.getAccessPointId());

        String uuid = UUID.randomUUID().toString();

        String parentUuid;
        if (apPart.getParentPart() != null) {
            parentUuid = getUuidForPart(apPart.getParentPart());
        } else {
            parentUuid = null;
        }

        PartXml part = CamXmlFactory.createPart(sdp, apPart, parentUuid, uuid);

        onPartCreated(apPart, uuid);

        part.setItms(createItems(apPart, partItems, externalSystemTypeCode));
        return part;
    }

    private ItemsXml createItems(ApPart apPart, Collection<ApItem> itemList, String externalSystemTypeCode) {
        ItemsXml items = new ItemsXml();
        if (CollectionUtils.isNotEmpty(itemList)) {
            createXmlItems(itemList, items.getItems(), externalSystemTypeCode);
        }
        return items;
    }

    /**
     * Fill list of XML items
     * 
     * @param itemList
     * @param binding
     * @param sdp
     * @param trgList
     */
    public void createXmlItems(Collection<ApItem> itemList, List<Object> trgList, String externalSystemTypeCode) {
        for (ApItem item : itemList) {
            String uuid = UUID.randomUUID().toString();
            Object i = CamXmlFactory.createItem(sdp, item, uuid, entityRefHandler, groovyService, externalSystemTypeCode, scope);
            if (i != null) {
                onItemCreated(item, uuid);
                trgList.add(i);
            }
        }
    }

    /**
     * Callback when item was created
     * 
     * @param item
     * @param uuid
     */
    protected void onItemCreated(ApItem item, String uuid) {
        // nop
    }

    protected void onPartCreated(ApPart apPart, String uuid) {
        // TODO Auto-generated method stub

    }

    /**
     * Return Uuid for given part
     * 
     * @param apPart
     * 
     * @return
     */
    protected String getUuidForPart(ApPart apPart) {
        String uuid = partUuidMap.get(apPart.getPartId());
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            partUuidMap.put(apPart.getPartId(), uuid);

            onPartCreated(apPart, uuid);
        }
        return uuid;
    }
}
