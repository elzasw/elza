package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundStructureExtensionRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionRepository;
import cz.tacr.elza.repository.StructureItemRepository;
import cz.tacr.elza.repository.StructureTypeRepository;
import org.castor.core.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    public StructureService(final StructureItemRepository structureItemRepository,
                            final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                            final StructureExtensionRepository structureExtensionRepository, final StructureDefinitionRepository structureDefinitionRepository,
                            final StructureDataRepository structureDataRepository,
                            final StructureTypeRepository structureTypeRepository,
                            final RulesExecutor rulesExecutor,
                            final ArrangementService arrangementService,
                            final DataRepository dataRepository,
                            final RuleService ruleService, final FundStructureExtensionRepository fundStructureExtensionRepository) {
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

        ArrData data = createData(structureItem.getData());

        ArrStructureItem createStructureItem = new ArrStructureItem();
        createStructureItem.setData(data);
        createStructureItem.setCreateChange(change);
        createStructureItem.setPosition(position);
        createStructureItem.setStructureData(structureData);
        createStructureItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
        createStructureItem.setItemType(structureItem.getItemType());
        createStructureItem.setItemSpec(structureItem.getItemSpec());

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

        validateRuleSet(fundVersion, structureItemDB.getItemType().getStructureType());

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


            ArrData updateData = updateData(structureItem.getData(), true);

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
            updateData(updateData, false);
        }

        return structureItemRepository.save(updateStructureItem);
    }

    /**
     * Provede kontrolu zda-li verze AS má stejná pravidla jako strukt. typ.
     *
     * @param fundVersion   verze AS
     * @param structureType strukturovaný typ
     */
    private void validateRuleSet(final ArrFundVersion fundVersion, final RulStructureType structureType) {
        if (!fundVersion.getRuleSet().equals(structureType.getRuleSet())) {
            throw new BusinessException("Fund a strukturovaný typ nemají stejná pravidla", BaseCode.INVALID_STATE)
                    .set("fund_rul_set", fundVersion.getRuleSet().getCode())
                    .set("structure_type_rul_set", structureType.getRuleSet().getCode());
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

        validateRuleSet(fundVersion, structureItemDB.getStructureData().getStructureType());

        structureItemDB.setDeleteChange(change);
        return structureItemRepository.save(structureItemDB);
    }

    /**
     * Vytvoření dat. Provede kopii z předlohy a založení v DB.
     *
     * @param data předloha dat
     * @return vytvořená data
     */
    private ArrData createData(final ArrData data) {
        return dataRepository.save(data.copy());
    }

    /**
     * Úprava dat. Provede uložení změněných dat, popřípadně založení kopie.
     *
     * @param data             ukládaná data
     * @param createNewVersion vytvořit nová data
     * @return uložená / nová data
     */
    private ArrData updateData(final ArrData data, final boolean createNewVersion) {
        if (createNewVersion) {
            return dataRepository.save(data.copy());
        } else {
            Assert.notNull(data.getDataId(), "Identifikátor musí být vyplněn");
            return dataRepository.save(data);
        }
    }

    /**
     * Vygenerování hodnoty pro hodnotu strukt. datového typu.
     *
     * @param structureData hodnota struktovaného datového typu
     * @return hodnota
     */
    public String generateValue(final ArrStructureData structureData) {

        // TODO slapa: do cache?
        RulStructureType structureType = structureData.getStructureType();
        File groovyFile = findGroovyFile(structureType, structureData.getFund());
        GroovyScriptService.GroovyScriptFile groovyScriptFile;
        try {
            groovyScriptFile = GroovyScriptService.GroovyScriptFile.createFromFile(groovyFile);
        } catch (IOException e) {
            throw new SystemException("Problém při zpracování groovy scriptu", e);
        }

        List<ArrStructureItem> structureItems = structureItemRepository.findByStructureDataAndDeleteChangeIsNull(structureData);

        Map<String, Object> input = new HashMap<>();
        input.put("ITEMS", structureItems);
        return (String) groovyScriptFile.evaluate(input);
    }

    /**
     * Vyhledání groovy scriptu podle strukturovaného typu k AS.
     *
     * @param structureType strukturovaný typ
     * @param fund          archivní soubor
     * @return nalezený groovy soubor
     */
    private File findGroovyFile(final RulStructureType structureType, final ArrFund fund) {
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeAndFundOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE, fund);
        RulComponent component;
        RulPackage rulPackage;
        if (structureExtensionDefinitions.size() > 0) {
            RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions.get(structureExtensionDefinitions.size() - 1);
            component = structureExtensionDefinition.getComponent();
            rulPackage = structureExtensionDefinition.getRulPackage();
        } else {
            List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository
                    .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }
        return new File(rulesExecutor.getDroolsDir(rulPackage.getCode(), structureType.getRuleSet().getCode())
                + File.separator + component.getFilename());
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
     * Vrátí strukt. data podle identifikátoru.
     *
     * @param structureDataId identifikátor hodnoty strukt. datového typu
     * @return entita
     */
    public ArrStructureData getStructureDataById(final Integer structureDataId) {
        ArrStructureData structureData = structureDataRepository.findOne(structureDataId);
        if (structureData == null) {
            throw new ObjectNotFoundException("Strukturovaná data neexistují: " + structureDataId, BaseCode.ID_NOT_EXIST).setId(structureDataId);
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
        validateRuleSet(fundVersion, structureData.getStructureType());
        RulItemType type = ruleService.getItemTypeById(itemTypeId);
        List<ArrStructureItem> structureItems = structureItemRepository.findOpenItems(type, structureData);

        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_ITEM);
        for (ArrStructureItem structureItem : structureItems) {
            structureItem.setDeleteChange(change);
        }

        structureItemRepository.save(structureItems);
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
        structureData.setValue(generateValue(structureData));
        return structureDataRepository.save(structureData);
    }

    /**
     * Získání seznamu typů atributů podle strukt. typu a verze AS.
     *
     * @param structureType strukturovaný typ
     * @param fundVersion   verze AS
     * @return seznam typu atributů
     */
    public List<RulItemTypeExt> getStructureItemTypes(final RulStructureType structureType, final ArrFundVersion fundVersion) {
        List<RulItemTypeExt> rulDescItemTypeExtList = ruleService.getAllItemTypes(structureType.getRuleSet());
        return rulesExecutor.executeStructureItemTypesRules(structureType, rulDescItemTypeExtList, fundVersion);
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

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_FUND_STRUCTURE_EXTENSION);
        ArrFundStructureExtension newFundStructureExtension = new ArrFundStructureExtension();
        newFundStructureExtension.setCreateChange(change);
        newFundStructureExtension.setFund(fundVersion.getFund());
        newFundStructureExtension.setStructureExtension(structureExtension);

        // TODO slapa: revalidace dotčených entit
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

        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_FUND_STRUCTURE_EXTENSION);
        fundStructureExtension.setDeleteChange(change);

        // TODO slapa: revalidace dotčených entit
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
}
