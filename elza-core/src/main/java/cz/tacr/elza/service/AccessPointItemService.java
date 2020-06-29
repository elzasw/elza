package cz.tacr.elza.service;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.ItemBooleanXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.ItemEnumXml;
import cz.tacr.cam.schema.cam.ItemIntegerXml;
import cz.tacr.cam.schema.cam.ItemLinkXml;
import cz.tacr.cam.schema.cam.ItemStringXml;
import cz.tacr.cam.schema.cam.ItemUnitDateXml;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.vo.ap.item.ApItemAccessPointRefVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.ap.item.ApUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.service.vo.DataRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccessPointItemService {

    private static final String OBJECT_ID_SEQUENCE_NAME = "ap_item|object_id";

    private final EntityManager em;
    private final StaticDataService staticDataService;
    private final ApItemRepository itemRepository;
    private final DataRepository dataRepository;
    private final SequenceService sequenceService;
    private final ExternalSystemService externalSystemService;
    private final ApBindingItemRepository bindingItemRepository;
    private final ApBindingRepository bindingRepository;

    public AccessPointItemService(final EntityManager em,
                                  final StaticDataService staticDataService,
                                  final ApItemRepository itemRepository,
                                  final DataRepository dataRepository,
                                  final SequenceService sequenceService,
                                  final ExternalSystemService externalSystemService,
                                  final ApBindingItemRepository bindingItemRepository,
                                  final ApBindingRepository bindingRepository) {
        this.em = em;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
        this.sequenceService = sequenceService;
        this.externalSystemService = externalSystemService;
        this.bindingItemRepository = bindingItemRepository;
        this.bindingRepository = bindingRepository;
    }

    /**
     * Odstranění prvků popisů u dočasných jmen a AP.
     */
    public void removeTempItems() {

        //TODO fantis
//        itemRepository.removeTempItems();
    }

    /**
     * Odstranění prvůk popisů u dočasných jmen a AP.
     */
    public void removeTempItems(final ApAccessPoint ap) {

        //TODO fantis
//        itemRepository.removeTempItems(ap);
    }

    @FunctionalInterface
    public interface CreateFunction {
        ApItem apply(final RulItemType itemType, final RulItemSpec itemSpec, final ApChange change, final int objectId, final int position);
    }

    /**
     * Provede změnu položek.
     *
     * @param items   položky k úpravě
     * @param itemsDb aktuální položky v DB
     * @param change  změna
     * @param create  funkce pro založené nové položky
     * @return nové položky, které ze vytvořili při změně
     */
    public List<ApItem> changeItems(final List<ApUpdateItemVO> items, final List<ApItem> itemsDb, final ApChange change, final CreateFunction create) {
        Map<Integer, ApItem> objectIdItemMap = itemsDb.stream().collect(Collectors.toMap(ApItem::getObjectId, Function.identity()));
        Map<Integer, List<ApItem>> typeIdItemsMap = itemsDb.stream().collect(Collectors.groupingBy(ApItem::getItemTypeId));

        List<ApItemVO> createItems = new ArrayList<>();
        List<ApItemVO> updateItems = new ArrayList<>();
        List<ApItemVO> deleteItems = new ArrayList<>();

        for (ApUpdateItemVO item : items) {
            UpdateOp updateOp = item.getUpdateOp();
            switch (updateOp) {
                case CREATE:
                    createItems.add(item.getItem());
                    break;
                case UPDATE:
                    updateItems.add(item.getItem());
                    break;
                case DELETE:
                    deleteItems.add(item.getItem());
                    break;
                default:
                    throw new NotImplementedException("Neimplementovaná operace: " + updateOp);
            }
        }

        // TODO: optimalizace při úpravě se stejným change id (bez odverzování) - pro deleteItems a updateItems
        deleteItems(deleteItems, typeIdItemsMap, itemsDb, objectIdItemMap, change);
        List<ApItem> itemsCreated = createItems(createItems, typeIdItemsMap, itemsDb, change, null, null, create);
        updateItems(updateItems, typeIdItemsMap, itemsDb, objectIdItemMap, change);

        itemRepository.save(itemsDb);

        return itemsCreated;
    }

    /**
     * Provede odstranění všech aktivních položek podle typu.
     *
     * @param repository repository pro nalezení platný položek
     * @param joinItem   vazební položka (podle které se vyhledávají položky)
     * @param itemType   typ
     * @param change     změna
     * @param <T>        generický typ vazební položky
     */
    public <T> void deleteItemsByType(ByType<T> repository, T joinItem, RulItemType itemType, ApChange change) {
        List<ApItem> items = repository.findValidItemsByType(joinItem, itemType);
        for (ApItem item : items) {
            item.setDeleteChange(change);
        }
        itemRepository.save(items);
    }

    /**
     * Odstraní všechny atributy části.
     *
     * @param part část
     * @param change změna
     */
    public void deletePartItems(ApPart part, ApChange change) {
        List<ApItem> items = itemRepository.findValidItemsByPart(part);
        for (ApItem item : items) {
            item.setDeleteChange(change);
        }
        itemRepository.save(items);
    }

    /**
     * Odstraní všechny atributy částí.
     *
     * @param partList seznam částí
     * @param change změna
     */
    public void deletePartsItems(List<ApPart> partList, ApChange change) {
        List<ApItem> items = itemRepository.findValidItemsByParts(partList);
        for (ApItem item : items) {
            item.setDeleteChange(change);
        }
        itemRepository.save(items);
    }

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

            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(item.getItemTypeId(), k -> new ArrayList<>());
            ArrData data = updateItem.createDataEntity(em);
            ApItem newItem = item.copy();
            item.setDeleteChange(change);

            newItem.setCreateChange(change);
            newItem.setData(data);
            newItem.setItemSpec(updateItem.getSpecId() == null ? null : sdp.getItemSpecById(updateItem.getSpecId()));

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
        dataRepository.save(dataToSave);
    }

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
    }

    /**
     * @return identifikátor pro nový item AP
     */
    public int nextItemObjectId() {
        return sequenceService.getNext(OBJECT_ID_SEQUENCE_NAME);
    }

    public List<ApItem> createItems(final List<ApItemVO> createItems,
                                    final Map<Integer, List<ApItem>> typeIdItemsMap,
                                    final List<ApItem> itemsDb,
                                    final ApChange change,
                                    final List<ApBindingItem> bindingItemList,
                                    final List<DataRef> dataRefList,
                                    final CreateFunction create) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        List<ApItem> itemsCreated = new ArrayList<>();
        for (ApItemVO createItem : createItems) {
            RulItemType itemType = sdp.getItemTypeById(createItem.getTypeId()).getEntity();
            RulItemSpec itemSpec = createItem.getSpecId() == null ? null : sdp.getItemSpecById(createItem.getSpecId());
            List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

            Integer positionWant = createItem.getPosition();
            int position = nextPosition(existsItems);

            if (positionWant != null) {
                Validate.isTrue(positionWant > 0);
                if (position > positionWant) {

                    List<ApItem> itemsToShift = findItemsGE(existsItems, positionWant);
                    List<ApItem> newItems = shiftItems(itemsToShift, 1, change);
                    itemsDb.addAll(newItems);
                    existsItems.addAll(newItems);

                    position = positionWant;
                }
            }

            ArrData data = createItem.createDataEntity(em);
            setBindingArrDataRecordRef(data, createItem, bindingItemList, dataRefList);

            ApItem itemCreated = create.apply(itemType, itemSpec, change, nextItemObjectId(), position);
            dataToSave.add(data);
            itemCreated.setData(data);
            itemsCreated.add(itemCreated);

            itemsDb.add(itemCreated);
            existsItems.add(itemCreated);

            changeBindingItemsItems(createItem, itemCreated, bindingItemList);
        }
        dataRepository.save(dataToSave);
        return itemsCreated;
    }

    private void setBindingArrDataRecordRef(ArrData data, ApItemVO createItem, List<ApBindingItem> bindingItemList, List<DataRef> dataRefList) {
        if (data instanceof ArrDataRecordRef && createItem instanceof ApItemAccessPointRefVO
                && CollectionUtils.isNotEmpty(bindingItemList) && dataRefList != null) {
            ApItemAccessPointRefVO apItemAccessPointRefVO = (ApItemAccessPointRefVO) createItem;

            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getItem() != null && createItem.getId() != null && apItemAccessPointRefVO.getExternalName() != null &&
                        bindingItem.getItem().getItemId() != null && bindingItem.getItem().getItemId().equals(createItem.getId())) {
                    dataRefList.add(new DataRef(bindingItem.getValue(), Long.parseLong(apItemAccessPointRefVO.getExternalName())));
                    break;
                }
            }

        }
    }

    private void changeBindingItemsItems(ApItemVO createItem, ApItem itemCreated, List<ApBindingItem> bindingItemList) {
        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            List<ApBindingItem> currentItemBindings = new ArrayList<>();
            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getItem() != null && createItem.getId() != null &&
                        bindingItem.getItem().getItemId() != null && bindingItem.getItem().getItemId().equals(createItem.getId())) {
                    bindingItem.setItem(itemCreated);
                    currentItemBindings.add(bindingItem);
                }
            }
            if (CollectionUtils.isNotEmpty(currentItemBindings)) {
                bindingItemRepository.save(currentItemBindings);
            }
        }
    }

    public List<ApItem> createItems(final List<Object> createItems,
                                    final ApChange change,
                                    final ApBinding binding,
                                    final List<DataRef> dataRefList,
                                    final CreateFunction create) {
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        List<ApItem> itemsCreated = new ArrayList<>();
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();

        for (Object createItem : createItems) {

            ApItem itemCreated = createItem(createItem, change, create, typeIdItemsMap, dataToSave, binding, dataRefList);
            itemsCreated.add(itemCreated);
        }
        dataRepository.save(dataToSave);
        return itemsCreated;
    }

    public ApItem createItem(final ApItem oldItem,
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

    private ApItem createItem(final Object createItem,
                              final ApChange change,
                              final CreateFunction create,
                              final Map<Integer, List<ApItem>> typeIdItemsMap,
                              final List<ArrData> dataToSave,
                              final ApBinding binding,
                              final List<DataRef> dataRefList) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemType itemType;
        RulItemSpec itemSpec;
        String uuid;
        ArrData data;

        if (createItem instanceof ItemBooleanXml) {
            ItemBooleanXml itemBoolean = (ItemBooleanXml) createItem;

            itemType = sdp.getItemType(itemBoolean.getT().getValue());
            itemSpec = itemBoolean.getS() == null ? null : sdp.getItemSpec(itemBoolean.getS().getValue());
            uuid = itemBoolean.getUuid().getValue();

            ArrDataBit dataBit = new ArrDataBit();
            dataBit.setValue(itemBoolean.getValue().isValue());
            dataBit.setDataType(DataType.BIT.getEntity());
            data = dataBit;
        } else if (createItem instanceof ItemEntityRefXml) {
            ItemEntityRefXml itemEntityRef = (ItemEntityRefXml) createItem;

            itemType = sdp.getItemType(itemEntityRef.getT().getValue());
            itemSpec = itemEntityRef.getS() == null ? null : sdp.getItemSpec(itemEntityRef.getS().getValue());
            uuid = itemEntityRef.getUuid().getValue();

            ArrDataRecordRef dataRecordRef = new ArrDataRecordRef();
            EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
            DataRef dataRef = new DataRef(uuid, entityRecordRef.getEid().getValue());
            dataRefList.add(dataRef);

            dataRecordRef.setDataType(DataType.RECORD_REF.getEntity());
            data = dataRecordRef;
        } else if (createItem instanceof ItemEnumXml) {
            ItemEnumXml itemEnum = (ItemEnumXml) createItem;

            itemType = sdp.getItemType(itemEnum.getT().getValue());
            itemSpec = itemEnum.getS() == null ? null : sdp.getItemSpec(itemEnum.getS().getValue());
            uuid = itemEnum.getUuid().getValue();

            ArrDataNull dataNull = new ArrDataNull();
            dataNull.setDataType(DataType.ENUM.getEntity());
            data = dataNull;
        } else if (createItem instanceof ItemIntegerXml) {
            ItemIntegerXml itemInteger = (ItemIntegerXml) createItem;

            itemType = sdp.getItemType(itemInteger.getT().getValue());
            itemSpec = itemInteger.getS() == null ? null : sdp.getItemSpec(itemInteger.getS().getValue());
            uuid = itemInteger.getUuid().getValue();

            ArrDataInteger dataInteger = new ArrDataInteger();
            dataInteger.setValue(itemInteger.getValue().getValue().intValue());
            dataInteger.setDataType(DataType.INT.getEntity());
            data = dataInteger;
        } else if (createItem instanceof ItemLinkXml) {
            ItemLinkXml itemLink = (ItemLinkXml) createItem;

            itemType = sdp.getItemType(itemLink.getT().getValue());
            itemSpec = itemLink.getS() == null ? null : sdp.getItemSpec(itemLink.getS().getValue());
            uuid = itemLink.getUuid().getValue();

            ArrDataUriRef dataUriRef = new ArrDataUriRef();
            dataUriRef.setValue(itemLink.getUrl().getValue());
            dataUriRef.setDescription(itemLink.getNm().getValue());
            dataUriRef.setSchema("http");
            dataUriRef.setArrNode(null);
            dataUriRef.setDataType(DataType.URI_REF.getEntity());
            data = dataUriRef;
        } else if (createItem instanceof ItemStringXml) {
            ItemStringXml itemString = (ItemStringXml) createItem;

            itemType = sdp.getItemType(itemString.getT().getValue());
            itemSpec = itemString.getS() == null ? null : sdp.getItemSpec(itemString.getS().getValue());
            uuid = itemString.getUuid().getValue();

            RulDataType dataType = itemType.getDataType();
            String code = dataType.getCode();
            DataType dt = DataType.fromCode(code);
            if (dt == null) {
                throw new IllegalStateException("Neznámý datový typ " + code);
            }
            switch(dt) {
                case STRING:
                    ArrDataString dataString = new ArrDataString();
                    dataString.setValue(itemString.getValue().getValue());
                    dataString.setDataType(DataType.STRING.getEntity());
                    data = dataString;
                    break;
                case TEXT:
                    ArrDataText dataText = new ArrDataText();
                    dataText.setValue(itemString.getValue().getValue());
                    dataText.setDataType(DataType.TEXT.getEntity());
                    data = dataText;
                    break;
                case COORDINATES:
                    ArrDataCoordinates dataCoordinates = new ArrDataCoordinates();
                    dataCoordinates.setValue(GeometryConvertor.convert(itemString.getValue().getValue()));
                    dataCoordinates.setDataType(DataType.COORDINATES.getEntity());
                    data = dataCoordinates;
                    break;
                default:
                    throw new IllegalStateException("Neznámý datový typ " + code);
            }

        } else if (createItem instanceof ItemUnitDateXml) {
            ItemUnitDateXml itemUnitDate = (ItemUnitDateXml) createItem;

            itemType = sdp.getItemType(itemUnitDate.getT().getValue());
            itemSpec = itemUnitDate.getS() == null ? null : sdp.getItemSpec(itemUnitDate.getS().getValue());
            uuid = itemUnitDate.getUuid().getValue();

            CalendarType calType = CalendarType.GREGORIAN;
            ArrDataUnitdate dataUnitDate = new ArrDataUnitdate();
            dataUnitDate.setValueFrom(itemUnitDate.getF().trim());
            dataUnitDate.setValueFromEstimated(itemUnitDate.isFe());
            dataUnitDate.setFormat(itemUnitDate.getFmt());
            dataUnitDate.setValueTo(itemUnitDate.getTo().trim());
            dataUnitDate.setValueToEstimated(itemUnitDate.isToe());
            if (itemUnitDate.getF() != null) {
                dataUnitDate.setNormalizedFrom(CalendarConverter.toSeconds(calType, LocalDateTime.parse(itemUnitDate.getF().trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                dataUnitDate.setNormalizedFrom(Long.MIN_VALUE);
            }

            if (itemUnitDate.getTo() != null) {
                dataUnitDate.setNormalizedTo(CalendarConverter.toSeconds(calType, LocalDateTime.parse(itemUnitDate.getTo().trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                dataUnitDate.setNormalizedTo(Long.MAX_VALUE);
            }

            dataUnitDate.setCalendarType(calType.getEntity());
            dataUnitDate.setDataType(DataType.UNITDATE.getEntity());
            data = dataUnitDate;
        } else {
            throw new IllegalArgumentException("Invalid item type");
        }


        List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());
        int position = nextPosition(existsItems);


        ApItem itemCreated = create.apply(itemType, itemSpec, change, nextItemObjectId(), position);
        itemCreated.setData(data);

        externalSystemService.createApBindingItem(binding, uuid, null, itemCreated);

        dataToSave.add(data);
        existsItems.add(itemCreated);
        return itemCreated;
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

    public List<Object> findNewOrChangedItems(List<Object> items, List<ApBindingItem> bindingItems, List<ApBindingItem> notChangeItems) {
        List<Object> changedItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(items)) {
            for (Object item : items) {
                if (item instanceof ItemBooleanXml) {
                    ItemBooleanXml itemBoolean = (ItemBooleanXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemBoolean.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemBoolean);
                    } else {
                        ApItem ib = bindingItem.getItem();
                        ArrDataBit dataBit = (ArrDataBit) ib.getData();
                        if (!(ib.getItemType().getCode().equals(itemBoolean.getT().getValue()) &&
                                compareItemSpec(ib.getItemSpec(), itemBoolean.getS()) &&
                                dataBit.isValue().equals(itemBoolean.getValue().isValue()))) {
                            changedItems.add(itemBoolean);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemEntityRefXml) {
                    ItemEntityRefXml itemEntityRef = (ItemEntityRefXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemEntityRef.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemEntityRef);
                    } else {
                        ApItem ier = bindingItem.getItem();
                        ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) ier.getData();
                        EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
                        if (!(ier.getItemType().getCode().equals(itemEntityRef.getT().getValue()) &&
                                compareItemSpec(ier.getItemSpec(), itemEntityRef.getS()) &&
                                dataRecordRef.getBinding().getValue().equals(String.valueOf(entityRecordRef.getEid().getValue())))) {

                            changedItems.add(itemEntityRef);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemEnumXml) {
                    ItemEnumXml itemEnum = (ItemEnumXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemEnum.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemEnum);
                    } else {
                        ApItem ie = bindingItem.getItem();
                        if (!(ie.getItemType().getCode().equals(itemEnum.getT().getValue()) &&
                                compareItemSpec(ie.getItemSpec(), itemEnum.getS()))) {

                            changedItems.add(itemEnum);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemIntegerXml) {
                    ItemIntegerXml itemInteger = (ItemIntegerXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemInteger.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemInteger);
                    } else {
                        ApItem ii = bindingItem.getItem();
                        ArrDataInteger dataInteger = (ArrDataInteger) ii.getData();
                        if (!(ii.getItemType().getCode().equals(itemInteger.getT().getValue()) &&
                                compareItemSpec(ii.getItemSpec(), itemInteger.getS()) &&
                                dataInteger.getValue().equals(itemInteger.getValue().getValue().intValue()))) {

                            changedItems.add(itemInteger);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemLinkXml) {
                    ItemLinkXml itemLink = (ItemLinkXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemLink.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemLink);
                    } else {
                        ApItem il = bindingItem.getItem();
                        ArrDataUriRef dataUriRef = (ArrDataUriRef) il.getData();
                        if (!(il.getItemType().getCode().equals(itemLink.getT().getValue()) &&
                                compareItemSpec(il.getItemSpec(), itemLink.getS()) &&
                                dataUriRef.getValue().equals(itemLink.getUrl().getValue()) &&
                                dataUriRef.getDescription().equals(itemLink.getNm().getValue()))) {

                            changedItems.add(itemLink);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemStringXml) {
                    ItemStringXml itemString = (ItemStringXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemString.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemString);
                    } else {
                        ApItem is = bindingItem.getItem();
                        String value;
                        switch(DataType.fromCode(is.getItemType().getDataType().getCode())) {
                            case STRING:
                                ArrDataString dataString = (ArrDataString) is.getData();
                                value = dataString.getValue();
                                break;
                            case TEXT:
                                ArrDataText dataText = (ArrDataText) is.getData();
                                value = dataText.getValue();
                                break;
                            case COORDINATES:
                                ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) is.getData();
                                value = GeometryConvertor.convert(dataCoordinates.getValue());
                                break;
                            default:
                                throw new IllegalStateException("Neznámý datový typ " + is.getItemType().getDataType().getCode());
                        }
                        if (!(is.getItemType().getCode().equals(itemString.getT().getValue()) &&
                                compareItemSpec(is.getItemSpec(), itemString.getS()) &&
                                value.equals(itemString.getValue().getValue()))) {

                            changedItems.add(itemString);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemUnitDateXml) {
                    ItemUnitDateXml itemUnitDate = (ItemUnitDateXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemUnitDate.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemUnitDate);
                    } else {
                        ApItem iud = bindingItem.getItem();
                        ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) iud.getData();
                        if (!(iud.getItemType().getCode().equals(itemUnitDate.getT().getValue()) &&
                                compareItemSpec(iud.getItemSpec(), itemUnitDate.getS()) &&
                                dataUnitdate.getValueFrom().equals(itemUnitDate.getF().trim()) &&
                                dataUnitdate.getValueFromEstimated().equals(itemUnitDate.isFe()) &&
                                dataUnitdate.getFormat().equals(itemUnitDate.getFmt()) &&
                                dataUnitdate.getValueTo().equals(itemUnitDate.getTo().trim()) &&
                                dataUnitdate.getValueToEstimated().equals(itemUnitDate.isToe()))) {

                            changedItems.add(itemUnitDate);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Invalid item type");
                }
            }
        }
        return changedItems;
    }

    private boolean compareItemSpec(RulItemSpec itemSpec, CodeXml itemSpecCode) {
        if (itemSpec == null) {
            return itemSpecCode == null;
        } else {
            return itemSpec.getCode().equals(itemSpecCode.getValue());
        }
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

    public List<ApItem> findItems(final Integer accessPointId, final Collection<RulItemType> itemTypes, final String partTypeCode) {
        return itemRepository.findItemsByAccessPointIdAndItemTypesAndPartTypeCode(accessPointId, itemTypes, partTypeCode);
    }

}
