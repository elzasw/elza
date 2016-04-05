package cz.tacr.elza.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.ArrNodeConformity.State;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.ArrangementController.TreeNodeFulltext;
import cz.tacr.elza.controller.ArrangementController.VersionValidationItem;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundRegisterScope;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RegScope;
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
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.ScopeRepository;
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
    private RuleService ruleService;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private FundRepository fundRepository;

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
    private BulkActionRunRepository faBulkActionRepository;

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private FundRegisterScopeRepository faRegisterRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    //TODO smazat závislost až bude DescItemService
    @Autowired
    protected FundRegisterScopeRepository fundRegisterScopeRepository;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private PolicyService policyService;

    /**
     * Vytvoření archivního souboru.
     *
     * @param name          název
     * @param ruleSet       pravidla
     * @param change        změna
     * @param uuid          uuid
     * @param internalCode  iterní kód
     * @param institution   instituce
     * @param dateRange     časový rozsah
     * @return vytvořený arch. soubor
     */
    public ArrFund createFund(final String name,
                              final RulRuleSet ruleSet,
                              final ArrChange change,
                              final String uuid,
                              final String internalCode,
                              final ParInstitution institution,
                              final String dateRange) {
        ArrFund fund = new ArrFund();
        fund.setCreateDate(LocalDateTime.now());
        fund.setName(name);
        fund.setInternalCode(internalCode);
        fund.setInstitution(institution);

        fund = fundRepository.save(fund);

        eventNotificationService
                .publishEvent(EventFactory.createIdEvent(EventType.FUND_CREATE, fund.getFundId()));

        //        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        ArrLevel rootLevel = createLevel(change, null, uuid, fund);
        createVersion(change, fund, ruleSet, rootLevel.getNode(), dateRange);

        return fund;
    }

    /**
     * @param fund
     * @param scopes
     * @return Upravená archivní pomůcka
     */
    @Transactional
    public ArrFund updateFund(final ArrFund fund, final List<RegScope> scopes) {
        Assert.notNull(fund);
        ArrFund originalFund = fundRepository.findOne(fund.getFundId());
        Assert.notNull(originalFund);

        originalFund.setName(fund.getName());
        originalFund.setInternalCode(fund.getInternalCode());
        originalFund.setInstitution(fund.getInstitution());

        fundRepository.save(originalFund);

        for (RegScope scope : scopes) {
            if (scope.getScopeId() == null) {
                scope.setCode(StringUtils.upperCase(Normalizer.normalize(StringUtils.replace(StringUtils.substring(scope.getName(), 0, 50).trim(), " ", "_"), Normalizer.Form.NFD)));
                scopeRepository.save(scope);
            }
        }

        synchRegScopes(originalFund, scopes);

        eventNotificationService
                .publishEvent(EventFactory.createIdEvent(EventType.FUND_UPDATE, originalFund.getFundId()));

        return originalFund;
    }

    /**
     * Pokud se jedná o typ osoby group, dojde k synchronizaci identifikátorů osoby. CRUD.
     */
    private void synchRegScopes(final ArrFund fund,
                                final Collection<RegScope> newRegScopes) {
        Assert.notNull(fund);

        Map<Integer, ArrFundRegisterScope> dbIdentifiersMap = Collections.EMPTY_MAP; /// Redundantní initializer
        dbIdentifiersMap = ElzaTools.createEntityMap(faRegisterRepository.findByFund(fund), i -> i.getScope().getScopeId());
        Set<ArrFundRegisterScope> removeScopes = new HashSet<>(dbIdentifiersMap.values());


        for (RegScope newScope : newRegScopes) {
            ArrFundRegisterScope oldScope = dbIdentifiersMap.get(newScope.getScopeId());

            if (oldScope == null) {
                oldScope = new ArrFundRegisterScope();
                oldScope.setFund(fund);
                oldScope.setScope(newScope);
            } else {
                removeScopes.remove(oldScope);
            }

            faRegisterRepository.save(oldScope);
        }

        faRegisterRepository.delete(removeScopes);
    }

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas. Pro root
     * vytvoří atributy podle scénáře.
     *
     * @param name            název archivní pomůcky
     * @param ruleSet         id pravidel podle kterých se vytváří popis
     * @param dateRange       vysčítaná informace o časovém rozsahu fondu
     * @param internalCode    interní označení
     * @return nová archivní pomůcka
     */
    public ArrFund createFundWithScenario(final String name,
                                          final RulRuleSet ruleSet,
                                          final String internalCode,
                                          final ParInstitution institution,
                                          final String dateRange) {
        ArrChange change = createChange();

        ArrFund fund = createFund(name, ruleSet, change, null, internalCode, institution, dateRange);

        List<RegScope> defaultScopes = registryService.findDefaultScopes();
        if (!defaultScopes.isEmpty()) {
            addScopeToFund(fund, defaultScopes.get(0));
        }

        ArrFundVersion version = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(fund.getFundId());

        ArrNode rootNode = version.getRootNode();
        ArrLevel rootLevel = levelRepository.findNodeInRootTreeByNodeId(rootNode, rootNode, version.getLockChange());

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

        ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(rootLevel.getNode().getNodeId()),
                NodeTypeOperation.CREATE_NODE, null, null, null);

        return fund;
    }


    public ArrFundVersion createVersion(final ArrChange createChange,
                                        final ArrFund fund,
                                        final RulRuleSet ruleSet,
                                        final ArrNode rootNode,
                                        final String dateRange) {
        ArrFundVersion version = new ArrFundVersion();
        version.setCreateChange(createChange);
        version.setFund(fund);
        version.setRuleSet(ruleSet);
        version.setRootNode(rootNode);
        version.setDateRange(dateRange);
        return fundVersionRepository.save(version);
    }

    public ArrLevel createLevel(final ArrChange createChange,
                                final ArrNode parentNode,
                                final String uuid,
                                final ArrFund fund) {
        ArrLevel level = new ArrLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode(uuid, fund));
        return levelRepository.save(level);
    }

    public ArrLevel createLevel(final ArrChange createChange,
                                final ArrNode parentNode,
                                final Integer position,
                                final ArrFund fund) {
        Assert.notNull(createChange);

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode(fund));
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

    public ArrNode createNode(ArrFund fund) {
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        node.setUuid(UUID.randomUUID().toString());
        node.setFund(fund);
        return nodeRepository.save(node);
    }

    public ArrNode createNode(final String uuid,
                              final ArrFund fund) {
        if (StringUtils.isBlank(uuid)) {
            return createNode(fund);
        }
        ArrNode node = new ArrNode();
        node.setLastUpdate(LocalDateTime.now());
        node.setUuid(uuid);
        node.setFund(fund);
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
     * @param fundId id archivní pomůcky
     */
    public void deleteFund(final Integer fundId) {
        Assert.notNull(fundId);

        if (!fundRepository.exists(fundId)) {
            return;
        }

        ArrFundVersion version = getOpenVersionByFundId(fundId);


        ArrNode rootNode = version.getRootNode();
        ArrLevel rootLevel = levelRepository.findNodeInRootTreeByNodeId(rootNode, rootNode, version.getLockChange());
        ArrNode node = rootLevel.getNode();

        fundVersionRepository.findVersionsByFundIdOrderByCreateDateAsc(fundId)
                .forEach(deleteVersion ->
                                deleteVersion(deleteVersion)
                );

        deleteLevelCascadeForce(rootLevel);
        nodeRepository.delete(node);


        packetRepository.findByFund(version.getFund()).forEach(packet -> packetRepository.delete(packet));
        ArrFund fund = fundRepository.findOne(fundId);
        faRegisterRepository.findByFund(fund).forEach(
                faScope -> faRegisterRepository.delete(faScope)
        );


        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FUND_DELETE, fundId));
        fundRepository.delete(fundId);
    }


    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     * - spustí přepočet stavů uzlů pro novou verzi
     *
     * @param version         verze, která se má uzavřít
     * @param ruleSet         pravidla podle kterých se vytváří popis v nové verzi
     * @param dateRange       vysčítaná informace o časovém rozsahu fondu
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    public ArrFundVersion approveVersion(final ArrFundVersion version,
                                         final RulRuleSet ruleSet,
                                         final String dateRange) {
        Assert.notNull(version);
        Assert.notNull(ruleSet);

        ArrFund fund = version.getFund();

        if (!fundRepository.exists(fund.getFundId())) {
            throw new ConcurrentUpdateException(
                    "Archivní soubor s identifikátorem " + fund.getFundId() + " již neexistuje.");
        }

        if (version.getLockChange() != null) {
            throw new ConcurrentUpdateException("Verze byla již uzavřena");
        }

        if (bulkActionService.isRunning(version)) {
            throw new IllegalStateException("Nelze uzavřít verzi, protože běží hromadná akce");
        }

        if (updateConformityInfoService.isRunning(version)) {
            throw new IllegalStateException("Nelze uzavřít verzi, protože běží validace");
        }

        ArrChange change = createChange();
        version.setLockChange(change);
        fundVersionRepository.save(version);

        ArrFundVersion newVersion = createVersion(change, fund, ruleSet, version.getRootNode(), dateRange);
        ruleService.conformityInfoAll(newVersion);

        eventNotificationService.publishEvent(
                new EventVersion(EventType.APPROVE_VERSION, version.getFundVersionId()));

        return newVersion;
    }

    private void deleteVersion(ArrFundVersion version) {
        Assert.notNull(version);

        updateConformityInfoService.terminateWorkerInVersionAndWait(version);

        bulkActionService.terminateBulkActions(version.getFundVersionId());

        faBulkActionRepository.findByFundVersionId(version.getFundVersionId()).forEach(action ->
                        faBulkActionRepository.delete(action)
        );

        nodeConformityInfoRepository.findByFundVersion(version).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        fundVersionRepository.delete(version);
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
     * @param fundId id archivní pomůcky
     * @return verze
     */
    public ArrFundVersion getOpenVersionByFundId(@RequestParam(value = "fundId") Integer fundId) {
        Assert.notNull(fundId);
        ArrFundVersion fundVersion = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(fundId);

        return fundVersion;
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
    public List<ArrDescItem> getDescItems(final ArrFundVersion version, final ArrNode node) {

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
    public List<ArrDescItem> copyOlderSiblingAttribute(final ArrFundVersion version,
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
    public boolean validLevelInVersion(final ArrLevel level, final ArrFundVersion version) {
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
     * Vyhledání id nodů podle hodnoty atributu.
     *
     * @param version   verze AP
     * @param nodeId    id uzlu pod kterým se má hledat, může být null
     * @param searchValue hledaná hodnota
     * @param depth     hloubka v jaké se hledá pod předaným nodeId
     *
     * @return seznam id uzlů které vyhovují parametrům
     */
    public Set<Integer> findNodeIdsByFulltext(ArrFundVersion version, Integer nodeId, String searchValue, Depth depth) {
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
    public void isValidAndOpenVersion(final ArrFundVersion version) {
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
    public ArrLevel lockNode(final ArrNode lockNode, final ArrFundVersion version) {
        ArrLevel lockLevel = levelRepository
                .findNodeInRootTreeByNodeId(lockNode, version.getRootNode(), version.getLockChange());
        Assert.notNull(lockLevel);
        ArrNode staticNodeDb = lockLevel.getNode();
        lockNode(staticNodeDb, lockNode);

        return lockLevel;
    }

    /**
     * Provede uzamčení nodu (zvýšení verze uzlu)
     *
     * @param dbNode   odpovídající uzel načtený z db
     * @param lockNode uzamykaný node
     * @return level nodu
     */
    public ArrNode lockNode(final ArrNode dbNode, final ArrNode lockNode) {
        Assert.notNull(dbNode);
        Assert.notNull(lockNode);

        lockNode.setUuid(dbNode.getUuid());
        lockNode.setLastUpdate(LocalDateTime.now());
        lockNode.setFund(dbNode.getFund());

        return nodeRepository.save(lockNode);
    }

    /**
     * Načte počet chyb verze archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     *
     * @return počet chyb
     */
    public Integer getVersionErrorCount(ArrFundVersion fundVersion) {
        return nodeConformityInfoRepository.findCountByFundVersionAndState(fundVersion, State.ERR);
    }

    public List<ArrNodeConformity> findConformityErrors(ArrFundVersion fundVersion, final Boolean showAll) {
        List<ArrNodeConformity> conformity = nodeConformityInfoRepository.findFirst20ByFundVersionAndStateOrderByNodeConformityIdAsc(fundVersion, State.ERR);

        if (conformity.isEmpty()) {
            return new ArrayList<>();
        }

        // TODO: bude se řešit v budoucnu úplně jinak

        List<ArrNodeConformity> nodeConformities = nodeConformityInfoRepository.fetchErrorAndMissingConformity(conformity, fundVersion, State.ERR);

        if (!showAll) {
            Set<Integer> nodeIds = nodeConformities.stream().map(arrNodeConformity -> arrNodeConformity.getNode().getNodeId()).collect(Collectors.toSet());

            Map<Integer, Map<Integer, Boolean>> visiblePolicyIds = policyService.getVisiblePolicyIds(new ArrayList<>(nodeIds), fundVersion, true);

            Iterator<ArrNodeConformity> nodeConformityIterator = nodeConformities.iterator();
            while (nodeConformityIterator.hasNext()) {
                ArrNodeConformity nodeConformity = nodeConformityIterator.next();
                Map<Integer, Boolean> visiblePolicy = visiblePolicyIds.get(nodeConformity.getNode().getNodeId());

                Iterator<ArrNodeConformityError> conformityErrorIterator = nodeConformity.getErrorConformity().iterator();
                Iterator<ArrNodeConformityMissing> conformityMissingIterator = nodeConformity.getMissingConformity().iterator();

                while (conformityErrorIterator.hasNext()) {
                    ArrNodeConformityError conformityError = conformityErrorIterator.next();
                    if (conformityError.getPolicyType() != null) {
                        Integer policyTypeId = conformityError.getPolicyType().getPolicyTypeId();
                        Boolean visible = visiblePolicy.get(policyTypeId);
                        if (visible != null && visible == false) {
                            conformityErrorIterator.remove();
                        }
                    }
                }

                while (conformityMissingIterator.hasNext()) {
                    ArrNodeConformityMissing conformityMissing = conformityMissingIterator.next();
                    if (conformityMissing.getPolicyType() != null) {
                        Integer policyTypeId = conformityMissing.getPolicyType().getPolicyTypeId();
                        Boolean visible = visiblePolicy.get(policyTypeId);
                        if (visible != null && visible == false) {
                            conformityMissingIterator.remove();
                        }
                    }
                }

                if (nodeConformity.getErrorConformity().size() == 0 && nodeConformity.getMissingConformity().size() == 0) {
                    nodeConformityIterator.remove();
                }
            }


        }

        return nodeConformities;
    }


    /**
     * Najde rodiče pro předané id nodů. Vrátí seznam objektů ve kterém je id nodu a jeho rodič.
     *
     * @param nodeIds id nodů
     * @param version verze AP
     *
     * @return seznam id nodů a jejich rodičů
     */
    public List<TreeNodeFulltext> createTreeNodeFulltextList(Set<Integer> nodeIds, ArrFundVersion version) {
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

    public List<VersionValidationItem> createVersionValidationItems(List<ArrNodeConformity> validationErrors, ArrFundVersion version) {
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

    public ArrFundRegisterScope addScopeToFund(ArrFund fund, RegScope scope) {
        Assert.notNull(fund);
        Assert.notNull(scope);

        ArrFundRegisterScope faRegisterScope = fundRegisterScopeRepository.findByFundAndScope(fund, scope);
        if (faRegisterScope != null) {
            logger.info("Vazbe mezi archivní pomůckou " + fund + " a třídou rejstříku " + scope + " již existuje.");
            return faRegisterScope;
        }

        faRegisterScope = new ArrFundRegisterScope();
        faRegisterScope.setFund(fund);
        faRegisterScope.setScope(scope);

        return fundRegisterScopeRepository.save(faRegisterScope);
    }

    /**
     * Vyhledání sousedních uzlů kolem určitého uzlu.
     *
     * @param version   verze AP
     * @param node      uzel
     * @param around    velikost okolí
     * @return  okolní uzly (včetně původního)
     */
    public List<ArrNode> findSiblingsAroundOfNode(final ArrFundVersion version, final ArrNode node, final Integer around) {
        List<ArrNode> siblings = nodeRepository.findNodesByDirection(node, version, RelatedNodeDirection.ALL_SIBLINGS);

        if (around <= 0) {
            throw new IllegalStateException("Velikost okolí musí být minimálně 1");
        }

        //požadujeme pouze nejbližšího sourozence před a za objektem
        int nodeIndex = siblings.indexOf(node);
        List<ArrNode> result = new ArrayList<>();

        int min = nodeIndex - around;
        int max = nodeIndex + around;

        min = min < 0 ? 0 : min;
        max = max > siblings.size() - 1 ? siblings.size() - 1 : max;

        for(int i = min; i <= max; i++) {
            result.add(siblings.get(i));
        }

        return result;
    }

}
