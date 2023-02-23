package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApRevItemRepository;
import cz.tacr.elza.repository.DataRepository;

@Service
public class RevisionItemService {

    private final static Logger logger = LoggerFactory.getLogger(RevisionItemService.class);

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private ApRevItemRepository revItemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private EntityManager em;


    public List<ApRevItem> findByParts(List<ApRevPart> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return Collections.emptyList();
        }
        return revItemRepository.findByParts(parts);
    }

    public void deleteRevisionItems(List<ApRevItem> items, ApChange change) {
        if (CollectionUtils.isNotEmpty(items)) {
            for (ApRevItem item : items) {
                item.setDeleteChange(change);
            }
            revItemRepository.saveAll(items);
        }
    }

    /**
     * Create new items
     *
     * Items are mapped to existing one using
     * objectId and origObjectId.
     *
     * @param part
     * @param createItems
     * @param change
     * @param updateOrigItems
     *            If true objectId is stored as originalObjectId
     *            If false objectId is stored as obectId
     * @return
     */
    public List<ApRevItem> createItems(final ApRevPart part,
                                       final List<ApItemVO> createItems,
                                       final ApChange change,
                                       final boolean updateOrigItems) {
        if (createItems.isEmpty()) {
            return Collections.emptyList();
        }

        // Map for position counting
        Map<Integer, List<ApRevItem>> itemsByType = new HashMap<>();

        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        List<ApRevItem> itemsCreated = new ArrayList<>();

        for (ApItemVO createItem : createItems) {
            ItemType itemType = sdp.getItemTypeById(createItem.getTypeId());
            RulItemSpec itemSpec = accessPointItemService.getItemSpecification(itemType, createItem);
            List<ApRevItem> existsItems = itemsByType.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

            Integer positionWant = createItem.getPosition();
            int position = nextPosition(existsItems);

            if (positionWant != null) {
                Validate.isTrue(positionWant > 0);
                if (position > positionWant) {

                    List<ApRevItem> itemsToShift = findItemsGE(existsItems, positionWant);
                    List<ApRevItem> newItems = shiftItems(itemsToShift, 1, change);
                    existsItems.addAll(newItems);

                    position = positionWant;
                }
            }

            ArrData data = createItem.createDataEntity(em);
            dataToSave.add(data);

            itemService.checkItemLengthLimit(itemType.getEntity(), data);

            Integer origObjectId = createItem.getOrigObjectId();
            Integer objectId = createItem.getObjectId();
            if (origObjectId == null && objectId == null) {
                objectId = accessPointItemService.nextItemObjectId();
            } else
            if (updateOrigItems) {
                if (createItem.getObjectId() != null) {
                    // objectId is stored as originalObjectId
                    origObjectId = createItem.getObjectId();
                    objectId = null;
                }
            }

            ApRevItem itemCreated = createItem(part, data, itemType.getEntity(), itemSpec, change,
                                               objectId, position, origObjectId, false);
            itemsCreated.add(itemCreated);

            existsItems.add(itemCreated);

        }

        dataRepository.saveAll(dataToSave);
        revItemRepository.saveAll(itemsCreated);
        logger.debug("Items created, ItemIds: {}", itemsCreated.stream().map(ApRevItem::getItemId).collect(Collectors.toList()));
        return itemsCreated;
    }

    private ApRevItem createItem(final ApRevPart part,
                                final ArrData data,
                                 final RulItemType it,
                                 final RulItemSpec is,
                                 final ApChange c,
                                 @Nullable final Integer objectId,
                                 final int position,
                                 @Nullable Integer origObjectId,
                                 final boolean deleted) {
        if (!((origObjectId == null) ^ (objectId == null))) {
            throw new BusinessException("only originalObjectId or objectId has to be set (not both)",
                    BaseCode.INVALID_STATE);
        }

        ApRevItem item = new ApRevItem();
        item.setData(data);
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        item.setPart(part);
        item.setOrigObjectId(origObjectId);
        item.setDeleted(deleted);
        return item;
    }

    public void createItems(List<ApRevPart> createdParts,
                            Map<Integer, List<ApRevItem>> revItemMap) {
        List<ApItem> createdItems = new ArrayList<>();
        List<ArrData> dataList = new ArrayList<>();
        for (ApRevPart revPart : createdParts) {
            List<ApRevItem> revItems = revItemMap.get(revPart.getPartId());

            for (ApRevItem revItem : revItems) {
                if (!revItem.isDeleted() && revItem.getData() != null) {
                    ArrData newData = revItem.getData().makeCopy();
                    dataList.add(newData);
                    createdItems.add(accessPointItemService.createItem(revPart.getOriginalPart(),
                            newData, revItem.getItemType(), revItem.getItemSpec(),
                            revItem.getCreateChange(), revItem.getObjectId(), revItem.getPosition()));
                }
            }
        }

        dataRepository.saveAll(dataList);
        itemRepository.saveAll(createdItems);
    }

    /**
     * Merge items to updated parts
     *
     * @param accessPoint
     * @param change
     * @param revParts
     * @param revPartMap
     * @param revItems
     */
    public void mergeItems(ApAccessPoint accessPoint,
                           ApChange change,
                           List<ApRevPart> revParts,
                           Map<Integer, ApPart> revPartMap,
                           List<ApRevItem> revItems) {
        List<ApItem> items = itemRepository.findValidItemsByAccessPoint(accessPoint);
        Map<Integer, ApItem> itemObjectIdMap = items.stream()
                .collect(Collectors.toMap(ApItem::getObjectId, Function.identity()));

        List<ApBindingItem> bindingItemList = bindingItemRepository.findByItems(items);

        Map<Integer, List<ApRevItem>> revItemPartMap = revItems.stream()
                .collect(Collectors.groupingBy(ApRevItem::getPartId));

        List<ApItem> itemsList = new ArrayList<>();
        List<ArrData> dataList = new ArrayList<>();
        List<ApItem> deletedItems = new ArrayList<>();
        // List of IDS of updated items with new ones
        Map<Integer, ApItem> updatedItems = new HashMap<>();

        for (ApRevPart revPart : revParts) {
            ApPart targetPart = revPartMap.get(revPart.getPartId());
            Validate.notNull(targetPart, "Part not saved, revPartId: %s", revPart.getPartId());

            List<ApRevItem> revPartItems = revItemPartMap.get(revPart.getPartId());
            if (CollectionUtils.isEmpty(revPartItems)) {
                continue;
            }

            for (ApRevItem revItem : revPartItems) {
                Validate.isTrue(revItem.getDeleteChange() == null);

                ApItem currItem;
                Integer origObjectId = revItem.getOrigObjectId();
                Integer objectId;
                if (origObjectId != null) {
                    // get current ApItem
                    currItem = itemObjectIdMap.get(origObjectId);
                    Validate.notNull(currItem, "Source item not found, objectId: %s", origObjectId);
                    Validate.isTrue(revItem.getObjectId() == null, "objectId has to be null for update");

                    objectId = origObjectId;

                    if (revItem.isDeleted()) {
                        // delete item
                        deletedItems.add(currItem);
                        continue;
                    }

                } else {
                    Validate.notNull(revItem.getObjectId());
                    Validate.isTrue(!revItem.isDeleted());

                    objectId = revItem.getObjectId();
                    currItem = null;
                }

                ArrData newData = revItem.getData().makeCopy();
                dataList.add(newData);
                ApItem newItem = accessPointItemService.createItem(targetPart,
                                                                   newData,
                                                                   revItem.getItemType(),
                                                                   revItem.getItemSpec(),
                                                                   revItem.getCreateChange(),
                                                                   objectId,
                                                                   revItem.getPosition());
                itemsList.add(newItem);

                if (origObjectId != null) {
                    // update item
                    Validate.notNull(currItem, "Source item not found, objectId: %s", origObjectId);
                    currItem.setDeleteChange(revItem.getCreateChange());
                    itemsList.add(currItem);

                    // Add to binding map
                    updatedItems.put(currItem.getItemId(), newItem);
                }
            }

        }

        // Mark old items as deleted
        // Due to DB constrain have to be done before new items are in place
        for (ApItem deleteItem : deletedItems) {
            deleteItem.setDeleteChange(change);
        }
        deletedItems = itemRepository.saveAll(deletedItems);
        itemRepository.flush();

        // Save new items
        dataRepository.saveAll(dataList);
        itemRepository.saveAll(itemsList);

        accessPointItemService.changeBindingItemsItems(updatedItems, bindingItemList);

        bindingItemRepository.flush();
        // delete items
        accessPointItemService.deleteItems(deletedItems, change);
        bindingItemRepository.flush();

    }

    private int nextPosition(final List<ApRevItem> existsItems) {
        if (existsItems.size() == 0) {
            return 1;
        }
        int position = 2;
        for (ApRevItem existsItem : existsItems) {
            if (existsItem.getDeleteChange() == null) {
                if (existsItem.getPosition() >= position) {
                    position = existsItem.getPosition() + 1;
                }
            }
        }
        return position;
    }

    private List<ApRevItem> findItemsGE(final List<ApRevItem> items, final int position) {
        List<ApRevItem> result = new ArrayList<>();
        for (ApRevItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() >= position) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<ApRevItem> shiftItems(final List<ApRevItem> items, final int diff, final ApChange change) {
        List<ApRevItem> newItems = new ArrayList<>();
        for (ApRevItem item : items) {
            if (item.getItemId() == null) {
                item.setPosition(item.getPosition() + diff);
            } else {
                ApRevItem newItem = item.copy();
                newItem.setCreateChange(change);
                newItem.setPosition(item.getPosition() + diff);
                newItems.add(newItem);

                item.setDeleteChange(change);
            }
        }
        return newItems;
    }

    public List<ApRevItem> findByPart(ApRevPart revPart) {
        return revItemRepository.findByPart(revPart);
    }

    public void createDeletedItems(ApRevPart revPart, ApChange apChange, List<ApItem> apItems) {
        if (CollectionUtils.isNotEmpty(apItems)) {
            List<ApRevItem> revItems = new ArrayList<>();
            for (ApItem apItem : apItems) {
                revItems.add(createItem(revPart, null, apItem.getItemType(), apItem.getItemSpec(), apChange,
                                        null, apItem.getPosition(), apItem.getObjectId(), true));
            }
            revItemRepository.saveAll(revItems);
        }
    }

    public boolean allItemsDeleted(List<ApRevItem> revItems, List<ApItem> apItems) {
        if (CollectionUtils.isNotEmpty(revItems)) {
            for (ApRevItem revItem : revItems) {
                if (!revItem.isDeleted()) {
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(apItems)) {
            for (ApItem apItem : apItems) {
                ApRevItem revItem = findRevItem(revItems, apItem);
                if (revItem == null || !revItem.isDeleted()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    private ApRevItem findRevItem(List<ApRevItem> revItems, ApItem apItem) {
        if (CollectionUtils.isNotEmpty(revItems)) {
            for (ApRevItem revItem : revItems) {
                if (revItem.getOrigObjectId() != null && revItem.getOrigObjectId().equals(apItem.getObjectId())) {
                    return revItem;
                }
            }
        }
        return null;
    }

    /**
     * Update revItem with new value
     *
     * @param change
     * @param revItem
     * @param drr
     * @return
     */
    public ApRevItem updateItem(ApChange change,
                                ApRevItem revItem, ArrData data) {
        Validate.isTrue(revItem.getDeleteChange() == null);

        revItem.setDeleteChange(change);

        revItem = revItemRepository.saveAndFlush(revItem);

        data = dataRepository.save(data);

        ApRevItem newItem = createItem(revItem.getPart(), data,
                                       revItem.getItemType(), revItem.getItemSpec(),
                                       change,
                                       revItem.getObjectId(),
                                       revItem.getPosition(),
                                       revItem.getOrigObjectId(),
                                       revItem.isDeleted());
        return revItemRepository.saveAndFlush(newItem);
    }

    public ApRevItem createItem(ApChange change, ApRevPart revPart, ApItem apItem, ArrData data) {
        data = dataRepository.save(data);

        ApRevItem newItem = createItem(revPart, data,
                                       apItem.getItemType(),
                                       apItem.getItemSpec(),
                                       change,
                                       null,
                                       apItem.getPosition(),
                                       apItem.getObjectId(),
                                       false);
        return revItemRepository.saveAndFlush(newItem);
    }
}
