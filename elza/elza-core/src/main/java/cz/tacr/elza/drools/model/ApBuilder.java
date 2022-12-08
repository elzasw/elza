package cz.tacr.elza.drools.model;

import static cz.tacr.elza.exception.codes.BaseCode.INVALID_STATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.drools.model.item.AbstractItem;
import cz.tacr.elza.drools.model.item.BoolItem;
import cz.tacr.elza.drools.model.item.IntItem;
import cz.tacr.elza.drools.model.item.Item;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

public class ApBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ApBuilder.class);

    private Integer id;
    private Integer preferredPartId;
    private String aeType;
    final private List<Part> parts = new ArrayList<>();
    /**
     * Map if partId to Part
     */
    final private Map<Integer, Part> partIdMap = new HashMap<>();
    /**
     * Map of revPartId to Part
     */
    final private Map<Integer, Part> revPartIdMap = new HashMap<>();

    final private Map<Integer, AbstractItem> objectIdItemMap = new HashMap<>();

    private StaticDataProvider sdp;

    private static final String IDN_VALUE = "IDN_VALUE";
    private static final String IDN_TYPE = "IDN_TYPE";
    private static final String REL_ENTITY = "REL_ENTITY";

    public ApBuilder(StaticDataProvider data) {
        this.sdp = data;
    }

    /**
     * Create new AccessPoint for drools
     * 
     * Accesspoint might have all fields null.
     * 
     * @return
     */
    public Ap build() {
        Ap ap = new Ap(id, aeType, parts);
        return ap;
    }

    private AbstractItem createItem(ApItem item) {
        cz.tacr.elza.core.data.ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
        DataType dataType = itemType.getDataType();
        String itemSpecCode = item.getItemSpecId() != null ? itemType.getItemSpecById(item.getItemSpecId()).getCode() : null;

        return createItem(item.getObjectId(), item.getItemId(), dataType, itemType, itemSpecCode, item.getData());
    }

    private AbstractItem createItem(ApRevItem revItem) {
        cz.tacr.elza.core.data.ItemType itemType = sdp.getItemTypeById(revItem.getItemTypeId());
        DataType dataType = itemType.getDataType();
        String itemSpecCode = revItem.getItemSpecId() != null ? itemType.getItemSpecById(revItem.getItemSpecId()).getCode() : null;
        int objectId = revItem.getObjectId() != null ? revItem.getObjectId() : -revItem.getItemId();  

        return createItem(objectId, null, dataType, itemType, itemSpecCode, revItem.getData());
    }

    private AbstractItem createItem(Integer objectId,
                                    Integer itemId,
                                    DataType dataType,
                                    cz.tacr.elza.core.data.ItemType itemType,
                                    String itemSpecCode,
                                    ArrData data) {

        String itemTypeCode = itemType.getCode();
        AbstractItem abstractItem;
        switch (dataType) {
        case ENUM:
            abstractItem = new Item(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), itemSpecCode);
            break;
        case BIT:
            ArrDataBit aeDataBit = (ArrDataBit) data;
            abstractItem = new BoolItem(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), aeDataBit
                    .getValueBoolean());
            break;
        case RECORD_REF:
            ArrDataRecordRef aeDataRecordRef = (ArrDataRecordRef) data;
            abstractItem = new IntItem(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), aeDataRecordRef
                    .getRecordId());
            break;
        case COORDINATES:
            ArrDataCoordinates aeDataCoordinates = (ArrDataCoordinates) data;
            abstractItem = new Item(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), GeometryConvertor
                    .convert(aeDataCoordinates.getValue()));
            break;
        case INT:
            ArrDataInteger aeDataInteger = (ArrDataInteger) data;
            abstractItem = new IntItem(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), aeDataInteger
                    .getValueInt());
            break;
        case STRING:
            ArrDataString aeDataString = (ArrDataString) data;
            abstractItem = new Item(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), aeDataString
                    .getStringValue());
            break;
        case TEXT:
            ArrDataText aeDataText = (ArrDataText) data;
            abstractItem = new Item(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), aeDataText
                    .getTextValue());
            break;
        case UNITDATE:
            ArrDataUnitdate aeDataUnitdate = (ArrDataUnitdate) data;
            abstractItem = new Item(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), UnitDateConvertor
                    .convertToString(aeDataUnitdate));
            break;
        case URI_REF:
            ArrDataUriRef arrDataUriRef = (ArrDataUriRef) data;
            abstractItem = new Item(itemId, itemTypeCode, itemSpecCode, dataType.getCode(), arrDataUriRef
                    .getUriRefValue());
            break;
        default:
            throw new SystemException("Invalid data type (RulItemType.DataType) " + dataType.getCode(), INVALID_STATE);
        }

        AbstractItem prevAbstrItem = objectIdItemMap.put(objectId, abstractItem);
        if (prevAbstrItem != null) {
            logger.error("Item nesmí být dvakrát, objectId: {}, itemId: {}, dataType: {}", objectId, itemId, dataType);
            throw new SystemException("Item can't be twice, objectId: " + objectId + ", itemId: " + itemId + ", fataType: " + dataType, INVALID_STATE);
        }

        return abstractItem;
    }

    private Part createPart(ApRevPart revPart, List<ApRevItem> revItems, boolean preferred) {
        List<AbstractItem> itemList;
        if (revItems != null) {
            itemList = new ArrayList<>(revItems.size());
            for (ApRevItem revItem : revItems) {
                if (revItem.isDeleted() || revItem.getDeleteChangeId() != null) {
                    continue;
                }
                AbstractItem item = createItem(revItem);
                itemList.add(item);
            }
        } else {
            itemList = Collections.emptyList();
        }

        Integer parentPartId = revPart.getParentPartId();
        Part result = new Part(null, parentPartId,
                PartType.fromValue(sdp.getPartTypeById(revPart.getPartTypeId()).getCode()),
                itemList, null, preferred);
        this.parts.add(result);
        this.revPartIdMap.put(revPart.getPartId(), result);
        return result;
    }

    private Part createPart(CachedPart part) {
        List<ApItem> itemList = part.getItems();
        List<AbstractItem> abstractItemList = new ArrayList<>();

        for (ApItem item : itemList) {
            abstractItemList.add(createItem(item));
        }

        Integer parentPartId = part.getParentPartId();
        boolean preferred = part.getPartId().equals(preferredPartId);
        Part result = new Part(part.getPartId(), parentPartId, PartType.fromValue(part.getPartTypeCode()),
                abstractItemList, null, preferred);
        this.parts.add(result);
        this.partIdMap.put(part.getPartId(), result);
        return result;
    }

    private Part createPart(ApPart apPart, List<ApItem> itemList) {
        List<AbstractItem> abstractItemList = new ArrayList<>();

        for (ApItem item : itemList) {
            if (Objects.equals(apPart.getPartId(), item.getPartId())) {
                abstractItemList.add(createItem(item));
            }
        }

        Integer parentPartId = apPart.getParentPartId();
        boolean preferred = apPart.getPartId().equals(preferredPartId);
        Part result = new Part(apPart.getPartId(), parentPartId,
                PartType.fromValue(sdp.getPartTypeById(apPart.getPartTypeId()).getCode()),
                abstractItemList, null, preferred);
        this.parts.add(result);
        this.partIdMap.put(result.getId(), result);
        return result;
    }

    public void setAccessPoint(CachedAccessPoint cachedAcessPoint) {
        
        id = cachedAcessPoint.getAccessPointId();
        preferredPartId = cachedAcessPoint.getPreferredPartId();

        setAeType(cachedAcessPoint.getApState().getApTypeId());

        for (CachedPart part : cachedAcessPoint.getParts()) {
            createPart(part);
        }

        fillParentParts();
        
    }

    public void setAccessPoint(ApState apState, List<ApPart> apParts, List<ApItem> itemList) {
        id = apState.getAccessPointId();
        preferredPartId = apState.getAccessPoint().getPreferredPartId();
        setAeType(apState.getApTypeId());

        for (ApPart apPart : apParts) {
            createPart(apPart, itemList);
        }

        fillParentParts();
    }

    private void fillParentParts() {
        for (Part part : parts) {
            if (part.getParentPartId() != null) {
                Part parentPart = partIdMap.get(part.getParentPartId());
                Validate.notNull(parentPart);
                part.setParent(parentPart);
            }
        }
    }

    public void setAeType(Integer apTypeId) {
        this.aeType = sdp.getApTypeById(apTypeId).getCode();
    }

    public Part getPart(Integer partId) {
        return partIdMap.get(partId);
    }

    public Map<Integer, Map<String, Relation>> createRelationMap() {
        Map<Integer, Map<String, Relation>> partRelationSpecMap = new HashMap<>();
        for (Part part : parts) {
            if (part.getType().equals(PartType.PT_REL)) {
                Integer key = part.getParent() != null ? part.getParent().getId() : -1;
                Map<String, Relation> relationSpecCount = partRelationSpecMap.computeIfAbsent(key,
                                                                                              k -> new HashMap<>());
                for (AbstractItem abstractItem : part.getItems()) {
                    if (abstractItem.getType().equals(REL_ENTITY)) {
                        Relation relation = relationSpecCount.get(abstractItem.getSpec());
                        if (relation == null) {
                            relation = new Relation(part);
                            relationSpecCount.put(abstractItem.getSpec(), relation);
                        } else {
                            relation.addPart(part);
                        }
                        break;
                    }
                }
            }
        }
        return partRelationSpecMap;
    }

    public Map<String, Integer> createIdentMap() {
        Map<String, Integer> identMap = new HashMap<>();
        for (Part part : parts) {
            if (part.getType().equals(PartType.PT_IDENT)) {
                for (AbstractItem abstractItem : part.getItems()) {
                    if (abstractItem.getType().equals(IDN_TYPE)) {
                        identMap.put(abstractItem.getSpec(), identMap.getOrDefault(abstractItem.getSpec(), 0) + 1);
                    }
                }
            }
        }
        return identMap;
    }

    public List<AbstractItem> createAbstractItemList() {
        List<AbstractItem> items = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(parts)) {
            for (Part part : parts) {
                if (CollectionUtils.isNotEmpty(part.getItems())) {
                    items.addAll(part.getItems());
                }
            }
        }
        return items;
    }

    public void setRevision(ApRevision revision, List<ApRevPart> revParts, List<ApRevItem> revItems) {
        // sort items to parts
        Map<Integer, List<ApRevItem>> itemPartMap = revItems.stream()
                .collect(Collectors.groupingBy(i -> i.getPartId(),
                                               Collectors.mapping(Function.identity(), Collectors.toList())));

        // parts with new parents
        List<ApRevPart> subpartsToCreate = new ArrayList<>();

        for (ApRevPart revPart : revParts) {
            if (revPart.getDeleteChangeId() != null) {
                // part is deleted
                if (revPart.getOriginalPartId() == null) {
                    // non original part -> simply skip
                } else {
                    // remove part and all its items
                    Part removePart = partIdMap.remove(revPart.getOriginalPartId());
                    parts.remove(removePart);
                }
                continue;
            }

            Part part;
            List<ApRevItem> items = itemPartMap.get(revPart.getPartId());
            // Update original part (if originalPartId is set)
            if (revPart.getOriginalPartId() != null) {
                part = partIdMap.get(revPart.getOriginalPartId());
                updatePart(part, items);
            } else {
                // new part
                // is new subpart of old part
                if (revPart.getParentPartId() != null) {
                    part = createPart(revPart, items, false);
                } else 
                // is new main part
                if(revPart.getRevParentPartId()==null) {
                    part = createPart(revPart, items, false);
                } else {
                    part = null;
                    // subpart of new main part - process in next run
                    subpartsToCreate.add(revPart);
                }
            }

            // reset pref part
            if (part != null && Objects.equals(revision.getRevPreferredPartId(), revPart.getPartId())) {
                if (this.preferredPartId != null) {
                    Part prefPart = this.partIdMap.get(this.preferredPartId);
                    prefPart.setPreferred(false);
                    preferredPartId = null;
                    part.setPreferred(true);
                }
            }
        }
        
        // create sub parts
        for (ApRevPart revPart : subpartsToCreate) {
            List<ApRevItem> items = itemPartMap.get(revPart.getPartId());
            createPart(revPart, items, false);
        }

    }

    private void updatePart(Part part, List<ApRevItem> revItems) {
        if (revItems == null) {
            return;
        }

        for (ApRevItem revItem : revItems) {
            Integer origObjectId = revItem.getOrigObjectId();
            //
            List<AbstractItem> itemList = part.getItems();
            if(origObjectId!=null) {
                // item is updated
                AbstractItem prevItem = objectIdItemMap.remove(origObjectId);
                itemList.remove(prevItem);

                if(revItem.isDeleted()) {
                    // deleted - nothing to add
                    continue;
                }
            }
            // add new item
            itemList.add(createItem(revItem));
        }

    }

    public Part getPartByRevPartId(Integer revPartId) {
        return revPartIdMap.get(revPartId);
    }
}
