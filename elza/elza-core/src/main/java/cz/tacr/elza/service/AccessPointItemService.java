package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.DataRepository;

@Service
public class AccessPointItemService {

    private final static Logger log = LoggerFactory.getLogger(AccessPointItemService.class);

    private static final String OBJECT_ID_SEQUENCE_NAME = "ap_item|object_id";

    private final EntityManager em;
    private final StaticDataService staticDataService;
    private final ApItemRepository itemRepository;
    private final DataRepository dataRepository;
    private final SequenceService sequenceService;
    private final ExternalSystemService externalSystemService;
    private final ApBindingItemRepository bindingItemRepository;
    private final ItemService itemService;

    public static class DeletedItems {

        List<ApItem> items;
        List<ApBindingItem> bindings;

        public DeletedItems(List<ApItem> items, List<ApBindingItem> bindings) {
            super();
            this.items = items;
            this.bindings = bindings;
        }

        public List<ApItem> getItems() {
            return items;
        }

        public List<ApBindingItem> getBindings() {
            return bindings;
        }
    };

    /**
     * Record ref and its value
     * 
     *
     */
    public static class ReferencedEntities {
        /**
         * Data item referencing entity
         */
        ArrDataRecordRef data;

        /**
         * External entity identifier
         * 
         * Stored in binding
         */
        String entityIdentifier;

        public ReferencedEntities(ArrDataRecordRef data, String entityIdentifier) {
            super();
            this.data = data;
            this.entityIdentifier = entityIdentifier;
        }

        public ArrDataRecordRef getData() {
            return data;
        }

        public String getEntityIdentifier() {
            return entityIdentifier;
        }

    }

    public AccessPointItemService(final EntityManager em,
                                  final StaticDataService staticDataService,
                                  final ApItemRepository itemRepository,
                                  final DataRepository dataRepository,
                                  final SequenceService sequenceService,
                                  final ExternalSystemService externalSystemService,
                                  final ApBindingItemRepository bindingItemRepository,
                                  final ItemService itemService) {
        this.em = em;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
        this.sequenceService = sequenceService;
        this.externalSystemService = externalSystemService;
        this.bindingItemRepository = bindingItemRepository;
        this.itemService = itemService;
    }

    // TODO: Kandidat na vymazani
    @FunctionalInterface
    public interface CreateFunction {
        ApItem apply(final RulItemType itemType, final RulItemSpec itemSpec, final ApChange change, final int objectId,
                     final int position);
    }

    /**
     * Odstraní všechny atributy části.
     *
     * @param part
     *            část
     * @param change
     *            změna
     * @return
     */
    public DeletedItems deletePartItems(ApPart part, ApChange change) {
        List<ApItem> items = itemRepository.findValidItemsByPart(part);
        return deleteItems(items, change);
    }

    /**
     * Odstraní kolekci item.
     *
     * @param items
     *            seznam
     * @param change
     *            změna
     */
    public DeletedItems deleteItems(List<ApItem> items, ApChange change) {
        if (items.isEmpty()) {
            return new DeletedItems(Collections.emptyList(), Collections.emptyList());
        }

        for (ApItem item : items) {
            log.debug("Deleting item, apItemId: {}", item.getItemId());
            item.setDeleteChange(change);
        }
        List<ApItem> deletedItems = itemRepository.saveAll(items);

        // Delete bindings
        List<ApBindingItem> bindingItems = this.bindingItemRepository.findByItems(deletedItems);
        if (bindingItems.size() > 0) {
            for (ApBindingItem bindingItem : bindingItems) {
                log.debug("Deleting binding item, apBindingItemId: {}, apItemId: {}",
                          bindingItem.getBindingItemId(),
                          bindingItem.getItem().getItemId());
                bindingItem.setDeleteChange(change);
            }
            bindingItems = bindingItemRepository.saveAll(bindingItems);

            return new DeletedItems(deletedItems, bindingItems);
        } else {
            return new DeletedItems(deletedItems, Collections.emptyList());
        }
    }

    public List<ApItem> deleteBindnedItems(List<ApBindingItem> bindingItemsInPart, ApChange apChange) {
        if (CollectionUtils.isEmpty(bindingItemsInPart)) {
            return Collections.emptyList();
        }
        List<ApItem> items = new ArrayList<>();
        for (ApBindingItem bindingItem : bindingItemsInPart) {
            ApItem item = bindingItem.getItem();
            if (item == null) {
                throw new IllegalStateException("Binding item is not item, itemBindingId: " + bindingItem
                        .getBindingItemId());
            }
            item.setDeleteChange(apChange);
            items.add(item);
        }

        bindingItemRepository.deleteAll(bindingItemsInPart);

        List<ApItem> deletedItems = itemRepository.saveAll(items);
        return deletedItems;

    }

    /*
    private void updateItems(final List<ApItemVO> updateItems,
                             final Map<Integer, List<ApItem>> typeIdItemsMap,
                             final List<ApItem> itemsDb,
                             final Map<Integer, ApItem> objectIdItemMap,
                             final ApChange change) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>();
        for (ApItemVO updateItem : updateItems) {
            Integer objectId = updateItem.getObjectId();
            ApItem item = objectIdItemMap.get(objectId);
            if (item == null) {
                throw new ObjectNotFoundException("Položka neexistuje", BaseCode.ID_NOT_EXIST).setId(objectId);
            }
    
            RulItemType itemType = sdp.getItemType(item.getItemTypeId());
    
            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(item.getItemTypeId(), k -> new ArrayList<>());
            ArrData data = updateItem.createDataEntity(em);
            ApItem newItem = item.copy();
            item.setDeleteChange(change);
    
            newItem.setCreateChange(change);
            newItem.setData(data);
            if (itemType.getUseSpecification() != null && itemType.getUseSpecification()) {
                // specification required
                if (updateItem.getSpecId() == null) {
                    throw new BusinessException("Received item without specification, itemType: " + itemType.getName(),
                            BaseCode.PROPERTY_IS_INVALID)
                                    .set("itemType", itemType.getCode());
                }
                RulItemSpec itemSpec = sdp.getItemSpecById(updateItem.getSpecId());
                if (itemSpec == null) {
                    throw new BusinessException("Received item without valid specification, itemType: " + itemType
                            .getName(),
                            BaseCode.PROPERTY_IS_INVALID)
                                    .set("itemType", itemType.getCode())
                                    .set("itemSpecId", updateItem.getSpecId());
                }
                newItem.setItemSpec(itemSpec);
            } else {
                // item type without specification
                if (updateItem.getSpecId() != null) {
                    throw new BusinessException("Received item with unexpected specification, itemType: " + itemType
                            .getName(),
                            BaseCode.PROPERTY_IS_INVALID)
                                    .set("itemType", itemType.getCode());
                }
                newItem.setItemSpec(null);
            }
    
            dataToSave.add(data);
    
            itemsDb.add(newItem);
            existsItems.add(newItem);
    
            Integer oldPosition = item.getPosition();
            Integer newPosition = updateItem.getPosition();
            Validate.notNull(newPosition);
            if (!Objects.equals(oldPosition, newPosition)) {
                Validate.isTrue(newPosition > 0);
                newPosition = Math.min(newPosition, findMaxPosition(existsItems));
                List<ApItem> itemsToShift = findItemsBetween(existsItems, oldPosition, newPosition);
                newItem.setPosition(newPosition);
                itemsToShift.remove(newItem);
                List<ApItem> newItems = shiftItems(itemsToShift, oldPosition.compareTo(newPosition), change);
                itemsDb.addAll(newItems);
                existsItems.addAll(newItems);
            }
        }
        dataRepository.saveAll(dataToSave);
    }
    */

    private int findMaxPosition(final List<ApItem> items) {
        int max = 1;
        for (ApItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() > max) {
                    max = item.getPosition();
                }
            }
        }
        return max;
    }

    /*
    private void deleteItems(final List<ApItemVO> deleteItems,
                             final Map<Integer, List<ApItem>> typeIdItemsMap,
                             final List<ApItem> itemsDb,
                             final Map<Integer, ApItem> objectIdItemMap,
                             final ApChange change) {
        for (ApItemVO deleteItem : deleteItems) {
            Integer objectId = deleteItem.getObjectId();
            ApItem item = objectIdItemMap.get(objectId);
            if (item == null) {
                throw new ObjectNotFoundException("Položka neexistuje", BaseCode.ID_NOT_EXIST).setId(objectId);
            }
    
            item.setDeleteChange(change);
            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(item.getItemTypeId(), k -> new ArrayList<>());
            List<ApItem> itemsToShift = findItemsGE(existsItems, item.getPosition());
            List<ApItem> newItems = shiftItems(itemsToShift, -1, change);
            itemsDb.addAll(newItems);
            existsItems.addAll(newItems);
        }
    }*/

    /**
     * @return identifikátor pro nový item AP
     */
    public int nextItemObjectId() {
        return sequenceService.getNext(OBJECT_ID_SEQUENCE_NAME);
    }

    /**
     * Založí atributy části.
     *
     * @param part
     *            část
     * @param createItems
     *            Vytvářené záznamy
     * @param change
     *            změna *
     * @param bindingItemList
     *            seznam soucasnych item bindings
     * @param dataRefList
     *            seznam odkazovaných entit
     * @return
     */
    public List<ApItem> createItems(final ApPart part,
                                    final List<ApItemVO> createItems,
                                    final ApChange change,
                                    final List<ApBindingItem> bindingItemList,
                                    final List<ReferencedEntities> dataRefList) {
        if (createItems.isEmpty()) {
            return Collections.emptyList();
        }

        // Map for position counting
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();

        // Map for replacing ApItem
        Map<Integer, ApItem> itemsMap = new HashMap<>();

        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        List<ApItem> itemsCreated = new ArrayList<>();
        for (ApItemVO createItem : createItems) {
            ItemType itemType = sdp.getItemTypeById(createItem.getTypeId());
            RulItemSpec itemSpec = getItemSpecification(itemType, createItem);

            // if(itemType.get)= createItem.getSpecId() == null ? null : sdp.getItemSpecById(createItem.getSpecId());
            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

            Integer positionWant = createItem.getPosition();
            int position = nextPosition(existsItems);

            if (positionWant != null) {
                Validate.isTrue(positionWant > 0);
                if (position > positionWant) {

                    List<ApItem> itemsToShift = findItemsGE(existsItems, positionWant);
                    List<ApItem> newItems = shiftItems(itemsToShift, 1, change);
                    existsItems.addAll(newItems);

                    position = positionWant;
                }
            }

            ArrData data = createItem.createDataEntity(em);
            // zkopirovani binding pro ArrDataRecordRef
            if (data instanceof ArrDataRecordRef) {
                setBindingArrDataRecordRef((ArrDataRecordRef) data, createItem, bindingItemList, dataRefList);
            }
            dataToSave.add(data);

            itemService.checkItemLengthLimit(itemType.getEntity(), data);

            ApItem itemCreated = createItem(part, data, itemType.getEntity(), itemSpec, change, nextItemObjectId(), position);
            itemsCreated.add(itemCreated);

            itemsMap.put(createItem.getId(), itemCreated);

            existsItems.add(itemCreated);

        }
        dataRepository.saveAll(dataToSave);
        itemRepository.saveAll(itemsCreated);
        changeBindingItemsItems(itemsMap, bindingItemList);
        log.debug("Items created, ItemIds: {}", itemsCreated.stream().map(ApItem::getItemId).collect(Collectors.toList()));
        return itemsCreated;
    }

    public RulItemSpec getItemSpecification(final ItemType itemType, final ApItemVO createItem) {
        RulItemSpec itemSpec;
        if (itemType.hasSpecifications()) {
            if (createItem.getSpecId() == null) {
                throw new BusinessException("Received item without specification, itemType: " + itemType.getEntity()
                        .getName(),
                        BaseCode.PROPERTY_IS_INVALID)
                        .set("itemType", itemType.getCode());
            }
            itemSpec = itemType.getItemSpecById(createItem.getSpecId());
            if (itemSpec == null) {
                throw new BusinessException("Received item without valid specification, itemType: " + itemType
                        .getEntity().getName(),
                        BaseCode.PROPERTY_IS_INVALID)
                        .set("itemType", itemType.getCode())
                        .set("itemSpecId", createItem.getSpecId());
            }
        } else {
            // item type without specification
            if (createItem.getSpecId() != null) {
                throw new BusinessException("Received item with unexpected specification, itemType: " + itemType
                        .getEntity()
                        .getName(),
                        BaseCode.PROPERTY_IS_INVALID)
                        .set("itemType", itemType.getCode());
            }
            itemSpec = null;
        }
        return itemSpec;
    }

    /**
     * Nastaveni vazby ne pripojene AP z puvodniho ArrDataRecordRef
     * 
     * Jako vedlejsi efekt funkce kontroluje existenci nastaveni vazby
     * na AP nebo na Binding na AP
     * 
     * @param data
     * @param createItem
     * @param bindingItemList
     * @param dataRefList
     */
    private void setBindingArrDataRecordRef(ArrDataRecordRef data, ApItemVO createItem,
                                            List<ApBindingItem> bindingItemList,
                                            List<ReferencedEntities> dataRefList) {
        Validate.isTrue(createItem instanceof ApItemAccessPointRefVO);

        ApItemAccessPointRefVO apItemAccessPointRefVO = (ApItemAccessPointRefVO) createItem;

        if (bindingItemList != null && dataRefList != null) {
            // Vyhledani stavajiciho binding
            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getItem() != null
                        && createItem.getId() != null
                        && apItemAccessPointRefVO.getExternalName() != null
                        && bindingItem.getItem().getItemId() != null
                        && bindingItem.getItem().getItemId().equals(createItem.getId())) {
                    // prevzeti puvodniho odkazu
                    ApItem origItem = bindingItem.getItem();
                    ArrDataRecordRef origData = (ArrDataRecordRef) origItem.getData();
                    data.setBinding(origData.getBinding());

                    dataRefList.add(new ReferencedEntities(data, apItemAccessPointRefVO.getExternalName()));
                    break;
                }
            }
        }

        // finalni kontrola
        if (data.getBinding() == null && data.getRecordId() == null) {
            throw new BusinessException("Missing record reference, dataId" + createItem.getId(),
                    BaseCode.INVALID_STATE)
                            .set("itemId", createItem.getId())
                            .set("objectId", createItem.getObjectId());
        }

    }

    /**
     * Nahradit všechna pole ApItem tabulky ApBindingItem podle mapy
     * 
     * @param itemsMap mapa zmen
     * @param bindingItemList
     */
    private void changeBindingItemsItems(Map<Integer, ApItem> itemsMap, List<ApBindingItem> bindingItemList) {
        if (CollectionUtils.isEmpty(bindingItemList)) {
            return;
        }
        List<ApBindingItem> currentItemBindings = new ArrayList<>();
        for (ApBindingItem bindingItem : bindingItemList) {
            ApItem item = itemsMap.get(bindingItem.getItemId());
            if (item != null) {
                bindingItem.setItem(item);
                currentItemBindings.add(bindingItem);
            }
        }
        if (CollectionUtils.isNotEmpty(currentItemBindings)) {
            bindingItemRepository.saveAll(currentItemBindings);
        }
    }

    public ApItem copyItem(final ApItem oldItem,
                             final ApChange change,
                             final ApPart apPart) {
        ApItem newItem = new ApItem();
        newItem.setCreateChange(change);
        newItem.setData(oldItem.getData());
        newItem.setItemSpec(oldItem.getItemSpec());
        newItem.setItemType(oldItem.getItemType());
        newItem.setObjectId(oldItem.getObjectId());
        newItem.setPosition(oldItem.getPosition());
        newItem.setPart(apPart);

        return newItem;
    }

    /**
     * Vytvoření entity hodnoty atributu přístupového bodu.
     *
     * @param accessPoint
     *            přístupový bod pro který atribut tvoříme
     * @param it
     *            typ atributu
     * @param is
     *            specifikace atribututu
     * @param c
     *            změna
     * @param objectId
     *            jednoznačný identifikátor položky (nemění se při odverzování)
     * @param position
     *            pozice
     * @return vytvořená položka
     */
    public ApItem createItem(final ApPart part,
                             final ArrData data,
                             final RulItemType it, final RulItemSpec is, final ApChange c,
                             final int objectId, final int position) {
        ApItem item = new ApItem();
        item.setData(data);
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        item.setPart(part);
        return item;
    }

    public ApItem createItem(final ApPart part,
                             final ArrData data,
                             final RulItemType it, final RulItemSpec is, final ApChange c,
                             List<ApItem> existsItems) {
        int position = nextPosition(existsItems);
        int objectId = nextItemObjectId();
        return createItem(part, data, it, is, c, objectId, position);
    }

    @Nullable
    private ApBindingItem findBindingItemByUuid(final List<ApBindingItem> bindingItemList, final String pid) {
        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getValue().equals(pid)) {
                    return bindingItem;
                }
            }
        }
        return null;
    }

    private List<ApItem> shiftItems(final List<ApItem> items, final int diff, final ApChange change) {
        List<ApItem> newItems = new ArrayList<>();
        for (ApItem item : items) {
            if (item.getItemId() == null) {
                item.setPosition(item.getPosition() + diff);
            } else {
                ApItem newItem = item.copy();
                newItem.setCreateChange(change);
                newItem.setPosition(item.getPosition() + diff);
                newItems.add(newItem);

                item.setDeleteChange(change);
            }
        }
        return newItems;
    }

    private List<ApItem> findItemsGE(final List<ApItem> items, final int position) {
        List<ApItem> result = new ArrayList<>();
        for (ApItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() >= position) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<ApItem> findItemsBetween(final List<ApItem> items, final int aPosition, final int bPosition) {
        int minPosition = Math.min(aPosition, bPosition);
        int maxPosition = Math.max(aPosition, bPosition);
        List<ApItem> result = new ArrayList<>();
        for (ApItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() >= minPosition && item.getPosition() <= maxPosition) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private int nextPosition(final List<ApItem> existsItems) {
        if (existsItems.size() == 0) {
            return 1;
        }
        int position = 2;
        for (ApItem existsItem : existsItems) {
            if (existsItem.getDeleteChange() == null) {
                if (existsItem.getPosition() >= position) {
                    position = existsItem.getPosition() + 1;
                }
            }
        }
        return position;
    }

    public List<ApItem> findItemsByParts(List<ApPart> parts) {
        return itemRepository.findValidItemsByParts(parts);
    }

    public List<ApItem> findItems(final Integer accessPointId, final RulItemType itemType, final String partTypeCode) {
        return itemRepository.findItemsByAccessPointIdAndItemTypeAndPartTypeCode(accessPointId, itemType, partTypeCode);
    }

    public List<ApItem> findItems(final Integer accessPointId, final Collection<RulItemType> itemTypes,
                                  final String partTypeCode) {
        return itemRepository.findItemsByAccessPointIdAndItemTypesAndPartTypeCode(accessPointId, itemTypes,
                                                                                  partTypeCode);
    }

    public List<ApItem> findItems(ApAccessPoint accessPoint) {
        return itemRepository.findValidItemsByAccessPoint(accessPoint);
    }

    public static void normalize(ArrDataUnitdate aeDataUnitdate) {

        String valueFrom = aeDataUnitdate.getValueFrom();
        if (valueFrom == null) {
            aeDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
        } else {
            LocalDateTime fromDate = LocalDateTime.parse(valueFrom.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(fromDate));
        }

        String valueTo = aeDataUnitdate.getValueTo();
        if (valueTo == null) {
            aeDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
        } else {
            LocalDateTime toDate = LocalDateTime.parse(valueTo.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(toDate));
        }
    }

    /**
     * Method will create external item and save all connected items
     * 
     * @param part
     * @param data
     * @param itemType
     * @param itemSpec
     * @param change
     * @param existsItems
     * @param binding
     */
    public ApItem createItemWithSave(ApPart part, ArrData data, RulItemType rulItemType, RulItemSpec itemSpec,
                                     ApChange change,
                                     List<ApItem> existsItems, ApBinding binding,
                                     String bindingValue) {

        data = dataRepository.save(data);

        itemService.checkItemLengthLimit(rulItemType, data);

        ApItem itemCreated = createItem(part, data, rulItemType, itemSpec, change, existsItems);
        itemCreated = itemRepository.save(itemCreated);

        if (binding != null) {
            externalSystemService.createApBindingItem(binding, change, bindingValue, null, itemCreated);
        }
        return itemCreated;
    }

}
