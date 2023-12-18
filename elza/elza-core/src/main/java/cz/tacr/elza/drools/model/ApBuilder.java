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

import cz.tacr.elza.common.db.HibernateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevState;
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
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.drools.model.item.AbstractItem;
import cz.tacr.elza.drools.model.item.BoolItem;
import cz.tacr.elza.drools.model.item.CoordinatesItem;
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
        RulItemSpec itemSpec = item.getItemSpecId() != null ? itemType.getItemSpecById(item.getItemSpecId()) : null;

        return createItem(item.getObjectId(), item.getItemId(), itemType, itemSpec, item.getData());
    }

    private AbstractItem createItem(ApRevItem revItem) {
        cz.tacr.elza.core.data.ItemType itemType = sdp.getItemTypeById(revItem.getItemTypeId());
        RulItemSpec itemSpec = revItem.getItemSpecId() != null ? itemType.getItemSpecById(revItem.getItemSpecId())
                : null;
        int objectId = revItem.getObjectId() != null ? revItem.getObjectId() : -revItem.getItemId();

        return createItem(objectId, null, itemType, itemSpec, revItem.getData());
    }

    private AbstractItem createItem(Integer objectId,
                                    Integer itemId,
                                    cz.tacr.elza.core.data.ItemType itemType,
                                    RulItemSpec itemSpec,
                                    ArrData data) {
        AbstractItem abstractItem;
        data = (ArrData) Hibernate.unproxy(data);
        // TODO data.createDroolsItem() vs switch
        switch (itemType.getDataType()) {
        case ENUM:
            abstractItem = new Item(itemId, itemType, itemSpec, (itemSpec != null) ? itemSpec.getCode() : null);
            break;
        case BIT:
            ArrDataBit aeDataBit = (ArrDataBit) data;
            abstractItem = new BoolItem(itemId, itemType, itemSpec, aeDataBit.getValueBoolean());
            break;
        case RECORD_REF:
            ArrDataRecordRef aeDataRecordRef = (ArrDataRecordRef) data;
            abstractItem = new IntItem(itemId, itemType, itemSpec, aeDataRecordRef.getRecordId());
            break;
        case COORDINATES:
            ArrDataCoordinates aeDataCoordinates = (ArrDataCoordinates) data;
            abstractItem = new CoordinatesItem(itemId, itemType, itemSpec, aeDataCoordinates.getValue());
            break;
        case INT:
            ArrDataInteger aeDataInteger = (ArrDataInteger) data;
            abstractItem = new IntItem(itemId, itemType, itemSpec, aeDataInteger.getValueInt());
            break;
        case STRING:
            ArrDataString aeDataString = (ArrDataString) data;
            abstractItem = new Item(itemId, itemType, itemSpec, aeDataString.getStringValue());
            break;
        case TEXT:
            ArrDataText aeDataText = (ArrDataText) data;
            abstractItem = new Item(itemId, itemType, itemSpec, aeDataText.getTextValue());
            break;
        case UNITDATE:
            ArrDataUnitdate aeDataUnitdate = (ArrDataUnitdate) data;
            abstractItem = new Item(itemId, itemType, itemSpec, UnitDateConvertor.convertToString(aeDataUnitdate));
            break;
        case URI_REF:
            ArrDataUriRef arrDataUriRef = (ArrDataUriRef) data;
            abstractItem = new Item(itemId, itemType, itemSpec, arrDataUriRef.getUriRefValue());
            break;
        default:
            throw new SystemException("Invalid data type (RulItemType.DataType) " + itemType.getDataType().getCode(),
                    INVALID_STATE);
        }

        AbstractItem prevAbstrItem = objectIdItemMap.put(objectId, abstractItem);
        if (prevAbstrItem != null) {
            logger.error("Item nesmí být dvakrát, objectId: {}, itemId: {}", objectId, itemId);
            throw new SystemException("Item can't be twice, objectId: " + objectId + ", itemId: " + itemId,
                    INVALID_STATE);
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

        Part parentPart = getParentPart(revPart);
        Part result = new Part(revPart.getPartId(),
                               PartType.fromValue(sdp.getPartTypeById(revPart.getPartTypeId()).getCode()),
                               itemList, parentPart, preferred);
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

        boolean preferred = part.getPartId().equals(preferredPartId);
        Part result = new Part(part.getPartId(),
                               PartType.fromValue(part.getPartTypeCode()),
                abstractItemList, null, preferred);
        this.parts.add(result);
        this.partIdMap.put(part.getPartId(), result);
        return result;
    }

    /**
     * Create part and store it in the map
     * 
     * Maps are created without parent. Use later method to
     * set parent mapping.
     * 
     * @param apPart
     * @param itemList
     * @return
     */
    private Part createPart(ApPart apPart, List<ApItem> itemList) {
        List<AbstractItem> abstractItemList = new ArrayList<>();

        for (ApItem item : itemList) {
            if (Objects.equals(apPart.getPartId(), item.getPartId())) {
                abstractItemList.add(createItem(item));
            }
        }

        boolean preferred = apPart.getPartId().equals(preferredPartId);
        Part result = new Part(apPart.getPartId(),
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

        // Fill parent parts
        for (CachedPart srcPart : cachedAcessPoint.getParts()) {
            fillParentPart(srcPart.getPartId(), srcPart.getParentPartId());
        }
    }

    public void setAccessPoint(ApState apState, List<ApPart> apParts, List<ApItem> itemList) {
        id = apState.getAccessPointId();
        preferredPartId = apState.getAccessPoint().getPreferredPartId();
        setAeType(apState.getApTypeId());

        for (ApPart apPart : apParts) {
            createPart(apPart, itemList);
        }

        // Fill parent parts
        for (ApPart srcPart : apParts) {
            fillParentPart(srcPart.getPartId(), srcPart.getParentPartId());
        }
    }

    /**
     * Fill parts with parent links
     * 
     * Method is used after all parts are ready
     * 
     * @param partId
     * @param parentPartId
     */
    private void fillParentPart(Integer partId, Integer parentPartId) {
        if (parentPartId == null) {
            return;
        }

        Part part = partIdMap.get(partId);
        Validate.notNull(part, "Part not found, partId: %s", partId);
        Part parentPart = partIdMap.get(parentPartId);
        Validate.notNull(parentPart, "Parent part not found, parentPartId: %s", parentPartId);
        part.setParent(parentPart);
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

    public void setRevision(ApRevState revState, List<ApRevPart> revParts, List<ApRevItem> revItems) {
        // set apType if exists
        if (revState.getTypeId() != null) {
            setAeType(revState.getTypeId());
        }

        // sort items to parts
        Map<Integer, List<ApRevItem>> itemPartMap = revItems.stream()
                .collect(Collectors.groupingBy(i -> i.getPartId(),
                                               Collectors.mapping(Function.identity(), Collectors.toList())));

        // parts with new parents
        List<ApRevPart> subpartsToCreate = new ArrayList<>();

        for (ApRevPart revPart : revParts) {
            if (revPart.getDeleteChangeId() != null || Boolean.TRUE.equals(revPart.isDeleted())) {
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
                } else {
                    // is new main part
                    if (revPart.getRevParentPartId() == null) {
                        part = createPart(revPart, items, false);
                    } else {
                        part = null;
                        // subpart of new main part - process in next run
                        subpartsToCreate.add(revPart);
                    }
                }
            }

            // reset pref part
            if (part != null && Objects.equals(revState.getRevPreferredPartId(), revPart.getPartId())) {
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
            if (origObjectId != null) {
                // item is updated
                AbstractItem prevItem = objectIdItemMap.remove(origObjectId);
                itemList.remove(prevItem);

                if (revItem.isDeleted()) {
                    // deleted - nothing to add
                    continue;
                }
            }
            // add new item
            itemList.add(createItem(revItem));
        }
    }

    private Part getParentPart(ApRevPart revPart) {
        Integer parentPartId = revPart.getParentPartId();
        Integer revParentPartId = revPart.getRevParentPartId();

        Part result = null;
        if (parentPartId != null) {
            result = partIdMap.get(parentPartId);
            Validate.notNull(result, "Missing parent part, parentPartId: %s", parentPartId);
        } else
        if (revParentPartId != null) {
            result = revPartIdMap.get(revParentPartId);
            Validate.notNull(result, "Missing rev. parent part, revParentPartId: %s", revParentPartId);
        }
        return result;
    }

    public Part getPartByRevPartId(Integer revPartId) {
        return revPartIdMap.get(revPartId);
    }
}
