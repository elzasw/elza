package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;

import cz.tacr.elza.api.ArrNodeConformity.State;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.ArrangementController.TreeNodeFulltext;
import cz.tacr.elza.controller.ArrangementController.VersionValidationItem;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaRegisterScope;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrVersionConformity;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FaRegisterScopeRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.VersionConformityRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVersion;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 12. 2015
 */
@Service
public class ArrangementService {

    private Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private RuleManager ruleManager;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private FindingAidRepository findingAidRepository;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private NodeConformityRepository nodeConformityInfoRepository;

    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorsRepository;

    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;

    @Autowired
    private VersionConformityRepository findingAidVersionConformityInfoRepository;

    @Autowired
    private BulkActionRunRepository faBulkActionRepository;

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    protected FaRegisterScopeRepository faRegisterScopeRepository;

    @Autowired
    private RegistryService registryService;



    public ArrFindingAid createFindingAid(String name, RulRuleSet ruleSet, RulArrangementType arrangementType,
                                          ArrChange change, String uuid) {
        ArrFindingAid findingAid = new ArrFindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName(name);

        findingAid = findingAidRepository.save(findingAid);

        eventNotificationService
                .publishEvent(EventFactory.createIdEvent(EventType.FINDING_AID_CREATE, findingAid.getFindingAidId()));

        //        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        ArrLevel rootNode = createLevel(change, null, uuid);
        createVersion(change, findingAid, arrangementType, ruleSet, rootNode);

        return findingAid;
    }

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas. Pro root
     * vytvoří atributy podle scénáře.
     *
     * @param name            název archivní pomůcky
     * @param arrangementType id typu výstupu
     * @param ruleSet         id pravidel podle kterých se vytváří popis
     * @return nová archivní pomůcka
     */
    public ArrFindingAid createFindingAidWithScenario(String name,
                                                      RulRuleSet ruleSet,
                                                      RulArrangementType arrangementType) {
        ArrChange change = createChange();

        ArrFindingAid findingAid = createFindingAid(name, ruleSet, arrangementType, change, null);

        List<RegScope> defaultScopes = registryService.findDefaultScopes();
        if (!defaultScopes.isEmpty()) {
            addScopeToFindingAid(findingAid, defaultScopes.get(0));
        }

        ArrFindingAidVersion version = findingAidVersionRepository
                .findByFindingAidIdAndLockChangeIsNull(findingAid.getFindingAidId());

        ArrLevel rootLevel = version.getRootLevel();

        // vyhledání scénářů
        List<ScenarioOfNewLevel> scenarioOfNewLevels = descriptionItemService.getDescriptionItemTypesForNewLevel(
                rootLevel, DirectionLevel.ROOT, version);

        // pokud existuje právě jeden, použijeme ho pro založení nových hodnot atributů
        if (scenarioOfNewLevels.size() == 1) {

            List<ArrDescItem> descItems = scenarioOfNewLevels.get(0).getDescItems();
            for (ArrDescItem descItem : descItems) {
                descItem.setNode(rootLevel.getNode());
                descriptionItemService.createDescriptionItem(descItem, rootLevel.getNode(), version, change);
            }
        } else if (scenarioOfNewLevels.size() > 1) {
            logger.error("Při založení AP bylo nalezeno více scénařů (" + scenarioOfNewLevels.size() + ")");
        }

        ruleService.conformityInfo(version.getFindingAidVersionId(), Arrays.asList(rootLevel.getNode().getNodeId()),
                NodeTypeOperation.CREATE_NODE, null, null, null);

        return findingAid;
    }


    public ArrFindingAidVersion createVersion(final ArrChange createChange,
                                              final ArrFindingAid findingAid,
                                              final RulArrangementType arrangementType,
                                              final RulRuleSet ruleSet,
                                              final ArrLevel rootNode) {
        ArrFindingAidVersion version = new ArrFindingAidVersion();
        version.setCreateChange(createChange);
        version.setArrangementType(arrangementType);
        version.setFindingAid(findingAid);
        version.setRuleSet(ruleSet);
        version.setRootLevel(rootNode);
        version.setLastChange(createChange);
        return findingAidVersionRepository.save(version);
    }

    public ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode, final String uuid) {
        ArrLevel level = new ArrLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode(uuid));
        return levelRepository.save(level);
    }

    public ArrLevel createLevel(final ArrChange createChange, final ArrNode parentNode, final Integer position) {
        Assert.notNull(createChange);

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode());
        return levelRepository.save(level);
    }

    public ArrLevel createLevel(ArrChange createChange, ArrNode node, ArrNode parentNode, int position) {
        Assert.notNull(createChange);

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(node);
        return levelRepository.save(level);
    }

    public ArrNode createNode() {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        node.setUuid(UUID.randomUUID().toString());
        return nodeRepository.save(node);
    }

    public ArrNode createNode(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return createNode();
        }
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        node.setUuid(uuid);
        return nodeRepository.save(node);
    }

    public ArrChange createChange() {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());

        return changeRepository.save(change);
    }

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param findingAidId id archivní pomůcky
     */
    public void deleteFindingAid(final Integer findingAidId) {
        Assert.notNull(findingAidId);

        if (!findingAidRepository.exists(findingAidId)) {
            return;
        }

        ArrFindingAidVersion version = getOpenVersionByFindingAidId(findingAidId);

        ArrLevel rootLevel = version.getRootLevel();
        ArrNode node = rootLevel.getNode();

        findingAidVersionRepository.findVersionsByFindingAidIdOrderByCreateDateAsc(findingAidId)
                .forEach(deleteVersion ->
                                deleteVersion(deleteVersion)
                );

        deleteLevelCascadeForce(rootLevel);
        nodeRepository.delete(node);


        packetRepository.findByFindingAid(version.getFindingAid()).forEach(packet -> packetRepository.delete(packet));

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FINDING_AID_DELETE, findingAidId));
        findingAidRepository.delete(findingAidId);
    }


    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     * - spustí přepočet stavů uzlů pro novou verzi
     *
     * @param version         verze, která se má uzavřít
     * @param arrangementType typ výstupu nové verze
     * @param ruleSet         pravidla podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    public ArrFindingAidVersion approveVersion(final ArrFindingAidVersion version,
                                               final RulArrangementType arrangementType,
                                               final RulRuleSet ruleSet) {
        Assert.notNull(version);
        Assert.notNull(arrangementType);
        Assert.notNull(ruleSet);

        ArrFindingAid findingAid = version.getFindingAid();

        if (!findingAidRepository.exists(findingAid.getFindingAidId())) {
            throw new ConcurrentUpdateException(
                    "Archivní pomůcka s identifikátorem " + findingAid.getFindingAidId() + " již neexistuje.");
        }

        if (version.getLockChange() != null) {
            throw new ConcurrentUpdateException("Verze byla již uzavřena");
        }

        List<BulkActionConfig> bulkActionConfigs = bulkActionService.runValidation(version.getFindingAidVersionId());
        if (bulkActionConfigs.size() > 0) {
            List<String> codes = new LinkedList<>();

            for (BulkActionConfig bulkActionConfig : bulkActionConfigs) {
                codes.add(bulkActionConfig.getCode());
            }

            ruleService.setVersionConformityInfo(ArrVersionConformity.State.ERR,
                    "Nebyly provedeny povinné hromadné akce " + codes + " před uzavřením verze", version);
        }

        ArrChange change = createChange();
        version.setLockChange(change);
        findingAidVersionRepository.save(version);


        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        ArrFindingAidVersion newVersion = createVersion(change, findingAid, arrangementType, ruleSet, version.getRootLevel());
        ruleService.conformityInfoAll(newVersion);

        eventNotificationService.publishEvent(
                new EventVersion(EventType.APPROVE_VERSION, version.getFindingAidVersionId()));

        return newVersion;
    }

    private void deleteVersion(ArrFindingAidVersion version) {
        Assert.notNull(version);

        updateConformityInfoService.terminateWorkerInVersionAndWait(version);

        bulkActionService.terminateBulkActions(version.getFindingAidVersionId());

        faBulkActionRepository.findByFaVersionId(version.getFindingAidVersionId()).forEach(action ->
                        faBulkActionRepository.delete(action)
        );

        nodeConformityInfoRepository.findByFaVersion(version).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        ArrVersionConformity versionConformityInfo = findingAidVersionConformityInfoRepository.findByVersion(version);
        if (versionConformityInfo != null) {
            findingAidVersionConformityInfoRepository.delete(versionConformityInfo);
        }

        findingAidVersionRepository.delete(version);
    }

    private void deleteConformityInfo(ArrNodeConformity conformityInfo) {
        nodeConformityErrorsRepository.findByNodeConformity(conformityInfo).forEach(error ->
                        nodeConformityErrorsRepository.delete(error)
        );
        nodeConformityMissingRepository.findByNodeConformity(conformityInfo).forEach(error ->
                        nodeConformityMissingRepository.delete(error)
        );

        nodeConformityInfoRepository.delete(conformityInfo);
    }


    private void deleteLevelCascadeForce(final ArrLevel level) {
        Set<ArrNode> nodes = new HashSet<>();
        ArrNode parentNode = level.getNode();
        for (ArrLevel childLevel : levelRepository.findByParentNode(parentNode)) {
            nodes.add(childLevel.getNode());
            deleteLevelCascadeForce(childLevel);
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeOrderByCreateChangeAsc(parentNode)) {
            deleteDescItemForce(descItem);
        }

        levelRepository.delete(level);
        nodes.forEach(node -> {
            deleteNodeForce(node);
        });
    }

    private void deleteNodeForce(ArrNode node) {
        Assert.notNull(node);

        nodeRegisterRepository.findByNode(node).forEach(relation -> {
            nodeRegisterRepository.delete(relation);
        });

        nodeConformityInfoRepository.findByNode(node).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        nodeRepository.delete(node);
    }

    private void deleteDescItemForce(final ArrDescItem descItem) {
        Assert.notNull(descItem);

        dataRepository.findByDescItem(descItem).forEach(data -> dataRepository.delete(data));
        descItemRepository.delete(descItem);
    }

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param findingAidId id archivní pomůcky
     * @return verze
     */
    public ArrFindingAidVersion getOpenVersionByFindingAidId(@RequestParam(value = "findingAidId") Integer findingAidId) {
        Assert.notNull(findingAidId);
        ArrFindingAidVersion faVersion = findingAidVersionRepository
                .findByFindingAidIdAndLockChangeIsNull(findingAidId);

        return faVersion;
    }

    public ArrLevel deleteLevelCascade(final ArrLevel level, final ArrChange deleteChange) {
        //pokud je level sdílený, smažeme pouze entitu, atributy ponecháme
        if (isLevelShared(level)) {
            return deleteLevelInner(level, deleteChange);
        }


        for (ArrLevel childLevel : levelRepository
                .findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(level.getNode())) {
            deleteLevelCascade(childLevel, deleteChange);
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeAndDeleteChangeIsNull(level.getNode())) {
            deleteDescItemInner(descItem, deleteChange);
        }

        return deleteLevelInner(level, deleteChange);
    }

    private boolean isLevelShared(final ArrLevel level) {
        Assert.notNull(level);

        return levelRepository.countByNode(level.getNode()) > 1;
    }

    private ArrLevel deleteLevelInner(final ArrLevel level, final ArrChange deleteChange) {
        Assert.notNull(level);

        ArrNode node = level.getNode();
        node.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(node);

        level.setDeleteChange(deleteChange);
        return levelRepository.save(level);
    }

    private void deleteDescItemInner(final ArrDescItem descItem, final ArrChange deleteChange) {
        Assert.notNull(descItem);

        descItem.setDeleteChange(deleteChange);
        ArrDescItem descItemTmp = new ArrDescItem();
        BeanUtils.copyProperties(descItem, descItemTmp);
        descItemRepository.save(descItemTmp);
    }

    /**
     * Vrací další identifikátor objektu pro atribut (oproti PK se zachovává při nové verzi)
     *
     * TODO:
     * Není dořešené, může dojít k přidělení stejného object_id dvěma různýmhodnotám atributu.
     * Řešit v budoucnu zrušením object_id (pravděpodobně GUID) nebo vytvořením nové entity,
     * kde bude object_id primární klíč a bude tak generován pomocí sekvencí hibernate.
     *
     * @return Identifikátor objektu
     */
    public Integer getNextDescItemObjectId() {
        Integer maxDescItemObjectId = descItemRepository.findMaxDescItemObjectId();
        if (maxDescItemObjectId == null) {
            maxDescItemObjectId = 0;
        }
        return maxDescItemObjectId + 1;
    }

    /**
     * Získání hodnot atributů podle verze AP a uzlu.
     *
     * @param version verze AP
     * @param node    uzel
     * @return seznam hodnot atributů
     */
    public List<ArrDescItem> getDescItems(final ArrFindingAidVersion version, final ArrNode node) {

        List<ArrDescItem> itemList;

        if (version.getLockChange() == null) {
            itemList = descItemRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            itemList = descItemRepository.findByNodeAndChange(node, version.getLockChange());
        }

        return descItemFactory.getDescItems(itemList);
    }


    /**
     * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
     *
     * @param version      verze stromu
     * @param descItemType typ atributu, který chceme zkopírovat
     * @param level        uzel, na který nastavíme hodnoty ze staršího bratra
     * @return vytvořené hodnoty
     */
    public List<ArrDescItem> copyOlderSiblingAttribute(final ArrFindingAidVersion version,
                                                       final RulDescItemType descItemType,
                                                       final ArrLevel level) {
        Assert.notNull(version);
        Assert.notNull(descItemType);
        Assert.notNull(level);

        isValidAndOpenVersion(version);

        Set<RulDescItemType> typeSet = new HashSet<>();
        typeSet.add(descItemType);

        ArrLevel olderSibling = levelRepository.findOlderSibling(level, version.getLockChange());
        if (olderSibling != null) {

            ArrChange change = createChange();

            List<ArrDescItem> siblingDescItems = descItemRepository.findOpenByNodeAndTypes(olderSibling.getNode(),
                    typeSet);
            List<ArrDescItem> nodeDescItems = descItemRepository.findOpenByNodeAndTypes(level.getNode(), typeSet);

            if (CollectionUtils.isNotEmpty(nodeDescItems)) {
                for (ArrDescItem nodeDescItem : nodeDescItems) {
                    descriptionItemService.deleteDescriptionItem(nodeDescItem, version, change, false);
                }
            }


            descriptionItemService
                    .copyDescItemWithDataToNode(level.getNode(), siblingDescItems, change, version);
        }

        descItemRepository.flush();

        eventNotificationService.publishEvent(EventFactory
                .createIdInVersionEvent(EventType.COPY_OLDER_SIBLING_ATTRIBUTE, version, level.getNode().getNodeId()));

        return descItemRepository.findOpenByNodeAndTypes(level.getNode(), typeSet);
    }

    /**
     * Zjistí, jestli patří vybraný level do dané verze.
     *
     * @param level   level
     * @param version verze
     * @return true pokud patří uzel do verze, jinak false
     */
    public boolean validLevelInVersion(final ArrLevel level, final ArrFindingAidVersion version) {
           Assert.notNull(level);
           Assert.notNull(version);
           Integer lockChange = version.getLockChange() == null
                                ? Integer.MAX_VALUE : version.getLockChange().getChangeId();

           Integer levelDeleteChange = level.getDeleteChange() == null ?
                                       Integer.MAX_VALUE : level.getDeleteChange().getChangeId();

           if (level.getCreateChange().getChangeId() < lockChange && levelDeleteChange >= lockChange) {
               return true;
           } else {
               return false;
           }
       }

    /**
     * Uložení poslední uživatelské změny nad AP k verzi AP
     *
     * @param change    ukládaná změna
     * @param versionId identifikátor verze AP
     * @return aktuální verze AP
     */
    public ArrFindingAidVersion saveLastChangeFaVersion(final ArrChange change, final Integer versionId) {
        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
        return saveLastChangeFaVersion(change, version);
    }

    /**
     * Uložení poslední uživatelské změny nad AP k verzi AP
     *
     * @param change  ukládaná změna
     * @param version verze AP
     * @return aktuální verze AP
     */
    public ArrFindingAidVersion saveLastChangeFaVersion(final ArrChange change, final ArrFindingAidVersion version) {

        if (!bulkActionService.existsChangeInWorkers(change)) {
            version.setLastChange(change);
            return findingAidVersionRepository.save(version);
        }

        return version;

    }

    /**
     * Vyhledání id nodů podle hodnoty atributu.
     *
     * @param version   verze AP
     * @param nodeId    id uzlu pod kterým se má hledat, může být null
     * @param searchValue hledaná hodnota
     * @param depth     hloubka v jaké se hledá pod předaným nodeId
     *
     * @return seznam id uzlů které vyhovují parametrům
     */
    public Set<Integer> findNodeIdsByFulltext(ArrFindingAidVersion version, Integer nodeId, String searchValue, Depth depth) {
        Assert.notNull(version);
        Assert.notNull(depth);

        ArrChange lockChange = version.getLockChange();
        Integer lockChangeId = lockChange == null ? null : lockChange.getChangeId();

        List<ArrNode> nodeIds = nodeRepository.findByFulltextAndVersionLockChangeId(searchValue, lockChangeId);

        Set<Integer> versionNodeIds = levelTreeCacheService.getAllNodeIdsByVersionAndParent(version, nodeId, depth);
        versionNodeIds.retainAll(nodeIds);

        return versionNodeIds;
    }

    /**
     * Kontrola verze, že existuje v DB a není uzavřena.
     *
     * @param version verze
     */
    public void isValidAndOpenVersion(final ArrFindingAidVersion version) {
        Assert.notNull(version);
        if (version == null) {
            throw new IllegalArgumentException("Verze neexistuje");
        }
        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Aktuální verze je zamčená");
        }
    }


    /**
     * Provede uzamčení nodu (zvýšení verze uzlu)
     *
     * @param lockNode uzamykaný node
     * @param version  verze stromu, do které patří uzel
     * @return level nodu
     */
    public ArrLevel lockNode(final ArrNode lockNode, final ArrFindingAidVersion version) {
        ArrLevel lockLevel = levelRepository
                .findNodeInRootTreeByNodeId(lockNode, version.getRootLevel().getNode(), version.getLockChange());
        Assert.notNull(lockLevel);
        ArrNode staticNodeDb = lockLevel.getNode();

        lockNode.setUuid(staticNodeDb.getUuid());
        lockNode.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(lockNode);

        return lockLevel;
    }

    /**
     * Načte počet chyb verze archivní pomůcky.
     *
     * @param findingAidVersion verze archivní pomůcky
     *
     * @return počet chyb
     */
    public Integer getVersionErrorCount(ArrFindingAidVersion findingAidVersion) {
        return nodeConformityInfoRepository.findCountByVersionAndState(findingAidVersion, State.ERR);
    }

    public List<ArrNodeConformity> findConformityErrors(ArrFindingAidVersion findingAidVersion) {
        List<ArrNodeConformity> conformity = nodeConformityInfoRepository.findFirst20ByFaVersionAndStateOrderByNodeConformityIdAsc(findingAidVersion, State.ERR);

        if (conformity.isEmpty()) {
            return new ArrayList<>();
        }

        return nodeConformityInfoRepository.fetchErrorAndMissingConformity(conformity, findingAidVersion, State.ERR);
    }


    /**
     * Najde rodiče pro předané id nodů. Vrátí seznam objektů ve kterém je id nodu a jeho rodič.
     *
     * @param nodeIds id nodů
     * @param version verze AP
     *
     * @return seznam id nodů a jejich rodičů
     */
    public List<TreeNodeFulltext> createTreeNodeFulltextList(Set<Integer> nodeIds, ArrFindingAidVersion version) {
        Assert.notNull(nodeIds);
        Assert.notNull(version);

        Map<Integer, TreeNodeClient> parentIdTreeNodeClientMap = levelTreeCacheService.findParentsWithTitles(nodeIds, version);

        List<Integer> sortedNodeIds = levelTreeCacheService.sortNodesByTreePosition(nodeIds, version);

        List<TreeNodeFulltext> result =  new ArrayList<>(sortedNodeIds.size());
        for (Integer nodeId : sortedNodeIds) {
            TreeNodeFulltext treeNodeFulltext = new TreeNodeFulltext();

            treeNodeFulltext.setNodeId(nodeId);
            treeNodeFulltext.setParent(parentIdTreeNodeClientMap.get(nodeId));

            result.add(treeNodeFulltext);
        }

        return result;
    }

    public List<VersionValidationItem> createVersionValidationItems(List<ArrNodeConformity> validationErrors, ArrFindingAidVersion version) {
        Map<Integer, String> validations = new LinkedHashMap<Integer, String>();
        for (ArrNodeConformity conformity : validationErrors) {
            String description = validations.get(conformity.getNode().getNodeId());

            if (description == null) {
                description = "";
            }

            List<String> descriptions = new LinkedList<String>();
            for (ArrNodeConformityError error : conformity.getErrorConformity()) {
                descriptions.add(error.getDescription());
            }

            for (ArrNodeConformityMissing missing : conformity.getMissingConformity()) {
                descriptions.add(missing.getDescription());
            }

            description += description + StringUtils.join(descriptions, " ");

            validations.put(conformity.getNode().getNodeId(), description);
        }

        Map<Integer, TreeNodeClient> parentIdTreeNodeClientMap = levelTreeCacheService.findParentsWithTitles(validations.keySet(), version);

        List<VersionValidationItem> versionValidationItems = new ArrayList<>(validations.size());
        for (Integer nodeId : validations.keySet()) {
            VersionValidationItem versionValidationItem = new VersionValidationItem();

            versionValidationItem.setDescription(validations.get(nodeId));
            versionValidationItem.setNodeId(nodeId);
            versionValidationItem.setParent(parentIdTreeNodeClientMap.get(nodeId));

            versionValidationItems.add(versionValidationItem);
        }

        return versionValidationItems;
    }

    public ArrFaRegisterScope addScopeToFindingAid(ArrFindingAid findingAid, RegScope scope) {
        Assert.notNull(findingAid);
        Assert.notNull(scope);

        ArrFaRegisterScope faRegisterScope = faRegisterScopeRepository.findByFindingAidAndScope(findingAid, scope);
        if (faRegisterScope != null) {
            logger.info("Vazbe mezi archivní pomůckou " + findingAid + " a třídou rejstříku " + scope + " již existuje.");
            return faRegisterScope;
        }

        faRegisterScope = new ArrFaRegisterScope();
        faRegisterScope.setFindingAid(findingAid);
        faRegisterScope.setScope(scope);

        return faRegisterScopeRepository.save(faRegisterScope);
    }

}
