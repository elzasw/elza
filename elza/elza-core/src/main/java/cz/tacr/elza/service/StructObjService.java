package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import cz.tacr.elza.common.db.HibernateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ArrStructuredObject.State;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.StructObjCode;
import cz.tacr.elza.packageimport.xml.SettingStructureTypes;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Servisní třída pro práci se strukturovanými datovými typy.
 *
 * @since 06.11.2017
 */
@Service
public class StructObjService {

    private final static Logger logger = LoggerFactory.getLogger(StructObjService.class);
    private final StructuredItemRepository structureItemRepository;
    private final StructuredTypeExtensionRepository structureExtensionRepository;
    private final StructuredObjectRepository structObjRepository;
    private final StructuredTypeRepository structureTypeRepository;
    private final ArrangementInternalService arrangementInternalService;
    private final DataRepository dataRepository;
    private final FundStructureExtensionRepository fundStructureExtensionRepository;
    private final StructObjValueService structObjService;
    private final ItemTypeRepository itemTypeRepository;
    private final EventNotificationService notificationService;
    private final SettingsService settingsService;
    private final StaticDataService staticDataService;
    private final StructObjInternalService structObjInternalService;
    private final PartTypeRepository partTypeRepository;
    private final DataService dataService;

    @Autowired
    public StructObjService(final StructuredItemRepository structureItemRepository,
                            final StructuredTypeExtensionRepository structureExtensionRepository,
                            final StructuredObjectRepository structureDataRepository,
                            final StructuredTypeRepository structureTypeRepository,
                            final ArrangementInternalService arrangementInternalService,
                            final DataRepository dataRepository,
                            final FundStructureExtensionRepository fundStructureExtensionRepository,
                            final StructObjValueService structureDataService,
                            final ItemTypeRepository itemTypeRepository,
                            final EventNotificationService notificationService,
                            final SettingsService settingsService,
                            final StaticDataService staticDataService,
                            final StructObjInternalService structObjInternalService,
                            final PartTypeRepository partTypeRepository,
                            final DataService dataService) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionRepository = structureExtensionRepository;
        this.structObjRepository = structureDataRepository;
        this.structureTypeRepository = structureTypeRepository;
        this.arrangementInternalService = arrangementInternalService;
        this.dataRepository = dataRepository;
        this.fundStructureExtensionRepository = fundStructureExtensionRepository;
        this.structObjService = structureDataService;
        this.itemTypeRepository = itemTypeRepository;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.staticDataService = staticDataService;
        this.structObjInternalService = structObjInternalService;
        this.partTypeRepository = partTypeRepository;
        this.dataService = dataService;
    }

    /**
     * Vyhledá platné položky k hodnotě strukt. datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return nalezené položky
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrStructuredItem> findStructureItems(@AuthParam(type = AuthParam.Type.FUND) final ArrStructuredObject structureObject) {
        return structObjService.findByStructuredObjectAndDeleteChangeIsNullFetchData(structureObject);
    }

    /**
     * Vyhledá platné položky k hodnotám strukt. datového typu.
     *
     * @param structureDataList hodnoty struktovaného datového typu
     * @return hodnota strukt. datového typu -> nalezené položky
     */
    public Map<ArrStructuredObject, List<ArrStructuredItem>> findStructureItems(final List<ArrStructuredObject> structureDataList) {
        List<List<ArrStructuredObject>> parts = Lists.partition(structureDataList, ObjectListIterator.getMaxBatchSize());
        List<ArrStructuredItem> structureItems = new ArrayList<>();
        for (List<ArrStructuredObject> part : parts) {
            structureItems.addAll(findByStructuredObjectListAndDeleteChangeIsNullFetchData(part));
        }
        return structureItems.stream().collect(Collectors.groupingBy(ArrStructuredItem::getStructuredObject));
    }

    /**
     * Vytvoření strukturovaných dat jako součást větší transakce
     */
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR })
    public ArrStructuredObject createStructObj(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                               final ArrChange change,
                                               final RulStructuredType structureType,
                                               final ArrStructuredObject.State state,
                                               final String uuid,
                                               List<ArrStructuredItem> items) {
        ArrStructuredObject structureData = new ArrStructuredObject.Builder(change, fund, structureType)
                .setState(state)
                .setUuid(uuid)
                .build();

        ArrStructuredObject structObj = structObjRepository.save(structureData);

        // store items
        if (items != null) {
            for (ArrStructuredItem item : items) {
                createItem(item, structObj, item.getPosition(), change);
            }
        }

        if (state == ArrStructuredObject.State.TEMP) {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureType.getCode(),
                    Collections.singletonList(structObj.getStructuredObjectId()),
                    null,
                    null,
                    null));
        } else {
            if (state != State.TEMP) {
                structObjService.addToValidate(structObj);
            }

            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureType.getCode(),
                    null,
                    Collections.singletonList(structObj.getStructuredObjectId()),
                    null,
                    null));
        }
        return structObj;
    }

    /**
     * Vytvoření strukturovaných dat.
     *
     * @param fund
     *            archivní soubor
     * @param structureType
     *            strukturovaný typ
     * @return vytvořená entita
     */
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR })
    public ArrStructuredObject createStructObj(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                               final RulStructuredType structureType,
                                               final ArrStructuredObject.State state) {
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA);
        return createStructObj(fund, change, structureType, state, null, null);
    }

    /**
     * Hromadné vytvoření strukturovaných dat.
     *
     * @param fund          archivní soubor
     * @param structureType strukturovaný typ
     * @param state         stav
     * @param change        změna
     * @param count         počet vytvářených položek
     * @return vytvořené entity
     */
    private List<ArrStructuredObject> createStructObjList(final ArrFund fund,
                                                              final RulStructuredType structureType,
                                                              final ArrStructuredObject.State state,
                                                              final ArrChange change,
                                                              int count) {
        List<ArrStructuredObject> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {

            ArrStructuredObject structureData = new ArrStructuredObject.Builder(change, fund, structureType)
                    .setState(state)
                    .build();

            result.add(structureData);
        }
        return structObjRepository.saveAll(result);
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param structObj
     *            hodnota struktovaného datového typu
     *
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<Integer> deleteStructObj(@AuthParam(type = AuthParam.Type.FUND) final Integer fundId,
                                         final List<ArrStructuredObject> structObjs) {
        return structObjInternalService.deleteStructObj(structObjs, null);
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param structObj
     *            hodnota struktovaného datového typu
     *
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void deleteStructObj(@AuthParam(type = AuthParam.Type.FUND) final Integer fundId,
                                final List<ArrStructuredObject> structObjs, ArrChange change) {
        structObjInternalService.deleteStructObj(structObjs, change);
    }

    /**
     * Nastavení přiřaditelnosti.
     *
     * @param fund              archivní soubor
     * @param structureDataList hodnoty strukturovaného datového typu
     * @param assignable        přiřaditelný
     * @return upravené entity
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrStructuredObject> setAssignableStructureDataList(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                                    final List<ArrStructuredObject> structureDataList,
                                                                    final boolean assignable) {
        if (structureDataList.size() == 0) {
            return Collections.emptyList();
        }
        for (ArrStructuredObject structureData : structureDataList) {
            if (structureData.getDeleteChange() != null) {
                throw new BusinessException("Nelze změnit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
            }
            if (!assignable && structureData.getErrorDescription() != null) {
                throw new BusinessException("Nelze zavřít, pokud strukturovaná data obsahují chyby", BaseCode.INVALID_STATE);
            }
            structureData.setAssignable(assignable);
        }

        notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                structureDataList.get(0).getStructuredType().getCode(),
                null,
                null,
                structureDataList.stream().map(ArrStructuredObject::getStructuredObjectId).collect(Collectors.toList()),
                null));
        return structObjRepository.saveAll(structureDataList);
    }

    /**
     * Vytvoření položky k hodnotě strukt. datového typu.
     *
     * @param structureItem   položka
     * @param structObjId identifikátor hodnoty strukt. datového typu
     * @param fundVersionId   identifikátor verze AS
     * @return vytvořená entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredItem createStructureItem(final ArrStructuredItem structureItem,
                                                 final Integer structObjId,
                                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {

        ArrStructuredObject structObj = getStructObjById(structObjId);

        ArrChange change = getChangeForStructObject(structObj, ArrChange.Type.ADD_STRUCTURE_ITEM);

        int nextPosition = findNextPosition(structObj, structureItem.getItemType());
        Integer position = getPosition(structureItem.getPosition(), structureItem.getItemType(), structObj, change, nextPosition);



        ArrStructuredItem save = createItem(structureItem, structObj, position, change);
        structObjService.addToValidate(structObj);
        return save;
    }

    protected ArrChange getChangeForStructObject(final ArrStructuredObject structObj, final ArrChange.Type type) {
        return arrangementInternalService.createChange(type);
    }

    protected Integer getPosition(final Integer positionWant, final RulItemType type, final ArrStructuredObject structObj, final ArrChange change, final int nextPosition) {
        Integer position;
        if (positionWant == null) {
            position = nextPosition;
        } else {
            position = positionWant;

            if (position <= 0) {
                throw new SystemException("Neplatný formát dat", BaseCode.PROPERTY_IS_INVALID).set("property", "position");
            }

            // pokud je požadovaná pozice menší než další volná, bude potřeba posunou níž položky
            if (position < nextPosition) {
                List<ArrStructuredItem> structureItemsToMove = findOpenItemsAfterPositionFetchData(type,
                                                             structObj, position - 1, null);

                nextVersionStructureItems(1, structureItemsToMove, change, true);
            }

            // pokud je požadovaná pozice větší než další volná, použije se další volná
            if (position > nextPosition) {
                position = nextPosition;
            }

        }
        return position;
    }

    private ArrStructuredItem createItem(ArrStructuredItem structureItem, ArrStructuredObject structObj,
                                         Integer position,
                                         ArrChange change) {

        ArrData data = createData(HibernateUtils.unproxy(structureItem.getData()), structureItem.getItemType().getDataType());

        ArrStructuredItem createStructureItem = new ArrStructuredItem();
        createStructureItem.setData(data);
        createStructureItem.setCreateChange(change);
        createStructureItem.setPosition(position);
        createStructureItem.setStructuredObject(structObj);
        createStructureItem.setDescItemObjectId(arrangementInternalService.getNextDescItemObjectId());
        createStructureItem.setItemType(structureItem.getItemType());
        createStructureItem.setItemSpec(structureItem.getItemSpec());

        ArrStructuredItem save = structureItemRepository.save(createStructureItem);
        return save;
    }

    /**
     * Odverzování položek.
     *
     * @param moveDiff             pokud je různé od 0, provede změnu pozic
     * @param structureItems       položky, které odverzováváme
     * @param change               změna, pod kterou se odverzovává
     * @param createNewDataVersion true - provede se odverzování i návazných dat
     * @return nově odverzované položky
     */
    private List<ArrStructuredItem> nextVersionStructureItems(final int moveDiff,
                                                              final List<ArrStructuredItem> structureItems,
                                                              final ArrChange change,
                                                              final boolean createNewDataVersion) {
        if (structureItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<ArrStructuredItem> resultStructureItems = new ArrayList<>(structureItems.size());

        for (ArrStructuredItem structureItem : structureItems) {
            // make copy without data and item_id
            ArrStructuredItem newStructureItem = structureItem.makeCopy();
            newStructureItem.setItemId(null);

            structureItem.setDeleteChange(change);
            newStructureItem.setCreateChange(change);
            if (moveDiff != 0) {
                newStructureItem.setPosition(newStructureItem.getPosition() + moveDiff);
            }

            resultStructureItems.add(newStructureItem);
        }

        structureItemRepository.saveAll(structureItems); // uložení změny deleteChange
        structureItemRepository.flush();

        if (createNewDataVersion) {
            List<ArrData> resultDataList = new ArrayList<>(resultStructureItems.size());
            for (ArrStructuredItem newStructureItem : resultStructureItems) {
                ArrData newData = ArrData.makeCopyWithoutId(HibernateUtils.unproxy(newStructureItem.getData()));
                newStructureItem.setData(newData);
                resultDataList.add(newData);
            }
            dataRepository.saveAll(resultDataList);
        }

        return structureItemRepository.saveAll(resultStructureItems);
    }

    /**
     * Vyhledá volnou pozici pro typ atributu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @param itemType      typ atributu
     * @return pozice pro další položku
     */
    private int findNextPosition(final ArrStructuredObject structureData, final RulItemType itemType) {
        List<ArrStructuredItem> structureItems = structureItemRepository.findOpenItemsAfterPosition(itemType,
                structureData, 0, PageRequest.of(0, 1, Sort.Direction.DESC, ArrItem.FIELD_POSITION));
        if (structureItems.size() == 0) {
            return 1;
        } else {
            return structureItems.get(0).getPosition() + 1;
        }
    }

    /**
     * Úprava položky k hodnotě strukt. datového typu.
     *
     * @param structureItem    položka
     * @param fundVersionId    identifikátor verze AS
     * @param createNewVersion true - verzovaná změna
     * @return upravená entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredItem updateStructureItem(final ArrStructuredItem structureItem,
                                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                 final boolean createNewVersion) {

        ArrStructuredItem structureItemDB = findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

        ArrStructuredObject structObj = structureItemDB.getStructuredObject();
        ArrChange change = getChangeForStructObject(structObj, ArrChange.Type.UPDATE_STRUCTURE_ITEM);

        ArrStructuredItem updateStructureItem;

        if (createNewVersion) {

            structureItemDB.setDeleteChange(change);
            structureItemRepository.save(structureItemDB);
            structureItemRepository.flush();

            Integer positionDB = structureItemDB.getPosition();
            Integer positionChange = structureItem.getPosition();

            Integer position;
            if (positionDB.equals(positionChange)) {
                position = positionDB;
            } else {
                int nextPosition = findNextPosition(structureItemDB.getStructuredObject(), structureItemDB.getItemType());

                if (positionChange == null || (positionChange > nextPosition - 1)) {
                    positionChange = nextPosition;
                }

                List<ArrStructuredItem> structureItemsToMove;
                Integer moveDiff;

                if (positionChange < positionDB) {
                    moveDiff = 1;
                    structureItemsToMove = structureItemRepository.findOpenItemsBetweenPositions(structureItemDB.getItemType(), structureItemDB.getStructuredObject(), positionChange, positionDB - 1);
                } else {
                    moveDiff = -1;
                    structureItemsToMove = structureItemRepository.findOpenItemsBetweenPositions(structureItemDB.getItemType(), structureItemDB.getStructuredObject(), positionDB + 1, positionChange);
                }

                nextVersionStructureItems(moveDiff, structureItemsToMove, change, false);

                position = positionChange;
            }


            ArrData updateData = updateData(HibernateUtils.unproxy(structureItem.getData()), structureItemDB.getItemType().getDataType());

            updateStructureItem = new ArrStructuredItem();
            updateStructureItem.setData(updateData);
            updateStructureItem.setCreateChange(change);
            updateStructureItem.setPosition(position);
            updateStructureItem.setStructuredObject(structureItemDB.getStructuredObject());
            updateStructureItem.setDescItemObjectId(structureItemDB.getDescItemObjectId());
            updateStructureItem.setItemType(structureItemDB.getItemType());
            updateStructureItem.setItemSpec(structureItem.getItemSpec());
        } else {
            updateStructureItem = structureItemDB;
            updateStructureItem.setItemSpec(structureItem.getItemSpec());
            // db data item
            ArrData updateData = HibernateUtils.unproxy(updateStructureItem.getData());
            // prepare dataToDb
            updateData.merge(HibernateUtils.unproxy(structureItem.getData()));
            dataRepository.save(updateData);
        }

        ArrStructuredItem save = structureItemRepository.save(updateStructureItem);
        structObjService.addToValidate(structObj);

        return save;
    }

    /**
     * Smazání položky k hodnotě strukt. datového typu.
     *
     * @param itemObjectId položka
     * @param fundVersionId identifikátor verze AS
     * @return smazaná položka
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredItem deleteStructureItem(final Integer itemObjectId,
                                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {

        ArrStructuredItem structureItemDB = findOpenItemFetchData(itemObjectId);
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + itemObjectId, BaseCode.ID_NOT_EXIST)
            	.setId(itemObjectId);
        }

        ArrStructuredObject structObj = structureItemDB.getStructuredObject();
        ArrChange change = getChangeForStructObject(structObj, ArrChange.Type.DELETE_STRUCTURE_ITEM);

        structureItemDB.setDeleteChange(change);

        ArrStructuredItem save = structureItemRepository.save(structureItemDB);
        structObjService.addToValidate(structObj);

        return save;
    }

    public List<ArrStructuredItem> findByStructObjIdAndDeleteChangeIsNullFetchData(Integer structuredObjectId) {
        return dataService.findItemsWithData(structureItemRepository.findByStructObjIdAndDeleteChangeIsNull(structuredObjectId));
    }

    private List<ArrStructuredItem> findByStructuredObjectListAndDeleteChangeIsNullFetchData(List<ArrStructuredObject> structuredObjectList) {
        return dataService.findItemsWithData(structureItemRepository.findByStructuredObjectListAndDeleteChangeIsNull(structuredObjectList));
    }

    private List<ArrStructuredItem> findOpenItemsAfterPositionFetchData(RulItemType itemType,
                                                                        ArrStructuredObject structuredObject,
                                                                        Integer position,
                                                                        Pageable pageRequest) {
        return dataService.findItemsWithData(structureItemRepository.findOpenItemsAfterPosition(itemType, structuredObject, position, pageRequest));
    }

    private ArrStructuredItem findOpenItemFetchData(Integer itemObjectId) {
        return dataService.findItemWithData(structureItemRepository.findOpenItem(itemObjectId));
    }

    /**
     * Vytvoření dat. Provede kopii z předlohy a založení v DB.
     *
     * @param data     předloha dat
     * @param dataType datový typ dat
     * @return vytvořená data
     */
    private ArrData createData(final ArrData data, final RulDataType dataType) {
        ArrData copy = ArrData.makeCopyWithoutId(data);
        copy.setDataType(dataType);
        return dataRepository.save(copy);
    }

    /**
     * Úprava dat. Provede uložení změněných dat, popřípadně založení kopie.
     *
     * @param data             ukládaná data
     * @param dataType         datový typ dat
     * @return uložená / nová data
     */
    private ArrData updateData(final ArrData data, final RulDataType dataType) {
        ArrData copy = ArrData.makeCopyWithoutId(data);
        copy.setDataType(dataType);
        return dataRepository.save(copy);
    }

    /**
     * Provede revalidaci předaných hodnot strukturovaného typu.
     *
     * @param structureDataList strukturovaný typ
     */
    private List<ArrStructuredObject> revalidateStructureData(final List<ArrStructuredObject> structureDataList) {
        Validate.notNull(structureDataList, "Musí být vyplněn list hodnot strukt. typu");

        for (ArrStructuredObject structureData : structureDataList) {
            structureData.setValue(null);
            structureData.setComplement(null);
            structureData.setErrorDescription(null);
        }
        structObjRepository.saveAll(structureDataList);
        structObjService.addToValidate(structureDataList);
        return structureDataList;
    }

    private void revalidateStructObjs(RulStructuredType structuredType, ArrFund fund) {
        List<Integer> structureDataIds = structObjRepository.findStructuredObjectIdByStructureTypeFund(structuredType,
                                                                                                       fund);
        structObjService.addIdsToValidate(structureDataIds);
    }

    /**
     * Provede revalidaci podle rozšíření strukt. typu.
     *
     * @param structureExtensions revalidované typy
     */
    public void revalidateStructureExtensions(final Collection<RulStructuredTypeExtension> structureExtensions) {
        if (structureExtensions.isEmpty()) {
            return;
        }
        List<Integer> structureDataIds = structObjRepository.findStructuredObjectIdByActiveStructureExtensions(structureExtensions);
        structObjService.addIdsToValidate(structureDataIds);
    }

    /**
     * Vrátí strukt. typ podle kódu.
     *
     * @param structureTypeCode kód strukt. typu
     * @return entita
     */
    public RulStructuredType getStructureTypeByCode(final String structureTypeCode) {
        RulStructuredType structureType = structureTypeRepository.findByCode(structureTypeCode);
        if (structureType == null) {
            throw new ObjectNotFoundException("Strukturovaný typ neexistuje: " + structureTypeCode, BaseCode.ID_NOT_EXIST).setId(structureTypeCode);
        }
        return structureType;
    }

    /**
     * Vrátí typ částí podle kódu.
     *
     * @param partTypeCode kód typu části
     * @return entita
     * @deprecated use {@link StructObjInternalService#getPartTypeByCode(java.lang.String)}
     */
    @Deprecated
    public RulPartType getPartTypeByCode(final String partTypeCode) {
        return structObjInternalService.getPartTypeByCode(partTypeCode);
    }

    /**
     * Vrátí strukt. data podle identifikátoru včetně načtených návazných entit.
     *
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @return entita
     */
    public ArrStructuredObject getStructObjById(final Integer structureDataId) {
        ArrStructuredObject structureData = structObjRepository.findOneFetch(structureDataId);
        if (structureData == null) {
            throw new ObjectNotFoundException("Strukturovaná data neexistují: " + structureDataId, BaseCode.ID_NOT_EXIST).setId(structureDataId);
        }
        return structureData;
    }

    public ArrStructuredObject getExistingStructObjByUUID(String uuid) {
        ArrStructuredObject structureData = structObjRepository.findActiveByUuidOneFetch(uuid);
        if (structureData == null) {
            throw new ObjectNotFoundException("Structured object not found, uuid: " + uuid, BaseCode.ID_NOT_EXIST)
                    .setId(uuid);
        }
        return structureData;
    }

    /**
     * Vrátí strukt. data podle identifikátorů včetně načtených návazných entit.
     *
     * Funkce zachovává pořadí vracených objektů.
     *
     * @param structureDataIds
     *            identifikátory hodnoty strukt. datového typu
     * @return entity strukturované typy ve stejném pořadí jako byl požadavek
     */
    public List<ArrStructuredObject> getStructObjByIds(final List<Integer> structureDataIds) {
        List<List<Integer>> idsParts = Lists.partition(structureDataIds, ObjectListIterator.getMaxBatchSize());

        final Map<Integer, ArrStructuredObject> soMap = new HashMap<>();
        for (List<Integer> idsPart : idsParts) {
            List<ArrStructuredObject> objs = structObjRepository.findByIdsFetch(idsPart);
            objs.forEach(so -> soMap.put(so.getStructuredObjectId(), so));
        }
        // final sort
        List<ArrStructuredObject> structureDataList = new ArrayList<>();
        List<Integer> notFoundItems = null;
        for (Integer srcId : structureDataIds) {
            ArrStructuredObject so = soMap.get(srcId);
            if (so == null) {
                if (notFoundItems == null) {
                    notFoundItems = new ArrayList<>();
                }
                notFoundItems.add(srcId);
            } else {
                structureDataList.add(so);
            }
        }
        if (notFoundItems != null) {
            throw new ObjectNotFoundException("Nenalezeny všechny strukturované typye", BaseCode.ID_NOT_EXIST)
                    .setId(notFoundItems);
        }
        return structureDataList;
    }

    /**
     * Vrátí strukt. data podle identifikátoru včetně načtených návazných entit.
     *
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @param fundVersion     verze AS
     * @return entita
     */
    @AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
    public ArrStructuredObject getStructObjById(final Integer structureDataId,
                                                    @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        return getStructObjById(structureDataId);
    }

    /**
     * Odstranění položek u strukt. dato podle typu atributu.
     *
     * @param fundVersionId   identifikátor verze AS
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @param itemTypeId      identifikátor typu atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredObject deleteStructureItemsByType(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                          final Integer structureDataId,
                                                          final Integer itemTypeId) {
        ArrStructuredObject structObj = getStructObjById(structureDataId);
        List<ArrStructuredItem> structureItems = structureItemRepository.findOpenItems(itemTypeId, structObj);

        ArrChange change = getChangeForStructObject(structObj, ArrChange.Type.DELETE_STRUCTURE_ITEM);
        for (ArrStructuredItem structureItem : structureItems) {
            structureItem.setDeleteChange(change);
        }

        structureItemRepository.saveAll(structureItems);

        structObjService.addToValidate(structObj);

        return structObj;
    }

    /**
     * Potvrzení hodnoty strukt. datového typu.
     *
     * @param fund          archivní soubor
     * @param structureData hodnota struktovaného datového typu
     * @return entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredObject confirmStructureData(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                    final ArrStructuredObject structureData) {
        return confirmInternal(fund, structureData);
    }

    /**
     * Potvrzení hodnoty strukt. datového typu.
     *
     * @param fund          archivní soubor
     * @param structureData hodnota struktovaného datového typu
     * @return entita
     */
    private ArrStructuredObject confirmInternal(final ArrFund fund,
                                                     final ArrStructuredObject structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze potvrdit smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        if (!structureData.getState().equals(ArrStructuredObject.State.TEMP)) {
            throw new BusinessException("Strukturovaná data nemají dočasný stav", BaseCode.INVALID_STATE);
        }
        int itemCount = structureItemRepository
                .countItemsByStructuredObjectAndDeleteChangeIsNull(structureData);
        if (itemCount == 0) {
            throw new BusinessException("Structured object without items cannot be confirmed.",
                    StructObjCode.NO_VALID_ITEMS)
                            .set("structObjId", structureData.getStructuredObjectId());
        }
        // reset temporary value -> final have to be calculated
        structureData.setValue(null);
        structureData.setComplement(null);
        structureData.setState(ArrStructuredObject.State.OK);
        ArrStructuredObject savedStructObj = structObjRepository.save(structureData);
        structObjService.addToValidate(savedStructObj);
        return savedStructObj;
    }

    /**
     * Vyhledání všech strukturovaných typů.
     *
     * @return nalezené entity
     */
    public List<RulStructuredType> findStructureTypes() {
        return structureTypeRepository.findAll();
    }

    /**
     * Vyhledání všech typů částí.
     *
     * @return nalezené entity
     */
    public List<RulPartType> findPartTypes() {
        return partTypeRepository.findAll();
    }

    /**
     * Vyhledání hodnot struktovaného datového typu.
     *
     * @param structureType strukturovaný typ
     * @param fund          archivní soubor
     * @param search        fulltext (může být prázdná)
     * @param assignable    hodnota je přiřaditelná (pokud je null, není bráno v potaz)
     * @param from          od položky
     * @param count         maximální počet položek
     * @return nalezené položky
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR,
    		UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public FilteredResult<ArrStructuredObject> findStructureData(final RulStructuredType structureType,
                                                                 @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                                 @Nullable final String search,
                                                                 @Nullable final Boolean assignable,
                                                                 final int from,
                                                                 final int count) {
        return structObjRepository.findStructureData(structureType.getStructuredTypeId(),
                                                     fund.getFundId(), search, assignable, from, count);
    }

    /**
     * Nastaví konkrétní rozšíření na AS.
     *
     * @param fundVersion         verze AS
     * @param structureType       strukturovaný typ
     * @param structureExtensions seznam rozšíření, které mají být aktivovány na AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public void setFundStructureExtensions(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                           final RulStructuredType structureType,
                                           final List<RulStructuredTypeExtension> structureExtensions) {
        for (RulStructuredTypeExtension se : structureExtensions) {
            validateFundStructureExtension(fundVersion, structureType, se);
        }

        List<ArrFundStructureExtension> fundStructureExtensions = fundStructureExtensionRepository.findByFundAndDeleteChangeIsNull(fundVersion.getFund());

        List<ArrFundStructureExtension> fundStructureExtensionsDelete = new ArrayList<>(fundStructureExtensions);
        fundStructureExtensionsDelete.removeIf(fundStructureExtension -> structureExtensions.contains(fundStructureExtension.getStructuredTypeExtension()));

        List<ArrFundStructureExtension> fundStructureExtensionsCreate = new ArrayList<>();

        for (RulStructuredTypeExtension structureExtension : structureExtensions) {
            boolean exists = false;
            for (ArrFundStructureExtension fundStructureExtension : fundStructureExtensions) {
                if (structureExtension.equals(fundStructureExtension.getStructuredTypeExtension())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                ArrFundStructureExtension fundStructureExtensionCreate = new ArrFundStructureExtension();
                fundStructureExtensionCreate.setFund(fundVersion.getFund());
                fundStructureExtensionCreate.setStructuredTypeExtension(structureExtension);
                fundStructureExtensionsCreate.add(fundStructureExtensionCreate);
            }
        }

        if (fundStructureExtensionsCreate.size() > 0 || fundStructureExtensionsDelete.size() > 0) {
            final ArrChange change = arrangementInternalService.createChange(ArrChange.Type.SET_FUND_STRUCTURE_EXT);
            fundStructureExtensionsCreate.forEach(fse -> fse.setCreateChange(change));
            fundStructureExtensionsDelete.forEach(fse -> fse.setDeleteChange(change));
            fundStructureExtensionRepository.saveAll(fundStructureExtensionsCreate);
            fundStructureExtensionRepository.saveAll(fundStructureExtensionsDelete);

            revalidateStructObjs(structureType, fundVersion.getFund());
        }
    }

    /**
     * Přidání položek struk. objektu z textové hodnoty.
     *
     * @param structuredObject strukturovaný objekt pro který zakládáme položky
     * @param value textová hodnota, která je parsována
     */
    public void addItemsFromValue(final ArrStructuredObject structuredObject, final String value) {
    	Objects.requireNonNull(structuredObject);
    	Objects.requireNonNull(value);
        StructObjValueService.ParseResult parseResult = structObjService.parseValue(structuredObject, value.trim());
        if (parseResult == null) {
            return; // typ nemá parsovací script, nic neděláme
        }
        Map<String, Object> items = parseResult.getItems();
        if (items.size() == 0) {
            logger.warn("Nepodařilo se získá žádné položky z '{}' pro strukturovaný objekt '{}'", value, structuredObject.getStructuredType().getCode());
            return;
        }
        addItemsFromMap(items, structuredObject);
        structObjService.addToValidate(structuredObject);
    }

    /**
     * Přidání položek struk. objektu.
     *
     * @param items            položky pro přidání [key - kód typu atributu, value - hodnota]
     * @param structuredObject strukturovaný objekt pro který zakládáme položky
     */
    private void addItemsFromMap(final Map<String, Object> items, final ArrStructuredObject structuredObject) {
        StaticDataProvider sdp = staticDataService.getData();

        for (Map.Entry<String, Object> entry : items.entrySet()) {
            ItemType itemType = sdp.getItemTypeByCode(entry.getKey());
            createStructureItem(itemType, entry.getValue(), structuredObject);
        }
    }

    /**
     * Vytvoření položky k hodnotě strukt. datového typu.
     *
     * @param itemType  typ atributu
     * @param value     hodnota atributu
     * @param structObj struk. objekt ke kterému zakládáme položku
     */
    private void createStructureItem(final ItemType itemType, final Object value, final ArrStructuredObject structObj) {

        ArrChange change = getChangeForStructObject(structObj, ArrChange.Type.ADD_STRUCTURE_ITEM);

        int nextPosition = findNextPosition(structObj, itemType.getEntity());
        Integer position = getPosition(null, itemType.getEntity(), structObj, change, nextPosition);

        DataType dataType = itemType.getDataType();
        ArrData data;
        switch (dataType) {
            case INT:
                data = new ArrDataInteger((Integer) value);
                break;
            case FORMATTED_TEXT:
            case TEXT:
                data = new ArrDataText((String) value);
                break;
            case STRING:
                data = new ArrDataString((String) value);
                break;
            default:
                data = null;
                break;
        }

        if (data == null) {
            logger.error("Nepodporovaný typ {} pro založení strukturované hodnoty z řetězce", dataType.getCode());
            return;
        }

        data.setDataType(dataType.getEntity());
        ArrData savedData = createData(data, dataType.getEntity());

        ArrStructuredItem structuredItem = new ArrStructuredItem();
        structuredItem.setItemType(itemType.getEntity());
        structuredItem.setItemSpec(null);
        structuredItem.setCreateChange(change);
        structuredItem.setData(savedData);
        structuredItem.setPosition(position);
        structuredItem.setStructuredObject(structObj);
        structuredItem.setDescItemObjectId(arrangementInternalService.getNextDescItemObjectId());
        structureItemRepository.save(structuredItem);
    }

    /**
     * Validace AS a rozšíření strukt. typu.
     *
     * @param fundVersion        verze AS
     * @param structureType      strukturovaný typ
     * @param structureExtension rozšížení strukt. typu
     */
    private void validateFundStructureExtension(final ArrFundVersion fundVersion, final RulStructuredType structureType, final RulStructuredTypeExtension structureExtension) {
        if (!structureType.equals(structureExtension.getStructuredType())) {
            throw new BusinessException("Rozšíření nespadá pod strukt. typ", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Vrací rozšíření strukt. typu podle kódu.
     *
     * @param structureExtensionCodes kódy rozšíření strukt. typu
     * @return entita
     */
    public List<RulStructuredTypeExtension> findStructureExtensionByCodes(final List<String> structureExtensionCodes) {
        List<RulStructuredTypeExtension> structureExtensions = structureExtensionRepository.findByCodeIn(structureExtensionCodes);
        if (structureExtensions.size() != structureExtensionCodes.size()) {
            throw new ObjectNotFoundException("Nenalezeny všechny rozšíření", BaseCode.ID_NOT_EXIST).setId(structureExtensionCodes);
        }
        return structureExtensions;
    }

    /**
     * Nalezne všechny dostupné rozšížení pro strukturovaný typ.
     *
     * @param structureType strukturovaný typ
     * @return nalezené entity
     */
    public List<RulStructuredTypeExtension> findAllStructureExtensions(final RulStructuredType structureType) {
        return structureExtensionRepository.findByStructureType(structureType);
    }

    /**
     * Nalezne aktivní rozšíření pro strukturovaný typ.
     *
     * @param fund          AS
     * @param structureType strukturovaný typ
     * @return nalezené entity
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<RulStructuredTypeExtension> findStructureExtensions(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                                    final RulStructuredType structureType) {
        return structureExtensionRepository.findActiveByFundAndStructureType(fund, structureType);
    }

    /**
     * Založení duplikátů strukturovaného datového typu a autoinkrementační.
     * Předloha musí být ve stavu {@link ArrStructuredObject.State#TEMP}.
     * Předloha je validována hned, nové hodnoty asynchronně.
     *
     * @param fundVersion   verze AS
     * @param structureData hodnota struktovaného datového typu, ze které se vychází
     * @param count         počet položek, které se budou budou vytvářet (včetně zdrojové hodnoty strukt. typu)
     * @param itemTypeIds   identifikátory typů, které se mají autoincrementovat
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void duplicateStructureDataBatch(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                            final ArrStructuredObject structureData,
                                            final int count,
                                            final List<Integer> itemTypeIds) {
        if (count <= 0) {
            throw new BusinessException("Počet vytvářených položek musí být kladný", BaseCode.INVALID_STATE);
        }
        if (structureData.getState() != ArrStructuredObject.State.TEMP) {
            throw new BusinessException("Neplatný stav hodnoty strukt. typu: " + structureData.getState(), BaseCode.INVALID_STATE);
        }

        List<RulItemType> itemTypes = findAndValidateIntItemTypes(fundVersion, itemTypeIds);
        List<ArrStructuredItem> structureItems = findStructureItems(structureData);

        validateStructureItems(itemTypes, structureItems);

        ArrChange change = arrangementInternalService.migrateChangeType(structureData.getCreateChange(), ArrChange.Type.ADD_STRUCTURE_DATA_BATCH);
        List<ArrStructuredObject> structureDataList = createStructObjList(fundVersion.getFund(),
                structureData.getStructuredType(), ArrStructuredObject.State.OK, change, count - 1);

        int countItems = structureDataList.size() * structureItems.size();
        if (countItems > 0) {
            List<ArrStructuredItem> newStructureItems = new ArrayList<>();
            List<ArrData> newDataList = new ArrayList<>();

            Map<RulItemType, Integer> autoincrementMap = createAutoincrementMap(structureItems, itemTypes);
            for (ArrStructuredObject newStructureData : structureDataList) {
                for (ArrStructuredItem structureItem : structureItems) {
                    ArrStructuredItem copyStructureItem = new ArrStructuredItem();
                    ArrData newData = ArrData.makeCopyWithoutId(HibernateUtils.unproxy(structureItem.getData()));
                    Integer val = autoincrementMap.get(structureItem.getItemType());
                    if (val != null) {
                        val++;
                        ((ArrDataInteger) newData).setIntegerValue(val);
                        autoincrementMap.put(structureItem.getItemType(), val);
                    }
                    newDataList.add(newData);
                    copyStructureItem.setData(newData);
                    copyStructureItem.setCreateChange(change);
                    copyStructureItem.setPosition(structureItem.getPosition());
                    copyStructureItem.setStructuredObject(newStructureData);
                    copyStructureItem.setDescItemObjectId(arrangementInternalService.getNextDescItemObjectId());
                    copyStructureItem.setItemType(structureItem.getItemType());
                    copyStructureItem.setItemSpec(structureItem.getItemSpec());
                    newStructureItems.add(copyStructureItem);
                }
            }
            dataRepository.saveAll(newDataList);
            structureItemRepository.saveAll(newStructureItems);
        }

        ArrStructuredObject confirmStructureData = confirmInternal(fundVersion.getFund(), structureData);
        structureDataList = revalidateStructureData(structureDataList);

        List<Integer> structureDataIds = structureDataList.stream().map(ArrStructuredObject::getStructuredObjectId).collect(Collectors.toList());
        structureDataIds.add(confirmStructureData.getStructuredObjectId());
        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructuredType().getCode(),
                null,
                structureDataIds,
                null,
                null));
    }

    /**
     * Sestavení mapy pro autoincrement hodnot.
     *
     * @param structureItems položky hodnoty strukt. typu
     * @param itemTypes      typy atributů, které vyžadujeme mezi hodnotami
     * @return výsledná mapa
     */
    private Map<RulItemType, Integer> createAutoincrementMap(final List<ArrStructuredItem> structureItems,
                                                             final List<RulItemType> itemTypes) {
        Map<RulItemType, Integer> result = new HashMap<>(itemTypes.size());
        for (RulItemType itemType : itemTypes) {
            for (ArrStructuredItem structureItem : structureItems) {
                if (structureItem.getItemType().equals(itemType)) {
                    ArrData data = HibernateUtils.unproxy(structureItem.getData());
                    result.put(itemType, data.getValueInt());
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Provede validaci položek předlohy strukt. typu.
     *
     * @param itemTypes      typy atributů, které vyžadujeme mezi hodnotami
     * @param structureItems položky hodnoty strukt. typu
     */
    private void validateStructureItems(final List<RulItemType> itemTypes, final List<ArrStructuredItem> structureItems) {
        List<RulItemType> itemTypesRequired = new ArrayList<>(itemTypes);
        for (ArrStructuredItem structureItem : structureItems) {
            itemTypesRequired.remove(structureItem.getItemType());
        }
        if (!itemTypesRequired.isEmpty()) {
            throw new BusinessException("Hodnota strukt. typu předlohy neobsahuje položky pro autoincrement", BaseCode.INVALID_STATE)
                    .set("codes", itemTypesRequired.stream().map(RulItemType::getCode).collect(Collectors.toList()));
        }
    }

    /**
     * Vyhledá a zvaliduje typy atributů. Validují se položky, že patří pod stejná pravidla. Kontrolují se typy, že jsou
     * datového typu {@link DataType#INT}.
     *
     * @param fundVersion verze AS
     * @param itemTypeIds identifikátory typů, které se mají autoincrementovat
     * @return nalezené a zvalidované entity
     */
    private List<RulItemType> findAndValidateIntItemTypes(final ArrFundVersion fundVersion, final List<Integer> itemTypeIds) {
        if (CollectionUtils.isEmpty(itemTypeIds)) {
            throw new BusinessException("Autoincrementující typ musí být alespoň jeden", BaseCode.INVALID_STATE);
        }

        List<RulItemType> itemTypes = itemTypeRepository.findAllById(itemTypeIds);
        if (itemTypes.size() != itemTypeIds.size()) {
            throw new BusinessException("Některý z typů atributů neexistuje", BaseCode.INVALID_STATE);
        }
        for (RulItemType itemType : itemTypes) {
            if (DataType.fromId(itemType.getDataTypeId()) != DataType.INT) {
                throw new BusinessException("Typ atributu " + itemType.getCode() + " není číselného typu", BaseCode.INVALID_STATE);
            }
        }
        return itemTypes;
    }

    /**
     * Hromadná úprava položek/hodnot strukt. typu.
     *
     * @param fundVersion              verze AS
     * @param structureType            strukturovaný typ
     * @param structureDataIds         identifikátory hodnoty strukt. datového typu
     * @param sourceStructureItems     nastavované položky
     * @param autoincrementItemTypeIds identifikátory typů, které se mají autoincrementovat
     * @param deleteItemTypeIds        identifikátory typů, které se mají odstranit
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void updateStructObjBatch(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                         final RulStructuredType structureType,
                                         final List<Integer> structureDataIds,
                                         final List<ArrStructuredItem> sourceStructureItems,
                                         final List<Integer> autoincrementItemTypeIds,
                                         final List<Integer> deleteItemTypeIds) {
        List<ArrStructuredObject> structureDataList = getStructObjByIds(structureDataIds);
        validateStructObj(fundVersion, structureType, structureDataList);

        List<RulItemType> autoincrementItemTypes = autoincrementItemTypeIds.isEmpty() ? Collections.emptyList() : findAndValidateIntItemTypes(fundVersion, autoincrementItemTypeIds);
        validateStructureItems(autoincrementItemTypes, sourceStructureItems);

        Map<ArrStructuredObject, List<ArrStructuredItem>> structureDataStructureItems = findStructureItems(structureDataList);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.UPDATE_STRUCT_DATA_BATCH);

        Set<Integer> allDeleteItemTypeIds = new HashSet<>(deleteItemTypeIds);
        allDeleteItemTypeIds.addAll(sourceStructureItems.stream().map(ArrStructuredItem::getItemTypeId).collect(Collectors.toList()));

        List<ArrStructuredItem> deleteStructureItems = new ArrayList<>();
        Map<RulItemType, Integer> autoincrementMap = createAutoincrementMap(sourceStructureItems, autoincrementItemTypes);

        List<ArrStructuredItem> newStructureItems = new ArrayList<>();
        List<ArrData> newDataList = new ArrayList<>();
        for (ArrStructuredObject structureData : structureDataList) {
            List<ArrStructuredItem> structureItems = structureDataStructureItems.get(structureData);
            if (CollectionUtils.isNotEmpty(structureItems)) {
                for (ArrStructuredItem structureItem : structureItems) {
                    if (allDeleteItemTypeIds.contains(structureItem.getItemTypeId())) {
                        structureItem.setDeleteChange(change);
                        deleteStructureItems.add(structureItem);
                    }
                }
            }

            Map<RulItemType, Integer> itemTypePositionMap = new HashMap<>();
            for (ArrStructuredItem structureItem : sourceStructureItems) {
                Integer position = itemTypePositionMap.computeIfAbsent(structureItem.getItemType(), k -> 0);
                position++;
                itemTypePositionMap.put(structureItem.getItemType(), position);

                ArrStructuredItem copyStructureItem = new ArrStructuredItem();
                ArrData newData = ArrData.makeCopyWithoutId(HibernateUtils.unproxy(structureItem.getData()));
                newData.setDataType(structureItem.getItemType().getDataType());
                Integer val = autoincrementMap.get(structureItem.getItemType());
                if (val != null) {
                    ((ArrDataInteger) newData).setIntegerValue(val);
                    // Increment for next item
                    val++;
                    autoincrementMap.put(structureItem.getItemType(), val);
                }
                newDataList.add(newData);
                copyStructureItem.setData(newData);
                copyStructureItem.setCreateChange(change);
                copyStructureItem.setPosition(position);
                copyStructureItem.setStructuredObject(structureData);
                copyStructureItem.setDescItemObjectId(arrangementInternalService.getNextDescItemObjectId());
                copyStructureItem.setItemType(structureItem.getItemType());
                copyStructureItem.setItemSpec(structureItem.getItemSpec());
                newStructureItems.add(copyStructureItem);
            }
        }
        structureItemRepository.saveAll(deleteStructureItems);
        dataRepository.saveAll(newDataList);
        structureItemRepository.saveAll(newStructureItems);

        revalidateStructureData(structureDataList);

        notifyAboutChange(fundVersion.getFundId(),
                          new EventStructureDataChange(fundVersion.getFundId(),
                                  structureType.getCode(),
                                  null,
                                  structureDataIds,
                                  null,
                                  null),
                          structureDataIds);
    }

    /**
     * Validace hodnot strukt. typu.
     *
     * @param fundVersion       verze AS
     * @param structureType     strukturovaný typ
     * @param StructObjList hodnoty strukturovaného datového typu
     */
    private void validateStructObj(final ArrFundVersion fundVersion,
                                       final RulStructuredType structureType,
                                       final List<ArrStructuredObject> StructObjList) {
        if (CollectionUtils.isEmpty(StructObjList)) {
            throw new BusinessException("Musí být upravována alespoň jedna hodnota strukt. typu", BaseCode.INVALID_STATE);
        }
        for (ArrStructuredObject structObj : StructObjList) {
            if (structObj.getDeleteChange() != null) {
                throw new BusinessException("Nelze upravit již smazaná strukturovaná data", BaseCode.INVALID_STATE)
                        .set("id", structObj.getStructuredObjectId());
            }
            if (!structObj.getFund().equals(fundVersion.getFund())) {
                throw new BusinessException("Strukturovaná data nepatří pod AS", BaseCode.INVALID_STATE)
                        .set("id", structObj.getStructuredObjectId());
            }
            if (!structObj.getStructuredType().equals(structureType)) {
                throw new BusinessException("Strukturovaná data jsou jiného strukt. datového typu", BaseCode.INVALID_STATE)
                        .set("id", structObj.getStructuredObjectId());
            }
        }
    }

    /**
     * Vyhledání povolených strukturovaných typů ve verzi AS.
     *
     * @param fundVersion verze AS
     * @return povolené entity
     */
    public List<RulStructuredType> findStructureTypes(final ArrFundVersion fundVersion) {
    	Objects.requireNonNull(fundVersion);

        List<UISettings> settings = settingsService.getGlobalSettings(UISettings.SettingsType.STRUCTURE_TYPES.toString(), UISettings.EntityType.RULE);
        UISettings settingsUse = null;
        for (UISettings setting : settings) {
            if (fundVersion.getRuleSetId().equals(setting.getEntityId())) {
                settingsUse = setting;
                break;
            }
        }

        if (settingsUse == null) {
            return Collections.emptyList();
        } else {
            SettingStructureTypes structureTypes = SettingStructureTypes.newInstance(settingsUse);
            Set<String> typeCodes = structureTypes.getItems().stream()
                    .map(SettingStructureTypes.Type::getCode)
                    .collect(Collectors.toSet());
            List<RulStructuredType> result = new ArrayList<>();
            for (RulStructuredType structureType : findStructureTypes()) {
                if (typeCodes.contains(structureType.getCode()) || structureType.getAnonymous()) {
                    result.add(structureType);
                }
            }
            return result;
        }
    }

    public Map<Integer, Map<Integer, ArrStructuredObject>> groupStructuredObjectByChange(Integer fundId, List<Integer> changeIdList) {

        if (changeIdList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Map<Integer, ArrStructuredObject>> changeIdStructuredObjectMap = new HashMap<>();

        for (ArrStructuredObject structuredObject : structObjRepository.findByFundAndCreateChange(fundId, changeIdList)) {
            Integer changeId = structuredObject.getCreateChange().getChangeId();
            changeIdStructuredObjectMap.computeIfAbsent(changeId, k -> new HashMap<>()).put(structuredObject.getStructuredObjectId(), structuredObject);
        }
        for (ArrStructuredObject structuredObject : structObjRepository.findByFundAndDeleteChange(fundId, changeIdList)) {
            Integer changeId = structuredObject.getDeleteChange().getChangeId();
            changeIdStructuredObjectMap.computeIfAbsent(changeId, k -> new HashMap<>()).put(structuredObject.getStructuredObjectId(), structuredObject);
        }
        for (ArrStructuredItem structuredItem : structureItemRepository.findByFundAndCreateChange(fundId, changeIdList)) {
            Integer changeId = structuredItem.getCreateChange().getChangeId();
            ArrStructuredObject structuredObject = structuredItem.getStructuredObject();
            changeIdStructuredObjectMap.computeIfAbsent(changeId, k -> new HashMap<>()).put(structuredObject.getStructuredObjectId(), structuredObject);
        }
        for (ArrStructuredItem structuredItem : structureItemRepository.findByFundAndDeleteChange(fundId, changeIdList)) {
            Integer changeId = structuredItem.getDeleteChange().getChangeId();
            ArrStructuredObject structuredObject = structuredItem.getStructuredObject();
            changeIdStructuredObjectMap.computeIfAbsent(changeId, k -> new HashMap<>()).put(structuredObject.getStructuredObjectId(), structuredObject);
        }
        return changeIdStructuredObjectMap;
    }

    public ArrStructuredObject getExistingStructObj(String value) {
        if (value.length() == 36) {
            return getExistingStructObjByUUID(value);
        } else {
            int id = Integer.parseInt(value);
            ArrStructuredObject so = getStructObjById(id);
            Validate.isTrue(so.getDeleteChangeId() == null, "Structured object is deleted, id: %i", id);
            return so;
        }
    }

    public void updateStructObj(ArrChange change, ArrStructuredObject structObj, List<ArrStructuredItem> items) {

        // drop all old items
        List<ArrStructuredItem> currItems = structObjService.findByStructuredObjectAndDeleteChangeIsNullFetchData(structObj);
        currItems.forEach(item -> item.setDeleteChange(change));
        structureItemRepository.saveAll(currItems);

        // create new items
        for (ArrStructuredItem newItem : items) {
            createItem(newItem, structObj, newItem.getPosition(), change);
        }

        revalidateStructureData(Collections.singletonList(structObj));

        List<Integer> structObjIds = Collections.singletonList(structObj.getStructuredObjectId());

        notifyAboutChange(structObj.getFundId(),
                          new EventStructureDataChange(structObj.getFundId(),
                                  structObj.getStructuredType().getCode(),
                                  null,
                                  null,
                                  structObjIds,
                                  null),
                          structObjIds);
    }

    private void notifyAboutChange(Integer fundId, EventStructureDataChange eventStructObjChange,
                                   List<Integer> structObjIds) {
        notificationService.publishEvent(eventStructObjChange);

        Collection<Integer> nodeIds = arrangementInternalService.findNodesByStructuredObjectIds(structObjIds)
                .keySet();
        if (!nodeIds.isEmpty()) {
            notificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, fundId,
                    nodeIds.toArray(new Integer[0])));
        }

    }

    public List<ArrStructuredObject> getUnusedStructObj(Collection<ArrStructuredObject> structObjsToDelete) {
        List<ArrStructuredObject> unusedStructObjs = new ArrayList<>(structObjsToDelete);
        List<ArrStructuredObject> usedStructObjs = new ArrayList<>();
        ObjectListIterator.forEachPage(structObjsToDelete, page -> usedStructObjs.addAll(structureItemRepository.findUsedStructuredObjects(page)));
        unusedStructObjs.removeAll(usedStructObjs);
        return unusedStructObjs;
    }

}
