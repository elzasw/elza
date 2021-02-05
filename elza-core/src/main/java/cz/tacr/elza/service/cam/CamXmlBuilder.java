package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.service.AccessPointDataService;
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
    protected final AccessPointDataService apDataService;

    protected final Map<Integer, String> partUuidMap = new HashMap<>();

    CamXmlBuilder(final StaticDataProvider sdp,
                  final ApAccessPoint accessPoint,
                  final EntityRefHandler entityRefHandler,
                  final GroovyService groovyService,
                  final AccessPointDataService apDataService,
                  final ApScope scope) {
        this.sdp = sdp;
        this.accessPoint = accessPoint;
        this.entityRefHandler = entityRefHandler;
        this.groovyService = groovyService;
        this.apDataService = apDataService;
        this.scope = scope;
    }

    protected NewItemsXml createNewItems(ApBindingItem changedPart, Collection<ApItem> itemList, String externalSystemTypeCode) {
        NewItemsXml newItems = new NewItemsXml();
        newItems.setPid(new UuidXml(changedPart.getValue()));
        newItems.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));

        createXmlItems(itemList, newItems.getItems(), externalSystemTypeCode);
        if (newItems.getItems().size() == 0) {
            return null;
        }

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
        if (CollectionUtils.isEmpty(partList)) {
            return Collections.emptyList();
        }

        // collection of removed parts from export
        Set<String> ignoredParts = new HashSet<>();

        List<PartXml> partXmlList = new ArrayList<>();
        for (ApPart part : partList) {
            List<ApItem> srcPartItems = itemMap.get(part.getPartId());

            // filter parts
            List<ApItem> partItems = filterOutItemsWithoutExtSysMapping(srcPartItems, externalSystemTypeCode);
            if (CollectionUtils.isNotEmpty(srcPartItems) && CollectionUtils.isEmpty(partItems)) {
                ignoredParts.add(getUuidForPart(part));
                continue;
            }
            PartXml partXml = createPart(part, partItems, externalSystemTypeCode);
            if (partXml == null) {
                continue;
            }
            partXmlList.add(partXml);
        }
        
        // filter subparts
        return partXmlList.stream()
                .filter(p -> p.getPrnt() == null || (!ignoredParts.contains(p.getPrnt().getValue())))
                .collect(Collectors.toList());
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

        ItemsXml itemsXml = createItems(apPart, partItems, externalSystemTypeCode);

        // check if anything to export
        if (itemsXml.getItems().size() == 0) {
            return null;
        }

        part.setItms(itemsXml);
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
            Object i = CamXmlFactory.createItem(sdp, item, uuid, entityRefHandler, groovyService, apDataService, externalSystemTypeCode, scope);
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

    /**
     * Metoda odfiltruje itemy, které nemají mapování v externím systému
     *
     * @param itemList itemy k filtrování
     * @param externalSystemTypeCode kód typu externího systému
     * @return kolekce itemů k poslání do externího systému
     */
    protected List<ApItem> filterOutItemsWithoutExtSysMapping(List<ApItem> itemList, String externalSystemTypeCode) {
        List<ApItem> filteredItems = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(itemList)) {
            for (ApItem item : itemList) {
                if (doesItemHaveExtSysMapping(item, externalSystemTypeCode)) {
                    filteredItems.add(item);
                }
            }
        }

        return filteredItems;
    }

    /**
     * Metoda odfiltruje itemy, které nemají mapování v externím systému
     *
     * @param itemList itemy k filtrování
     * @param externalSystemTypeCode kód typu externího systému
     * @return kolekce itemů k poslání do externího systému
     */
    protected List<ApBindingItem> filterOutBindingItemsWithoutExtSysMapping(List<ApBindingItem> itemList, String externalSystemTypeCode) {
        List<ApBindingItem> filteredItems = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(itemList)) {
            for (ApBindingItem bindingItem : itemList) {
                ApItem item = bindingItem.getItem();
                if (doesItemHaveExtSysMapping(item, externalSystemTypeCode)) {
                    filteredItems.add(bindingItem);
                }
            }
        }

        return filteredItems;
    }

    /**
     * Metoda ověří zda-li má typ a specifikace itemu mapování do externího systému
     *
     * @param item item k filtrování
     * @param externalSystemTypeCode kód typu externího systému
     * @return
     */
    protected boolean doesItemHaveExtSysMapping(ApItem item, String externalSystemTypeCode) {
        ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());

        String camItemTypeCode = groovyService.findItemTypeCode(externalSystemTypeCode, itemType.getCode(), scope.getRuleSetId());
        if (camItemTypeCode == null) {
            return false;
        }

        if (item.getItemSpecId() != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(item.getItemSpecId());
            String camItemSpecCode = groovyService.findItemSpecCode(externalSystemTypeCode, itemSpec.getCode(), scope.getRuleSetId());
            if (camItemSpecCode == null) {
                return false;
            }
        }

        return true;
    }
}
