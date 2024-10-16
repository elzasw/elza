package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.db.HibernateUtils;
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
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
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
    private final DataService dataService;

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
                                  final ItemService itemService,
                                  final DataService dataService) {
        this.em = em;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
        this.sequenceService = sequenceService;
        this.externalSystemService = externalSystemService;
        this.bindingItemRepository = bindingItemRepository;
        this.itemService = itemService;
        this.dataService = dataService;
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
        List<ApItem> items = findValidItemsByPart(part);
        return deleteItems(items, change);
    }

    /**
     * Odstranění kolekci item.
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
        List<ApBindingItem> bindingItems = deleteBindingItems(deletedItems, change);

        return new DeletedItems(deletedItems, bindingItems);
    }

    /**
     *  Odstranění seznamu BindingItem by ApItem
     *
     * @param deletedItems - seznam ApItem
     * @param change
     * @return List<ApBindingItem>
     */
    public List<ApBindingItem> deleteBindingItems(List<ApItem> deletedItems, ApChange change) {
        List<ApBindingItem> bindingItems = bindingItemRepository.findByItems(deletedItems);
        if (bindingItems.size() > 0) {
            for (ApBindingItem bindingItem : bindingItems) {
                log.debug("Deleting binding item, apBindingItemId: {}, apItemId: {}",
                          bindingItem.getBindingItemId(),
                          bindingItem.getItem().getItemId());
                bindingItem.setDeleteChange(change);
            }
            return bindingItemRepository.saveAll(bindingItems);
        }
        return Collections.emptyList();
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
    // TODO: method is used also to update existing items
    //       in such case we should retain objectId and not issue new one
    //       see nextItemObjectId()
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
                    ArrData origData = HibernateUtils.unproxy(origItem.getData());
                    Validate.isTrue(origData instanceof ArrDataRecordRef);
                    ArrDataRecordRef origDataRRef = (ArrDataRecordRef) origData;
                    data.setBinding(origDataRRef.getBinding());

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
     * @param itemsMap
     *            mapa zmen
     * @param bindingItemList
     * @return list of changed bindings
     */
    public List<ApBindingItem> changeBindingItemsItems(Map<Integer, ApItem> itemsMap,
                                                       Collection<ApBindingItem> bindingItemList) {
        if (CollectionUtils.isEmpty(bindingItemList)) {
            return Collections.emptyList();
        }
        if (itemsMap == null || itemsMap.size() == 0) {
            return Collections.emptyList();
        }
        List<ApBindingItem> currentItemBindings = new ArrayList<>();
        for (ApBindingItem bindingItem : bindingItemList) {
            ApItem item = itemsMap.get(bindingItem.getItemId());
            if (item != null) {
                bindingItem.setItem(item);
                currentItemBindings.add(bindingItem);
            }
        }
        if (CollectionUtils.isEmpty(currentItemBindings)) {
            return Collections.emptyList();
        }
        return bindingItemRepository.saveAll(currentItemBindings);
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
                             final RulItemType it,
                             final RulItemSpec is, final ApChange c,
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

    public List<ApItem> findValidItemsByPart(ApPart part) {
        return dataService.findItemsWithData(itemRepository.findValidItemsByPart(part));
    }

    public List<ApItem> findItemsByParts(List<ApPart> parts) {
        return dataService.findItemsWithData(itemRepository.findValidItemsByParts(parts));
    }

    public List<ApItem> findValidItemsByPartId(Integer partId) {
        return dataService.findItemsWithData(itemRepository.findValidItemsByPartId(partId));
    }

    public List<ApItem> findItems(final Integer accessPointId, final RulItemType itemType, final String partTypeCode) {
        return dataService.findItemsWithData(itemRepository.findItemsByAccessPointIdAndItemTypeAndPartTypeCode(accessPointId, itemType, partTypeCode));
    }

    public List<ApItem> findItems(final Integer accessPointId, final Collection<RulItemType> itemTypes,
                                  final String partTypeCode) {
        return dataService.findItemsWithData(itemRepository.findItemsByAccessPointIdAndItemTypesAndPartTypeCode(accessPointId, itemTypes, partTypeCode));
    }

    public List<ApItem> findValidItemsByAccessPoint(ApAccessPoint accessPoint) {
        return dataService.findItemsWithData(itemRepository.findValidItemsByAccessPoint(accessPoint));
    }

    public List<ApItem> findNewerValidItemsByAccessPoint(ApAccessPoint accessPoint, Integer changeId) {
        return dataService.findItemsWithData(itemRepository.findNewerValidItemsByAccessPoint(accessPoint, changeId));
    }

    public List<ApItem> findValidItemsByAccessPoints(Collection<ApAccessPoint> accessPoints) {
        return dataService.findItemsWithData(itemRepository.findValidItemsByAccessPoints(accessPoints));
    }

    public List<ApItem> findValidItemsByAccessPointMultiFetch(ApAccessPoint accessPoint) {
        return dataService.findItemsWithData(itemRepository.findValidItemsByAccessPointMultiFetch(accessPoint));
    }

    public List<ApItem> findItemByEntity(ApAccessPoint replaced) {
        return dataService.findItemsWithData(itemRepository.findItemByEntity(replaced));
    }

    public List<ApItem> findUnbindedItemByBinding(ApBinding binding) {
        return dataService.findItemsWithData(itemRepository.findUnbindedItemByBinding(binding));
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

    /**
     * Update existing item with new value
     *
     * Updated item is flushed to the DB. It is up to caller
     * to sync cache and revalidate AP
     *
     * @param change
     * @param apItem
     * @param drr
     * @return
     */
    public ApItem updateItem(ApChange change, ApItem apItem, ArrData data) {
        Validate.isTrue(apItem.getDeleteChange() == null);

        apItem.setDeleteChange(change);
        apItem = itemRepository.saveAndFlush(apItem);

        data = dataRepository.save(data);

        // create new item
        ApItem ret = createItem(apItem.getPart(), data,
                                apItem.getItemType(),
                                apItem.getItemSpec(), change,
                                apItem.getObjectId(),
                                apItem.getPosition());

        ret = itemRepository.saveAndFlush(ret);
        return ret;
    }

    /**
     * Kontrola, zda seznam ApItems obsahuje prvek ApItem
     * 
     * @param item
     * @param items
     * @return boolean
     */
    public boolean isApItemInList(ApItem item, List<ApItem> items) {
        for (ApItem i : items) {
            if (Objects.equals(item.getItemTypeId(), i.getItemTypeId())
                    && Objects.equals(item.getItemSpecId(), i.getItemSpecId())
                    && item.getData().isEqualValue(i.getData())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vyhledávání v seznamu ApItem podle ItemType & ItemSpec
     * 
     * @param apItem
     * @param items
     * @return
     */
    public ApItem findByTypeAndSpec(ApItem item, List<ApItem> items) {
        for (ApItem i : items) {
            if (Objects.equals(item.getItemTypeId(), i.getItemTypeId()) 
                    && Objects.equals(item.getItemSpecId(), i.getItemSpecId())) {
                return i;
            }
        }
        return null;
    }
}
