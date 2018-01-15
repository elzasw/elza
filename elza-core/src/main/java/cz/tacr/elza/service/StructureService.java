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

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.repository.StructureExtensionRepository;
import cz.tacr.elza.repository.StructureItemRepository;
import cz.tacr.elza.repository.StructureTypeRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;

/**
 * Servisní třída pro práci se strukturovanými datovými typy.
 *
 * @since 06.11.2017
 */
@Service
public class StructureService {

    private final StructureItemRepository structureItemRepository;
    private final StructureExtensionRepository structureExtensionRepository;
    private final StructureDataRepository structureDataRepository;
    private final StructureTypeRepository structureTypeRepository;
    private final ArrangementService arrangementService;
    private final DataRepository dataRepository;
    private final RuleService ruleService;
    private final FundStructureExtensionRepository fundStructureExtensionRepository;
    private final StructureDataService structureDataService;
    private final ItemTypeRepository itemTypeRepository;
    private final ChangeRepository changeRepository;
    private final EventNotificationService notificationService;

    @Autowired
    public StructureService(final StructureItemRepository structureItemRepository,
                            final StructureExtensionRepository structureExtensionRepository,
                            final StructureDataRepository structureDataRepository,
                            final StructureTypeRepository structureTypeRepository,
                            final ArrangementService arrangementService,
                            final DataRepository dataRepository,
                            final RuleService ruleService,
                            final FundStructureExtensionRepository fundStructureExtensionRepository,
                            final StructureDataService structureDataService,
                            final ItemTypeRepository itemTypeRepository,
                            final ChangeRepository changeRepository,
                            final EventNotificationService notificationService) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionRepository = structureExtensionRepository;
        this.structureDataRepository = structureDataRepository;
        this.structureTypeRepository = structureTypeRepository;
        this.arrangementService = arrangementService;
        this.dataRepository = dataRepository;
        this.ruleService = ruleService;
        this.fundStructureExtensionRepository = fundStructureExtensionRepository;
        this.structureDataService = structureDataService;
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
    public List<ArrStructureItem> findStructureItems(@AuthParam(type = AuthParam.Type.FUND) final ArrStructureData structureData) {
        return structureItemRepository.findByStructureDataAndDeleteChangeIsNull(structureData);
    }

    /**
     * Vyhledá platné položky k hodnotám strukt. datového typu.
     *
     * @param structureDataList hodnoty struktovaného datového typu
     * @return hodnota strukt. datového typu -> nalezené položky
     */
    public Map<ArrStructureData, List<ArrStructureItem>> findStructureItems(final List<ArrStructureData> structureDataList) {
        List<List<ArrStructureData>> parts = Lists.partition(structureDataList, 1000);
        List<ArrStructureItem> structureItems = new ArrayList<>();
        for (List<ArrStructureData> part : parts) {
            structureItems.addAll(structureItemRepository.findByStructureDataListAndDeleteChangeIsNullFetchData(part));
        }
        return structureItems.stream().collect(Collectors.groupingBy(ArrStructureItem::getStructureData));
    }

    /**
     * Vytvoření strukturovaných dat.
     *
     * @param fund          archivní soubor
     * @param structureType strukturovaný typ
     * @return vytvořená entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructureData createStructureData(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                final RulStructureType structureType,
                                                final ArrStructureData.State state) {
        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA);
        ArrStructureData structureData = new ArrStructureData();
        structureData.setAssignable(true);
        structureData.setCreateChange(change);
        structureData.setFund(fund);
        structureData.setStructureType(structureType);
        structureData.setState(state);

        ArrStructureData createStructureData = structureDataRepository.save(structureData);
        if (state == ArrStructureData.State.TEMP) {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureType.getCode(),
                    Collections.singletonList(createStructureData.getStructureDataId()),
                    null,
                    null,
                    null));
        } else {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureType.getCode(),
                    null,
                    Collections.singletonList(createStructureData.getStructureDataId()),
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
    private List<ArrStructureData> createStructureDataList(final ArrFund fund,
                                                           final RulStructureType structureType,
                                                           final ArrStructureData.State state,
                                                           final ArrChange change,
                                                           int count) {
        List<ArrStructureData> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ArrStructureData structureData = new ArrStructureData();
            structureData.setAssignable(true);
            structureData.setCreateChange(change);
            structureData.setFund(fund);
            structureData.setStructureType(structureType);
            structureData.setState(state);
            result.add(structureData);
        }
        return structureDataRepository.save(result);
    }

    /**
     * Smazání hodnoty strukturovaného datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return smazaná entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructureData deleteStructureData(@AuthParam(type = AuthParam.Type.FUND) final ArrStructureData structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze odstranit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        if (structureData.getState() == ArrStructureData.State.TEMP) {
            structureItemRepository.deleteByStructureData(structureData);
            dataRepository.deleteByStructureData(structureData);
            ArrChange change = structureDataRepository.findTempChangeByStructureData(structureData);
            structureDataRepository.delete(structureData);
            changeRepository.delete(change);
            return structureData;
        } else {
            ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_DATA);
            structureData.setDeleteChange(change);

            notificationService.publishEvent(new EventStructureDataChange(structureData.getFundId(),
                    structureData.getStructureType().getCode(),
                    null,
                    null,
                    null,
                    Collections.singletonList(structureData.getStructureDataId())));

            return structureDataRepository.save(structureData);
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
    public List<ArrStructureData> setAssignableStructureDataList(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                                 final List<ArrStructureData> structureDataList,
                                                                 final boolean assignable) {
        if (structureDataList.size() == 0) {
            return Collections.emptyList();
        }
        for (ArrStructureData structureData : structureDataList) {
            if (structureData.getDeleteChange() != null) {
                throw new BusinessException("Nelze změnit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
            }
            structureData.setAssignable(assignable);
        }

        notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                structureDataList.get(0).getStructureType().getCode(),
                null,
                null,
                structureDataList.stream().map(ArrStructureData::getStructureDataId).collect(Collectors.toList()),
                null));
        return structureDataRepository.save(structureDataList);
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
    public ArrStructureItem createStructureItem(final ArrStructureItem structureItem,
                                                final Integer structureDataId,
                                                @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {

        ArrStructureData structureData = getStructureDataById(structureDataId);
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        if (!fundVersion.getRuleSet().equals(structureData.getStructureType().getRuleSet())) {
            throw new BusinessException("Fund a strukturovaný typ nemají stejná pravidla", BaseCode.INVALID_STATE)
                    .set("fund_rul_set", fundVersion.getRuleSet().getCode())
                    .set("structure_type_rul_set", structureData.getStructureType().getRuleSet().getCode());
        }

        ArrChange change = structureData.getState() == ArrStructureData.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_ITEM);

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
                List<ArrStructureItem> structureItemsToMove = structureItemRepository.findOpenItemsAfterPositionFetchData(structureItem.getItemType(),
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

        ArrStructureItem createStructureItem = new ArrStructureItem();
        createStructureItem.setData(data);
        createStructureItem.setCreateChange(change);
        createStructureItem.setPosition(position);
        createStructureItem.setStructureData(structureData);
        createStructureItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
        createStructureItem.setItemType(structureItem.getItemType());
        createStructureItem.setItemSpec(structureItem.getItemSpec());

        ArrStructureItem save = structureItemRepository.save(createStructureItem);
        structureDataService.validate(save.getStructureData());

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructureType().getCode(),
                null,
                null,
                Collections.singletonList(save.getStructureData().getStructureDataId()),
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
    private List<ArrStructureItem> nextVersionStructureItems(final int moveDiff,
                                                             final List<ArrStructureItem> structureItems,
                                                             final ArrChange change,
                                                             final boolean createNewDataVersion) {
        if (structureItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<ArrStructureItem> resultStructureItems = new ArrayList<>(structureItems.size());

        for (ArrStructureItem structureItem : structureItems) {
            ArrStructureItem newStructureItem = structureItem.copy();

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
            for (ArrStructureItem newStructureItem : resultStructureItems) {
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
    private int findNextPosition(final ArrStructureData structureData, final RulItemType itemType) {
        List<ArrStructureItem> structureItems = structureItemRepository.findOpenItemsAfterPosition(itemType,
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
    public ArrStructureItem updateStructureItem(final ArrStructureItem structureItem,
                                                @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                final boolean createNewVersion) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        ArrStructureItem structureItemDB = structureItemRepository.findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

        ArrStructureData structureData = structureItemDB.getStructureData();
        ArrChange change = structureData.getState() == ArrStructureData.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.UPDATE_STRUCTURE_ITEM);

        validateRuleSet(fundVersion, structureItemDB.getItemType());

        ArrStructureItem updateStructureItem;

        if (createNewVersion) {

            structureItemDB.setDeleteChange(change);
            structureItemRepository.save(structureItemDB);

            Integer positionDB = structureItemDB.getPosition();
            Integer positionChange = structureItem.getPosition();

            Integer position;
            if (positionDB.equals(positionChange)) {
                position = positionDB;
            } else {
                int nextPosition = findNextPosition(structureItemDB.getStructureData(), structureItemDB.getItemType());

                if (positionChange == null || (positionChange > nextPosition - 1)) {
                    positionChange = nextPosition;
                }

                List<ArrStructureItem> structureItemsToMove;
                Integer moveDiff;

                if (positionChange < positionDB) {
                    moveDiff = 1;
                    structureItemsToMove = structureItemRepository.findOpenItemsBetweenPositions(structureItemDB.getItemType(), structureItemDB.getStructureData(), positionChange, positionDB - 1);
                } else {
                    moveDiff = -1;
                    structureItemsToMove = structureItemRepository.findOpenItemsBetweenPositions(structureItemDB.getItemType(), structureItemDB.getStructureData(), positionDB + 1, positionChange);
                }

                nextVersionStructureItems(moveDiff, structureItemsToMove, change, false);

                position = positionChange;
            }


            ArrData updateData = updateData(structureItem.getData(), true, structureItemDB.getItemType().getDataType());

            updateStructureItem = new ArrStructureItem();
            updateStructureItem.setData(updateData);
            updateStructureItem.setCreateChange(change);
            updateStructureItem.setPosition(position);
            updateStructureItem.setStructureData(structureItemDB.getStructureData());
            updateStructureItem.setDescItemObjectId(structureItemDB.getDescItemObjectId());
            updateStructureItem.setItemType(structureItemDB.getItemType());
            updateStructureItem.setItemSpec(structureItem.getItemSpec());
        } else {
            updateStructureItem = structureItemDB;
            updateStructureItem.setItemSpec(structureItem.getItemSpec());
            ArrData updateData = updateStructureItem.getData();
            updateData.merge(structureItem.getData());
            updateData(updateData, false, structureItemDB.getItemType().getDataType());
        }

        ArrStructureItem save = structureItemRepository.save(updateStructureItem);
        structureDataService.validate(save.getStructureData());

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructureType().getCode(),
                null,
                null,
                Collections.singletonList(save.getStructureData().getStructureDataId()),
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
        if (itemType.getStructureType() != null && !fundVersion.getRuleSet().equals(itemType.getStructureType().getRuleSet())) {
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
    public ArrStructureItem deleteStructureItem(final ArrStructureItem structureItem,
                                                @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        ArrStructureItem structureItemDB = structureItemRepository.findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

        ArrStructureData structureData = structureItemDB.getStructureData();
        ArrChange change = structureData.getState() == ArrStructureData.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        validateRuleSet(fundVersion, structureItemDB.getItemType());

        structureItemDB.setDeleteChange(change);

        ArrStructureItem save = structureItemRepository.save(structureItemDB);
        structureDataService.validate(save.getStructureData());

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructureType().getCode(),
                null,
                null,
                Collections.singletonList(save.getStructureData().getStructureDataId()),
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
    private ArrData updateData(final ArrData data, final boolean createNewVersion, final RulDataType dataType) {
        if (createNewVersion) {
            ArrData copy = ArrData.makeCopyWithoutId(data);
            copy.setDataType(dataType);
            return dataRepository.save(copy);
        } else {
            Assert.notNull(data.getDataId(), "Identifikátor musí být vyplněn");
            return dataRepository.save(data);
        }
    }

    /**
     * Provede revalidaci předaných hodnot strukturovaného typu.
     *
     * @param structureDataList strukturovaný typ
     */
    private List<ArrStructureData> revalidateStructureData(final List<ArrStructureData> structureDataList) {
        Assert.notNull(structureDataList, "Musí být vyplněn list hodnot strukt. typu");

        for (ArrStructureData structureData : structureDataList) {
            structureData.setValue(null);
            structureData.setErrorDescription(null);
        }
        structureDataRepository.save(structureDataList);
        structureDataService.addToValidate(structureDataList);
        return structureDataList;
    }

    /**
     * Provede revalidaci podle strukt. typu.
     *
     * @param structureTypes revalidované typy
     */
    public void revalidateStructureTypes(final Collection<RulStructureType> structureTypes) {
        if (structureTypes.isEmpty()) {
            return;
        }
        List<Integer> structureDataIds = structureDataRepository.findStructureDataIdByStructureTypes(structureTypes);
        structureDataService.addIdsToValidate(structureDataIds);
    }

    /**
     * Provede revalidaci podle rozšíření strukt. typu.
     *
     * @param structureExtensions revalidované typy
     */
    public void revalidateStructureExtensions(final Collection<RulStructureExtension> structureExtensions) {
        if (structureExtensions.isEmpty()) {
            return;
        }
        List<Integer> structureDataIds = structureDataRepository.findStructureDataIdByActiveStructureExtensions(structureExtensions);
        structureDataService.addIdsToValidate(structureDataIds);
    }

    /**
     * Vrátí strukt. typ podle kódu.
     *
     * @param structureTypeCode kód strukt. typu
     * @return entita
     */
    public RulStructureType getStructureTypeByCode(final String structureTypeCode) {
        RulStructureType structureType = structureTypeRepository.findByCode(structureTypeCode);
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
    public ArrStructureData getStructureDataById(final Integer structureDataId) {
        ArrStructureData structureData = structureDataRepository.findOneFetch(structureDataId);
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
    public List<ArrStructureData> getStructureDataByIds(final List<Integer> structureDataIds) {
        List<List<Integer>> idsParts = Lists.partition(structureDataIds, 1000);
        List<ArrStructureData> structureDataList = new ArrayList<>();
        for (List<Integer> idsPart : idsParts) {
            structureDataList.addAll(structureDataRepository.findByIdsFetch(idsPart));
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
    public ArrStructureData getStructureDataById(final Integer structureDataId,
                                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        ArrStructureData structureData = getStructureDataById(structureDataId);
        if (!structureData.getStructureType().getRuleSet().equals(fundVersion.getRuleSet())) {
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
    public ArrStructureData deleteStructureItemsByType(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                       final Integer structureDataId,
                                                       final Integer itemTypeId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructureData structureData = getStructureDataById(structureDataId);
        RulItemType type = ruleService.getItemTypeById(itemTypeId);
        validateRuleSet(fundVersion, type);
        List<ArrStructureItem> structureItems = structureItemRepository.findOpenItems(type, structureData);

        ArrChange change = structureData.getState() == ArrStructureData.State.TEMP ? structureData.getCreateChange() : arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        for (ArrStructureItem structureItem : structureItems) {
            structureItem.setDeleteChange(change);
        }

        structureItemRepository.save(structureItems);

        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructureType().getCode(),
                null,
                null,
                Collections.singletonList(structureData.getStructureDataId()),
                null));

        return structureDataService.validate(structureData);
    }

    /**
     * Potvrzení hodnoty strukt. datového typu.
     *
     * @param fund          archivní soubor
     * @param structureData hodnota struktovaného datového typu
     * @return entita
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrStructureData confirmStructureData(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                 final ArrStructureData structureData) {
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
    private ArrStructureData confirmStructureData(final ArrFund fund,
                                                  final ArrStructureData structureData,
                                                  final boolean event) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze potvrdit smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        if (!structureData.getState().equals(ArrStructureData.State.TEMP)) {
            throw new BusinessException("Strukturovaná data nemají dočasný stav", BaseCode.INVALID_STATE);
        }
        structureData.setState(ArrStructureData.State.OK);
        ArrStructureData confirmStructureData = structureDataService.validate(structureData);
        if (event) {
            notificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                    structureData.getStructureType().getCode(),
                    null,
                    Collections.singletonList(confirmStructureData.getStructureDataId()),
                    null,
                    null));
        }
        return confirmStructureData;
    }

    /**
     * Vyhledání strukturovaných typů podle pravidel.
     *
     * @param fundVersion verze AS
     * @return nalezené entity
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<RulStructureType> findStructureTypes(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
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
    public FilteredResult<ArrStructureData> findStructureData(final RulStructureType structureType,
                                                              @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                              @Nullable final String search,
                                                              @Nullable final Boolean assignable,
                                                              final int from,
                                                              final int count) {
        return structureDataRepository.findStructureData(structureType.getStructureTypeId(), fund.getFundId(), search, assignable, from, count);
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
                                           final RulStructureType structureType,
                                           final List<RulStructureExtension> structureExtensions) {
        structureExtensions.forEach(se -> validateFundStructureExtension(fundVersion, structureType, se));

        List<ArrFundStructureExtension> fundStructureExtensions = fundStructureExtensionRepository.findByFundAndDeleteChangeIsNull(fundVersion.getFund());

        List<ArrFundStructureExtension> fundStructureExtensionsDelete = new ArrayList<>(fundStructureExtensions);
        fundStructureExtensionsDelete.removeIf(fundStructureExtension -> structureExtensions.contains(fundStructureExtension.getStructureExtension()));

        List<ArrFundStructureExtension> fundStructureExtensionsCreate = new ArrayList<>();

        for (RulStructureExtension structureExtension : structureExtensions) {
            boolean exists = false;
            for (ArrFundStructureExtension fundStructureExtension : fundStructureExtensions) {
                if (structureExtension.equals(fundStructureExtension.getStructureExtension())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                ArrFundStructureExtension fundStructureExtensionCreate = new ArrFundStructureExtension();
                fundStructureExtensionCreate.setFund(fundVersion.getFund());
                fundStructureExtensionCreate.setStructureExtension(structureExtension);
                fundStructureExtensionsCreate.add(fundStructureExtensionCreate);
            }
        }

        if (fundStructureExtensionsCreate.size() > 0 || fundStructureExtensionsDelete.size() > 0) {
            final ArrChange change = arrangementService.createChange(ArrChange.Type.SET_FUND_STRUCTURE_EXT);
            fundStructureExtensionsCreate.forEach(fse -> fse.setCreateChange(change));
            fundStructureExtensionsDelete.forEach(fse -> fse.setDeleteChange(change));
            fundStructureExtensionRepository.save(fundStructureExtensionsCreate);
            fundStructureExtensionRepository.save(fundStructureExtensionsDelete);
            Set<RulStructureType> structureTypes = new HashSet<>();
            fundStructureExtensionsCreate.forEach(fse -> structureTypes.add(fse.getStructureExtension().getStructureType()));
            fundStructureExtensionsDelete.forEach(fse -> structureTypes.add(fse.getStructureExtension().getStructureType()));
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
    private void validateFundStructureExtension(final ArrFundVersion fundVersion, final RulStructureType structureType, final RulStructureExtension structureExtension) {
        if (!structureType.equals(structureExtension.getStructureType())) {
            throw new BusinessException("Rozšíření nespadá pod strukt. typ", BaseCode.INVALID_STATE);
        }
        if (!fundVersion.getRuleSet().equals(structureExtension.getStructureType().getRuleSet())) {
            throw new BusinessException("AS a rozšíření mají rozdílná pravidla", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Vrací rozšíření strukt. typu podle kódu.
     *
     * @param structureExtensionCodes kódy rozšíření strukt. typu
     * @return entita
     */
    public List<RulStructureExtension> findStructureExtensionByCodes(final List<String> structureExtensionCodes) {
        List<RulStructureExtension> structureExtensions = structureExtensionRepository.findByCodeIn(structureExtensionCodes);
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
    public List<RulStructureExtension> findAllStructureExtensions(final RulStructureType structureType) {
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
    public List<RulStructureExtension> findStructureExtensions(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                                               final RulStructureType structureType) {
        return structureExtensionRepository.findActiveByFundAndStructureType(fund, structureType);
    }

    /**
     * Založení duplikátů strukturovaného datového typu a autoinkrementační.
     * Předloha musí být ve stavu {@link ArrStructureData.State#TEMP}.
     * Předloha je validována hned, nové hodnoty asynchronně.
     *
     * @param fundVersion   verze AS
     * @param structureData hodnota struktovaného datového typu, ze které se vychází
     * @param count         počet položek, které se budou budou vytvářet (včetně zdrojové hodnoty strukt. typu)
     * @param itemTypeIds   identifikátory typů, které se mají autoincrementovat
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void duplicateStructureDataBatch(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                            final ArrStructureData structureData,
                                            final int count,
                                            final List<Integer> itemTypeIds) {
        if (count <= 0) {
            throw new BusinessException("Počet vytvářených položek musí být kladný", BaseCode.INVALID_STATE);
        }
        if (structureData.getState() != ArrStructureData.State.TEMP) {
            throw new BusinessException("Neplatný stav hodnoty strukt. typu: " + structureData.getState(), BaseCode.INVALID_STATE);
        }
        if (!fundVersion.getRuleSet().equals(structureData.getStructureType().getRuleSet())) {
            throw new BusinessException("Pravidla AS nesouhlasí s pravidly hodnoty strukt. typu", BaseCode.INVALID_STATE);
        }

        List<RulItemType> itemTypes = findAndValidateIntItemTypes(fundVersion, itemTypeIds);
        List<ArrStructureItem> structureItems = findStructureItems(structureData);

        validateStructureItems(itemTypes, structureItems);

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA_BATCH);
        List<ArrStructureData> structureDataList = createStructureDataList(fundVersion.getFund(),
                structureData.getStructureType(), ArrStructureData.State.OK, change, count - 1);

        int countItems = structureDataList.size() * structureItems.size();
        if (countItems > 0) {
            List<ArrStructureItem> newStructureItems = new ArrayList<>();
            List<ArrData> newDataList = new ArrayList<>();

            Map<RulItemType, Integer> autoincrementMap = createAutoincrementMap(structureItems, itemTypes);
            for (ArrStructureData newStructureData : structureDataList) {
                for (ArrStructureItem structureItem : structureItems) {
                    ArrStructureItem copyStructureItem = new ArrStructureItem();
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
                    copyStructureItem.setStructureData(newStructureData);
                    copyStructureItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
                    copyStructureItem.setItemType(structureItem.getItemType());
                    copyStructureItem.setItemSpec(structureItem.getItemSpec());
                    newStructureItems.add(copyStructureItem);
                }
            }
            dataRepository.save(newDataList);
            structureItemRepository.save(newStructureItems);
        }

        ArrStructureData confirmStructureData = confirmStructureData(fundVersion.getFund(), structureData, false);
        structureDataList = revalidateStructureData(structureDataList);

        List<Integer> structureDataIds = structureDataList.stream().map(ArrStructureData::getStructureDataId).collect(Collectors.toList());
        structureDataIds.add(confirmStructureData.getStructureDataId());
        notificationService.publishEvent(new EventStructureDataChange(fundVersion.getFundId(),
                structureData.getStructureType().getCode(),
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
    private Map<RulItemType, Integer> createAutoincrementMap(final List<ArrStructureItem> structureItems,
                                                             final List<RulItemType> itemTypes) {
        Map<RulItemType, Integer> result = new HashMap<>(itemTypes.size());
        for (RulItemType itemType : itemTypes) {
            for (ArrStructureItem structureItem : structureItems) {
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
    private void validateStructureItems(final List<RulItemType> itemTypes, final List<ArrStructureItem> structureItems) {
        List<RulItemType> itemTypesRequired = new ArrayList<>(itemTypes);
        for (ArrStructureItem structureItem : structureItems) {
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
    public void updateStructureDataBatch(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                         final RulStructureType structureType,
                                         final List<Integer> structureDataIds,
                                         final List<ArrStructureItem> sourceStructureItems,
                                         final List<Integer> autoincrementItemTypeIds,
                                         final List<Integer> deleteItemTypeIds) {
        List<ArrStructureData> structureDataList = getStructureDataByIds(structureDataIds);
        validateStructureData(fundVersion, structureType, structureDataList);

        List<RulItemType> autoincrementItemTypes = autoincrementItemTypeIds.isEmpty() ? Collections.emptyList() : findAndValidateIntItemTypes(fundVersion, autoincrementItemTypeIds);
        validateStructureItems(autoincrementItemTypes, sourceStructureItems);

        Map<ArrStructureData, List<ArrStructureItem>> structureDataStructureItems = findStructureItems(structureDataList);
        ArrChange change = arrangementService.createChange(ArrChange.Type.UPDATE_STRUCT_DATA_BATCH);

        Set<Integer> allDeleteItemTypeIds = new HashSet<>(deleteItemTypeIds);
        allDeleteItemTypeIds.addAll(sourceStructureItems.stream().map(ArrStructureItem::getItemTypeId).collect(Collectors.toList()));

        List<ArrStructureItem> deleteStructureItems = new ArrayList<>();
        Map<RulItemType, Integer> autoincrementMap = createAutoincrementMap(sourceStructureItems, autoincrementItemTypes);

        List<ArrStructureItem> newStructureItems = new ArrayList<>();
        List<ArrData> newDataList = new ArrayList<>();
        for (ArrStructureData structureData : structureDataList) {
            List<ArrStructureItem> structureItems = structureDataStructureItems.get(structureData);
            if (CollectionUtils.isNotEmpty(structureItems)) {
                for (ArrStructureItem structureItem : structureItems) {
                    if (allDeleteItemTypeIds.contains(structureItem.getItemTypeId())) {
                        structureItem.setDeleteChange(change);
                        deleteStructureItems.add(structureItem);
                    }
                }
            }

            Map<RulItemType, Integer> itemTypePositionMap = new HashMap<>();
            for (ArrStructureItem structureItem : sourceStructureItems) {
                Integer position = itemTypePositionMap.computeIfAbsent(structureItem.getItemType(), k -> 0);
                position++;
                itemTypePositionMap.put(structureItem.getItemType(), position);

                ArrStructureItem copyStructureItem = new ArrStructureItem();
                ArrData newData = ArrData.makeCopyWithoutId(structureItem.getData());
                newData.setDataType(structureItem.getItemType().getDataType());
                Integer val = autoincrementMap.get(structureItem.getItemType());
                if (val != null) {
                    val++;
                    ((ArrDataInteger) newData).setValue(val);
                    autoincrementMap.put(structureItem.getItemType(), val);
                }
                newDataList.add(newData);
                copyStructureItem.setData(newData);
                copyStructureItem.setCreateChange(change);
                copyStructureItem.setPosition(position);
                copyStructureItem.setStructureData(structureData);
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
     * @param structureDataList hodnoty strukturovaného datového typu
     */
    private void validateStructureData(final ArrFundVersion fundVersion,
                                       final RulStructureType structureType,
                                       final List<ArrStructureData> structureDataList) {
        if (CollectionUtils.isEmpty(structureDataList)) {
            throw new BusinessException("Musí být upravována alespoň jedna hodnota strukt. typu", BaseCode.INVALID_STATE);
        }
        for (ArrStructureData structureData : structureDataList) {
            if (structureData.getDeleteChange() != null) {
                throw new BusinessException("Nelze upravit již smazaná strukturovaná data", BaseCode.INVALID_STATE)
                        .set("id", structureData.getStructureDataId());
            }
            if (!structureData.getFund().equals(fundVersion.getFund())) {
                throw new BusinessException("Strukturovaná data nepatří pod AS", BaseCode.INVALID_STATE)
                        .set("id", structureData.getStructureDataId());
            }
            if (!structureData.getStructureType().equals(structureType)) {
                throw new BusinessException("Strukturovaná data jsou jiného strukt. datového typu", BaseCode.INVALID_STATE)
                        .set("id", structureData.getStructureDataId());
            }
        }
    }
}
