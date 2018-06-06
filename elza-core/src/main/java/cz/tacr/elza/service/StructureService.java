package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.castor.core.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;

/**
 * Servisní třída pro práci se strukturovanými datovými typy.
 *
 * @since 06.11.2017
 */
@Service
public class StructureService {

    private final StructuredItemRepository structureItemRepository;
    private final StructuredTypeExtensionRepository structureExtensionRepository;
    private final StructuredObjectRepository structObjRepository;
    private final StructuredTypeRepository structureTypeRepository;
    private final ArrangementService arrangementService;
    private final DataRepository dataRepository;
    private final RuleService ruleService;
    private final FundStructureExtensionRepository fundStructureExtensionRepository;
    private final StructObjService structObjService;
    private final ItemTypeRepository itemTypeRepository;
    private final ChangeRepository changeRepository;
    private final EventNotificationService notificationService;

    @Autowired
    public StructureService(final StructuredItemRepository structureItemRepository,
                            final StructuredTypeExtensionRepository structureExtensionRepository,
                            final StructuredObjectRepository structureDataRepository,
                            final StructuredTypeRepository structureTypeRepository,
                            final ArrangementService arrangementService,
                            final DataRepository dataRepository,
                            final RuleService ruleService,
                            final FundStructureExtensionRepository fundStructureExtensionRepository,
                            final StructObjService structureDataService,
                            final ItemTypeRepository itemTypeRepository,
                            final ChangeRepository changeRepository,
                            final EventNotificationService notificationService) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionRepository = structureExtensionRepository;
        this.structObjRepository = structureDataRepository;
        this.structureTypeRepository = structureTypeRepository;
        this.arrangementService = arrangementService;
        this.dataRepository = dataRepository;
        this.ruleService = ruleService;
        this.fundStructureExtensionRepository = fundStructureExtensionRepository;
        this.structObjService = structureDataService;
        this.itemTypeRepository = itemTypeRepository;
        this.changeRepository = changeRepository;
        this.notificationService = notificationService;
    }

    /**
     * Vyhledá platné položky k hodnotě strukt. datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return nalezené položky
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrStructuredItem> findStructureItems(@AuthParam(type = AuthParam.Type.FUND) final ArrStructuredObject structureData) {
        return structureItemRepository.findByStructuredObjectAndDeleteChangeIsNullFetchData(structureData);
    }

    /**
     * Vyhledá platné položky k hodnotám strukt. datového typu.
     *
     * @param structureDataList hodnoty struktovaného datového typu
     * @return hodnota strukt. datového typu -> nalezené položky
     */
    public Map<ArrStructuredObject, List<ArrStructuredItem>> findStructureItems(final List<ArrStructuredObject> structureDataList) {
        List<List<ArrStructuredObject>> parts = Lists.partition(structureDataList, 1000);
        List<ArrStructuredItem> structureItems = new ArrayList<>();
        for (List<ArrStructuredObject> part : parts) {
            structureItems.addAll(structureItemRepository.findByStructuredObjectListAndDeleteChangeIsNullFetchData(part));
        }
        return structureItems.stream().collect(Collectors.groupingBy(ArrStructuredItem::getStructuredObject));
    }

    /**
     * Vytvoření strukturovaných dat.
     *
     * @param fund          archivní soubor
     * @param structureType strukturovaný typ
     * @return vytvořená entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredObject createStructObj(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                   final RulStructuredType structureType,
                                                   final ArrStructuredObject.State state) {
        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA);
        ArrStructuredObject structureData = new ArrStructuredObject();
        structureData.setAssignable(true);
        structureData.setCreateChange(change);
        structureData.setFund(fund);
        structureData.setStructuredType(structureType);
        structureData.setState(state);

        ArrStructuredObject createStructureData = structObjRepository.save(structureData);
        if (state == ArrStructuredObject.State.TEMP) {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureType.getCode(),
                    Collections.singletonList(createStructureData.getStructuredObjectId()),
                    null,
                    null,
                    null));
        } else {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureType.getCode(),
                    null,
                    Collections.singletonList(createStructureData.getStructuredObjectId()),
                    null,
                    null));
        }
        return createStructureData;
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
            ArrStructuredObject structureData = new ArrStructuredObject();
            structureData.setAssignable(true);
            structureData.setCreateChange(change);
            structureData.setFund(fund);
            structureData.setStructuredType(structureType);
            structureData.setState(state);
            result.add(structureData);
        }
        return structObjRepository.save(result);
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param structObj
     *            hodnota struktovaného datového typu
     * 
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void deleteStructObj(@AuthParam(type = AuthParam.Type.FUND) final ArrStructuredObject structObj) {
        if (structObj.getDeleteChange() != null) {
            throw new BusinessException("Nelze odstranit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }

        if (structObj.getState() == ArrStructuredObject.State.TEMP) {

            // remove temporary object
            structureItemRepository.deleteByStructuredObject(structObj);
            dataRepository.deleteByStructuredObject(structObj);
            ArrChange change = structObjRepository.findTempChangeByStructuredObject(structObj);
            structObjRepository.delete(structObj);
            changeRepository.delete(change);

        } else {

            // drop permanent object

            // check usage
            Integer count = structureItemRepository.countItemsByStructuredObject(structObj);
            if (count > 0) {
                throw new BusinessException("Existují návazné entity, položka nelze smazat", ArrangementCode.STRUCTURE_DATA_DELETE_ERROR)
                        .level(Level.WARNING)
                        .set("count", count)
                                .set("id", structObj.getStructuredObjectId());
            }

            ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_DATA);
            structObj.setDeleteChange(change);

            structObjRepository.save(structObj);

            // check duplicates for deleted item
            structObjService.addToValidate(structObj);

            notificationService.publishEvent(new EventStructureDataChange(structObj.getFundId(),
                    structObj.getStructuredType().getCode(),
                    null,
                    null,
                    null,
                    Collections.singletonList(structObj.getStructuredObjectId())));
        }
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
            structureData.setAssignable(assignable);
        }

        notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                structureDataList.get(0).getStructuredType().getCode(),
                null,
                null,
                structureDataList.stream().map(ArrStructuredObject::getStructuredObjectId).collect(Collectors.toList()),
                null));
        return structObjRepository.save(structureDataList);
    }

    /**
     * Vytvoření položky k hodnotě strukt. datového typu.
     *
     * @param structureItem   položka
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @param fundVersionId   identifikátor verze AS
     * @return vytvořená entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredItem createStructureItem(final ArrStructuredItem structureItem,
                                                 final Integer structureDataId,
                                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {

        ArrStructuredObject structureData = getStructObjById(structureDataId);
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        if (!fundVersion.getRuleSet().equals(structureData.getStructuredType().getRuleSet())) {
            throw new BusinessException("Fund a strukturovaný typ nemají stejná pravidla", BaseCode.INVALID_STATE)
                    .set("fund_rul_set", fundVersion.getRuleSet().getCode())
                    .set("structure_type_rul_set", structureData.getStructuredType().getRuleSet().getCode());
        }

        ArrChange change = structureData.getState() == ArrStructuredObject.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_ITEM);

        int nextPosition = findNextPosition(structureData, structureItem.getItemType());
        Integer position;

        if (structureItem.getPosition() == null) {
            position = nextPosition;
        } else {
            position = structureItem.getPosition();

            if (position <= 0) {
                throw new SystemException("Neplatný formát dat", BaseCode.PROPERTY_IS_INVALID).set("property", "position");
            }

            // pokud je požadovaná pozice menší než další volná, bude potřeba posunou níž položky
            if (position < nextPosition) {
                List<ArrStructuredItem> structureItemsToMove = structureItemRepository.findOpenItemsAfterPositionFetchData(structureItem.getItemType(),
                        structureData, position - 1, null);

                nextVersionStructureItems(1, structureItemsToMove, change, true);
            }

            // pokud je požadovaná pozice větší než další volná, použije se další volná
            if (position > nextPosition) {
                position = nextPosition;
            }

        }

        validateRuleSet(fundVersion, structureItem.getItemType());

        ArrData data = createData(structureItem.getData(), structureItem.getItemType().getDataType());

        ArrStructuredItem createStructureItem = new ArrStructuredItem();
        createStructureItem.setData(data);
        createStructureItem.setCreateChange(change);
        createStructureItem.setPosition(position);
        createStructureItem.setStructuredObject(structureData);
        createStructureItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
        createStructureItem.setItemType(structureItem.getItemType());
        createStructureItem.setItemSpec(structureItem.getItemSpec());

        ArrStructuredItem save = structureItemRepository.save(createStructureItem);
        structObjService.generateAndValidate(save.getStructuredObject());

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructuredType().getCode(),
                null,
                null,
                Collections.singletonList(save.getStructuredObject().getStructuredObjectId()),
                null));

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
            newStructureItem.setData(null);
            newStructureItem.setItemId(null);

            structureItem.setDeleteChange(change);
            newStructureItem.setCreateChange(change);
            if (moveDiff != 0) {
                newStructureItem.setPosition(newStructureItem.getPosition() + moveDiff);
            }

            resultStructureItems.add(newStructureItem);
        }

        structureItemRepository.save(structureItems); // uložení změny deleteChange

        if (createNewDataVersion) {
            List<ArrData> resultDataList = new ArrayList<>(resultStructureItems.size());
            for (ArrStructuredItem newStructureItem : resultStructureItems) {
                ArrData newData = ArrData.makeCopyWithoutId(newStructureItem.getData());
                newStructureItem.setData(newData);
                resultDataList.add(newData);
            }
            dataRepository.save(resultDataList);
        }

        return structureItemRepository.save(resultStructureItems);
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
                structureData, 0, new PageRequest(0, 1, Sort.Direction.DESC, ArrItem.POSITION));
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
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        ArrStructuredItem structureItemDB = structureItemRepository.findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

        ArrStructuredObject structureData = structureItemDB.getStructuredObject();
        ArrChange change = structureData.getState() == ArrStructuredObject.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.UPDATE_STRUCTURE_ITEM);

        validateRuleSet(fundVersion, structureItemDB.getItemType());

        ArrStructuredItem updateStructureItem;

        if (createNewVersion) {

            structureItemDB.setDeleteChange(change);
            structureItemRepository.save(structureItemDB);

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


            ArrData updateData = updateData(structureItem.getData(), structureItemDB.getItemType().getDataType());

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
            ArrData updateData = updateStructureItem.getData();
            // prepare dataToDb
            updateData.merge(structureItem.getData());
            dataRepository.save(updateData);
        }

        ArrStructuredItem save = structureItemRepository.save(updateStructureItem);
        structObjService.generateAndValidate(save.getStructuredObject());

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructuredType().getCode(),
                null,
                null,
                Collections.singletonList(save.getStructuredObject().getStructuredObjectId()),
                null));

        return save;
    }

    /**
     * Provede kontrolu zda-li verze AS má stejná pravidla jako strukt. typ.
     *
     * @param fundVersion verze AS
     * @param itemType    typ atributu
     */
    private void validateRuleSet(final ArrFundVersion fundVersion, final RulItemType itemType) {
        if (itemType.getStructuredType() != null && !fundVersion.getRuleSet().equals(itemType.getStructuredType().getRuleSet())) {
            throw new BusinessException("Fund a strukturovaný typ nemají stejná pravidla", BaseCode.INVALID_STATE)
                    .set("fund_rul_set", fundVersion.getRuleSet().getCode())
                    .set("structure_type_rul_set", itemType.getRuleSet().getCode());
        }
    }

    /**
     * Smazání položky k hodnotě strukt. datového typu.
     *
     * @param structureItem položka
     * @param fundVersionId identifikátor verze AS
     * @return smazaná položka
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredItem deleteStructureItem(final ArrStructuredItem structureItem,
                                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        ArrStructuredItem structureItemDB = structureItemRepository.findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

        ArrStructuredObject structureData = structureItemDB.getStructuredObject();
        ArrChange change = structureData.getState() == ArrStructuredObject.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        validateRuleSet(fundVersion, structureItemDB.getItemType());

        structureItemDB.setDeleteChange(change);

        ArrStructuredItem save = structureItemRepository.save(structureItemDB);
        structObjService.generateAndValidate(save.getStructuredObject());

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructuredType().getCode(),
                null,
                null,
                Collections.singletonList(save.getStructuredObject().getStructuredObjectId()),
                null));

        return save;
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
     * @param createNewVersion vytvořit nová data
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
        Assert.notNull(structureDataList, "Musí být vyplněn list hodnot strukt. typu");

        for (ArrStructuredObject structureData : structureDataList) {
            structureData.setValue(null);
            structureData.setErrorDescription(null);
        }
        structObjRepository.save(structureDataList);
        structObjService.addToValidate(structureDataList);
        return structureDataList;
    }

    /**
     * Provede revalidaci podle strukt. typu.
     *
     * @param structureTypes revalidované typy
     */
    public void revalidateStructureTypes(final Collection<RulStructuredType> structureTypes) {
        if (structureTypes.isEmpty()) {
            return;
        }
        List<Integer> structureDataIds = structObjRepository.findStructuredObjectIdByStructureTypes(structureTypes);
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

    /**
     * Vrátí strukt. data podle identifikátorů včetně načtených návazných entit.
     *
     * @param structureDataIds identifikátory hodnoty strukt. datového typu
     * @return entity
     */
    public List<ArrStructuredObject> getStructObjByIds(final List<Integer> structureDataIds) {
        List<List<Integer>> idsParts = Lists.partition(structureDataIds, 1000);
        List<ArrStructuredObject> structureDataList = new ArrayList<>();
        for (List<Integer> idsPart : idsParts) {
            structureDataList.addAll(structObjRepository.findByIdsFetch(idsPart));
        }
        if (structureDataList.size() != structureDataIds.size()) {
            throw new ObjectNotFoundException("Nenalezeny všechny rozšíření", BaseCode.ID_NOT_EXIST).setId(structureDataIds);
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructuredObject getStructObjById(final Integer structureDataId,
                                                    @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        ArrStructuredObject structureData = getStructObjById(structureDataId);
        if (!structureData.getStructuredType().getRuleSet().equals(fundVersion.getRuleSet())) {
            throw new BusinessException("Pravidla AS nesouhlasí s pravidly hodnoty strukt. typu", BaseCode.INVALID_STATE);
        }
        return structureData;
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
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructuredObject structObj = getStructObjById(structureDataId);
        RulItemType type = ruleService.getItemTypeById(itemTypeId);
        validateRuleSet(fundVersion, type);
        List<ArrStructuredItem> structureItems = structureItemRepository.findOpenItems(type, structObj);

        ArrChange change = structObj.getState() == ArrStructuredObject.State.TEMP ? structObj.getCreateChange() : arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        for (ArrStructuredItem structureItem : structureItems) {
            structureItem.setDeleteChange(change);
        }

        structureItemRepository.save(structureItems);

        structObjService.generateAndValidate(structObj);

        // Notification about deleted item
        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structObj.getStructuredType().getCode(),
                null,
                null,
                Collections.singletonList(structObj.getStructuredObjectId()),
                null));

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
        return confirmStructureData(fund, structureData, true);
    }

    /**
     * Potvrzení hodnoty strukt. datového typu.
     *
     * @param fund          archivní soubor
     * @param structureData hodnota struktovaného datového typu
     * @param event         odeslat websocket event
     * @return entita
     */
    private ArrStructuredObject confirmStructureData(final ArrFund fund,
                                                     final ArrStructuredObject structureData,
                                                     final boolean event) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze potvrdit smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        if (!structureData.getState().equals(ArrStructuredObject.State.TEMP)) {
            throw new BusinessException("Strukturovaná data nemají dočasný stav", BaseCode.INVALID_STATE);
        }
        // reset temporary value -> final have to be calculated
        structureData.setValue(null);
        structureData.setState(ArrStructuredObject.State.OK);
        ArrStructuredObject savedStructObj = structObjRepository.save(structureData);
        structObjService.generateAndValidate(savedStructObj);
        if (event) {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    savedStructObj.getStructuredType().getCode(),
                    null,
                    Collections.singletonList(savedStructObj.getStructuredObjectId()),
                    null,
                    null));
        }
        return savedStructObj;
    }

    /**
     * Vyhledání strukturovaných typů podle pravidel.
     *
     * @param fundVersion verze AS
     * @return nalezené entity
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<RulStructuredType> findStructureTypes(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        return structureTypeRepository.findByRuleSet(fundVersion.getRuleSet());
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public FilteredResult<ArrStructuredObject> findStructureData(final RulStructuredType structureType,
                                                                 @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                                 @Nullable final String search,
                                                                 @Nullable final Boolean assignable,
                                                                 final int from,
                                                                 final int count) {
        return structObjRepository.findStructureData(structureType.getStructuredTypeId(), fund.getFundId(), search, assignable, from, count);
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
        structureExtensions.forEach(se -> validateFundStructureExtension(fundVersion, structureType, se));

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
            final ArrChange change = arrangementService.createChange(ArrChange.Type.SET_FUND_STRUCTURE_EXT);
            fundStructureExtensionsCreate.forEach(fse -> fse.setCreateChange(change));
            fundStructureExtensionsDelete.forEach(fse -> fse.setDeleteChange(change));
            fundStructureExtensionRepository.save(fundStructureExtensionsCreate);
            fundStructureExtensionRepository.save(fundStructureExtensionsDelete);
            Set<RulStructuredType> structureTypes = new HashSet<>();
            fundStructureExtensionsCreate.forEach(fse -> structureTypes.add(fse.getStructuredTypeExtension().getStructuredType()));
            fundStructureExtensionsDelete.forEach(fse -> structureTypes.add(fse.getStructuredTypeExtension().getStructuredType()));
            revalidateStructureTypes(structureTypes);
        }
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
        if (!fundVersion.getRuleSet().equals(structureExtension.getStructuredType().getRuleSet())) {
            throw new BusinessException("AS a rozšíření mají rozdílná pravidla", BaseCode.INVALID_STATE);
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
        if (!fundVersion.getRuleSet().equals(structureData.getStructuredType().getRuleSet())) {
            throw new BusinessException("Pravidla AS nesouhlasí s pravidly hodnoty strukt. typu", BaseCode.INVALID_STATE);
        }

        List<RulItemType> itemTypes = findAndValidateIntItemTypes(fundVersion, itemTypeIds);
        List<ArrStructuredItem> structureItems = findStructureItems(structureData);

        validateStructureItems(itemTypes, structureItems);

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA_BATCH);
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
                    ArrData newData = ArrData.makeCopyWithoutId(structureItem.getData());
                    Integer val = autoincrementMap.get(structureItem.getItemType());
                    if (val != null) {
                        val++;
                        ((ArrDataInteger) newData).setValue(val);
                        autoincrementMap.put(structureItem.getItemType(), val);
                    }
                    newDataList.add(newData);
                    copyStructureItem.setData(newData);
                    copyStructureItem.setCreateChange(change);
                    copyStructureItem.setPosition(structureItem.getPosition());
                    copyStructureItem.setStructuredObject(newStructureData);
                    copyStructureItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
                    copyStructureItem.setItemType(structureItem.getItemType());
                    copyStructureItem.setItemSpec(structureItem.getItemSpec());
                    newStructureItems.add(copyStructureItem);
                }
            }
            dataRepository.save(newDataList);
            structureItemRepository.save(newStructureItems);
        }

        ArrStructuredObject confirmStructureData = confirmStructureData(fundVersion.getFund(), structureData, false);
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
                    result.put(itemType, structureItem.getData().getValueInt());
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

        List<RulItemType> itemTypes = itemTypeRepository.findAll(itemTypeIds);
        if (itemTypes.size() != itemTypeIds.size()) {
            throw new BusinessException("Některý z typů atributů neexistuje", BaseCode.INVALID_STATE);
        }
        for (RulItemType itemType : itemTypes) {
            if (!itemType.getRuleSet().equals(fundVersion.getRuleSet())) {
                throw new BusinessException("Typ atributu " + itemType.getCode() + " nepatří k pravidlům " + fundVersion.getRuleSet().getCode(), BaseCode.INVALID_STATE);
            }
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
        ArrChange change = arrangementService.createChange(ArrChange.Type.UPDATE_STRUCT_DATA_BATCH);

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
                ArrData newData = ArrData.makeCopyWithoutId(structureItem.getData());
                newData.setDataType(structureItem.getItemType().getDataType());
                Integer val = autoincrementMap.get(structureItem.getItemType());
                if (val != null) {
                    ((ArrDataInteger) newData).setValue(val);
                    // Increment for next item
                    val++;
                    autoincrementMap.put(structureItem.getItemType(), val);
                }
                newDataList.add(newData);
                copyStructureItem.setData(newData);
                copyStructureItem.setCreateChange(change);
                copyStructureItem.setPosition(position);
                copyStructureItem.setStructuredObject(structureData);
                copyStructureItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
                copyStructureItem.setItemType(structureItem.getItemType());
                copyStructureItem.setItemSpec(structureItem.getItemSpec());
                newStructureItems.add(copyStructureItem);
            }
        }
        structureItemRepository.save(deleteStructureItems);
        dataRepository.save(newDataList);
        structureItemRepository.save(newStructureItems);

        revalidateStructureData(structureDataList);

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureType.getCode(),
                null,
                structureDataIds,
                null,
                null));
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
}
