package cz.tacr.elza.service;

import cz.tacr.cam._2019.*;
import cz.tacr.elza.common.GeometryConvertor;
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

    public AccessPointItemService(final EntityManager em,
                                  final StaticDataService staticDataService,
                                  final ApItemRepository itemRepository,
                                  final DataRepository dataRepository,
                                  final SequenceService sequenceService,
                                  final ExternalSystemService externalSystemService) {
        this.em = em;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
        this.sequenceService = sequenceService;
        this.externalSystemService = externalSystemService;
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
        List<ApItem> itemsCreated = createItems(createItems, typeIdItemsMap, itemsDb, change, create);
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

    public List<ApItem> createItems(final List<ApItemVO> createItems, final Map<Integer, List<ApItem>> typeIdItemsMap, final List<ApItem> itemsDb, final ApChange change, final CreateFunction create) {
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
            ApItem itemCreated = create.apply(itemType, itemSpec, change, nextItemObjectId(), position);
            dataToSave.add(data);
            itemCreated.setData(data);
            itemsCreated.add(itemCreated);

            itemsDb.add(itemCreated);
            existsItems.add(itemCreated);
        }
        dataRepository.save(dataToSave);
        return itemsCreated;
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

        if (createItem instanceof ItemBoolean) {
            ItemBoolean itemBoolean = (ItemBoolean) createItem;

            itemType = sdp.getItemType(itemBoolean.getT());
            itemSpec = itemBoolean.getS() == null ? null : sdp.getItemSpec(itemBoolean.getS());
            uuid = itemBoolean.getUuid();

            ArrDataBit dataBit = new ArrDataBit();
            dataBit.setValue(itemBoolean.isValue());
            dataBit.setDataType(DataType.BIT.getEntity());
            data = dataBit;
        } else if (createItem instanceof ItemEntityRef) {
            ItemEntityRef itemEntityRef = (ItemEntityRef) createItem;

            itemType = sdp.getItemType(itemEntityRef.getT());
            itemSpec = itemEntityRef.getS() == null ? null : sdp.getItemSpec(itemEntityRef.getS());
            uuid = itemEntityRef.getUuid();

            ArrDataRecordRef dataRecordRef = new ArrDataRecordRef();
            DataRef dataRef = new DataRef(uuid, itemEntityRef.getEr().getEid());
            dataRefList.add(dataRef);

            dataRecordRef.setDataType(DataType.RECORD_REF.getEntity());
            data = dataRecordRef;
        } else if (createItem instanceof ItemEnum) {
            ItemEnum itemEnum = (ItemEnum) createItem;

            itemType = sdp.getItemType(itemEnum.getT());
            itemSpec = itemEnum.getS() == null ? null : sdp.getItemSpec(itemEnum.getS());
            uuid = itemEnum.getUuid();

            ArrDataNull dataNull = new ArrDataNull();
            dataNull.setDataType(DataType.ENUM.getEntity());
            data = dataNull;
        } else if (createItem instanceof ItemInteger) {
            ItemInteger itemInteger = (ItemInteger) createItem;

            itemType = sdp.getItemType(itemInteger.getT());
            itemSpec = itemInteger.getS() == null ? null : sdp.getItemSpec(itemInteger.getS());
            uuid = itemInteger.getUuid();

            ArrDataInteger dataInteger = new ArrDataInteger();
            dataInteger.setValue(itemInteger.getValue().intValue());
            dataInteger.setDataType(DataType.INT.getEntity());
            data = dataInteger;
        } else if (createItem instanceof ItemLink) {
            ItemLink itemLink = (ItemLink) createItem;

            itemType = sdp.getItemType(itemLink.getT());
            itemSpec = itemLink.getS() == null ? null : sdp.getItemSpec(itemLink.getS());
            uuid = itemLink.getUuid();

            ArrDataUriRef dataUriRef = new ArrDataUriRef();
            dataUriRef.setValue(itemLink.getUrl());
            dataUriRef.setDescription(itemLink.getNm());
            dataUriRef.setSchema("");
            dataUriRef.setArrNode(null);
            dataUriRef.setDataType(DataType.URI_REF.getEntity());
            data = dataUriRef;
        } else if (createItem instanceof ItemString) {
            ItemString itemString = (ItemString) createItem;

            itemType = sdp.getItemType(itemString.getT());
            itemSpec = itemString.getS() == null ? null : sdp.getItemSpec(itemString.getS());
            uuid = itemString.getUuid();

            RulDataType dataType = itemType.getDataType();
            String code = dataType.getCode();
            DataType dt = DataType.fromCode(code);
            if (dt == null) {
                throw new IllegalStateException("Neznámý datový typ " + code);
            }
            switch(dt) {
                case STRING:
                    ArrDataString dataString = new ArrDataString();
                    dataString.setValue(itemString.getValue());
                    dataString.setDataType(DataType.STRING.getEntity());
                    data = dataString;
                    break;
                case TEXT:
                    ArrDataText dataText = new ArrDataText();
                    dataText.setValue(itemString.getValue());
                    dataText.setDataType(DataType.TEXT.getEntity());
                    data = dataText;
                    break;
                case COORDINATES:
                    ArrDataCoordinates dataCoordinates = new ArrDataCoordinates();
                    dataCoordinates.setValue(GeometryConvertor.convert(itemString.getValue()));
                    dataCoordinates.setDataType(DataType.COORDINATES.getEntity());
                    data = dataCoordinates;
                    break;
                default:
                    throw new IllegalStateException("Neznámý datový typ " + code);
            }

        } else if (createItem instanceof ItemUnitDate) {
            ItemUnitDate itemUnitDate = (ItemUnitDate) createItem;

            itemType = sdp.getItemType(itemUnitDate.getT());
            itemSpec = itemUnitDate.getS() == null ? null : sdp.getItemSpec(itemUnitDate.getS());
            uuid = itemUnitDate.getUuid();

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

    public List<Object> findNewOrChangedItems(List<Object> items, List<ApBindingItem> bindingItems) {
        List<Object> changedItems = new ArrayList<>();

        return changedItems;
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
