package cz.tacr.elza.service;

import com.google.common.collect.Lists;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.factory.ExtendedObjectsFactory;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.rules.ItemTypeExtBuilder;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.eventnotification.events.EventNodeIdVersionInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Servisní třída pro pravidla.
 *
 */
@Service
public class RuleService {

    @Autowired
    private EntityManager entityManager;
    @Autowired
	private StaticDataService staticDataService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private SettingsService settingsService;
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
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ArrDescItemsPostValidator descItemsPostValidator;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;
    @Autowired
    private OutputTypeRepository outputTypeRepository;

    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private ArrangementExtensionRepository arrangementExtensionRepository;

    @Autowired
    private ExtensionRuleRepository extensionRuleRepository;

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    public synchronized ArrNodeConformityExt setConformityInfo(final Integer faLevelId, final Integer fundVersionId, final Long asyncRequestId) {
        ArrLevel level = levelRepository.findOne(faLevelId);
        if(canUpdateConformity(asyncRequestId, level.getNode().getNodeId())) {
            return setConformityInfo(faLevelId,fundVersionId);
        } else {
            return null;
        }
    }

    public boolean canUpdateConformity(final Long asyncRequestId, final Integer nodeId) {
        return asyncRequestService.canUpdateConformity(asyncRequestId, nodeId);
    }

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

        if (!arrangementInternalService.validLevelInVersion(level, version)) {
            throw new SystemException("Level s id " + faLevelId + " nespadá do verze s id " + fundVersionId);
        }

        List<DataValidationResult> validationResults = rulesExecutor.executeDescItemValidationRules(level, version);
        List<DataValidationResult> validationResultsBasic = descItemsPostValidator.postValidateNodeDescItems(level, version);

        Iterator<DataValidationResult> iterator = validationResultsBasic.iterator();
        while(iterator.hasNext()) {
            DataValidationResult validationResult = iterator.next();
            if (validationResult.getPolicyTypeCode() == null) {
                throw new SystemException("Validation result without policy type code", BaseCode.INVALID_STATE)
                        .set("message", validationResult.getMessage());
            }
        }

        validationResults.addAll(validationResultsBasic);
        ArrNodeConformityExt result = updateNodeConformityInfo(level, version, validationResults);

        entityManager.detach(nodeBeforeValidation);
        ArrNode nodeAfterValidation = nodeRepository.getOneCheckExist(nodeId);
        Integer nodeVersionAfterValidation = nodeAfterValidation.getVersion();

        if (!nodeVersionBeforeValidation.equals(nodeVersionAfterValidation)) {
            logger.info("Během validace došlo ke změně verze uzlu " + nodeId);
            //throw new LockVersionChangeException("Behem validace doslo ke zmene verze uzlu " + nodeId);
        }

        return result;
    }

    /**
     * Načtení seznamu všech šablon, seřazeného podle názvu.
     * @return seznam šablon
     */
    public List<RulTemplate> getTemplates(final String outputTypeCode) {
        if (outputTypeCode == null) {
            return templateRepository.findAll(new Sort(Sort.Direction.ASC, RulTemplate.FIELD_NAME));
        }
        RulOutputType outputType = outputTypeRepository.findByCode(outputTypeCode);
        Assert.notNull(outputType, "Typ outputu s kodem '" + outputTypeCode + "' nebyl nalezen");

        return templateRepository.findNotDeletedByOutputType(outputType, new Sort(Sort.Direction.ASC, RulTemplate.FIELD_NAME));
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
                // policy type has to be set
                Validate.notNull(validationResult.getPolicyType());

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
                default:
                    throw new IllegalStateException();
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
    // TODO: Funkce nedava smysl pokud kombinuje vice uzlu a vice item z techto uzlu
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

    public Set<RelatedNodeDirection> conformityInfo(final Integer fundVersionId,
                                                    final Collection<Integer> nodeIds,
                                                    final NodeTypeOperation nodeTypeOperation,
                                                    final List<ArrDescItem> createDescItems,
                                                    final List<ArrDescItem> updateDescItems,
                                                    final List<ArrDescItem> deleteDescItems,
                                                    final Integer validationPriority) {

        Set<RelatedNodeDirection> impactOnConformityInfo = getImpactOnConformityInfo(fundVersionId, nodeTypeOperation,
                createDescItems, updateDescItems, deleteDescItems);

        deleteConformityInfo(fundVersionId, nodeIds, impactOnConformityInfo, validationPriority);

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
        logger.info("Conformity Info All");
        if (!nodes.isEmpty()) {
            asyncRequestService.enqueue(fundVersion,nodes, AsyncTypeEnum.NODE, null);
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
        deleteConformityInfo(fundVersionId,nodeIds,deleteDirections,null);
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
                                      final Collection<RelatedNodeDirection> deleteDirections,
                                      final Integer validationPriority) {
        Validate.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Validate.notEmpty(nodeIds, "Musí být vyplněna alespoň jedna JP");

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
            asyncRequestService.enqueue(version,deleteNodes.stream().collect(Collectors.toList()), AsyncTypeEnum.NODE,validationPriority);
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
     *
     * @param version    verze archivní pomůcky
     * @param node       uzel
     * @return seznam typů hodnot atributů se specifikacemi
     */
    public List<RulItemTypeExt> getDescriptionItemTypes(final ArrFundVersion version,
                                                        final ArrNode node) {

        ArrLevel level = levelRepository.findByNode(node, version.getLockChange());

		List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();

        return rulesExecutor.executeDescItemTypesRules(level, rulDescItemTypeExtList, version);
    }

    /**
     * Získání všech hodnot typů atributů se specifikacemi.
     *
     * @return typy hodnot atributů
     */
	@Transactional
    public List<RulItemTypeExt> getAllDescriptionItemTypes() {
		StaticDataProvider sdp = staticDataService.getData();

		ItemTypeExtBuilder builder = new ItemTypeExtBuilder();
        builder.add(sdp.getItemTypes());

		return builder.getResult();
    }

    /**
	 * Získání typů atributů se specifikacemi pro pravidla
	 *
	 * @return typy hodnot atributů
	 */
	private List<RulItemTypeExt> getRulesetDescriptionItemTypes() {
		StaticDataProvider sdp = staticDataService.getData();

		ItemTypeExtBuilder builder = new ItemTypeExtBuilder();
        builder.add(sdp.getItemTypes());

		return builder.getResult();
	}

    /**
     * Načtení seznamu kódů atributů - implicitní atributy pro zobrazení tabulky hromadných akcí, seznam je seřazený podle
     * pořadí, které jedefinováno u atributů.
     * @return seznam kódů
     */
    public List<SettingGridView.ItemType> getGridView() {

        // načtený globální oblíbených
        List<UISettings> gridViews = settingsService.getGlobalSettings(UISettings.SettingsType.GRID_VIEW.toString(),
                                                                       null);

        for (UISettings gridView : gridViews) {
            SettingGridView view = SettingGridView.newInstance(gridView);
            if (CollectionUtils.isNotEmpty(view.getItemTypes())) {
                return view.getItemTypes();
            }
        }

        return null;
    }

    /**
     * Vrací typy atributu.
     *
     * @param output výstup
     * @return seznam typů
     */
    public List<RulItemTypeExt> getOutputItemTypes(final ArrOutput output) {
        RulOutputType outputType = output.getOutputType();
        List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();

        List<RulItemTypeAction> itemTypeActions = itemTypeActionRepository.findAll();
        Map<Integer, RulItemType> itemTypeMap = new HashMap<>();

        for (RulItemTypeAction itemTypeAction : itemTypeActions) {
            itemTypeMap.put(itemTypeAction.getItemType().getItemTypeId(), itemTypeAction.getItemType());
        }

        List<ArrItemSettings> settings = itemSettingsRepository.findByOutput(output);
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

        // check if rule exists
        RulComponent component = outputType.getComponent();
        if (component != null) {
            return rulesExecutor.executeOutputItemTypesRules(output, rulDescItemTypeExtList);
        } else {
            // return item types without change if rules do not exists
            return rulDescItemTypeExtList;
        }
    }

    /**
     * Vytvoření vazby mezi JP a definicí řídících pravidel.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeExtension  vazba
     * @return  vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrNodeExtension createNodeExtension(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                                final Integer nodeId,
                                                final ArrNodeExtension nodeExtension) {
        Assert.notNull(nodeExtension, "Přirazení musí být vyplněno");
        Assert.isNull(nodeExtension.getNodeExtensionId(), "Identifikátor přiřazení nesmí být vyplěn");

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.ADD_NODE_EXTENSION, node);

        node.setVersion(nodeExtension.getNode().getVersion());
        saveNode(node, change);

        validateNodeExtension(nodeExtension, versionId);

        nodeExtension.setNode(node);
        nodeExtension.setCreateChange(change);
        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_EXTENSION_CHANGE, versionId, nodeExtension.getNode().getNodeId(), nodeExtension.getNode().getVersion()));

        nodeExtensionRepository.saveAndFlush(nodeExtension);
        arrangementCacheService.createNodeExtension(nodeId, nodeExtension);

        deleteConformityInfo(versionId, Collections.singleton(nodeId), Collections.singleton(RelatedNodeDirection.DESCENDANTS));

        return nodeExtension;
    }

    /**
     * Odstranění vazby mezi nodem a definicí řídících pravidel.
     *
     * @param versionId     identifikátor verze AP
     * @param nodeId        identifikátor JP
     * @param nodeExtension  vazba
     * @return vazba
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrNodeExtension deleteNodeExtension(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                                final Integer nodeId,
                                                final ArrNodeExtension nodeExtension) {
        Assert.notNull(nodeExtension, "Rejstříkové heslo musí být vyplněno");
        Assert.notNull(nodeExtension.getNodeExtensionId(), "Identifikátor musí být vyplněn");

        ArrNodeExtension nodeExtensionDB = nodeExtensionRepository.findOne(nodeExtension.getNodeExtensionId());

        ArrNode node = nodeRepository.findOne(nodeId);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_NODE_EXTENSION, node);

        node.setVersion(nodeExtension.getNode().getVersion());
        saveNode(node, change);

        validateNodeExtension(nodeExtensionDB, versionId);

        nodeExtensionDB.setDeleteChange(change);

        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_EXTENSION_CHANGE, versionId, node.getNodeId(), node.getVersion()));

        nodeExtensionRepository.save(nodeExtensionDB);
        arrangementCacheService.deleteNodeExtension(nodeId, nodeExtensionDB.getNodeExtensionId());

        deleteConformityInfo(versionId, Collections.singleton(nodeId), Collections.singleton(RelatedNodeDirection.DESCENDANTS));

        return nodeExtensionDB;
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node   uzel
     * @param change
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node, final ArrChange change) {
        node.setLastUpdate(change.getChangeDate().toLocalDateTime());
        nodeRepository.save(node);
        nodeRepository.flush();
        return node;
    }

    /**
     * Validuje entitu před uložením.
     *
     * @param nodeExtension entita
     * @param fundVersionId
     */
    private void validateNodeExtension(final ArrNodeExtension nodeExtension, final Integer fundVersionId) {
        if (nodeExtension.getDeleteChange() != null) {
            throw new IllegalStateException("Nelze vytvářet či modifikovat změnu," +
                    " která již byla smazána (má delete change).");
        }

        if (nodeExtension.getNode() == null) {
            throw new IllegalArgumentException("Není vyplněna JP");
        }
        if (nodeExtension.getArrangementExtension() == null) {
            throw new IllegalArgumentException("Nejsou definovány rozšířené pravidla");
        }
        List<RulArrangementExtension> arrangementExtensions = findArrangementExtensionsByFundVersionId(fundVersionId);
        if (!arrangementExtensions.contains(nodeExtension.getArrangementExtension())) {
            throw new IllegalArgumentException("Řídící pravidla nejsou pro pravidla AS");
        }
    }

    /**
     * Vyhledá možné definice rozšíření pro řídící pravidla podle verze AS.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return nalezené definice
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public List<RulArrangementExtension> findArrangementExtensionsByFundVersionId(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(fundVersionId, "Identifikátor verze AS musí být vyplněn");
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
        RulRuleSet ruleSet = version.getRuleSet();
        return arrangementExtensionRepository.findByRuleSet(ruleSet);
    }

    /**
     * Vyhledá nastavené definice rozšíření pro řídící pravidla pro JP z verze AS.
     *
     * @param fundVersionId  identifikátor verze archivní pomůcky
     * @param nodeId identifikátor JP
     * @return nalezené definice
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public List<RulArrangementExtension> findArrangementExtensionsByNodeId(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                                           final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(fundVersionId, "Identifikátor verze AS musí být vyplněn");
        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
        ArrNode node = nodeRepository.getOneCheckExist(nodeId);
        if (!node.getFundId().equals(version.getFundId())) {
            throw new IllegalArgumentException("JP nespadá pod verzi AS");
        }
        return arrangementExtensionRepository.findByNode(node);
    }

    /**
     * Vyhledá všechny použité/zděděné definice rozšíření pro řídící pravidla pro JP. Seřazené podle názvu.
     *
     * @param nodeId identifikátor JP
     * @return nalezené definice
     */
    public List<RulArrangementExtension> findAllArrangementExtensionsByNodeId(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        return arrangementExtensionRepository.findByNodeIdToRoot(nodeId);
    }

    public RulItemType getItemTypeById(final Integer itemTypeId) {
        RulItemType itemType = itemTypeRepository.findOne(itemTypeId);
        if (itemType == null) {
            throw new ObjectNotFoundException("Neexistuje typ: " + itemTypeId, BaseCode.ID_NOT_EXIST).setId(itemTypeId);
        }
        return itemType;
    }

    public List<RulExtensionRule> findExtensionRuleByNode(final ArrNode node, final RulExtensionRule.RuleType attributeTypes) {
        Assert.notNull(node, "JP musí být vyplněna");

        List<ArrNodeExtension> nodeExtensions = nodeExtensionRepository.findAllByNodeIdFromRoot(node.getNodeId());

        LinkedHashSet<RulArrangementExtension> arrangementExtensions = new LinkedHashSet<>();
        for (ArrNodeExtension nodeExtension : nodeExtensions) {
            arrangementExtensions.add(nodeExtension.getArrangementExtension());
        }
        List<RulArrangementExtension> arrangementExtensionsFinal = new ArrayList<>(arrangementExtensions);

        return extensionRuleRepository.findExtensionRules(arrangementExtensionsFinal, attributeTypes);
    }

    /**
     * Nastaví nodu konkrétní extensions. Synchronizuje dodaný set. Dovytvoří potřebné, smaže nepotřebné.
     *
     * @param versionId Fund version Id
     * @param nodeId Node Id
     * @param arrExtensionIds Set Id nastavovaných ArrExtensions
     */
    public void syncNodeExtensions(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                   final Integer nodeId,
                                   final Set<Integer> arrExtensionIds) {

        Assert.notNull(arrExtensionIds, "Musí být předán seznam rozšíření");

        ArrNode node = nodeRepository.getOneCheckExist(nodeId);

        final Set<Integer> toDeleteIds = arrangementExtensionRepository.findByNode(node).stream().map(RulArrangementExtension::getArrangementExtensionId).collect(Collectors.toSet());
        final Set<Integer> toAddIds = new HashSet<>();
        arrExtensionIds.forEach(i -> {
            if (toDeleteIds.contains(i)) {
                toDeleteIds.remove(i);
            } else {
                toAddIds.add(i);
            }
        });

        if (toDeleteIds.isEmpty() && toAddIds.isEmpty()) {
            // Vše je v konzistentním stavu
            return;
        }

        // Změna pod kterou uvidíme nastavení
        final ArrChange change = arrangementInternalService.createChange(ArrChange.Type.SET_NODE_EXTENSION, node);

        // Uložení node pro zaznamenání change
        saveNode(node, change);

        List<ArrNodeExtension> toDelete = null;
        if (!toDeleteIds.isEmpty()) {
            // Seznam ke smazání
            toDelete = nodeExtensionRepository.findByArrExtensionIdsAndNodeNotDeleted(toDeleteIds, node);

            // Nastavení smazání
            toDelete.forEach(i -> i.setDeleteChange(change));

            toDelete = nodeExtensionRepository.save(toDelete);
        }

        List<ArrNodeExtension> toAdd = null;
        if (!toAddIds.isEmpty()) {
            // Seznam ArrExts k přidání
            final List<RulArrangementExtension> toAddExts = arrangementExtensionRepository.findAll(toAddIds);

            // Seznam platných rozšíření pro verzi fund
            final List<RulArrangementExtension> validArrExts = findArrangementExtensionsByFundVersionId(versionId);

            // Validace + příprava listu k uložení
            toAdd = toAddExts.stream().map(i -> {
                if (!validArrExts.contains(i)) {
                    throw new IllegalArgumentException("Řídící pravidla nejsou pro pravidla AS");
                }
                final ArrNodeExtension ext = new ArrNodeExtension();
                ext.setCreateChange(change);
                ext.setNode(node);
                ext.setArrangementExtension(i);
                return ext;
            }).collect(Collectors.toList());

            toAdd = nodeExtensionRepository.save(toAdd);
        }

        nodeExtensionRepository.flush();

        eventNotificationService.publishEvent(new EventNodeIdVersionInVersion(EventType.FUND_EXTENSION_CHANGE, versionId, node.getNodeId(), node.getVersion()));

        if (toDelete != null) {
            toDelete.forEach(i -> arrangementCacheService.deleteNodeExtension(nodeId, i.getNodeExtensionId()));
        }

        if (toAdd != null) {
            toAdd.forEach(i -> arrangementCacheService.createNodeExtension(nodeId, i));
        }

        deleteConformityInfo(versionId, Collections.singleton(nodeId), Lists.newArrayList(RelatedNodeDirection.NODE, RelatedNodeDirection.DESCENDANTS));
    }

    /**
     * Získání seznamu typů atributů podle strukt. typu a verze AS.
     *
     * @param structTypeId
     *            strukturovaný typ
     * @param fundVersion
     *            verze AS
     * @param structureItems
     *            seznam položek strukturovaného datového typu
     * @return seznam typu atributů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<RulItemTypeExt> getStructureItemTypes(final Integer structTypeId,
                                                      @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                      final List<ArrStructuredItem> structureItems) {
        return getStructureItemTypesInternal(structTypeId, fundVersion, structureItems);
    }

    /**
     * Získání seznamu typů atributů podle strukt. typu a verze AS - internal.
     *
     * @param structTypeId  id strukturovaného typu
     * @param fundVersion    verze AS
     * @param structureItems seznam položek strukturovaného datového typu
     * @return seznam typu atributů
     */
    public List<RulItemTypeExt> getStructureItemTypesInternal(final Integer structTypeId,
                                                              final ArrFundVersion fundVersion,
                                                              final List<ArrStructuredItem> structureItems) {
        List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();
        return rulesExecutor.executeStructureItemTypesRules(structTypeId, rulDescItemTypeExtList, fundVersion,
                                                            structureItems);
    }


    /**
     * Vrátí typy atributů ve stejném pořadí v jakém jsou předáná jejich id.
     *
     * @param ids id typů atribuů
     * @return typy atributů
     */
    public List<RulItemType> findItemTypesByIdsOrdered(List<Integer> ids) {
        Map<Integer, RulItemType> rulItemTypeMap = itemTypeRepository.findAll(ids).stream().
                collect(Collectors.toMap(RulItemType::getItemTypeId, Function.identity()));

        return ids.stream().
                map(id -> rulItemTypeMap.get(id)).
                collect(Collectors.toList());
    }

    public List<RulItemTypeExt> getFragmentItemTypesInternal(final RulStructuredType fragmentType, final List<ApItem> items) {
        List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();
        return rulesExecutor.executeFragmentItemTypesRules(fragmentType, rulDescItemTypeExtList, items);
    }

    public List<RulItemTypeExt> getApItemTypesInternal(final ApType type, final List<ApItem> items, final ApRule.RuleType ruleType) {
        List<RulItemTypeExt> rulDescItemTypeExtList = getRulesetDescriptionItemTypes();
        return rulesExecutor.executeApItemTypesRules(type, rulDescItemTypeExtList, items, ruleType);
    }
}
