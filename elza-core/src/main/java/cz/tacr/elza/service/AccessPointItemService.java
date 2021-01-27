package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import cz.tacr.cam.schema.cam.ItemBinaryXml;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

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
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.RulDataType;
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
import cz.tacr.elza.service.cam.CamHelper;
import cz.tacr.elza.service.vo.DataRef;

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
    private final AccessPointDataService accessPointDataService;

    public AccessPointItemService(final EntityManager em,
                                  final StaticDataService staticDataService,
                                  final ApItemRepository itemRepository,
                                  final DataRepository dataRepository,
                                  final SequenceService sequenceService,
                                  final ExternalSystemService externalSystemService,
                                  final ApBindingItemRepository bindingItemRepository,
                                  final ApBindingRepository bindingRepository,
                                  final AccessPointDataService accessPointDataService) {
        this.em = em;
        this.staticDataService = staticDataService;
        this.itemRepository = itemRepository;
        this.dataRepository = dataRepository;
        this.sequenceService = sequenceService;
        this.externalSystemService = externalSystemService;
        this.bindingItemRepository = bindingItemRepository;
        this.bindingRepository = bindingRepository;
        this.accessPointDataService = accessPointDataService;
    }

    @FunctionalInterface
    public interface CreateFunction {
        ApItem apply(final RulItemType itemType, final RulItemSpec itemSpec, final ApChange change, final int objectId, final int position);
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
        itemRepository.saveAll(items);
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
        itemRepository.saveAll(items);
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
            ItemType itemType = sdp.getItemTypeById(createItem.getTypeId());
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
            // if(itemType.get)= createItem.getSpecId() == null ? null : sdp.getItemSpecById(createItem.getSpecId());
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

            ApItem itemCreated = create.apply(itemType.getEntity(), itemSpec, change, nextItemObjectId(), position);
            dataToSave.add(data);
            itemCreated.setData(data);
            itemsCreated.add(itemCreated);

            itemsDb.add(itemCreated);
            existsItems.add(itemCreated);

            changeBindingItemsItems(createItem, itemCreated, bindingItemList);
        }
        dataRepository.saveAll(dataToSave);
        return itemsCreated;
    }

    private void setBindingArrDataRecordRef(ArrData data, ApItemVO createItem, List<ApBindingItem> bindingItemList, List<DataRef> dataRefList) {
        if (data instanceof ArrDataRecordRef && createItem instanceof ApItemAccessPointRefVO
                && CollectionUtils.isNotEmpty(bindingItemList) && dataRefList != null) {
            ApItemAccessPointRefVO apItemAccessPointRefVO = (ApItemAccessPointRefVO) createItem;

            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getItem() != null && createItem.getId() != null && apItemAccessPointRefVO.getExternalName() != null &&
                        bindingItem.getItem().getItemId() != null && bindingItem.getItem().getItemId().equals(createItem.getId())) {
                    dataRefList.add(new DataRef(bindingItem.getValue(), apItemAccessPointRefVO.getExternalName()));
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
                bindingItemRepository.saveAll(currentItemBindings);
            }
        }
    }

    public List<ApItem> createItems(final List<Object> createItems,
                                    final ApChange change,
                                    final ApBinding binding,
                                    final List<DataRef> dataRefList,
                                    final CreateFunction create) {
        List<ApItem> itemsCreated = new ArrayList<>();
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();

        for (Object createItem : createItems) {

            ApItem itemCreated = createItem(createItem, change, create, typeIdItemsMap, binding, dataRefList);
            itemsCreated.add(itemCreated);
        }
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
                              final ApBinding binding,
                              final List<DataRef> dataRefList) {
        StaticDataProvider sdp = staticDataService.getData();
        RulItemType itemType;
        RulItemSpec itemSpec;
        String uuid;
        ArrData data;
        if (createItem instanceof ItemBinaryXml) {
            ItemBinaryXml itemBinary = (ItemBinaryXml) createItem;

            itemType = sdp.getItemType(itemBinary.getT().getValue());
            itemSpec = itemBinary.getS() == null ? null : sdp.getItemSpec(itemBinary.getS().getValue());
            uuid = CamHelper.getUuid(itemBinary.getUuid());

            ArrDataCoordinates dataCoordinates = new ArrDataCoordinates();
            String value = accessPointDataService.convertCoordinatesToEWKT(itemBinary.getValue().getValue());
            dataCoordinates.setValue(GeometryConvertor.convert(value));
            dataCoordinates.setDataType(DataType.COORDINATES.getEntity());
            data = dataCoordinates;
        } else if (createItem instanceof ItemBooleanXml) {
            ItemBooleanXml itemBoolean = (ItemBooleanXml) createItem;

            itemType = sdp.getItemType(itemBoolean.getT().getValue());
            itemSpec = itemBoolean.getS() == null ? null : sdp.getItemSpec(itemBoolean.getS().getValue());
            uuid = CamHelper.getUuid(itemBoolean.getUuid());

            ArrDataBit dataBit = new ArrDataBit();
            dataBit.setBitValue(itemBoolean.getValue().isValue());
            dataBit.setDataType(DataType.BIT.getEntity());
            data = dataBit;
        } else if (createItem instanceof ItemEntityRefXml) {
            ItemEntityRefXml itemEntityRef = (ItemEntityRefXml) createItem;

            itemType = sdp.getItemType(itemEntityRef.getT().getValue());
            itemSpec = itemEntityRef.getS() == null ? null : sdp.getItemSpec(itemEntityRef.getS().getValue());
            uuid = CamHelper.getUuid(itemEntityRef.getUuid());

            ArrDataRecordRef dataRecordRef = new ArrDataRecordRef();

            String extIdent = CamHelper.getEntityIdorUuid(itemEntityRef);

            DataRef dataRef = new DataRef(uuid, extIdent);
            dataRefList.add(dataRef);

            dataRecordRef.setDataType(DataType.RECORD_REF.getEntity());
            data = dataRecordRef;
        } else if (createItem instanceof ItemEnumXml) {
            ItemEnumXml itemEnum = (ItemEnumXml) createItem;

            itemType = sdp.getItemType(itemEnum.getT().getValue());
            itemSpec = itemEnum.getS() == null ? null : sdp.getItemSpec(itemEnum.getS().getValue());
            uuid = CamHelper.getUuid(itemEnum.getUuid());

            ArrDataNull dataNull = new ArrDataNull();
            dataNull.setDataType(DataType.ENUM.getEntity());
            data = dataNull;
        } else if (createItem instanceof ItemIntegerXml) {
            ItemIntegerXml itemInteger = (ItemIntegerXml) createItem;

            itemType = sdp.getItemType(itemInteger.getT().getValue());
            itemSpec = itemInteger.getS() == null ? null : sdp.getItemSpec(itemInteger.getS().getValue());
            uuid = CamHelper.getUuid(itemInteger.getUuid());

            ArrDataInteger dataInteger = new ArrDataInteger();
            dataInteger.setIntegerValue(itemInteger.getValue().getValue().intValue());
            dataInteger.setDataType(DataType.INT.getEntity());
            data = dataInteger;
        } else if (createItem instanceof ItemLinkXml) {
            ItemLinkXml itemLink = (ItemLinkXml) createItem;

            itemType = sdp.getItemType(itemLink.getT().getValue());
            itemSpec = itemLink.getS() == null ? null : sdp.getItemSpec(itemLink.getS().getValue());
            uuid = CamHelper.getUuid(itemLink.getUuid());

            ArrDataUriRef dataUriRef = new ArrDataUriRef();
            dataUriRef.setUriRefValue(itemLink.getUrl().getValue());
            dataUriRef.setDescription(itemLink.getNm().getValue());
            dataUriRef.setSchema(ArrDataUriRef.createSchema(itemLink.getUrl().getValue()));
            dataUriRef.setArrNode(null);
            dataUriRef.setDataType(DataType.URI_REF.getEntity());
            data = dataUriRef;
        } else if (createItem instanceof ItemStringXml) {
            ItemStringXml itemString = (ItemStringXml) createItem;

            itemType = sdp.getItemType(itemString.getT().getValue());
            itemSpec = itemString.getS() == null ? null : sdp.getItemSpec(itemString.getS().getValue());
            uuid = CamHelper.getUuid(itemString.getUuid());

            RulDataType dataType = itemType.getDataType();
            String code = dataType.getCode();
            DataType dt = DataType.fromCode(code);
            if (dt == null) {
                throw new IllegalStateException("Neznámý datový typ " + code);
            }
            switch(dt) {
                case STRING:
                    ArrDataString dataString = new ArrDataString();
                    dataString.setStringValue(itemString.getValue().getValue());
                    dataString.setDataType(DataType.STRING.getEntity());
                    data = dataString;
                    break;
                case TEXT:
                    ArrDataText dataText = new ArrDataText();
                    dataText.setTextValue(itemString.getValue().getValue());
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
                    throw new IllegalStateException("Nepodporovaný datový typ uložen jako řetězec: " + code +
                            ", itemType:" + itemString.getT().getValue());
            }

        } else if (createItem instanceof ItemUnitDateXml) {
            ItemUnitDateXml itemUnitDate = (ItemUnitDateXml) createItem;

            itemType = sdp.getItemType(itemUnitDate.getT().getValue());
            itemSpec = itemUnitDate.getS() == null ? null : sdp.getItemSpec(itemUnitDate.getS().getValue());
            uuid = CamHelper.getUuid(itemUnitDate.getUuid());

            CalendarType calType = CalendarType.GREGORIAN;
            ArrDataUnitdate dataUnitDate = new ArrDataUnitdate();
            dataUnitDate.setValueFrom(itemUnitDate.getF().trim());
            dataUnitDate.setValueFromEstimated(itemUnitDate.isFe());
            if (dataUnitDate.getValueFromEstimated() == null) {
                dataUnitDate.setValueFromEstimated(false);
            }
            dataUnitDate.setFormat(itemUnitDate.getFmt());
            dataUnitDate.setValueTo(itemUnitDate.getTo().trim());
            dataUnitDate.setValueToEstimated(itemUnitDate.isToe());
            if (dataUnitDate.getValueToEstimated() == null) {
                dataUnitDate.setValueToEstimated(false);
            }
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

        // check specification (if correctly used)
        Boolean useSpec = itemType.getUseSpecification();
        if (useSpec != null && useSpec) {
            if (itemSpec == null) {
                throw new BusinessException("Received item without specification, itemType: " + itemType.getName(),
                        BaseCode.PROPERTY_IS_INVALID)
                                .set("itemType", itemType.getCode())
                                .set("itemTypeName", itemType.getName());
            }
        } else {
            if (itemSpec != null) {
                throw new BusinessException("Received item with unexpected specification, itemType: " + itemType
                        .getName(), BaseCode.PROPERTY_IS_INVALID)
                                .set("itemType", itemType.getCode())
                                .set("itemTypeName", itemType.getName());
            }
        }


        List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());
        int position = nextPosition(existsItems);


        ApItem itemCreated = create.apply(itemType, itemSpec, change, nextItemObjectId(), position);
        dataRepository.save(data);
        itemCreated.setData(data);
        itemRepository.save(itemCreated);

        if (binding != null) {
            externalSystemService.createApBindingItem(binding, uuid, null, itemCreated);
        }

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

    public List<Object> findNewOrChangedItems(List<Object> items, List<ApBindingItem> bindingItems,
                                              List<ApBindingItem> notChangeItems) {
        List<Object> changedItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(items)) {
            for (Object item : items) {
                if (item instanceof ItemBinaryXml) {
                    ItemBinaryXml itemBinary = (ItemBinaryXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemBinary.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemBinary);
                    } else {
                        ApItem is = bindingItem.getItem();
                        ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) is.getData();
                        String value = GeometryConvertor.convert(dataCoordinates.getValue());
                        String xmlValue = accessPointDataService.convertCoordinatesToEWKT(itemBinary.getValue().getValue());
                        if (!(is.getItemType().getCode().equals(itemBinary.getT().getValue()) &&
                                compareItemSpec(is.getItemSpec(), itemBinary.getS()) &&
                                xmlValue.equals(value))) {
                            changedItems.add(itemBinary);
                        } else {
                            notChangeItems.add(bindingItem);
                            bindingItems.remove(bindingItem);
                        }
                    }
                } else if (item instanceof ItemBooleanXml) {
                    ItemBooleanXml itemBoolean = (ItemBooleanXml) item;
                    ApBindingItem bindingItem = findBindingItemByUuid(bindingItems, itemBoolean.getUuid().getValue());

                    if (bindingItem == null) {
                        changedItems.add(itemBoolean);
                    } else {
                        ApItem ib = bindingItem.getItem();
                        ArrDataBit dataBit = (ArrDataBit) ib.getData();
                        if (!(ib.getItemType().getCode().equals(itemBoolean.getT().getValue()) &&
                                compareItemSpec(ib.getItemSpec(), itemBoolean.getS()) &&
                                dataBit.isBitValue().equals(itemBoolean.getValue().isValue()))) {
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
                        String entityRefId = CamHelper.getEntityIdorUuid(entityRecordRef);
                        if (!(ier.getItemType().getCode().equals(itemEntityRef.getT().getValue()) &&
                                compareItemSpec(ier.getItemSpec(), itemEntityRef.getS()) &&
                                dataRecordRef.getBinding().getValue().equals(entityRefId))) {

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
                                dataInteger.getIntegerValue().equals(itemInteger.getValue().getValue().intValue()))) {

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
                                dataUriRef.getUriRefValue().equals(itemLink.getUrl().getValue()) &&
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
                                value = dataString.getStringValue();
                                break;
                            case TEXT:
                                ArrDataText dataText = (ArrDataText) is.getData();
                                value = dataText.getTextValue();
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

    public static void normalize(ArrDataUnitdate aeDataUnitdate) {

        CalendarType calendarType = CalendarType.GREGORIAN;

        String valueFrom = aeDataUnitdate.getValueFrom();
        if (valueFrom == null) {
            aeDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
        } else {
            LocalDateTime fromDate = LocalDateTime.parse(valueFrom.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendarType, fromDate));
        }

        String valueTo = aeDataUnitdate.getValueTo();
        if (valueTo == null) {
            aeDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
        } else {
            LocalDateTime toDate = LocalDateTime.parse(valueTo.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, toDate));
        }
    }

}
