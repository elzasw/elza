package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;

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
import cz.tacr.elza.service.cam.CamXmlFactory.EntityRefHandler;

/**
 * Builder for CAM XML
 *
 * This builder will create XML for one binding
 */
abstract public class CamXmlBuilder {

    protected final StaticDataProvider sdp;
    protected final ApAccessPoint accessPoint;

    protected final EntityRefHandler entityRefHandler;

    protected final Map<Integer, String> partUuidMap = new HashMap<>();

    CamXmlBuilder(final StaticDataProvider sdp, final ApAccessPoint accessPoint,
                  final EntityRefHandler entityRefHandler) {
        this.sdp = sdp;
        this.accessPoint = accessPoint;
        this.entityRefHandler = entityRefHandler;
    }

    protected NewItemsXml createNewItems(ApBindingItem changedPart, Collection<ApItem> itemList) {
        NewItemsXml newItems = new NewItemsXml();
        newItems.setPid(new UuidXml(changedPart.getValue()));
        newItems.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));

        createXmlItems(itemList, newItems.getItems());

        return newItems;
    }

    protected PartsXml createParts(Collection<ApPart> partList,
                                   Map<Integer, List<ApItem>> itemMap) {
        PartsXml parts = new PartsXml();

        ApPart preferPart = accessPoint.getPreferredPart();
        if (preferPart != null) {
            parts.getList().add(createPart(preferPart, itemMap));
            if (CollectionUtils.isNotEmpty(partList)) {
                for (ApPart part : partList) {
                    if (!part.getPartId().equals(preferPart.getPartId())) {
                        parts.getList().add(createPart(part, itemMap));
                    }
                }
            }
        }
        return parts;
    }

    protected List<PartXml> createPartList(Collection<ApPart> partList,
                                           Map<Integer, List<ApItem>> itemMap) {
        List<PartXml> partXmlList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(partList)) {
            for (ApPart part : partList) {
                partXmlList.add(createPart(part, itemMap));
            }
        }
        return partXmlList;
    }

    private PartXml createPart(ApPart apPart, Map<Integer, List<ApItem>> itemMap) {
        String uuid = UUID.randomUUID().toString();

        String parentUuid;
        if (apPart.getParentPart() != null) {
            parentUuid = getUuidForPart(apPart.getParentPart());
        } else {
            parentUuid = null;
        }

        PartXml part = CamXmlFactory.createPart(sdp, apPart, parentUuid, uuid);

        onPartCreated(apPart, uuid);

        Collection<ApItem> itemList = itemMap.getOrDefault(apPart.getPartId(), Collections.emptyList());
        part.setItms(createItems(apPart, itemList));
        return part;
    }

    private ItemsXml createItems(ApPart apPart, Collection<ApItem> itemList) {
        ItemsXml items = new ItemsXml();
        if (CollectionUtils.isNotEmpty(itemList)) {
            createXmlItems(itemList, items.getItems());
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
    public void createXmlItems(Collection<ApItem> itemList, List<Object> trgList) {
        for (ApItem item : itemList) {
            String uuid = UUID.randomUUID().toString();
            Object i = CamXmlFactory.createItem(sdp, item, uuid, entityRefHandler);
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