package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructureItemRepository;
import cz.tacr.elza.repository.StructureTypeRepository;
import org.castor.core.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: vyplnit popis třídy
 *
 * @since 06.11.2017
 */
@Service
public class StructureService {

    private final GroovyScriptService groovyScriptService;
    private final StructureItemRepository structureItemRepository;
    private final RuleSetRepository ruleSetRepository;
    private final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;
    private final StructureDefinitionRepository structureDefinitionRepository;
    private final StructureDataRepository structureDataRepository;
    private final StructureTypeRepository structureTypeRepository;
    private final RulesExecutor rulesExecutor;
    private final ArrangementService arrangementService;
    private final DataRepository dataRepository;
    private final RuleService ruleService;

    @Autowired
    public StructureService(final GroovyScriptService groovyScriptService,
                            final StructureItemRepository structureItemRepository,
                            final RuleSetRepository ruleSetRepository,
                            final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                            final StructureDefinitionRepository structureDefinitionRepository,
                            final StructureDataRepository structureDataRepository,
                            final StructureTypeRepository structureTypeRepository,
                            final RulesExecutor rulesExecutor,
                            final ArrangementService arrangementService,
                            final DataRepository dataRepository,
                            final RuleService ruleService) {
        this.groovyScriptService = groovyScriptService;
        this.structureItemRepository = structureItemRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.structureDataRepository = structureDataRepository;
        this.structureTypeRepository = structureTypeRepository;
        this.rulesExecutor = rulesExecutor;
        this.arrangementService = arrangementService;
        this.dataRepository = dataRepository;
        this.ruleService = ruleService;
    }

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

    public ArrStructureData updateStructureData(final ArrStructureData structureData) {
        // TODO slapa: přegenerování value?
        return structureDataRepository.save(structureData);
    }

    public ArrStructureData deleteStructureData(final ArrStructureData structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze odstranit již smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_STRUCTURE_DATA);
        structureData.setDeleteChange(change);
        return structureDataRepository.save(structureData);
    }

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

        structureItemRepository.save(createStructureItem);

        return createStructureItem;
    }

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

    private int findNextPosition(final ArrStructureData structureData, final RulItemType itemType) {
        List<ArrStructureItem> structureItems = structureItemRepository.findOpenItemsAfterPosition(itemType,
                structureData, 0, new PageRequest(0, 1, Sort.Direction.DESC, ArrItem.POSITION));
        if (structureItems.size() == 0) {
            return 1;
        } else {
            return structureItems.get(0).getPosition() + 1;
        }
    }

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

            ArrData updateData = structureItem.getData().copy();
            dataRepository.save(updateData);

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
            dataRepository.save(updateData);
        }

        return structureItemRepository.save(updateStructureItem);
    }

    private void validateRuleSet(final ArrFundVersion fundVersion, final RulStructureType structureType) {
        if (!fundVersion.getRuleSet().equals(structureType.getRuleSet())) {
            throw new BusinessException("Fund a strukturovaný typ nemají stejná pravidla", BaseCode.INVALID_STATE)
                    .set("fund_rul_set", fundVersion.getRuleSet().getCode())
                    .set("structure_type_rul_set", structureType.getRuleSet().getCode());
        }
    }

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

    private ArrData createData(final ArrData data) {
        return dataRepository.save(data.copy());
    }

    private ArrData updateData(final ArrData data, final boolean createNewVersion) {
        if (createNewVersion) {
            return dataRepository.save(data.copy());
        } else {
            Assert.notNull(data.getDataId(), "Identifikátor musí být vyplněn");
            return dataRepository.save(data);
        }
    }

    public String generateValue(final ArrStructureData structureData) throws IOException {

        // TODO slapa: do cache?
        RulStructureType structureType = structureData.getStructureType();
        File groovyFile = findGroovyFile(structureType);
        GroovyScriptService.GroovyScriptFile groovyScriptFile = GroovyScriptService.GroovyScriptFile.createFromFile(groovyFile);

        List<ArrStructureItem> structureItems = structureItemRepository.findByStructureDataAndDeleteChangeIsNull(structureData);

        Map<String, Object> input = new HashMap<>();
        input.put("ITEMS", structureItems);
        return (String) groovyScriptFile.evaluate(input);
    }

    private File findGroovyFile(final RulStructureType structureType) {
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE);
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

    public RulStructureType getStructureTypeByCode(final String structureTypeCode) {
        RulStructureType structureType = structureTypeRepository.findByCode(structureTypeCode);
        if (structureType == null) {
            throw new ObjectNotFoundException("Strukturovaný typ neexistuje: " + structureTypeCode, BaseCode.ID_NOT_EXIST).setId(structureTypeCode);
        }
        return structureType;
    }

    public ArrStructureData getStructureDataById(final Integer structureDataId) {
        ArrStructureData structureData = structureDataRepository.findOne(structureDataId);
        if (structureData == null) {
            throw new ObjectNotFoundException("Strukturovaná data neexistují: " + structureDataId, BaseCode.ID_NOT_EXIST).setId(structureDataId);
        }
        return structureData;
    }

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

    public ArrStructureData confirmStructureData(final ArrStructureData structureData) {
        if (structureData.getDeleteChange() != null) {
            throw new BusinessException("Nelze potvrdit smazaná strukturovaná data", BaseCode.INVALID_STATE);
        }
        structureData.setState(ArrStructureData.State.OK);
        revalidateStructureData(structureData);
        return structureDataRepository.save(structureData);
    }

    private void revalidateStructureData(final ArrStructureData structureData) {
        if (ArrStructureData.State.TEMP.equals(structureData.getState())) {
            return;
        }

        structureData.setValue(null);
    }

    public List<RulItemTypeExt> getStructureItemTypes(final RulStructureType structureType) {
        List<RulItemTypeExt> rulDescItemTypeExtList = ruleService.getAllItemTypes(structureType.getRuleSet());
        return rulesExecutor.executeStructureItemTypesRules(structureType, rulDescItemTypeExtList);
    }

}
