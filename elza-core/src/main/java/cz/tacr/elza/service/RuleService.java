package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.OutputCode;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.controller.factory.ExtendedObjectsFactory;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.LockVersionChangeException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.utils.ObjectListIterator;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Servisní třída pro pravidla.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 13.01.2016
 */
@Service
public class RuleService {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PackageService packageService;

    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private NodeConformityRepository nodeConformityInfoRepository;
    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorsRepository;
    @Autowired
    private ExtendedObjectsFactory extendedObjectsFactory;
    @Autowired
    private ItemSpecRepository itemSpecRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ArrDescItemsPostValidator descItemsPostValidator;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;
    @Autowired
    private OutputTypeRepository outputTypeRepository;

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    /**
     * Provede validaci atributů vybraného uzlu a nastaví jejich validační hodnoty.
     *
     * @param faLevelId   id uzlu
     * @param fundVersionId id verze
     * @return stav validovaného uzlu
     */
    public ArrNodeConformityExt setConformityInfo(final Integer faLevelId, final Integer fundVersionId) {
        Assert.notNull(faLevelId, "Musí být vyplněn identifikátor levelu");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrLevel level = levelRepository.findOne(faLevelId);
        Integer nodeId = level.getNode().getNodeId();

        ArrNode nodeBeforeValidation = nodeRepository.getOneCheckExist(nodeId);
        Integer nodeVersionBeforeValidation = nodeBeforeValidation.getVersion();

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (!arrangementService.validLevelInVersion(level, version)) {
            throw new SystemException("Level s id " + faLevelId + " nespadá do verze s id " + fundVersionId);
        }

        List<DataValidationResult> validationResults = rulesExecutor.executeDescItemValidationRules(level, version);
        List<DataValidationResult> validationResultsBasic = descItemsPostValidator.postValidateNodeDescItems(level, version);

        Iterator<DataValidationResult> iterator = validationResultsBasic.iterator();
        while(iterator.hasNext()) {
            DataValidationResult validationResult = iterator.next();
            if (validationResult.getPolicyTypeCode() == null) {
                logger.error("Validační výsledek nemá vyplněný kód typu kontroly, proto nebude použit. " + validationResult);
                iterator.remove();
            }
        }

        validationResults.addAll(validationResultsBasic);

        ArrNodeConformityExt result = updateNodeConformityInfo(level, version, validationResults);

        entityManager.detach(nodeBeforeValidation);
        ArrNode nodeAfterValidation = nodeRepository.getOneCheckExist(nodeId);
        Integer nodeVersionAfterValidation = nodeAfterValidation.getVersion();

        if (!nodeVersionBeforeValidation.equals(nodeVersionAfterValidation)) {
            throw new LockVersionChangeException("Behem validace doslo ke zmene verze uzlu " + nodeId);
        }

        return result;
    }

    /**
     * Načtení seznamu všech šablon, seřazeného podle názvu.
     * @return seznam šablon
     */
    public List<RulTemplate> getTemplates(final String outputTypeCode) {
        if (outputTypeCode == null) {
            return templateRepository.findAll(new Sort(Sort.Direction.ASC, RulTemplate.NAME));
        }
        RulOutputType outputType = outputTypeRepository.findByCode(outputTypeCode);
        Assert.notNull(outputType, "Typ outputu s kodem '" + outputTypeCode + "' nebyl nalezen");

        return templateRepository.findNotDeletedByOutputType(outputType, new Sort(Sort.Direction.ASC, RulTemplate.NAME));
    }


    /**
     * Provede uložení stavu pro daný uzel podle výsledku validace.
     *
     * @param level             validaovaný uzel
     * @param version           verze, do které spadá uzel
     * @param validationResults seznam validačních chyb
     */
    private ArrNodeConformityExt updateNodeConformityInfo(final ArrLevel level,
                                                          final ArrFundVersion version,
                                                          final List<DataValidationResult> validationResults) {

        ArrNodeConformity conformityInfo = nodeConformityInfoRepository
                .findByNodeAndFundVersion(level.getNode(), version);

        if (conformityInfo != null && conformityInfo.getState().equals(ArrNodeConformity.State.OK)) {
            conformityInfo.setDate(new Date());
        } else {
            if (conformityInfo != null) {
                deleteConformityInfo(Arrays.asList(conformityInfo));
            }
            conformityInfo = new ArrNodeConformity();
            conformityInfo.setNode(level.getNode());
            conformityInfo.setFundVersion(version);
            conformityInfo.setDate(new Date());
        }


        if (validationResults.isEmpty()) {
            conformityInfo.setState(ArrNodeConformity.State.OK);
            nodeConformityInfoRepository.save(conformityInfo);
        } else {
            conformityInfo.setState(ArrNodeConformity.State.ERR);
            nodeConformityInfoRepository.save(conformityInfo);

            for (DataValidationResult validationResult : validationResults) {
                switch (validationResult.getResultType()) {
                    case MISSING:
                        ArrNodeConformityMissing missing = new ArrNodeConformityMissing();
                        missing.setNodeConformity(conformityInfo);
                        missing.setItemType(validationResult.getType());
                        missing.setItemSpec(validationResult.getSpec());
                        missing.setDescription(validationResult.getMessage());
                        missing.setPolicyType(validationResult.getPolicyType());
                        nodeConformityMissingRepository.save(missing);
                        break;
                    case ERROR:
                        ArrNodeConformityError error = new ArrNodeConformityError();
                        error.setNodeConformity(conformityInfo);
                        error.setDescItem(validationResult.getDescItem());
                        error.setDescription(validationResult.getMessage());
                        error.setPolicyType(validationResult.getPolicyType());
                        nodeConformityErrorsRepository.save(error);
                        break;
                }
            }

        }

        return extendedObjectsFactory.createNodeConformityInfoExt(conformityInfo, true);
    }

    /**
     * Získání stavu JP pro seznam JP.
     *
     * @param nodeIds   seznam id nodů, od kterých se získat stav
     * @param version   verze archivní pomůcky
     * @return  stavy JP
     */
    public Map<Integer, ArrNodeConformityExt> getNodeConformityInfoForNodes(final Collection<Integer> nodeIds,
                                                                           final ArrFundVersion version) {
        Map<Integer, ArrNodeConformityExt> result = new HashMap<>();

        if (nodeIds.size() == 0) {
            return result;
        }

        ObjectListIterator<Integer> iteratorNodeIds = new ObjectListIterator<>(nodeIds);

        while (iteratorNodeIds.hasNext()) {
            List<Integer> partNodeIds = iteratorNodeIds.next();

            List<ArrNodeConformity> conformityInfos = nodeConformityInfoRepository
                    .findByNodeIdsAndFundVersion(partNodeIds, version);

            ArrayList<Integer> conformityInfoIds = conformityInfos.stream().map(ArrNodeConformity::getNodeConformityId)
                    .collect(Collectors.toCollection(ArrayList::new));

            ObjectListIterator<Integer> iteratorConformityIds = new ObjectListIterator<>(conformityInfoIds);

            Map<Integer, List<ArrNodeConformityMissing>> missings = new HashMap<>();
            Map<Integer, List<ArrNodeConformityError>> errors = new HashMap<>();

            while (iteratorConformityIds.hasNext()) {
                List<Integer> partIds = iteratorConformityIds.next();

                List<ArrNodeConformityMissing> partMissings = nodeConformityMissingRepository
                        .findByConformityIds(partIds);

                for (ArrNodeConformityMissing partMissing : partMissings) {
                    Integer conformityId = partMissing.getNodeConformity().getNodeConformityId();
                    List<ArrNodeConformityMissing> missingList = missings.get(conformityId);

                    if (missingList == null) {
                        missingList = new ArrayList<>();
                        missings.put(conformityId, missingList);
                    }

                    missingList.add(partMissing);
                }

                List<ArrNodeConformityError> partErrors = nodeConformityErrorsRepository.findByConformityIds(partIds);
                for (ArrNodeConformityError partError : partErrors) {
                    Integer conformityId = partError.getNodeConformity().getNodeConformityId();

                    List<ArrNodeConformityError> errorList = errors.get(conformityId);

                    if (errorList == null) {
                        errorList = new ArrayList<>();
                        errors.put(conformityId, errorList);
                    }

                    errorList.add(partError);
                }
            }

            for (ArrNodeConformity conformityInfo : conformityInfos) {

                ArrNodeConformityExt conformity = new ArrNodeConformityExt();
                BeanUtils.copyProperties(conformityInfo, conformity);

                conformity.setErrorList(errors.get(conformity.getNodeConformityId()));
                conformity.setMissingList(missings.get(conformity.getNodeConformityId()));

                result.put(conformityInfo.getNode().getNodeId(), conformity);
            }

        }

        return result;
    }

    /**
     * Provede úpravů (smazání) stavů uzlů podle pravidel.
     *
     * @param fundVersionId       verze nodů
     * @param nodeIds           seznam id nodů, od kterých se má prohledávat
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    public Set<RelatedNodeDirection> conformityInfo(final Integer fundVersionId,
                                                    final Collection<Integer> nodeIds,
                                                    final NodeTypeOperation nodeTypeOperation,
                                                    final List<ArrDescItem> createDescItems,
                                                    final List<ArrDescItem> updateDescItems,
                                                    final List<ArrDescItem> deleteDescItems) {

        Set<RelatedNodeDirection> impactOnConformityInfo = getImpactOnConformityInfo(fundVersionId, nodeTypeOperation,
                createDescItems, updateDescItems, deleteDescItems);

        deleteConformityInfo(fundVersionId, nodeIds, impactOnConformityInfo);

        return impactOnConformityInfo;
    }

    /**
     * Provede vytvoření stavů uzlů podle pravidel u nové verze AP.
     *
     * @param fundVersion verze AP
     */
    public void conformityInfoAll(final ArrFundVersion fundVersion) {

        ArrNode rootNode = fundVersion.getRootNode();

        List<ArrNode> nodes = nodeRepository
                .findNodesByDirection(rootNode, fundVersion, RelatedNodeDirection.ALL);

        nodes.add(rootNode);

        if (!nodes.isEmpty()) {
            updateConformityInfoService.updateInfoForNodesAfterCommit(nodes, fundVersion);
        }
    }

    /**
     * Zjistí podle pravidel dopad na změnu stavů uzlů.
     *
     * @param fundVersionId       verze nodů
     * @param nodeTypeOperation typ operace
     * @param createDescItems   hodnoty atributů k vytvoření
     * @param updateDescItems   hodnoty atributů k upravení
     * @param deleteDescItems   hodnoty atributů ke smazání
     * @return seznam dopadů
     */
    private Set<RelatedNodeDirection> getImpactOnConformityInfo(final Integer fundVersionId,
                                                                final NodeTypeOperation nodeTypeOperation,
                                                                final List<ArrDescItem> createDescItems,
                                                                final List<ArrDescItem> updateDescItems,
                                                                final List<ArrDescItem> deleteDescItems) {

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId, ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
        }

        return rulesExecutor
                .executeImpactOfChangesLevelStateRules(createDescItems, updateDescItems, deleteDescItems,
                        nodeTypeOperation, version);
    }

    /**
     * Pro vybrané nody s danou verzí smaže všechny stavy v daných směrech od nodů.
     *
     * @param fundVersionId      verze nodů
     * @param nodeIds          seznam id nodů, od kterých se má prohledávat
     * @param deleteDirections směry prohledávání (null pokud se mají smazat stavy zadaných nodů .
     */
    private void deleteConformityInfo(final Integer fundVersionId,
                                      final Collection<Integer> nodeIds,
                                      final Collection<RelatedNodeDirection> deleteDirections) {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notEmpty(nodeIds, "Musí být vyplněna alespoň jedna JP");

        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        Set<ArrNode> deleteNodes = new HashSet<>();

        if (CollectionUtils.isEmpty(deleteDirections)) {
            deleteNodes.addAll(nodes);
        } else {

            for (RelatedNodeDirection deleteDirection : deleteDirections) {
                for (ArrNode node : nodes) {
                    deleteNodes.addAll(nodeRepository.findNodesByDirection(node, version, deleteDirection));
                }
            }
        }


        if (!deleteNodes.isEmpty()) {
            List<ArrNodeConformity> deleteInfos = nodeConformityInfoRepository
                    .findByNodesAndFundVersion(deleteNodes, version);

            deleteConformityInfo(deleteInfos);
            updateConformityInfoService.updateInfoForNodesAfterCommit(deleteNodes, version);
        }
    }

    /**
     * Smaže všechny vybrané stavy.
     *
     * @param infos stavy ke smazání
     */
    private void deleteConformityInfo(final Collection<ArrNodeConformity> infos) {

        if (CollectionUtils.isNotEmpty(infos)) {
            List<ArrNodeConformityMissing> missing = nodeConformityMissingRepository
                    .findByNodeConformityInfos(infos);
            if (CollectionUtils.isNotEmpty(missing)) {
                nodeConformityMissingRepository.delete(missing);
            }

            List<ArrNodeConformityError> errors = nodeConformityErrorsRepository.findByNodeConformityInfos(infos);
            if (CollectionUtils.isNotEmpty(errors)) {
                nodeConformityErrorsRepository.delete(errors);
            }

            nodeConformityInfoRepository.delete(infos);
        }
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     * Používá výchozí strategie
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @param nodeId              identifikátor uzlu
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulItemTypeExt> getDescriptionItemTypes(final Integer fundVersionId,
                                                        final Integer nodeId) {
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        if (version == null) {
            throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId, ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
        }

        ArrNode node = nodeRepository.findOne(nodeId);

        if (node == null) {
            throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + nodeId, ArrangementCode.NODE_NOT_FOUND).set("id", nodeId);
        }

        return getDescriptionItemTypes(version, node);
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     *
     * @param version    verze archivní pomůcky
     * @param node       uzel
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulItemTypeExt> getDescriptionItemTypes(final ArrFundVersion version,
                                                        final ArrNode node) {

        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootNode(),
                version.getLockChange());

        List<RulItemTypeExt> rulDescItemTypeExtList = getAllDescriptionItemTypes(version.getRuleSet());

        return rulesExecutor.executeDescItemTypesRules(level, rulDescItemTypeExtList, version);
    }

    /**
     * Získání všech hodnot typů atributů se specifikacemi.
     *
     * @return typy hodnot atributů
     */
    public List<RulItemTypeExt> getAllDescriptionItemTypes() {
        return getAllDescriptionItemTypes(null);
    }

    /**
     * Vrací typy atributů podle pravidla.
     *
     * @param ruleSet pravidla
     * @return seznam typů
     */
    public List<String> getItemTypeCodesByRuleSet(final RulRuleSet ruleSet) {
        return itemTypeRepository.findByRuleSet(ruleSet).stream()
                .map(RulItemType::getCode)
                .collect(Collectors.toList());
    }

    /**
     * Získání typů atributů se specifikacemi podle balíčku.
     *
     * @param ruleSet balíček, podle kterého filtrujeme, pokud je null, vezmou se všechny
     * @return typy hodnot atributů
     */
    public List<RulItemTypeExt> getAllDescriptionItemTypes(@Nullable final RulRuleSet ruleSet) {

        List<RulItemType> itemTypeList;

        if (ruleSet == null) {
            itemTypeList = itemTypeRepository.findAll();
        } else {
            itemTypeList = itemTypeRepository.findByRuleSet(ruleSet);
        }

        List<RulItemTypeExt> rulDescItemTypeExtList = createExt(itemTypeList);

        RulPolicyType policyType = policyService.getPolicyTypes().stream().findFirst().get();

        // projde všechny typy atributů
        for (RulItemTypeExt rulDescItemTypeExt : rulDescItemTypeExtList) {

            rulDescItemTypeExt.setType(RulItemType.Type.IMPOSSIBLE);
            rulDescItemTypeExt.setRepeatable(true);
            rulDescItemTypeExt.setCalculable(false);
            rulDescItemTypeExt.setCalculableState(false);
            rulDescItemTypeExt.setIndefinable(false);
            rulDescItemTypeExt.setPolicyTypeCode(policyType.getCode());

            List<RulItemSpecExt> itemSpecList = rulDescItemTypeExt.getRulItemSpecList();

            // projde všechny specifikace typů atributů
            for (RulItemSpecExt rulDescItemSpecExt : itemSpecList) {
                rulDescItemSpecExt.setType(RulItemSpec.Type.IMPOSSIBLE);
                rulDescItemSpecExt.setRepeatable(true);
                rulDescItemSpecExt.setPolicyTypeCode(policyType.getCode());
            }

            // seřadit dle viewOrder
            itemSpecList.sort((o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null || o1.getViewOrder() == null) {
                    return -1;
                }
                if (o2 == null || o2.getViewOrder() == null) {
                    return 1;
                }
                return o1.getViewOrder().compareTo(o2.getViewOrder());
            });
        }

        rulDescItemTypeExtList.sort((o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null || o1.getViewOrder() == null) {
                return -1;
            }
            if (o2 == null || o2.getViewOrder() == null) {
                return 1;
            }
            return o1.getViewOrder().compareTo(o2.getViewOrder());
        });

        return rulDescItemTypeExtList;
    }

    /**
     * Načtení seznamu kódů atributů - implicitní atributy pro zobrazení tabulky hromadných akcí, seznam je seřazený podle
     * pořadí, které jedefinováno u atributů.
     * @param ruleSet pravidla
     * @return seznam kódů
     */
    public List<SettingGridView.ItemType> getGridView(final RulRuleSet ruleSet) {

        // načtený globální oblíbených
        List<UISettings> gridViews = settingsService.getGlobalSettings(UISettings.SettingsType.GRID_VIEW, UISettings.EntityType.RULE);

        for (UISettings gridView : gridViews) {
            if (gridView.getRulPackage().getPackageId().equals(ruleSet.getPackage().getPackageId())) {
                SettingGridView view = (SettingGridView) packageService.convertSetting(gridView, ruleSet);
                if (CollectionUtils.isNotEmpty(view.getItemTypes())) {
                    return view.getItemTypes();
                }
            }
        }

        return null;
    }

    /**
     * Vytvoření seznamu rozšířených typů hodnot atributů se specifikacemi podle seznamu typů hodnot atributů.
     *
     * @param itemTypeList seznam typů hodnot atributů
     * @return seznam typů hodnot atributů se specifikacemi
     */
    private List<RulItemTypeExt> createExt(final List<RulItemType> itemTypeList) {
        if (itemTypeList.isEmpty()) {
            return new LinkedList<>();
        }

        List<RulItemSpec> listDescItem = itemSpecRepository.findByItemTypeIds(itemTypeList);
        Map<Integer, List<RulItemSpec>> itemSpecMap =
                ElzaTools.createGroupMap(listDescItem, p -> p.getItemType().getItemTypeId());

        List<RulItemTypeExt> result = new LinkedList<>();
        for (RulItemType rulDescItemType : itemTypeList) {
            RulItemTypeExt descItemTypeExt = new RulItemTypeExt();
            BeanUtils.copyProperties(rulDescItemType, descItemTypeExt, "columnsDefinition");
            descItemTypeExt.setColumnsDefinition(rulDescItemType.getColumnsDefinition());
            List<RulItemSpec> itemSpecList =
                    itemSpecMap.get(rulDescItemType.getItemTypeId());
            if (itemSpecList != null) {
                for (RulItemSpec rulDescItemSpec : itemSpecList) {
                    RulItemSpecExt descItemSpecExt = new RulItemSpecExt();
                    BeanUtils.copyProperties(rulDescItemSpec, descItemSpecExt);
                    descItemTypeExt.getRulItemSpecList().add(descItemSpecExt);
                }
            }
            result.add(descItemTypeExt);
        }

        return result;
    }

    /**
     * Získání rozšířených typů hodnot atributů se specifikacemi.
     * Používá výchozí strategie
     *
     * @param outputDefinitionId identifikátor výstupu
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulItemTypeExt> getOutputItemTypes(final Integer outputDefinitionId) {
        Assert.notNull(outputDefinitionId, "Identifikátor definice výstupu musí být vyplněn");

        ArrOutputDefinition outputDefinition = outputService.findOutputDefinition(outputDefinitionId);

        if (outputDefinition == null) {
            throw new ObjectNotFoundException("Nebyl nalezen výstup s ID=" + outputDefinitionId, OutputCode.OUTPUT_NOT_EXISTS).set("id", outputDefinitionId);
        }

        return getOutputItemTypes(outputDefinition);
    }

    /**
     * Vrací typy atributu.
     *
     * @param outputDefinition výstup
     * @return seznam typů
     */
    public List<RulItemTypeExt> getOutputItemTypes(final ArrOutputDefinition outputDefinition) {
        List<RulItemTypeExt> rulDescItemTypeExtList = getAllDescriptionItemTypes(outputDefinition.getOutputType().getRuleSet());

        List<RulItemTypeAction> itemTypeActions = itemTypeActionRepository.findAll();
        Map<Integer, RulItemType> itemTypeMap = new HashMap<>();

        for (RulItemTypeAction itemTypeAction : itemTypeActions) {
            itemTypeMap.put(itemTypeAction.getItemType().getItemTypeId(), itemTypeAction.getItemType());
        }

        List<ArrItemSettings> settings = itemSettingsRepository.findByOutputDefinition(outputDefinition);
        Map<Integer, Boolean> settingsMap = new HashMap<>();

        for (ArrItemSettings setting : settings) {
            settingsMap.put(setting.getItemType().getItemTypeId(), setting.getBlockActionResult());
        }

        for (RulItemTypeExt rulItemTypeExt : rulDescItemTypeExtList) {
            if (itemTypeMap.get(rulItemTypeExt.getItemTypeId()) != null) {
                rulItemTypeExt.setCalculable(true);
                rulItemTypeExt.setIndefinable(false);
                Boolean state = settingsMap.get(rulItemTypeExt.getItemTypeId());
                rulItemTypeExt.setCalculableState(state == null ? false : state);
            }
        }

        return rulesExecutor.executeOutputItemTypesRules(outputDefinition, rulDescItemTypeExtList);
    }
}
