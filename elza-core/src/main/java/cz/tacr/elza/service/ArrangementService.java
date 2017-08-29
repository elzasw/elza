package cz.tacr.elza.service;

import com.google.common.collect.Iterables;
import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.asynchactions.UpdateConformityInfoService;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.ArrangementController.TreeNodeFulltext;
import cz.tacr.elza.controller.ArrangementController.VersionValidationItem;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.filter.SearchParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundRegisterScope;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformity.State;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UIVisiblePolicy;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ConcurrentUpdateException;
import cz.tacr.elza.exception.InvalidQueryException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoLinkRequestRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DaoRequestRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DigitizationRequestRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.VisiblePolicyRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventFund;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.utils.ObjectListIterator;
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

import javax.annotation.Nullable;
import javax.persistence.Query;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 12. 2015
 */
@Service
public class ArrangementService {

    //TODO smazat závislost až bude DescItemService
    @Autowired
    protected FundRegisterScopeRepository fundRegisterScopeRepository;
    private Log logger = LogFactory.getLog(this.getClass());
    @Autowired
    private LevelTreeCacheService levelTreeCacheService;
    @Autowired
    private UserService userService;
    @Autowired
    private UpdateConformityInfoService updateConformityInfoService;
    @Autowired
    private BulkActionService bulkActionService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private ItemRepository itemRepository;
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
    private BulkActionNodeRepository faBulkActionNodeRepository;
    @Autowired
    private PacketRepository packetRepository;
    @Autowired
    private DescItemFactory descItemFactory;
    @Autowired
    private FundRegisterScopeRepository faRegisterRepository;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private OutputItemRepository outputItemRepository;
    @Autowired
    private RegistryService registryService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private NodeOutputRepository nodeOutputRepository;

    @Autowired
    private VisiblePolicyRepository visiblePolicyRepository;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private OutputFileRepository outputFileRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;

    @Autowired
    private RevertingChangesService revertingChangesService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DigitizationRequestRepository digitizationRequestRepository;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;

    @Autowired
    private DaoRequestRepository daoRequestRepository;

    @Autowired
    private DaoLinkRequestRepository daoLinkRequestRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private CachedNodeRepository cachedNodeRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    public static final String UNDEFINED = "Nezjištěno";

    /**
     * Poslední přidělené object id.
     */
    private Integer maxDescItemObjectId = null;

    /**
     * Vytvoření archivního souboru.
     *
     * @param name         název
     * @param ruleSet      pravidla
     * @param change       změna
     * @param uuid         uuid
     * @param internalCode iterní kód
     * @param institution  instituce
     * @param dateRange    časový rozsah
     * @return vytvořený arch. soubor
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
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
        fund.setUuid(uuid);

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
     * @param ruleSet
     * @param scopes  @return Upravená archivní pomůcka
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrFund updateFund(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                              final RulRuleSet ruleSet,
                              final List<RegScope> scopes) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(ruleSet, "Pravidla musí být vyplněna");

        ArrFund originalFund = fundRepository.findOne(fund.getFundId());
        Assert.notNull(originalFund, "AS neexistuje");

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


        ArrFundVersion openVersion = getOpenVersionByFundId(originalFund.getFundId());
        if (!ruleSet.equals(openVersion.getRuleSet())) {
            openVersion.setRuleSet(ruleSet);
            fundVersionRepository.save(openVersion);

            ruleService.conformityInfoAll(openVersion);
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
        Assert.notNull(fund, "AS musí být vyplněn");

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
     * @param name         název archivní pomůcky
     * @param ruleSet      id pravidel podle kterých se vytváří popis
     * @param dateRange    vysčítaná informace o časovém rozsahu fondu
     * @param internalCode interní označení
     * @return nová archivní pomůcka
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
    public ArrFund createFundWithScenario(final String name,
                                          final RulRuleSet ruleSet,
                                          final String internalCode,
                                          final ParInstitution institution,
                                          final String dateRange) {
        ArrChange change = createChange(ArrChange.Type.CREATE_AS);

        ArrFund fund = createFund(name, ruleSet, change, generateUuid(), internalCode, institution, dateRange);

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


    private ArrFundVersion createVersion(final ArrChange createChange,
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

    private ArrLevel createLevel(final ArrChange createChange,
                                 final ArrNode parentNode,
                                 final String uuid,
                                 final ArrFund fund) {
        ArrLevel level = new ArrLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode(uuid, fund, createChange));
        return levelRepository.saveAndFlush(level);
    }

    public ArrLevel createLevel(final ArrChange createChange,
                                final ArrNode parentNode,
                                final int position,
                                final ArrFund fund) {
        Assert.notNull(createChange, "Change nesmí být prázdná");

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNode(fund, createChange));
        return levelRepository.saveAndFlush(level);
    }

    public ArrLevel createLevelSimple(final ArrChange createChange,
                                      final ArrNode parentNode,
                                      final int position,
                                      final String uuid,
                                      final ArrFund fund) {
        Assert.notNull(createChange, "Change nesmí být prázdná");

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(createNodeSimple(fund, uuid, createChange));
        return levelRepository.save(level);
    }

    public ArrLevel createLevel(final ArrChange createChange, final ArrNode node, final ArrNode parentNode, final int position) {
        Assert.notNull(createChange, "Změna musí být vyplněna");

        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        level.setNodeParent(parentNode);
        level.setNode(node);
        return levelRepository.saveAndFlush(level);
    }


    /**
     * Vytvoření jednoznačného identifikátoru požadavku.
     *
     * @return jednoznačný identifikátor
     */
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }

    public ArrNode createNode(final ArrFund fund, final ArrChange createChange) {
        ArrNode node = new ArrNode();
        node.setLastUpdate(createChange.getChangeDate());
        node.setUuid(generateUuid());
        node.setFund(fund);
        nodeRepository.save(node);
        arrangementCacheService.createNode(node.getNodeId());
        return node;
    }

    public ArrNode createNodeSimple(final ArrFund fund, final String uuid, final ArrChange createChange) {
        ArrNode node = new ArrNode();
        node.setLastUpdate(createChange.getChangeDate());
        node.setUuid(uuid == null ? generateUuid() : uuid);
        node.setFund(fund);
        nodeRepository.save(node);
        return node;
    }

    public ArrNode createNode(final String uuid,
                              final ArrFund fund,
                              final ArrChange change) {
        if (StringUtils.isBlank(uuid)) {
            return createNode(fund, change);
        }
        ArrNode node = new ArrNode();
        node.setLastUpdate(change.getChangeDate());
        node.setUuid(uuid);
        node.setFund(fund);
        nodeRepository.save(node);
        arrangementCacheService.createNode(node.getNodeId());
        return node;
    }

    /**
     * Vytvoření objektu pro změny s primárním uzlem.
     *
     * @param type        typ změny
     * @param primaryNode primární uzel
     * @return objekt změny
     */
    public ArrChange createChange(@Nullable final ArrChange.Type type,
                                  @Nullable final ArrNode primaryNode) {
        ArrChange change = new ArrChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(LocalDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = new UsrUser();
            user.setUserId(userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setPrimaryNode(primaryNode);

        return changeRepository.save(change);
    }

    /**
     * Vytvoření objektu pro změny.
     *
     * @param type typ změny
     * @return objekt změny
     */
    public ArrChange createChange(@Nullable final ArrChange.Type type) {
        return createChange(type, null);
    }

    /**
     * Dodatečné nastavení primární vazby u změny.
     *
     * @param change        změna u které primární uzel nastavujeme
     * @param primaryNodeId identifikátor uzlu
     * @return upravená změna
     */
    public ArrChange setPrimaryNodeId(final ArrChange change,
                                      final Integer primaryNodeId) {
        ArrNode primaryNode = new ArrNode();
        primaryNode.setNodeId(primaryNodeId);
        return setPrimaryNode(change, primaryNode);
    }

    /**
     * Dodatečné nastavení primární vazby u změny.
     *
     * @param change      změna u které primární uzel nastavujeme
     * @param primaryNode uzel
     * @return upravená změna
     */
    public ArrChange setPrimaryNode(final ArrChange change,
                                    final ArrNode primaryNode) {
        change.setPrimaryNode(primaryNode);
        return changeRepository.save(change);
    }

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param fundId id archivní pomůcky
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
    public void deleteFund(final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");

        if (!fundRepository.exists(fundId)) {
            return;
        }

        ArrFundVersion version = getOpenVersionByFundId(fundId);
        ArrFund fund = version.getFund();

        List<ArrOutputDefinition> outputDefinitions = outputDefinitionRepository.findByFund(fund);

        if (!outputDefinitions.isEmpty()) {
            Collection<Integer> dataIdsToDelete = new HashSet<>();
            for (ArrOutputDefinition outputDefinition : outputDefinitions) {
                outputRepository.delete(outputDefinition.getOutputs());
                nodeOutputRepository.delete(outputDefinition.getOutputNodes());
                dataIdsToDelete.addAll(dataRepository.findByIdsOutputDefinition(outputDefinition));
                outputItemRepository.deleteByOutputDefinition(outputDefinition);
                faBulkActionRepository.deleteByOutputDefinition(outputDefinition);
                itemSettingsRepository.deleteByOutputDefinition(outputDefinition);
                outputFileRepository.deleteByOutputDefinition(outputDefinition);
                outputResultRepository.deleteByOutputDefinition(outputDefinition);
                outputDefinitionRepository.delete(outputDefinition);
            }
            outputItemRepository.flush();
            ObjectListIterator<Integer> dataIdsToDeleteIterator = new ObjectListIterator<>(dataIdsToDelete);
            while (dataIdsToDeleteIterator.hasNext()) {
                List<Integer> ids = dataIdsToDeleteIterator.next();
                dataRepository.deleteByIds(ids);
                dataRepository.flush();
            }
        }

        cachedNodeRepository.deleteByFund(fund);

        ArrNode rootNode = version.getRootNode();
        ArrLevel rootLevel = levelRepository.findNodeInRootTreeByNodeId(rootNode, rootNode, version.getLockChange());
        ArrNode node = rootLevel.getNode();

        fundVersionRepository.findVersionsByFundIdOrderByCreateDateDesc(fundId)
                .forEach(deleteVersion ->
                        deleteVersion(deleteVersion)
                );


        policyService.deleteFundVisiblePolicies(fund);
        userService.deleteByFund(fund);

        requestQueueItemRepository.deleteByFund(fund);

        digitizationRequestNodeRepository.deleteByFund(fund);
        digitizationRequestRepository.deleteByFund(fund);

        daoLinkRequestRepository.deleteByFund(fund);

        daoRequestDaoRepository.deleteByFund(fund);
        daoRequestRepository.deleteByFund(fund);

        deleteFundLevels(rootLevel);

        daoLinkRepository.deleteByNode(node);
        changeRepository.deleteByPrimaryNode(node);

        nodeRegisterRepository.findByNode(node).forEach(relation -> {
            nodeRegisterRepository.delete(relation);
        });

        nodeConformityInfoRepository.findByNode(node).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });
        arrangementCacheService.deleteNode(node.getNodeId());
        nodeRepository.delete(node);

        dmsService.deleteFilesByFund(fund);

        packetRepository.findByFund(fund).forEach(packet -> packetRepository.delete(packet));

        faRegisterRepository.findByFund(fund).forEach(
                faScope -> faRegisterRepository.delete(faScope)
        );

        daoFileRepository.deleteByFund(fund);
        daoFileGroupRepository.deleteByFund(fund);
        daoRepository.deleteByFund(fund);
        daoPackageRepository.deleteByFund(fund);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.FUND_DELETE, fundId));

        nodeRepository.deleteByFund(fund);

        fundRepository.delete(fundId);

        Query deleteNotUseChangesQuery = revertingChangesService.createDeleteNotUseChangesQuery();
        deleteNotUseChangesQuery.executeUpdate();
    }


    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     * - spustí přepočet stavů uzlů pro novou verzi
     *
     * @param version   verze, která se má uzavřít
     * @param dateRange vysčítaná informace o časovém rozsahu fondu
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrFundVersion approveVersion(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                         final String dateRange) {
        Assert.notNull(version, "Verze AS musí být vyplněna");

        ArrFund fund = version.getFund();

        if (!fundRepository.exists(fund.getFundId())) {
            throw new ObjectNotFoundException("AS s ID=" + fund.getFundId() + " nebylo nalezeno", ArrangementCode.FUND_NOT_FOUND).set("id", fund.getFundId());
        }

        if (version.getLockChange() != null) {
            throw new BusinessException("Verze AS s ID=" + fund.getFundId() + " je již uzavřena", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (bulkActionService.isRunning(version)) {
            throw new BusinessException("Nelze uzavřít verzi AS s ID=" + fund.getFundId() + ", protože běží hromadná akce", ArrangementCode.VERSION_CANNOT_CLOSE_ACTION);
        }

        if (updateConformityInfoService.isRunning(version)) {
            throw new BusinessException("Nelze uzavřít verzi AS s ID=" + fund.getFundId() + ", protože běží validace", ArrangementCode.VERSION_CANNOT_CLOSE_VALIDATION);
        }

        ArrChange change = createChange(null);
        version.setLockChange(change);
        fundVersionRepository.save(version);

        ArrFundVersion newVersion = createVersion(change, fund, version.getRuleSet(), version.getRootNode(), dateRange);
        ruleService.conformityInfoAll(newVersion);

        eventNotificationService.publishEvent(
                new EventFund(EventType.APPROVE_VERSION, version.getFund().getFundId(), version.getFundVersionId()));

        return newVersion;
    }

    private void deleteVersion(final ArrFundVersion version) {
        Assert.notNull(version, "Verze AS musí být vyplněna");

        updateConformityInfoService.terminateWorkerInVersionAndWait(version);

        bulkActionService.terminateBulkActions(version.getFundVersionId());

        faBulkActionRepository.findByFundVersionId(version.getFundVersionId()).forEach(action -> {
            faBulkActionNodeRepository.deleteByBulkAction(action);
            faBulkActionRepository.delete(action);
        });

        nodeConformityInfoRepository.findByFundVersion(version).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        fundVersionRepository.delete(version);
    }

    private void deleteConformityInfo(final ArrNodeConformity conformityInfo) {
        nodeConformityErrorsRepository.findByNodeConformity(conformityInfo).forEach(error ->
                nodeConformityErrorsRepository.delete(error)
        );
        nodeConformityMissingRepository.findByNodeConformity(conformityInfo).forEach(error ->
                nodeConformityMissingRepository.delete(error)
        );

        nodeConformityInfoRepository.delete(conformityInfo);
    }


    /**
     * Smaže uzly pro celou archivní pomůcku.
     *
     * @param rootLevel kořenový uzel archivní pomůcky
     */
    private void deleteFundLevels(final ArrLevel rootLevel) {
        Assert.notNull(rootLevel, "Level musí být vyplněn");

        Set<ArrNode> nodesToDelete = new HashSet<>();
        Set<ArrData> dataToDelete = new HashSet<>();

        deleteLevelCascadeForce(rootLevel, nodesToDelete, dataToDelete);

        List<Integer> nodeIds = new ArrayList<>(nodesToDelete.size());
        for (ArrNode node : nodesToDelete) {
            nodeIds.add(node.getNodeId());
        }
        arrangementCacheService.deleteNodes(nodeIds);
        dataRepository.delete(dataToDelete);
        nodesToDelete.forEach(this::deleteNodeForce);
    }

    private void deleteLevelCascadeForce(final ArrLevel level, final Set<ArrNode> nodesToDelete, final Set<ArrData> dataToDelete) {
        ArrNode parentNode = level.getNode();
        for (ArrLevel childLevel : levelRepository.findByParentNode(parentNode)) {
            nodesToDelete.add(childLevel.getNode());
            deleteLevelCascadeForce(childLevel, nodesToDelete, dataToDelete);
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeOrderByCreateChangeAsc(parentNode)) {
            deleteDescItemForce(descItem, dataToDelete);
        }

        levelRepository.delete(level);
    }

    private void deleteNodeForce(final ArrNode node) {
        Assert.notNull(node, "Musí být vyplněno");

        nodeRegisterRepository.findByNode(node).forEach(relation -> {
            nodeRegisterRepository.delete(relation);
        });

        nodeConformityInfoRepository.findByNode(node).forEach(conformityInfo -> {
            deleteConformityInfo(conformityInfo);
        });

        daoLinkRepository.deleteByNode(node);

        changeRepository.deleteByPrimaryNode(node);

        nodeRepository.delete(node);
    }

    private void deleteDescItemForce(final ArrDescItem descItem, final Set<ArrData> dataToDelete) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");

        ArrData data = descItem.getData();

        descItemRepository.delete(descItem);

        if (data != null) {
            dataToDelete.add(data);
        }
    }

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param fundId id archivní pomůcky
     * @return verze
     */
    public ArrFundVersion getOpenVersionByFundId(@RequestParam(value = "fundId") final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
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
        Assert.notNull(level, "Level musí být vyplněn");

        return levelRepository.countByNode(level.getNode()) > 1;
    }

    private ArrLevel deleteLevelInner(final ArrLevel level, final ArrChange deleteChange) {
        Assert.notNull(level, "Musí být vyplněno");

        ArrNode node = level.getNode();
        node.setLastUpdate(deleteChange.getChangeDate());
        nodeRepository.save(node);

        level.setDeleteChange(deleteChange);
        return levelRepository.saveAndFlush(level);
    }

    private void deleteDescItemInner(final ArrDescItem descItem, final ArrChange deleteChange) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");

        descItem.setDeleteChange(deleteChange);
        ArrDescItem descItemTmp;
        //try {
        descItemTmp = new ArrDescItem();
        /*} catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }*/
        BeanUtils.copyProperties(descItem, descItemTmp);
        descItemRepository.save(descItemTmp);
    }

    /**
     * Vrací další identifikátor objektu pro atribut (oproti PK se zachovává při nové verzi)
     * <p>
     * TODO:
     * Není dořešené, může dojít k přidělení stejného object_id dvěma různýmhodnotám atributu.
     * Řešit v budoucnu zrušením object_id (pravděpodobně GUID) nebo vytvořením nové entity,
     * kde bude object_id primární klíč a bude tak generován pomocí sekvencí hibernate.
     *
     * @return Identifikátor objektu
     */
    public synchronized Integer getNextDescItemObjectId() {
        if (maxDescItemObjectId == null) {
            maxDescItemObjectId = itemRepository.findMaxItemObjectId();
            if (maxDescItemObjectId == null) {
                maxDescItemObjectId = 0;
            }
        }
        maxDescItemObjectId++;
        return maxDescItemObjectId;
    }

    /**
     * Získání hodnot atributů podle verze AP a uzlu.
     *
     * @param version verze AP
     * @param node    uzel
     * @return seznam hodnot atributů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDescItem> getDescItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                          final ArrNode node) {

        return getArrDescItemsInternal(version, node);
    }

    /**
     * Získání hodnot atributů podle verze AP a uzlu.
     * <b> Metoda nekontroluje oprávnění. </b>
     *
     * @param version verze AP
     * @param node    uzel
     * @return seznam hodnot atributů
     */
    public List<ArrDescItem> getArrDescItemsInternal(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version, final ArrNode node) {
        List<ArrDescItem> itemList;

        if (version.getLockChange() == null) {
            itemList = descItemRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            itemList = descItemRepository.findByNodeAndChange(node, version.getLockChange());
        }

        return itemList;
    }

    /**
     * Získání hodnot atributů podle verze AP a uzlu.
     * <b> Metoda nekontroluje oprávnění. </b>
     *
     * @param version verze AP
     * @param node    uzel
     * @return seznam hodnot atributů
     */
    public List<ArrDescItem> getArrDescItems(final ArrFundVersion version, final ArrNode node) {
        List<ArrDescItem> itemList;

        if (version.getLockChange() == null) {
            itemList = descItemRepository.findByNodeAndDeleteChangeIsNull(node);
        } else {
            itemList = descItemRepository.findByNodeAndChange(node, version.getLockChange());
        }

        return itemList;
    }


    /**
     * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
     *
     * @param version      verze stromu
     * @param descItemType typ atributu, který chceme zkopírovat
     * @param level        uzel, na který nastavíme hodnoty ze staršího bratra
     * @return vytvořené hodnoty
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrDescItem> copyOlderSiblingAttribute(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                                       final RulItemType descItemType,
                                                       final ArrLevel level,
                                                       final ArrChange change) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        Assert.notNull(level, "Musí být vyplněno");

        isValidAndOpenVersion(version);

        Set<RulItemType> typeSet = new HashSet<>();
        typeSet.add(descItemType);

        ArrLevel olderSibling = levelRepository.findOlderSibling(level, version.getLockChange());
        if (olderSibling == null) {
        	throw new BusinessException("Node does not have older sibling, levelId="+level.getLevelId(), BaseCode.INVALID_STATE);
        }

        // Read source data
        List<ArrDescItem> siblingDescItems = descItemRepository.findOpenByNodeAndTypes(olderSibling.getNode(), typeSet);

        // Delete old values for these items
        List<ArrDescItem> nodeDescItems = descItemRepository.findOpenByNodeAndTypes(level.getNode(), typeSet);
        List<ArrDescItem> deletedDescItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeDescItems)) {
            List<Integer> descItemObjectIdsDeleted = new ArrayList<>(nodeDescItems.size());
            for (ArrDescItem nodeDescItem : nodeDescItems) {
             	ArrDescItem deletedItem = descriptionItemService.deleteDescriptionItem(nodeDescItem, version, change, false);
                descItemObjectIdsDeleted.add(deletedItem.getDescItemObjectId());
                deletedDescItems.add(deletedItem);
            }
            arrangementCacheService.deleteDescItems(nodeDescItems.get(0).getNodeId(), descItemObjectIdsDeleted);
        }

        final List<ArrDescItem> newDescItems = descriptionItemService
                    .copyDescItemWithDataToNode(level.getNode(), siblingDescItems, change, version);
        // update cache
        arrangementCacheService.createDescItems(level.getNodeId(), newDescItems);

        descItemRepository.flush();

        eventNotificationService.publishEvent(EventFactory
                .createIdInVersionEvent(EventType.COPY_OLDER_SIBLING_ATTRIBUTE, version, level.getNode().getNodeId()));

        // revalidate node
        ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(level.getNode().getNodeId()),
        		NodeTypeOperation.SAVE_DESC_ITEM, newDescItems, null, deletedDescItems);

        // Should it be taken from cache?
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
        Assert.notNull(level, "Musí být vyplněno");
        Assert.notNull(version, "Verze AS musí být vyplněna");
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
     * @param version     verze AP
     * @param nodeId      id uzlu pod kterým se má hledat, může být null
     * @param searchValue hledaná hodnota
     * @param depth       hloubka v jaké se hledá pod předaným nodeId
     * @return seznam id uzlů které vyhovují parametrům
     */
    public Set<Integer> findNodeIdsByFulltext(final ArrFundVersion version, final Integer nodeId, final String searchValue, final Depth depth) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(depth, "Hloubka musí být vyplněna");

        ArrChange lockChange = version.getLockChange();
        Integer lockChangeId = lockChange == null ? null : lockChange.getChangeId();
        Integer fundId = version.getFund().getFundId();

        Set<Integer> nodeIds = nodeRepository.findByFulltextAndVersionLockChangeId(searchValue, fundId, lockChangeId);

        Set<Integer> versionNodeIds = levelTreeCacheService.getAllNodeIdsByVersionAndParent(version, nodeId, depth);
        versionNodeIds.retainAll(nodeIds);

        return versionNodeIds;
    }

    /**
     * Vyhledání id nodů podle lucene dotazu. např: +specification:*čís* -fulltextValue:ddd
     *
     * @param version     verze AP
     * @param nodeId      id uzlu pod kterým se má hledat, může být null
     * @param searchValue lucene dotaz (např: +specification:*čís* -fulltextValue:ddd)
     * @param depth hloubka v jaké se hledá pod předaným nodeId
     * @return seznam id uzlů které vyhovují parametrům
     * @throws InvalidQueryException neplatný lucene dotaz
     */
    public Set<Integer> findNodeIdsByLuceneQuery(final ArrFundVersion version, final Integer nodeId,
                                                 final String searchValue, final Depth depth) throws InvalidQueryException {
        Assert.notNull(version, "Verze AS musí být vyplněna");

        if (StringUtils.isBlank(searchValue)) {
            return levelTreeCacheService.getAllNodeIdsByVersionAndParent(version, nodeId, depth);
        }

        ArrChange lockChange = version.getLockChange();
        Integer lockChangeId = lockChange == null ? null : lockChange.getChangeId();
        Integer fundId = version.getFund().getFundId();

        Set<Integer> nodeIds = nodeRepository.findByLuceneQueryAndVersionLockChangeId(searchValue, fundId, lockChangeId);

        Set<Integer> versionNodeIds = levelTreeCacheService.getAllNodeIdsByVersionAndParent(version, nodeId, depth);
        versionNodeIds.retainAll(nodeIds);

        return versionNodeIds;
    }

    /**
     * Vyhledání id nodů podle parametrů.
     *
     * @param version     verze AP
     * @param nodeId      id uzlu pod kterým se má hledat, může být null
     * @param searchParams parametry pro rozšířené vyhledávání
     * @param depth       hloubka v jaké se hledá pod předaným nodeId
     *
     * @return množina id uzlů které vyhovují parametrům
     */
    public Set<Integer> findNodeIdsBySearchParams(final ArrFundVersion version, final Integer nodeId,
            final List<SearchParam> searchParams, final Depth depth) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(depth, "Musí být vyplněno");
        Assert.notEmpty(searchParams, "Musí existovat vyhledávající parametr");

        ArrChange lockChange = version.getLockChange();
        Integer lockChangeId = lockChange == null ? null : lockChange.getChangeId();
        Integer fundId = version.getFund().getFundId();

        Set<Integer> nodeIds = nodeRepository.findBySearchParamsAndVersionLockChangeId(searchParams, fundId, lockChangeId);

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
        if (version == null) {
            throw new BusinessException("Verze neexistuje", ArrangementCode.FUND_VERSION_NOT_FOUND);
        }
        if (version.getLockChange() != null) {
            throw new BusinessException("Aktuální verze je zamčená", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
    }


    /**
     * Provede uzamčení nodu (zvýšení verze uzlu)
     *
     * @param lockNode uzamykaný node
     * @param version  verze stromu, do které patří uzel
     * @param change   změna
     * @return level nodu
     */
    public ArrLevel lockNode(final ArrNode lockNode, final ArrFundVersion version, final ArrChange change) {
        ArrLevel lockLevel = levelRepository
                .findNodeInRootTreeByNodeId(lockNode, version.getRootNode(), version.getLockChange());
        Assert.notNull(lockLevel, "Musí být vyplněno");
        ArrNode staticNodeDb = lockLevel.getNode();
        lockNode(staticNodeDb, lockNode, change);

        return lockLevel;
    }

    /**
     * Provede uzamčení nodu (zvýšení verze uzlu)
     *
     * @param dbNode   odpovídající uzel načtený z db
     * @param lockNode uzamykaný node
     * @param change
     * @return level nodu
     */
    public ArrNode lockNode(final ArrNode dbNode, final ArrNode lockNode, final ArrChange change) {
        Assert.notNull(dbNode, "Musí být vyplněno");
        Assert.notNull(lockNode, "Musí být vyplněno");

        lockNode.setUuid(dbNode.getUuid());
        lockNode.setLastUpdate(change.getChangeDate());
        lockNode.setFund(dbNode.getFund());

        return nodeRepository.save(lockNode);
    }

    /**
     * Načte počet chyb verze archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     * @return počet chyb
     */
    public Integer getVersionErrorCount(final ArrFundVersion fundVersion) {
        return nodeConformityInfoRepository.findCountByFundVersionAndState(fundVersion, State.ERR);
    }

    public List<ArrNodeConformity> findConformityErrors(final ArrFundVersion fundVersion, final Boolean showAll) {
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
     * @return seznam id nodů a jejich rodičů
     */
    public List<TreeNodeFulltext> createTreeNodeFulltextList(final Set<Integer> nodeIds, final ArrFundVersion version) {
        Assert.notNull(nodeIds, "Musí být vyplněno");
        Assert.notNull(version, "Verze AS musí být vyplněna");

        Map<Integer, TreeNodeClient> parentIdTreeNodeClientMap = levelTreeCacheService.findParentsWithTitles(nodeIds, version);

        List<Integer> sortedNodeIds = levelTreeCacheService.sortNodesByTreePosition(nodeIds, version);

        List<TreeNodeFulltext> result = new ArrayList<>(sortedNodeIds.size());
        for (Integer nodeId : sortedNodeIds) {
            TreeNodeFulltext treeNodeFulltext = new TreeNodeFulltext();

            treeNodeFulltext.setNodeId(nodeId);
            treeNodeFulltext.setParent(parentIdTreeNodeClientMap.get(nodeId));

            result.add(treeNodeFulltext);
        }

        return result;
    }

    public List<VersionValidationItem> createVersionValidationItems(final List<ArrNodeConformity> validationErrors, final ArrFundVersion version) {
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

    @AuthMethod(permission = {UsrPermission.Permission.REG_SCOPE_WR_ALL, UsrPermission.Permission.REG_SCOPE_WR})
    public ArrFundRegisterScope addScopeToFund(final ArrFund fund,
                                               @AuthParam(type = AuthParam.Type.SCOPE) final RegScope scope) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(scope, "Scope musí být vyplněn");

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
     * @param version verze AP
     * @param node    uzel
     * @param around  velikost okolí
     * @return okolní uzly (včetně původního)
     */
    public List<ArrNode> findSiblingsAroundOfNode(final ArrFundVersion version, final ArrNode node, final Integer around) {
        List<ArrNode> siblings = nodeRepository.findNodesByDirection(node, version, RelatedNodeDirection.ALL_SIBLINGS);

        if (around <= 0) {
            throw new SystemException("Velikost okolí musí být minimálně 1");
        }

        //požadujeme pouze nejbližšího sourozence před a za objektem
        int nodeIndex = siblings.indexOf(node);
        List<ArrNode> result = new ArrayList<>();

        int min = nodeIndex - around;
        int max = nodeIndex + around;

        min = min < 0 ? 0 : min;
        max = max > siblings.size() - 1 ? siblings.size() - 1 : max;

        for (int i = min; i <= max; i++) {
            result.add(siblings.get(i));
        }

        return result;
    }

    /**
     * Vrací výsek chybných JP podle indexů.
     *
     * @param fundVersion verze archivní pomůcky
     * @param indexFrom   od indexu
     * @param indexTo     do indexu
     * @return
     */
    public ArrangementController.ValidationItems getValidationNodes(final ArrFundVersion fundVersion,
                                                                    final Integer indexFrom,
                                                                    final Integer indexTo) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(indexFrom, "Musí být vyplněno");
        Assert.notNull(indexTo, "Musí být vyplněno");

        if (indexFrom < 0 || indexFrom >= indexTo) {
            throw new IllegalArgumentException("Neplatné vstupní parametry");
        }

        List<Integer> nodes = createErrorTree(fundVersion, null);
        int countAll = nodes.size();

        int count = indexTo - indexFrom;
        Iterable<Integer> nodeIds = Iterables.limit(Iterables.skip(nodes, indexFrom), count);
        Set<Integer> nodesLimited = new HashSet<>(count);
        for (Integer integer : nodeIds) {
            nodesLimited.add(integer);
        }

        return new ArrangementController.ValidationItems(levelTreeCacheService.getNodeItemsWithParents(nodesLimited, fundVersion), countAll);
    }

    private List<Integer> createErrorTree(final ArrFundVersion fundVersion, @Nullable final FoundNode foundNode) {
        Integer rootNodeId = fundVersion.getRootNode().getNodeId();
        TreeNode rootTreeNode = null;
        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(fundVersion);

        for (TreeNode treeNode : versionTreeCache.values()) {
            if (treeNode.getId().equals(rootNodeId)) {
                rootTreeNode = treeNode;
                break;
            }
        }

        if (rootTreeNode == null) {
            throw new ObjectNotFoundException("Nenalezen kořen stromu ve verzi " + fundVersion.getFundVersionId(),
                    ArrangementCode.NODE_NOT_FOUND).set("id", rootNodeId);
        }

        List<UIVisiblePolicy> policies = visiblePolicyRepository.findByFund(fundVersion.getFund());

        // nodeId / policyTypeId / visible
        Map<Integer, Map<Integer, Boolean>> policiesMap = new HashMap<>();

        for (UIVisiblePolicy policy : policies) {
            Integer nodeId = policy.getNode().getNodeId();
            Integer policyTypeId = policy.getPolicyType().getPolicyTypeId();
            Boolean visible = policy.getVisible();
            Map<Integer, Boolean> policyTypes = policiesMap.get(nodeId);
            if (policyTypes == null) {
                policyTypes = new HashMap<>();
                policiesMap.put(nodeId, policyTypes);
            }
            policyTypes.put(policyTypeId, visible);
        }

        List<ArrNodeConformityError> errors = nodeConformityErrorsRepository.findErrorsByFundVersion(fundVersion);
        List<ArrNodeConformityMissing> missings = nodeConformityMissingRepository.findMissingsByFundVersion(fundVersion);

        // nodeId / policyTypeIds
        Map<Integer, Set<Integer>> nodeProblemsMap = new HashMap<>();

        for (ArrNodeConformityError error : errors) {
            Integer nodeId = error.getNodeConformity().getNode().getNodeId();
            Integer policyTypeId = error.getPolicyType().getPolicyTypeId();
            addNodeProblem(nodeProblemsMap, nodeId, policyTypeId);
        }

        for (ArrNodeConformityMissing missing : missings) {
            Integer nodeId = missing.getNodeConformity().getNode().getNodeId();
            Integer policyTypeId = missing.getPolicyType().getPolicyTypeId();
            addNodeProblem(nodeProblemsMap, nodeId, policyTypeId);
        }

        List<Integer> nodes = new ArrayList<>();

        Map<Integer, Boolean> defaultPolicy = policyService.getPolicyTypes(fundVersion).stream()
                .collect(Collectors.toMap(i -> i.getPolicyTypeId(), i -> true));
        recursiveAddNodes(nodes, rootTreeNode, defaultPolicy, policiesMap, nodeProblemsMap, foundNode);
        return nodes;
    }

    /**
     * Vyhledává chyby po/před zvolené JP.
     *
     * @param fundVersion verze archivního fondu
     * @param nodeId      identifikátor uzlu, od kterého vyhledávám
     * @param direction   směr hledání
     * @return výsledek hledání
     */
    public ArrangementController.ValidationItems findErrorNode(final ArrFundVersion fundVersion,
                                                               final Integer nodeId,
                                                               final Integer direction) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(direction, "Směr musí být vyplněn");
        Assert.isTrue(direction != 0, "Směr musí být rozdílný od 0");

        FoundNode foundNode = new FoundNode(nodeId);
        List<Integer> nodes = createErrorTree(fundVersion, foundNode);
        int countAll = nodes.size();

        if (foundNode.getNode() == null || countAll == 0) {
            return new ArrangementController.ValidationItems(null, countAll);
        } else {
            Set<Integer> nodesLimited = new HashSet<>();

            Integer index = direction > 0 ? foundNode.getIndex() + 1 : foundNode.getIndex() - 1;

            if (!nodes.contains(nodeId) && direction > 0) {
                index--;
            }

            if (index < 0) {
                index = nodes.size() - 1;
            } else if (index > nodes.size() - 1) {
                index = 0;
            }
            nodesLimited.add(nodes.get(index));
            List<NodeItemWithParent> nodeItemsWithParents = levelTreeCacheService.getNodeItemsWithParents(nodesLimited, fundVersion);

            return new ArrangementController.ValidationItems(nodeItemsWithParents, countAll);
        }
    }

    /**
     * Přidání typu chyby k nodu
     *
     * @param nodeProblemsMap typy problémů
     * @param nodeId          identifikátor uzlu
     * @param policyTypeId    identifikátor typu
     */
    private void addNodeProblem(final Map<Integer, Set<Integer>> nodeProblemsMap, final Integer nodeId, final Integer policyTypeId) {
        Set<Integer> policyTypeIds = nodeProblemsMap.get(nodeId);
        if (policyTypeIds == null) {
            policyTypeIds = new HashSet<>();
            nodeProblemsMap.put(nodeId, policyTypeIds);
        }
        policyTypeIds.add(policyTypeId);
    }

    /**
     * Vyhledání hodnot atributů k požadovaným jednotkám popisu.
     *
     * @param nodeIds identifikátory jednotky popisu
     * @return mapa - klíč identifikátor jed. popisu, hodnota - seznam hodnot atributu
     */
    public Map<Integer, List<ArrDescItem>> findByNodes(final Collection<Integer> nodeIds) {
        return descItemRepository.findByNodes(nodeIds);
    }

    /**
     * Rekurzivní procházení a přidávání JP s chybou
     *
     * @param nodeIds           seznam chybných JP (postupně přidávaný)
     * @param treeNode          aktuálně procházená JP
     * @param parentPolicyTypes viditelnost rodiče
     * @param policiesMap       mapa všech nastavení nad JP
     * @param nodeProblemsMap   mapa všech chybných JP podle typu
     * @param foundNode         pomocný objekt pro vyhledání další chyby
     */
    private void recursiveAddNodes(final List<Integer> nodeIds,
                                   final TreeNode treeNode,
                                   final Map<Integer, Boolean> parentPolicyTypes,
                                   final Map<Integer, Map<Integer, Boolean>> policiesMap,
                                   final Map<Integer, Set<Integer>> nodeProblemsMap,
                                   final FoundNode foundNode) {
        Integer nodeId = treeNode.getId();

        // výchozí status pro přidání uzlu mezi chybové
        Boolean status = false;

        // pokud se vyhledává další, předchozí chyba
        // porovnává se procházenými uzly a připadně označí
        if (foundNode != null && foundNode.getNodeId().equals(nodeId)) {
            // index, na kterém je další/předchozí chyba
            foundNode.setIndex(nodeIds.size());
            foundNode.setNode(treeNode);
        }

        // set typů problémů
        Set<Integer> nodeProblems = nodeProblemsMap.get(nodeId);

        // výchozí nastavení pro daný uzel
        Map<Integer, Boolean> policyTypes = policiesMap.get(nodeId);

        // aktuální nastavení pro daný uzel (výchozí se bere z rodiče)
        Map<Integer, Boolean> nodePolicyTypes = parentPolicyTypes;

        // pokud existují nějaké změny pro tento uzel, provede se přepsání podle nastavní nad uzlem
        if (policyTypes != null) {
            nodePolicyTypes = new HashMap<>(parentPolicyTypes);
            for (Integer policyTypeId : nodePolicyTypes.keySet()) {
                nodePolicyTypes.put(policyTypeId, policyTypes.get(policyTypeId));
            }
        }

        // prohledávají se všechny chyby a pokud nalezneme alespoň jednu neskrytou, označíme uzel jako chybový
        if (nodeProblems != null) {
            for (Map.Entry<Integer, Boolean> entry : nodePolicyTypes.entrySet()) {
                Boolean value = entry.getValue();
                if (value != null && value.equals(true) && nodeProblems.contains(entry.getKey())) {
                    status = true;
                    break;
                }
            }
        }

        // přidat uzel mezi chybové?
        if (status) {
            nodeIds.add(nodeId);
        }

        // rekurzivní procházení potomků
        if (treeNode.getChilds() != null) {
            for (TreeNode node : treeNode.getChilds()) {
                recursiveAddNodes(nodeIds, node, nodePolicyTypes, policiesMap, nodeProblemsMap, foundNode);
            }
        }
    }

    /**
     * Detekuje, zdali nad předanými uzly byly provedené změny po předaných změnách.
     *
     * @param nodes           seznam nodů od kterých prohledáváme
     * @param changes         seznam změn vůči kterým porovnáváme
     * @param includeParents  zahrnout rodiče k root?
     * @param includeChildren zahrnout podstrom?
     * @return mapa, zdali bylo něco upraveno podle změn
     */
    public Map<ArrChange, Boolean> detectChangeNodes(final Set<ArrNode> nodes,
                                                     final Set<ArrChange> changes,
                                                     final boolean includeParents,
                                                     final boolean includeChildren) {

        Map<ArrChange, Boolean> result = new HashMap<>();

        for (ArrChange change : changes) {
            for (ArrNode node : nodes) {

                if (includeChildren) {
                    List<Integer> nodeIdsSubtree = levelRepository.findNodeIdsSubtree(node, change);
                    if (nodeIdsSubtree.size() > 0) {
                        result.put(change, true);
                        break;
                    }
                }

                if (includeParents) {
                    List<Integer> nodeIdsParent = levelRepository.findNodeIdsParent(node, change);
                    if (nodeIdsParent.size() > 0) {
                        result.put(change, true);
                        break;
                    }
                }

                result.put(change, false);
            }
        }

        return result;
    }

    /**
     * Pomocná třída pro vyhledávání další chyby.
     */
    private class FoundNode {

        private Integer nodeId;

        private TreeNode node;

        private Integer index;

        public FoundNode(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public TreeNode getNode() {
            return node;
        }

        public void setNode(final TreeNode node) {
            this.node = node;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(final Integer index) {
            this.index = index;
        }

        public Integer getNodeId() {
            return nodeId;
        }
    }
}
