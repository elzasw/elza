package cz.tacr.elza.service;

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
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionRepository;
import cz.tacr.elza.repository.StructureItemRepository;
import cz.tacr.elza.repository.StructureTypeRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.castor.core.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servisní třída pro práci se strukturovanými datovými typy.
 *
 * @since 06.11.2017
 */
@Service
public class StructureService {

    private final StructureItemRepository structureItemRepository;
    private final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;
    private final StructureExtensionRepository structureExtensionRepository;
    private final StructureDefinitionRepository structureDefinitionRepository;
    private final StructureDataRepository structureDataRepository;
    private final StructureTypeRepository structureTypeRepository;
    private final RulesExecutor rulesExecutor;
    private final ArrangementService arrangementService;
    private final DataRepository dataRepository;
    private final RuleService ruleService;
    private final FundStructureExtensionRepository fundStructureExtensionRepository;
    private final StructureDataService structureDataService;
    private final ItemTypeRepository itemTypeRepository;

    @Autowired
    public StructureService(final StructureItemRepository structureItemRepository,
                            final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                            final StructureExtensionRepository structureExtensionRepository,
                            final StructureDefinitionRepository structureDefinitionRepository,
                            final StructureDataRepository structureDataRepository,
                            final StructureTypeRepository structureTypeRepository,
                            final RulesExecutor rulesExecutor,
                            final ArrangementService arrangementService,
                            final DataRepository dataRepository,
                            final RuleService ruleService, final FundStructureExtensionRepository fundStructureExtensionRepository,
                            final StructureDataService structureDataService,
                            final ItemTypeRepository itemTypeRepository) {
        this.structureItemRepository = structureItemRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.structureExtensionRepository = structureExtensionRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.structureDataRepository = structureDataRepository;
        this.structureTypeRepository = structureTypeRepository;
        this.rulesExecutor = rulesExecutor;
        this.arrangementService = arrangementService;
        this.dataRepository = dataRepository;
        this.ruleService = ruleService;
        this.fundStructureExtensionRepository = fundStructureExtensionRepository;
        this.structureDataService = structureDataService;
        this.itemTypeRepository = itemTypeRepository;
    }

    /**
     * Vyhledá platné položky k hodnotě strukt. datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return nalezené položky
     */
    public List<ArrStructureItem> findStructureItems(final ArrStructureData structureData) {
        return structureItemRepository.findByStructureDataAndDeleteChangeIsNull(structureData);
    }

    /**
     * Vytvoření strukturovaných dat.
     *
     * @param fund          archivní soubor
     * @param structureType strukturovaný typ
     * @return vytvořená entita
     */
    public ArrStructureData createStructureData(final ArrFund fund,
                                                final RulStructureType structureType,
                                                final ArrStructureData.State state) {
        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_DATA);
        ArrStructureData structureData = new ArrStructureData();
        structureData.setAssignable(true);
        structureData.setCreateChange(change);
        structureData.setFund(fund);
        structureData.setStructureType(structureType);
        structureData.setState(state);
        return structureDataRepository.save(structureData);
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
    public ArrStructureData deleteStructureData(final ArrStructureData structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze odstranit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_DATA);
        structureData.setDeleteChange(change);
        return structureDataRepository.save(structureData);
    }

    /**
     * Vytvoření položky k hodnotě strukt. datového typu.
     *
     * @param structureItem   položka
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @param fundVersionId   identifikátor verze AS
     * @return vytvořená entita
     */
    public ArrStructureItem createStructureItem(final ArrStructureItem structureItem,
                                                final Integer structureDataId,
                                                final Integer fundVersionId) {

        ArrStructureData structureData = getStructureDataById(structureDataId);
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        if (!fundVersion.getRuleSet().equals(structureData.getStructureType().getRuleSet())) {
            throw new BusinessException("Fund a strukturovaný typ nemají stejná pravidla", BaseCode.INVALID_STATE)
                    .set("fund_rul_set", fundVersion.getRuleSet().getCode())
                    .set("structure_type_rul_set", structureData.getStructureType().getRuleSet().getCode());
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_STRUCTURE_ITEM, null);

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

        structureDataService.addToValidate(structureData);
        return structureItemRepository.save(createStructureItem);
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
                ArrData newData = newStructureItem.getData().copy();
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
    public ArrStructureItem updateStructureItem(final ArrStructureItem structureItem,
                                                final Integer fundVersionId,
                                                final boolean createNewVersion) {
        ArrChange change = arrangementService.createChange(ArrChange.Type.UPDATE_STRUCTURE_ITEM);
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        ArrStructureItem structureItemDB = structureItemRepository.findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

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

        structureDataService.addToValidate(structureItemDB.getStructureData());
        return structureItemRepository.save(updateStructureItem);
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
    public ArrStructureItem deleteStructureItem(final ArrStructureItem structureItem, final Integer fundVersionId) {
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);

        ArrStructureItem structureItemDB = structureItemRepository.findOpenItemFetchData(structureItem.getDescItemObjectId());
        if (structureItemDB == null) {
            throw new ObjectNotFoundException("Neexistuje položka s OID: " + structureItem.getDescItemObjectId(), BaseCode.ID_NOT_EXIST).setId(structureItem.getDescItemObjectId());
        }

        validateRuleSet(fundVersion, structureItemDB.getItemType());

        structureItemDB.setDeleteChange(change);
        structureDataService.addToValidate(structureItemDB.getStructureData());
        return structureItemRepository.save(structureItemDB);
    }

    /**
     * Vytvoření dat. Provede kopii z předlohy a založení v DB.
     *
     * @param data     předloha dat
     * @param dataType datový typ dat
     * @return vytvořená data
     */
    private ArrData createData(final ArrData data, final RulDataType dataType) {
        ArrData copy = data.copy();
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
            ArrData copy = data.copy();
            copy.setDataType(dataType);
            return dataRepository.save(copy);
        } else {
            Assert.notNull(data.getDataId(), "Identifikátor musí být vyplněn");
            return dataRepository.save(data);
        }
    }

    /**
     * Provede revalidaci hodnot strukturovaného typu pro AS.
     *
     * @param fundVersion   verze AS
     * @param structureType strukturovaný typ
     */
    private void revalidateStructureData(final ArrFundVersion fundVersion, final RulStructureType structureType) {
        Assert.notNull(fundVersion, "Musí být vybrána verze AS");
        Assert.notNull(structureType, "Musí být vybrán strukturovaný typ");

        List<ArrStructureData> structureDataList = structureDataRepository.findByStructureTypeAndFundAndDeleteChangeIsNull(structureType, fundVersion.getFund());
        revalidateStructureData(structureDataList);
    }

    /**
     * Provede revalidaci předaných hodnot strukturovaného typu.
     *
     * @param structureDataList strukturovaný typ
     */
    private void revalidateStructureData(final List<ArrStructureData> structureDataList) {
        Assert.notNull(structureDataList, "Musí být vyplněn list hodnot strukt. typu");

        for (ArrStructureData structureData : structureDataList) {
            structureData.setValue(null);
            structureData.setErrorDescription(null);
        }
        structureDataRepository.save(structureDataList);
        structureDataService.addToValidate(structureDataList);
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
     * Vrátí strukt. data podle identifikátoru včetně načtených návazných entit.
     *
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @param fundVersion     verze AS
     * @return entita
     */
    public ArrStructureData getStructureDataById(final Integer structureDataId, final ArrFundVersion fundVersion) {
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
    public void deleteStructureItemsByType(final Integer fundVersionId,
                                           final Integer structureDataId,
                                           final Integer itemTypeId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        ArrStructureData structureData = getStructureDataById(structureDataId);
        RulItemType type = ruleService.getItemTypeById(itemTypeId);
        validateRuleSet(fundVersion, type);
        List<ArrStructureItem> structureItems = structureItemRepository.findOpenItems(type, structureData);

        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        for (ArrStructureItem structureItem : structureItems) {
            structureItem.setDeleteChange(change);
        }

        structureItemRepository.save(structureItems);
        structureDataService.addToValidate(structureData);
    }

    /**
     * Potvrzení hodnoty strukt. datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return entita
     */
    public ArrStructureData confirmStructureData(final ArrStructureData structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze potvrdit smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        if (!structureData.getState().equals(ArrStructureData.State.TEMP)) {
            throw new BusinessException("Strukturovaná data nemají dočasný stav", BaseCode.INVALID_STATE);
        }
        structureData.setState(ArrStructureData.State.OK);
        return structureDataService.validate(structureData);
    }

    /**
     * Vyhledání strukturovaných typů podle pravidel.
     *
     * @param ruleSet pravidla
     * @return nalezené entity
     */
    public List<RulStructureType> findStructureTypes(final RulRuleSet ruleSet) {
        return structureTypeRepository.findByRuleSet(ruleSet);
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
    public FilteredResult<ArrStructureData> findStructureData(final RulStructureType structureType,
                                                              final ArrFund fund,
                                                              @Nullable final String search,
                                                              @Nullable final Boolean assignable,
                                                              final int from,
                                                              final int count) {
        return structureDataRepository.findStructureData(structureType.getStructureTypeId(), fund.getFundId(), search, assignable, from, count);
    }

    /**
     * Aktivuje rozšíření u archivního souboru.
     *
     * @param fundVersion        verze AS
     * @param structureExtension rozšížení strukt. typu
     * @return vytvořená entita
     */
    public ArrFundStructureExtension addFundStructureExtension(final ArrFundVersion fundVersion,
                                                               final RulStructureExtension structureExtension) {
        validateFundStrucutureExtension(fundVersion, structureExtension);

        ArrFundStructureExtension fundStructureExtension = fundStructureExtensionRepository
                .findByFundAndStructureExtensionAndDeleteChangeIsNull(fundVersion.getFund(), structureExtension);
        if (fundStructureExtension != null) {
            throw new BusinessException("U AS je již rozšíření aktivováno", BaseCode.INVALID_STATE);
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_FUND_STRUCTURE_EXT);
        ArrFundStructureExtension newFundStructureExtension = new ArrFundStructureExtension();
        newFundStructureExtension.setCreateChange(change);
        newFundStructureExtension.setFund(fundVersion.getFund());
        newFundStructureExtension.setStructureExtension(structureExtension);

        revalidateStructureData(fundVersion, structureExtension.getStructureType());
        return fundStructureExtensionRepository.save(newFundStructureExtension);
    }

    /**
     * Deaktivuje rozšíření u archivního souboru.
     *
     * @param fundVersion        verze AS
     * @param structureExtension rozšížení strukt. typu
     * @return smazaná entita
     */
    public ArrFundStructureExtension deleteFundStructureExtension(final ArrFundVersion fundVersion, final RulStructureExtension structureExtension) {
        validateFundStrucutureExtension(fundVersion, structureExtension);

        ArrFundStructureExtension fundStructureExtension = fundStructureExtensionRepository
                .findByFundAndStructureExtensionAndDeleteChangeIsNull(fundVersion.getFund(), structureExtension);
        if (fundStructureExtension == null) {
            throw new BusinessException("U AS rozšíření není aktivováno", BaseCode.INVALID_STATE);
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_FUND_STRUCTURE_EXT);
        fundStructureExtension.setDeleteChange(change);

        revalidateStructureData(fundVersion, structureExtension.getStructureType());
        return fundStructureExtensionRepository.save(fundStructureExtension);
    }

    /**
     * Validace AS a rozšíření strukt. typu.
     *
     * @param fundVersion        verze AS
     * @param structureExtension rozšížení strukt. typu
     */
    private void validateFundStrucutureExtension(final ArrFundVersion fundVersion, final RulStructureExtension structureExtension) {
        if (!fundVersion.getRuleSet().equals(structureExtension.getStructureType().getRuleSet())) {
            throw new BusinessException("AS a rozšíření mají rozdílná pravidla", BaseCode.INVALID_STATE);
        }
    }

    /**
     * Vrací rozšíření strukt. typu podle kódu.
     *
     * @param structureExtensionCode kód rozšíření strukt. typu
     * @return entita
     */
    public RulStructureExtension getStructureExtensionByCode(final String structureExtensionCode) {
        RulStructureExtension structureExtension = structureExtensionRepository.findByCode(structureExtensionCode);
        if (structureExtension == null) {
            throw new ObjectNotFoundException("Rozšíření neexistuje: " + structureExtensionCode, BaseCode.ID_NOT_EXIST).setId(structureExtensionCode);
        }
        return structureExtension;
    }

    /**
     * Nalezne všechny dostupné rozšížení pro strukturovaný typ.
     *
     * @param fundVersion verze AS
     * @return nalezené entity
     */
    public List<RulStructureExtension> findAllStructureExtensions(final ArrFundVersion fundVersion) {
        return structureExtensionRepository.findByRuleSet(fundVersion.getRuleSet());
    }

    /**
     * Nalezne aktivní rozšíření pro strukturovaný typ.
     *
     * @param fundVersion verze AS
     * @return nalezené entity
     */
    public List<RulStructureExtension> findStructureExtensions(final ArrFundVersion fundVersion) {
        return structureExtensionRepository.findActiveByFundAndRuleSet(fundVersion.getFund(), fundVersion.getRuleSet());
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
    public void duplicateStructureDataBatch(final ArrFundVersion fundVersion,
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
                    ArrData newData = structureItem.getData().copy();
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

        confirmStructureData(structureData);
        revalidateStructureData(structureDataList);
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

}
