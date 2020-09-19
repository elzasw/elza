package cz.tacr.elza.service;

import static cz.tacr.elza.repository.ExceptionThrow.fund;
import static cz.tacr.elza.repository.ExceptionThrow.node;
import static cz.tacr.elza.repository.ExceptionThrow.refTemplate;
import static cz.tacr.elza.repository.ExceptionThrow.refTemplateMapType;
import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Iterables;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.ArrangementController.TreeNodeFulltext;
import cz.tacr.elza.controller.ArrangementController.VersionValidationItem;
import cz.tacr.elza.controller.vo.ArrFundFulltextResult;
import cz.tacr.elza.controller.vo.ArrRefTemplateEditVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateMapSpecVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateMapTypeVO;
import cz.tacr.elza.controller.vo.ArrRefTemplateVO;
import cz.tacr.elza.controller.vo.FindFundsResult;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.filter.SearchParam;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.core.security.AuthParam.Type;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundRegisterScope;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformity.State;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrRefTemplate;
import cz.tacr.elza.domain.ArrRefTemplateMapSpec;
import cz.tacr.elza.domain.ArrRefTemplateMapType;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UIVisiblePolicy;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ConcurrentUpdateException;
import cz.tacr.elza.exception.InvalidQueryException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ArrRefTemplateMapSpecRepository;
import cz.tacr.elza.repository.ArrRefTemplateMapTypeRepository;
import cz.tacr.elza.repository.ArrRefTemplateRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRegisterScopeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeConformityErrorRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;
import cz.tacr.elza.repository.NodeConformityRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.VisiblePolicyRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.arrangement.DeleteFundAction;
import cz.tacr.elza.service.arrangement.DeleteFundHistoryAction;
import cz.tacr.elza.service.arrangement.MultiplItemChangeContext;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventFund;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Main arrangement service.
 * <p>
 * This service can be used by controller. All operations are checking
 * permissions.
 */
@Service
@Configuration
public class ArrangementService {

    private static final Pattern UUID_PATTERN = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");

    @Autowired
    private FundRegisterScopeRepository faRegisterRepository;

    //TODO smazat závislost až bude DescItemService
    @Autowired
    protected FundRegisterScopeRepository fundRegisterScopeRepository;
    final private static Logger logger = LoggerFactory.getLogger(ArrangementService.class);
    @Autowired
    private LevelTreeCacheService levelTreeCacheService;
    @Autowired
    private UserService userService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private EventNotificationService eventNotificationService;
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
    private ArrRefTemplateRepository refTemplateRepository;
    @Autowired
    private ArrRefTemplateMapTypeRepository refTemplateMapTypeRepository;
    @Autowired
    private ArrRefTemplateMapSpecRepository refTemplateMapSpecRepository;
    @Autowired
    private NodeConformityRepository nodeConformityInfoRepository;
    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;
    @Autowired
    private NodeConformityErrorRepository nodeConformityErrorsRepository;
    @Autowired
    private DescItemRepository descItemRepository;
    @Autowired
    private AccessPointService accessPointService;
    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private VisiblePolicyRepository visiblePolicyRepository;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private ApplicationContext appCtx;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    DaoRepository daoRepository;
    
    @Autowired
    DaoService daoService;

    public static final String UNDEFINED = "Nezjištěno";

    /**
     * Načtení verze na základě id.
     *
     * @param fundVersionId id souboru
     * @return konkrétní verze
     * @throws ObjectNotFoundException objekt nenalezen
     */
    public ArrFundVersion getFundVersion(@NotNull Integer fundVersionId) {
        return fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));
    }

    /**
     * Načtení souboru na základě id.
     *
     * @param fundId id souboru
     * @return konkrétní AP
     * @throws ObjectNotFoundException objekt nenalezen
     */
    public ArrFund getFund(@NotNull Integer fundId) {
        return fundRepository.findById(fundId)
                .orElseThrow(fund(fundId));
    }

    /**
     * Try to find fund by string
     *
     * @param fundIdentifier
     * @return fund, throw exception if not found
     */
    public ArrFund getFundByString(String fundIdentifier) {
        logger.debug("Looking for fund: {}", fundIdentifier);
        // try to find by uuid
        if (fundIdentifier.length() == 36) {
            ArrFund fund = fundRepository.findByRootNodeUuid(fundIdentifier);
            if (fund != null) {
                logger.debug("Found by UUID as {}", fund.getFundId());
                return fund;
            }
        }
        // try to find by internal code
        ArrFund fund = fundRepository.findByInternalCode(fundIdentifier);
        if (fund != null) {
            logger.debug("Found by internal code as {}", fund.getFundId());
            return fund;
        }

        // try to find by id
        try {
            Integer id = Integer.valueOf(fundIdentifier);
            return getFund(id);
        } catch (NumberFormatException nfe) {
            throw new ObjectNotFoundException("Nebyl nalezen AS s ID=" + fundIdentifier, ArrangementCode.FUND_NOT_FOUND)
                    .setId(fundIdentifier);
        }
    }

    /**
     * Načtení uzlu na základě id.
     *
     * @param nodeId
     *            id souboru
     * @return konkrétní uzel
     * @throws ObjectNotFoundException
     *             objekt nenalezen
     */
    public ArrNode getNode(@NotNull Integer nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(node(nodeId));
    }

    /**
     * Vytvoření archivního souboru.
     *
     * @param name         název
     * @param ruleSet      pravidla
     * @param change       změna
     * @param uuid         uuid
     * @param internalCode iterní kód
     * @param institution  instituce
     * @return vytvořený arch. soubor
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_CREATE})
    public ArrFund createFund(final String name,
                              final RulRuleSet ruleSet,
                              final ArrChange change,
                              final String uuid,
                              final String internalCode,
                              final ParInstitution institution,
                              final Integer fundNumber,
                              final String unitdate,
                              final String mark) {
        ArrFund fund = createFund(name, internalCode, institution,fundNumber, unitdate, mark);

        eventNotificationService
                .publishEvent(EventFactory.createIdEvent(EventType.FUND_CREATE, fund.getFundId()));

        //        Assert.isTrue(ruleSet.equals(arrangementType.getRuleSet()));

        ArrLevel rootLevel = createRootLevel(change, uuid, fund);
        createVersion(change, fund, ruleSet, rootLevel.getNode());

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
                              final List<ApScope> scopes) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(ruleSet, "Pravidla musí být vyplněna");

        ArrFund originalFund = fundRepository.findById(fund.getFundId())
                .orElseThrow(fund(fund.getFundId()));

        originalFund.setName(fund.getName());
        originalFund.setInternalCode(fund.getInternalCode());
        originalFund.setInstitution(fund.getInstitution());
        originalFund.setFundNumber(fund.getFundNumber());
        originalFund.setUnitdate(fund.getUnitdate());
        originalFund.setMark(fund.getMark());

        fundRepository.save(originalFund);

        ArrFundVersion openVersion = getOpenVersionByFundId(originalFund.getFundId());
        if (!ruleSet.equals(openVersion.getRuleSet())) {
            openVersion.setRuleSet(ruleSet);
            fundVersionRepository.save(openVersion);

            ruleService.conformityInfoAll(openVersion);
        }

        if (scopes != null) {
            for (ApScope scope : scopes) {
                if (scope.getScopeId() == null) {
                    scope.setCode(StringUtils.upperCase(Normalizer.normalize(StringUtils
                            .replace(StringUtils.substring(scope.getName(), 0, 50).trim(), " ", "_"), Normalizer.Form.NFD)));
                    scopeRepository.save(scope);
                }
            }
            synchApScopes(originalFund, scopes);
        }

        eventNotificationService
                .publishEvent(EventFactory.createIdEvent(EventType.FUND_UPDATE, originalFund.getFundId()));

        return originalFund;
    }

    /**
     * Pokud se jedná o typ osoby group, dojde k synchronizaci identifikátorů osoby. CRUD.
     */
    private void synchApScopes(final ArrFund fund,
                               final Collection<ApScope> newApScopes) {
        Assert.notNull(fund, "AS musí být vyplněn");

        Map<Integer, ArrFundRegisterScope> dbIdentifiersMap = ElzaTools
                .createEntityMap(faRegisterRepository.findByFund(fund), i -> i.getScope().getScopeId());
        Set<ArrFundRegisterScope> removeScopes = new HashSet<>(dbIdentifiersMap.values());

        for (ApScope newScope : newApScopes) {
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

        faRegisterRepository.deleteAll(removeScopes);
    }

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní
     * aktuální datum a čas. Pro root
     * vytvoří atributy podle scénáře.
     *
     * @param name
     *            název archivní pomůcky
     * @param ruleSet
     *            id pravidel podle kterých se vytváří popis
     * @param internalCode
     *            interní označení
     * @param institution
     * @param fundNumber
     * @param unitdate
     * @param mark
     * @param uuid
     * @param scopes
     *            Seznam oblastí, může být null
     * @return nová archivní pomůcka
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_CREATE})
    public ArrFund createFundWithScenario(final String name,
                                          final RulRuleSet ruleSet,
                                          final String internalCode,
                                          final ParInstitution institution,
                                          final Integer fundNumber,
                                          final String unitdate,
                                          final String mark,
                                          String uuid,
                                          List<ApScope> scopes) {
        ArrChange change = createChange(ArrChange.Type.CREATE_AS);

        if (uuid == null || uuid.isEmpty()) {
            uuid = generateUuid();
        }

        ArrFund fund = createFund(name, ruleSet, change, uuid, internalCode,
                                  institution, fundNumber, unitdate, mark);

        if (scopes != null) {
            for (ApScope scope : scopes) {
                addScopeToFund(fund, scope);
            }
        }

        ArrFundVersion version = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(fund.getFundId());

        ArrLevel rootLevel = levelRepository.findByNode(version.getRootNode(), version.getLockChange());

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

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_CREATE})
    public ArrFund createFund(final String name,
                              final String internalCode,
                              final ParInstitution institution,
                              final Integer fundNumber,
                              final String unitdate,
                              final String mark) {
        ArrFund fund = new ArrFund();
        fund.setCreateDate(LocalDateTime.now());
        fund.setName(name);
        fund.setInternalCode(internalCode);
        fund.setInstitution(institution);
        fund.setFundNumber(fundNumber);
        fund.setUnitdate(unitdate);
        fund.setMark(mark);
        return fundRepository.save(fund);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_VER_WR, UsrPermission.Permission.FUND_ADMIN})
    public ArrFundVersion createVersion(final ArrChange createChange,
                                        @AuthParam(type = Type.FUND) final ArrFund fund,
                                        final RulRuleSet ruleSet,
                                        final ArrNode rootNode) {
        ArrFundVersion version = new ArrFundVersion();
        version.setCreateChange(createChange);
        version.setFund(fund);
        version.setRuleSet(ruleSet);
        version.setRootNode(rootNode);
        return fundVersionRepository.save(version);
    }

    private ArrLevel createRootLevel(final ArrChange createChange,
                                     final String uuid,
                                     final ArrFund fund) {
        ArrLevel level = new ArrLevel();
        level.setPosition(1);
        level.setCreateChange(createChange);
        level.setNodeParent(null);
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
        node.setLastUpdate(createChange.getChangeDate().toLocalDateTime());
        node.setUuid(generateUuid());
        node.setFund(fund);
        nodeRepository.save(node);
        nodeCacheService.createEmptyNode(node);
        return node;
    }

    public ArrNode createNodeSimple(final ArrFund fund, final String uuid, final ArrChange createChange) {
        ArrNode node = new ArrNode();
        node.setLastUpdate(createChange.getChangeDate().toLocalDateTime());
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
        node.setLastUpdate(change.getChangeDate().toLocalDateTime());
        node.setUuid(uuid);
        node.setFund(fund);
        nodeRepository.save(node);
        nodeCacheService.createEmptyNode(node);
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
        change.setChangeDate(OffsetDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
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
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
    public void deleteFund(final Integer fundId) {
        Validate.notNull(fundId, "Nebyl vyplněn identifikátor AS");

        DeleteFundAction dfa = appCtx.getBean(DeleteFundAction.class);
        dfa.run(fundId);
    }

    /**
     * Smaže historii archivní pomůcky se zadaným id.
     *
     * @param fundId id archivní pomůcky
     */
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
    public void deleteFundHistory(final Integer fundId) {
        Validate.notNull(fundId, "Nebyl vyplněn identifikátor AS");

        DeleteFundHistoryAction dfa = appCtx.getBean(DeleteFundHistoryAction.class);
        dfa.run(fundId);
    }

    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     * - spustí přepočet stavů uzlů pro novou verzi
     *
     * @param version   verze, která se má uzavřít
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN, UsrPermission.Permission.FUND_VER_WR})
    public ArrFundVersion approveVersion(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version) {
        Assert.notNull(version, "Verze AS musí být vyplněna");

        ArrFund fund = version.getFund();

        if (!fundRepository.existsById(fund.getFundId())) {
            throw new ObjectNotFoundException("AS s ID=" + fund.getFundId() + " nebylo nalezeno", ArrangementCode.FUND_NOT_FOUND).set("id", fund.getFundId());
        }

        if (version.getLockChange() != null) {
            throw new BusinessException("Verze AS s ID=" + fund.getFundId() + " je již uzavřena", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (asyncRequestService.isFundBulkActionRunning(version)) {
            throw new BusinessException("Nelze uzavřít verzi AS s ID=" + fund.getFundId() + ", protože běží hromadná akce", ArrangementCode.VERSION_CANNOT_CLOSE_ACTION);
        }

        if (asyncRequestService.isFundNodeRunning(version)) {
            throw new BusinessException("Nelze uzavřít verzi AS s ID=" + fund.getFundId() + ", protože běží validace", ArrangementCode.VERSION_CANNOT_CLOSE_VALIDATION);
        }

        ArrChange change = createChange(null);
        version.setLockChange(change);
        fundVersionRepository.save(version);

        ArrFundVersion newVersion = createVersion(change, fund, version.getRuleSet(), version.getRootNode());
        ruleService.conformityInfoAll(newVersion);

        eventNotificationService.publishEvent(
                new EventFund(EventType.APPROVE_VERSION, version.getFund().getFundId(), version.getFundVersionId()));

        return newVersion;
    }

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param fundId id archivní pomůcky
     * @return verze
     */
    public ArrFundVersion getOpenVersionByFundId(final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
        return fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
    }

    /**
     * Načte neuzavřené verze archivních pomůcek.
     *
     * @param fundIds ids archivních pomůcek
     * @return verze
     * @deprecated use cz.tacr.elza.service.ArrangementInternalService#getOpenVersionsByFundIds(java.util.Collection)
     */
    @Deprecated
    public List<ArrFundVersion> getOpenVersionsByFundIds(final Collection<Integer> fundIds) {
        return arrangementInternalService.getOpenVersionsByFundIds(fundIds);
    }

    /**
     * Kaskádově smaže všechny levely od počátečního
     *
     * @param baselevel počáteční level
     * @param deleteChange záznam o provedených změnách
     * @param allDeletedLevels list všech levelů, které se budou mazat
     * @param deleteLevelsWithAttachedDao povolit nebo zakázat mazání úrovně s objektem dao
     * @return
     */
    public ArrLevel deleteLevelCascade(final ArrLevel baselevel, final ArrChange deleteChange,
                                       List<ArrLevel> allDeletedLevels, final boolean deleteLevelsWithAttachedDao) {

        for (ArrLevel childLevel : levelRepository
                .findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(baselevel.getNode())) {
            deleteLevelCascade(childLevel, deleteChange, allDeletedLevels, deleteLevelsWithAttachedDao);
        }

        ArrNode node = baselevel.getNode();
        ArrFund fund = baselevel.getNode().getFund();
        ArrFundVersion fundVersion = getOpenVersionByFundId(fund.getFundId());

        // check if connected Dao(type=Level) exists
        if (!deleteLevelsWithAttachedDao && daoRepository.existsDaoByNodeAndDaoTypeIsLevel(node.getNodeId())) {
            throw new SystemException("Uzel " + node.getNodeId() + " má připojený objekt dao typu LEVEL")
                    .set("nodeId", node.getNodeId());
        }

        for (ArrDescItem descItem : descItemRepository.findByNodeAndDeleteChangeIsNull(baselevel.getNode())) {
            descItem.setDeleteChange(deleteChange);
            descItemRepository.save(descItem);
        }

        daoService.deleteDaoLinkByNode(fundVersion, node);

        List<ArrDescItem> arrDescItemList = descItemRepository.findByUriDataNode(node);

        arrDescItemList = arrDescItemList.stream().map(i -> {
            em.detach(i);
            return (ArrDescItem) HibernateUtils.unproxy(i);
        }).collect(Collectors.toList());


        for (ArrDescItem arrDescItem : arrDescItemList) {
            //pokud se item bude mazat, není potřeba u něj předělávat UriRef
            if (!allDeletedLevels.contains(levelRepository.findByNodeIdAndDeleteChangeIsNull(arrDescItem.getNodeId()))) {
                ArrDataUriRef arrDataUriRef = new ArrDataUriRef((ArrDataUriRef) arrDescItem.getData());
                arrDataUriRef.setDataId(null);
                arrDataUriRef.setArrNode(null);
                arrDataUriRef.setDeletingProcess(true);
                arrDescItem.setData(arrDataUriRef);
                descriptionItemService.updateDescriptionItem(arrDescItem, fundVersion, deleteChange);
            }
        }
        return deleteLevelInner(baselevel, deleteChange);
    }

    private ArrLevel deleteLevelInner(final ArrLevel level, final ArrChange deleteChange) {
        Assert.notNull(level, "Musí být vyplněno");

        ArrNode node = level.getNode();
        node.setLastUpdate(deleteChange.getChangeDate().toLocalDateTime());
        nodeRepository.save(node);

        level.setDeleteChange(deleteChange);
        return levelRepository.saveAndFlush(level);
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
    public Integer getNextDescItemObjectId() {
        return arrangementInternalService.getNextDescItemObjectId();
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
            throw new BusinessException("Node does not have older sibling, levelId=" + level.getLevelId(), BaseCode.INVALID_STATE);
        }

        // Read source data
        List<ArrDescItem> siblingDescItems = descItemRepository.findOpenByNodeAndTypes(olderSibling.getNode(), typeSet);

        MultiplItemChangeContext changeContext = descriptionItemService.createChangeContext(version.getFundVersionId());

        // Delete old values for these items
        // ? Can we use descriptionItemService.deleteDescriptionItemsByType
        List<ArrDescItem> nodeDescItems = descItemRepository.findOpenByNodeAndTypes(level.getNode(), typeSet);
        if (CollectionUtils.isNotEmpty(nodeDescItems)) {
            for (ArrDescItem descItem : nodeDescItems) {
                descriptionItemService.deleteDescriptionItem(descItem, version,
                        change, false, changeContext);
            }
        }

        final List<ArrDescItem> newDescItems = descriptionItemService
                .copyDescItemWithDataToNode(level.getNode(), siblingDescItems, change, version,
                        changeContext);

        changeContext.flush();

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

        return level.getCreateChange().getChangeId() < lockChange && levelDeleteChange >= lockChange;
    }

    /**
     * Vyhledání id nodů podle hodnoty atributu.
     *
     * @param fundList id fondů, do kterých uzly patří
     * @return seznam id uzlů které vyhovují parametrům
     */
    public List<ArrFundFulltextResult> findFundsByFulltext(final String searchValue, final Collection<ArrFund> fundList) {

        List<ArrFundToNodeList> fundToNodeList = nodeRepository.findFundIdsByFulltext(searchValue, fundList);
        fundFulltextSession().set(fundToNodeList);

        List<ArrFundFulltextResult> resultList = new ArrayList<>();

        if (!fundToNodeList.isEmpty()) {
            List<Integer> fundIds = fundList.stream().map(ArrFund::getFundId).collect(Collectors.toList());
            Map<Integer, ArrFundVersion> fundIdVersionsMap = getOpenVersionsByFundIds(fundIds).stream()
                    .collect(Collectors.toMap(ArrFundVersion::getFundId, Function.identity()));
            Map<Integer, ArrFund> fundMap = fundList.stream().collect(Collectors.toMap(ArrFund::getFundId, Function.identity()));

            for (ArrFundToNodeList fundCount : fundToNodeList) {
                ArrFundFulltextResult result = new ArrFundFulltextResult();
                ArrFund fund = fundMap.get(fundCount.getFundId());
                ArrFundVersion fundVersion = fundIdVersionsMap.get(fundCount.getFundId());
                result.setName(fund.getName());
                result.setId(fundCount.getFundId());
                result.setCount(fundCount.getNodeCount());
                result.setFundVersionId(fundVersion.getFundVersionId());
                resultList.add(result);
            }
        }
        return resultList;
    }

    protected ArrFundToNodeList getFundToNodeListFromSession(Integer fundId) {
        Holder<List<ArrFundToNodeList>> holder = fundFulltextSession();
        List<ArrFundToNodeList> list = holder.get();
        if (list == null) {
            throw new SystemException("Nenalezena session data");
        }
        for (ArrFundToNodeList fundToNodeList : list) {
            if (fundId.equals(fundToNodeList.getFundId())) {
                return fundToNodeList;
            }
        }
        return null;
    }

    public List<TreeNodeVO> getNodeListByFulltext(Integer fundId) {
        ArrFundToNodeList fundToNodeList = getFundToNodeListFromSession(fundId);
        if (fundToNodeList != null) {
            List<Integer> nodeIdList = fundToNodeList.getNodeIdList();
            if (nodeIdList.size() > 20) {
                nodeIdList = nodeIdList.subList(0, 20);
            }
            ArrFundVersion fundVersion = getOpenVersionByFundId(fundToNodeList.getFundId());
            List<Integer> sortedList = levelTreeCacheService.sortNodesByTreePosition(nodeIdList, fundVersion);
            return levelTreeCacheService.getNodesByIds(sortedList, fundVersion.getFundVersionId());
        }
        return Collections.emptyList();
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
     * @param depth       hloubka v jaké se hledá pod předaným nodeId
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

    public ArrFundVersion getFundVersionById(final Integer fundVersionId) {
        return fundVersionRepository.getOneCheckExist(fundVersionId);
    }

    /**
     * Vyhledání id nodů podle parametrů.
     *
     * @param version      verze AP
     * @param nodeId       id uzlu pod kterým se má hledat, může být null
     * @param searchParams parametry pro rozšířené vyhledávání
     * @param depth        hloubka v jaké se hledá pod předaným nodeId
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
        ArrLevel lockLevel = levelRepository.findByNode(lockNode, version.getLockChange());
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
        lockNode.setLastUpdate(change.getChangeDate().toLocalDateTime());
        lockNode.setFund(dbNode.getFund());

        return nodeRepository.save(lockNode);
    }

    /**
     * Finds level for fund version by node. Node will be locked during transaction.
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public ArrLevel lockLevel(ArrNodeVO nodeVO, ArrFundVersion fundVersion) {
        Integer nodeId = nodeVO.getId();
        Assert.notNull(nodeId, "Node id must be set");
        ArrNode node = em.getReference(ArrNode.class, nodeId);
        em.lock(node, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        return levelRepository.findByNode(node, fundVersion.getLockChange());
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
    public List<TreeNodeFulltext> createTreeNodeFulltextList(final Collection<Integer> nodeIds,
                                                             final ArrFundVersion version) {
        Assert.notNull(nodeIds, "Musí být vyplněno");
        Assert.notNull(version, "Verze AS musí být vyplněna");

        Map<Integer, TreeNodeVO> parentIdTreeNodeClientMap = levelTreeCacheService.findParentsWithTitles(nodeIds, version);

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

    public List<VersionValidationItem> createVersionValidationItems(final List<ArrNodeConformity> validationErrors,
                                                                    final ArrFundVersion version) {
        Map<Integer, String> validations = new LinkedHashMap<>();
        for (ArrNodeConformity conformity : validationErrors) {
            String description = validations.get(conformity.getNode().getNodeId());

            List<String> descriptions = new LinkedList<>();
            if (description != null) {
                descriptions.add(description);
            }

            for (ArrNodeConformityError error : conformity.getErrorConformity()) {
                descriptions.add(error.getDescription());
            }

            for (ArrNodeConformityMissing missing : conformity.getMissingConformity()) {
                descriptions.add(missing.getDescription());
            }

            description = StringUtils.join(descriptions, " ");

            validations.put(conformity.getNode().getNodeId(), description);
        }

        Map<Integer, TreeNodeVO> parentIdTreeNodeClientMap = levelTreeCacheService.findParentsWithTitles(validations.keySet(), version);

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

    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public ArrFundRegisterScope addScopeToFund(final ArrFund fund,
                                               @AuthParam(type = AuthParam.Type.SCOPE) final ApScope scope) {
        Assert.notNull(fund, "AS musí být vyplněn");
        Assert.notNull(scope, "Scope musí být vyplněn");

        ArrFundRegisterScope faRegisterScope = fundRegisterScopeRepository.findByFundAndScope(fund, scope);
        if (faRegisterScope != null) {
            logger.info("Vazbe mezi archivním souborem " + fund + " a třídou rejstříku " + scope + " již existuje.");
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
        List<Integer> nodesLimited = new ArrayList<>(count);
        for (Integer integer : nodeIds) {
            nodesLimited.add(integer);
        }

        return new ArrangementController.ValidationItems(levelTreeCacheService.getNodeItemsWithParents(nodesLimited, fundVersion), countAll);
    }

    public TreeNode getRootTreeNode(@NotNull ArrFundVersion fundVersion) {

        Integer rootNodeId = fundVersion.getRootNode().getNodeId();
        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(fundVersion);

        TreeNode rootTreeNode = null;
        for (TreeNode treeNode : versionTreeCache.values()) {
            if (treeNode.getId().equals(rootNodeId)) {
                rootTreeNode = treeNode;
                break;
            }
        }

        if (rootTreeNode == null) {
            throw new ObjectNotFoundException("Nenalezen kořen stromu ve verzi " + fundVersion.getFundVersionId(),
                    ArrangementCode.NODE_NOT_FOUND).setId(rootNodeId);
        }

        return rootTreeNode;
    }

    private List<Integer> createErrorTree(final ArrFundVersion fundVersion, @Nullable final FoundNode foundNode) {

        TreeNode rootTreeNode = getRootTreeNode(fundVersion);

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
            Integer index = direction > 0 ? foundNode.getIndex() + 1 : foundNode.getIndex() - 1;

            if (!nodes.contains(nodeId) && direction > 0) {
                index--;
            }

            if (index < 0) {
                index = nodes.size() - 1;
            } else if (index > nodes.size() - 1) {
                index = 0;
            }
            List<Integer> nodesLimited = new ArrayList<>(1);
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
     * Provede přidání do front uzly, které nemají záznam v arr_node_conformity. Obvykle to jsou
     * uzly, které se validovaly během ukončení aplikačního serveru.
     * <p>
     * Metoda je pouštěna po startu aplikačního serveru.
     */
    @Transactional(value = Transactional.TxType.MANDATORY)
    public void startNodeValidation(boolean onStart) {
        // TransactionTemplate tmpl = new TransactionTemplate(txManager);
        Map<Integer, ArrFundVersion> fundVersionMap = new HashMap<>();
        Map<Integer, List<ArrNode>> fundNodesMap = new HashMap<>();

        // zjištění všech uzlů, které nemají validaci
        List<ArrNode> nodes = nodeRepository.findByNodeConformityIsNull();


        // roztřídění podle AF
        for (ArrNode node : nodes) {
            Integer fundId = node.getFund().getFundId();
            List<ArrNode> addedNodes = fundNodesMap.get(fundId);
            if (addedNodes == null) {
                addedNodes = new LinkedList<>();
                fundNodesMap.put(fundId, addedNodes);
            }
            addedNodes.add(node);
        }

        // načtení otevřených verzí AF
        List<ArrFundVersion> openVersions = fundVersionRepository.findAllOpenVersion();

        // vytvoření převodní mapy "id AF->verze AF"
        for (ArrFundVersion openVersion : openVersions) {
            fundVersionMap.put(openVersion.getFund().getFundId(), openVersion);
        }

        // projde všechny fondy
        for (Map.Entry<Integer, List<ArrNode>> entry : fundNodesMap.entrySet()) {
            Integer fundId = entry.getKey();
            ArrFundVersion version = fundVersionMap.get(fundId);

            if (version == null) {
                logger.error("Pro AF s ID=" + fundId + " byly nalezeny nezvalidované uzly (" + entry.getValue()
                        + "), které nejsou z otevřené verze AF");
                continue;
            }

            // přidávání nodů je nutné dělat ve vlastní transakci (podle updateInfoForNodesAfterCommit)
            logger.info("Přidání " + entry.getValue().size() + " uzlů do fronty pro zvalidování");
            if(onStart) {
                asyncRequestService.enqueue(version, entry.getValue());
            }

        }
    }

    public ParInstitution getInstitution(String identifier) {
        ParInstitution institution;
        if(checkInstitutionUUID(identifier)) {
            institution = institutionRepository.findByAccessPointUUID(identifier);
        } else {
            institution = institutionRepository.findByInternalCode(identifier);
        }
        return institution;
    }

    private boolean checkInstitutionUUID(String institutionIdentifier) {
        return UUID_PATTERN.matcher(institutionIdentifier).matches();

    }

    public ApScope getApScope(String s) {
        ApScope entity = scopeRepository.findByCode(s);
        if(entity == null) {
            entity = new ApScope();
            entity.setCode(s);
            entity.setName(s);
        }
        return entity;
    }


    /**
     * @return vrací session uživatele
     */
    @Bean
    @Scope("session")
    public Holder<List<ArrFundToNodeList>> fundFulltextSession() {
        return new Holder<>();
    }

    public ArrNode findNodeByUuid(final String nodeUuid) {
        return nodeRepository.findOneByUuid(nodeUuid);
    }

    public FindFundsResult findFundsByFullTextInsIdentifier(List<ArrFund> fundList, String fulltext, String institutionIdentifier, Integer max, Integer from) {
        FindFundsResult fundsResult = new FindFundsResult();
        List<ArrFundToNodeList> fundToNodeList = nodeRepository.findFundIdsByFulltext(fulltext, fundList, max, from);
        fundFulltextSession().set(fundToNodeList);

        if (!fundToNodeList.isEmpty()) {
            List<Integer> fundIds = fundList.stream().map(ArrFund::getFundId).collect(Collectors.toList());
            Map<Integer, ArrFundVersion> fundIdVersionsMap = getOpenVersionsByFundIds(fundIds).stream()
                    .collect(Collectors.toMap(ArrFundVersion::getFundId, Function.identity()));
            Map<Integer, ArrFund> fundMap = fundList.stream().collect(Collectors.toMap(ArrFund::getFundId, Function.identity()));

            for (ArrFundToNodeList fundCount : fundToNodeList) {
                Fund result = new Fund();
                ArrFund fund = fundMap.get(fundCount.getFundId());
                result.setName(fund.getName());
                result.setId(fundCount.getFundId());
                fundsResult.addFundsItem(result);
            }
            fundsResult.setTotalCount(fundToNodeList.size());
        }
        return fundsResult;
    }

    public ArrRefTemplateVO createRefTemplate(Integer fundId) {
        ArrFund fund = getFund(fundId);
        ArrRefTemplate refTemplate = createRefTemplate(fund);

        ArrRefTemplateVO arrRefTemplateVO = new ArrRefTemplateVO();
        arrRefTemplateVO.setId(refTemplate.getRefTemplateId());
        arrRefTemplateVO.setName(refTemplate.getName());
        return arrRefTemplateVO;
    }

    private ArrRefTemplate createRefTemplate(ArrFund fund) {
        ArrRefTemplate arrRefTemplate = new ArrRefTemplate();
        arrRefTemplate.setFund(fund);
        arrRefTemplate.setName("Šablona");
        return refTemplateRepository.save(arrRefTemplate);
    }

    public void deleteRefTemplate(Integer templateId) {
        ArrRefTemplate refTemplate = refTemplateRepository.findById(templateId)
                .orElseThrow(refTemplate(templateId));
        List<ArrRefTemplateMapType> refTemplateMapTypes = refTemplateMapTypeRepository.findByRefTemplate(refTemplate);
        if (CollectionUtils.isNotEmpty(refTemplateMapTypes)) {
            refTemplateMapSpecRepository.deleteByRefTemplateMapTypes(refTemplateMapTypes);
            refTemplateMapTypeRepository.deleteAll(refTemplateMapTypes);
        }
        refTemplateRepository.delete(refTemplate);
    }

    public ArrRefTemplateVO updateRefTemplate(Integer templateId, ArrRefTemplateEditVO refTemplateEditVO) {
        ArrRefTemplate rt = refTemplateRepository.findById(templateId)
                .orElseThrow(refTemplate(templateId));
        ArrRefTemplate refTemplate = updateRefTemplate(rt, refTemplateEditVO);
        List<ArrRefTemplateMapType> refTemplateMapTypes = refTemplateMapTypeRepository.findByRefTemplate(refTemplate);
        List<ArrRefTemplateMapSpec> refTemplateMapSpecs = refTemplateMapSpecRepository.findByRefTemplate(refTemplate);

        return createRefTemplateVO(refTemplate, refTemplateMapTypes, refTemplateMapSpecs);
    }

    public ArrRefTemplate updateRefTemplate(ArrRefTemplate refTemplate, ArrRefTemplateEditVO refTemplateEditVo) {
        StaticDataProvider sdp = staticDataService.getData();
        refTemplate.setName(refTemplateEditVo.getName());
        refTemplate.setItemNodeRef(sdp.getItemType(refTemplateEditVo.getItemTypeId()));
        return refTemplateRepository.save(refTemplate);
    }

    private ArrRefTemplateVO createRefTemplateVO(ArrRefTemplate refTemplate,
                                                 List<ArrRefTemplateMapType> refTemplateMapTypes,
                                                 List<ArrRefTemplateMapSpec> refTemplateMapSpecs) {
        ArrRefTemplateVO arrRefTemplateVO = new ArrRefTemplateVO();
        arrRefTemplateVO.setId(refTemplate.getRefTemplateId());
        arrRefTemplateVO.setName(refTemplate.getName());
        arrRefTemplateVO.setItemTypeId(refTemplate.getItemNodeRef() != null ? refTemplate.getItemNodeRef().getItemTypeId() : null);
        arrRefTemplateVO.setRefTemplateMapTypeVOList(createRefTemplateMapTypeVOList(refTemplateMapTypes, refTemplateMapSpecs));
        return arrRefTemplateVO;
    }

    private List<ArrRefTemplateMapTypeVO> createRefTemplateMapTypeVOList(List<ArrRefTemplateMapType> refTemplateMapTypes,
                                                                         List<ArrRefTemplateMapSpec> refTemplateMapSpecs) {
        List<ArrRefTemplateMapTypeVO> refTemplateMapTypeVOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(refTemplateMapTypes)) {
            for (ArrRefTemplateMapType refTemplateMapType : refTemplateMapTypes) {
                refTemplateMapTypeVOList.add(createRefTemplateMapTypeVO(refTemplateMapType, refTemplateMapSpecs));
            }
        }
        return refTemplateMapTypeVOList;
    }

    private ArrRefTemplateMapTypeVO createRefTemplateMapTypeVO(ArrRefTemplateMapType refTemplateMapType, List<ArrRefTemplateMapSpec> refTemplateMapSpecs) {
        ArrRefTemplateMapTypeVO refTemplateMapTypeVO = new ArrRefTemplateMapTypeVO();
        refTemplateMapTypeVO.setId(refTemplateMapType.getRefTemplateMapTypeId());
        refTemplateMapTypeVO.setFromItemTypeId(refTemplateMapType.getFormItemType() != null ? refTemplateMapType.getFormItemType().getItemTypeId() : null);
        refTemplateMapTypeVO.setToItemTypeId(refTemplateMapType.getToItemType() != null ? refTemplateMapType.getToItemType().getItemTypeId() : null);
        refTemplateMapTypeVO.setFromParentLevel(refTemplateMapType.getFromParentLevel());
        refTemplateMapTypeVO.setMapAllSpec(refTemplateMapType.getMapAllSpec());
        refTemplateMapTypeVO.setRefTemplateMapSpecVOList(createRefTemplateMapSpecVOList(refTemplateMapType, refTemplateMapSpecs));

        return refTemplateMapTypeVO;
    }

    private List<ArrRefTemplateMapSpecVO> createRefTemplateMapSpecVOList(ArrRefTemplateMapType refTemplateMapType, List<ArrRefTemplateMapSpec> refTemplateMapSpecs) {
        List<ArrRefTemplateMapSpecVO> refTemplateMapSpecVOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(refTemplateMapSpecs)) {
            for (ArrRefTemplateMapSpec refTemplateMapSpec : refTemplateMapSpecs) {
                if (refTemplateMapSpec.getRefTemplateMapType().getRefTemplateMapTypeId().equals(refTemplateMapType.getRefTemplateMapTypeId())) {
                    refTemplateMapSpecVOList.add(createRefTemplateMapSpecVO(refTemplateMapSpec));
                }
            }
        }
        return refTemplateMapSpecVOList;
    }

    private ArrRefTemplateMapSpecVO createRefTemplateMapSpecVO(ArrRefTemplateMapSpec refTemplateMapSpec) {
        ArrRefTemplateMapSpecVO refTemplateMapSpecVO = new ArrRefTemplateMapSpecVO();
        refTemplateMapSpecVO.setId(refTemplateMapSpec.getRefTemplateMapSpecId());
        refTemplateMapSpecVO.setFromItemSpecId(refTemplateMapSpec.getFromItemSpec() != null ? refTemplateMapSpec.getFromItemSpec().getItemSpecId() : null);
        refTemplateMapSpecVO.setToItemSpecId(refTemplateMapSpec.getToItemSpec() != null ? refTemplateMapSpec.getToItemSpec().getItemSpecId() : null);
        return refTemplateMapSpecVO;
    }

    public List<ArrRefTemplateVO> getRefTemplates(Integer fundId) {
        ArrFund fund = getFund(fundId);
        List<ArrRefTemplateVO> refTemplateVOList = new ArrayList<>();
        List<ArrRefTemplate> refTemplateList = refTemplateRepository.findByFund(fund);
        if (CollectionUtils.isNotEmpty(refTemplateList)) {
            Map<Integer, List<ArrRefTemplateMapType>> refTemplateMapTypeMap = refTemplateMapTypeRepository.findByRefTemplates(refTemplateList).stream()
                    .collect(Collectors.groupingBy(r -> r.getRefTemplate().getRefTemplateId()));
            Map<Integer, List<ArrRefTemplateMapSpec>> refTemplateMapSpecMap = refTemplateMapSpecRepository.findByRefTemplates(refTemplateList).stream()
                    .collect(Collectors.groupingBy(r -> r.getRefTemplateMapType().getRefTemplate().getRefTemplateId()));

            for (ArrRefTemplate refTemplate : refTemplateList) {
                List<ArrRefTemplateMapType> refTemplateMapTypes = refTemplateMapTypeMap.getOrDefault(refTemplate.getRefTemplateId(), new ArrayList<>());
                List<ArrRefTemplateMapSpec> refTemplateMapSpecs = refTemplateMapSpecMap.getOrDefault(refTemplate.getRefTemplateId(), new ArrayList<>());
                refTemplateVOList.add(createRefTemplateVO(refTemplate, refTemplateMapTypes, refTemplateMapSpecs));
            }
        }
        return refTemplateVOList;
    }

    public ArrRefTemplateMapTypeVO createRefTemplateMapType(Integer templateId, ArrRefTemplateMapTypeVO refTemplateMapTypeFormVO) {
        StaticDataProvider sdp = staticDataService.getData();
        ArrRefTemplate refTemplate = refTemplateRepository.findById(templateId)
                .orElseThrow(refTemplate(templateId));

        ArrRefTemplateMapType refTemplateMapType = new ArrRefTemplateMapType();
        refTemplateMapType.setRefTemplate(refTemplate);
        refTemplateMapType.setFormItemType(sdp.getItemType(refTemplateMapTypeFormVO.getFromItemTypeId()));
        refTemplateMapType.setToItemType(sdp.getItemType(refTemplateMapTypeFormVO.getToItemTypeId()));
        refTemplateMapType.setFromParentLevel(refTemplateMapTypeFormVO.getFromParentLevel() != null ? refTemplateMapTypeFormVO.getFromParentLevel() : false);
        refTemplateMapType.setMapAllSpec(refTemplateMapTypeFormVO.getMapAllSpec() != null ? refTemplateMapTypeFormVO.getMapAllSpec() : false);
        refTemplateMapTypeRepository.save(refTemplateMapType);

        List<ArrRefTemplateMapSpec> refTemplateMapSpecs = createRefTemplateMapSpecs(refTemplateMapType, refTemplateMapTypeFormVO.getRefTemplateMapSpecVOList(), sdp);
        return createRefTemplateMapTypeVO(refTemplateMapType, refTemplateMapSpecs);
    }

    private List<ArrRefTemplateMapSpec> createRefTemplateMapSpecs(ArrRefTemplateMapType refTemplateMapType, List<ArrRefTemplateMapSpecVO> refTemplateMapSpecVOList, StaticDataProvider sdp) {
        List<ArrRefTemplateMapSpec> refTemplateMapSpecs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(refTemplateMapSpecVOList)) {
            for (ArrRefTemplateMapSpecVO refTemplateMapSpecVO : refTemplateMapSpecVOList) {

                ArrRefTemplateMapSpec refTemplateMapSpec = new ArrRefTemplateMapSpec();
                refTemplateMapSpec.setRefTemplateMapType(refTemplateMapType);
                if (refTemplateMapSpecVO.getFromItemSpecId() != null) {
                    refTemplateMapSpec.setFromItemSpec(sdp.getItemSpecById(refTemplateMapSpecVO.getFromItemSpecId()));
                }
                if (refTemplateMapSpecVO.getToItemSpecId() != null) {
                    refTemplateMapSpec.setToItemSpec(sdp.getItemSpecById(refTemplateMapSpecVO.getToItemSpecId()));
                }
                refTemplateMapSpecs.add(refTemplateMapSpec);
            }
            refTemplateMapSpecRepository.saveAll(refTemplateMapSpecs);
        }
        return refTemplateMapSpecs;
    }

    public ArrRefTemplateMapTypeVO updateRefTemplateMapType(Integer templateId, Integer mapTypeId, ArrRefTemplateMapTypeVO refTemplateMapTypeFormVO) {
        StaticDataProvider sdp = staticDataService.getData();
        ArrRefTemplateMapType refTemplateMapType = refTemplateMapTypeRepository.findById(mapTypeId)
                .orElseThrow(refTemplateMapType(mapTypeId));
        refTemplateMapSpecRepository.deleteByRefTemplateMapType(refTemplateMapType);

        refTemplateMapType.setFormItemType(sdp.getItemType(refTemplateMapTypeFormVO.getFromItemTypeId()));
        refTemplateMapType.setToItemType(sdp.getItemType(refTemplateMapTypeFormVO.getToItemTypeId()));
        refTemplateMapType.setFromParentLevel(refTemplateMapTypeFormVO.getFromParentLevel() != null ? refTemplateMapTypeFormVO.getFromParentLevel() : false);
        refTemplateMapType.setMapAllSpec(refTemplateMapTypeFormVO.getMapAllSpec() != null ? refTemplateMapTypeFormVO.getMapAllSpec() : false);

        List<ArrRefTemplateMapSpec> refTemplateMapSpecs = createRefTemplateMapSpecs(refTemplateMapType, refTemplateMapTypeFormVO.getRefTemplateMapSpecVOList(), sdp);
        return createRefTemplateMapTypeVO(refTemplateMapType, refTemplateMapSpecs);
    }

    public void deleteRefTemplateMapType(Integer templateId, Integer mapTypeId) {
        ArrRefTemplateMapType refTemplateMapType = refTemplateMapTypeRepository.findById(mapTypeId)
                .orElseThrow(refTemplateMapType(mapTypeId));
        refTemplateMapSpecRepository.deleteByRefTemplateMapType(refTemplateMapType);
        refTemplateMapTypeRepository.delete(refTemplateMapType);
    }

    public void synchronizeNodes(final Integer nodeId,
                                 final Integer nodeVersion,
                                 final Boolean childrenNodes,
                                 final ArrChange change) {
        ArrNode node = getNode(nodeId);
        if (node != null) {
            List<ArrDescItem> nodeItems = descItemRepository.findByNodeAndDeleteChangeIsNull(node);
            if (CollectionUtils.isNotEmpty(nodeItems)) {
                for (ArrDescItem descItem : nodeItems) {
                    synchronizeNodes(descItem, nodeId, nodeVersion, change);
                }
            }
            if (childrenNodes) {
                List<ArrLevel> childrenLevels = levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(node);
                if (CollectionUtils.isNotEmpty(childrenLevels)) {
                    for (ArrLevel childrenLevel : childrenLevels) {
                        ArrNode childrenNode = childrenLevel.getNode();
                        synchronizeNodes(childrenNode.getNodeId(), childrenNode.getVersion(), true, change);
                    }
                }
            }
        }
    }

    public void synchronizeNodes(final ArrDescItem descItem, final Integer nodeId, final Integer nodeVersion, ArrChange change) {
        if (descItem.getData().getDataType().getCode().equals(DataType.URI_REF.getCode())) {
            ArrDataUriRef dataUriRef = (ArrDataUriRef) descItem.getData();

            if (dataUriRef.getRefTemplate() != null && dataUriRef.getArrNode() != null &&
                    dataUriRef.getRefTemplate().getItemNodeRef().getItemTypeId().equals(descItem.getItemType().getItemTypeId())) {
                ArrNode node = getNode(nodeId);
                Map<Integer, List<ArrDescItem>> nodeItemMap = descItemRepository.findByNodeAndDeleteChangeIsNull(node).stream()
                        .collect(Collectors.groupingBy(ArrItem::getItemTypeId));

                ArrNode sourceNode = dataUriRef.getArrNode();
                Map<Integer, List<ArrDescItem>> sourceNodeItemMap = descItemRepository.findByNodeAndDeleteChangeIsNull(sourceNode).stream()
                        .collect(Collectors.groupingBy(ArrItem::getItemTypeId));

                if (change == null) {
                    change = createChange(ArrChange.Type.SYNCHRONIZE_JP);
                }

                synchronizeNodes(node, nodeVersion, nodeItemMap, sourceNode, sourceNodeItemMap, dataUriRef.getRefTemplate(), change);
            }
        }
    }

    private void synchronizeNodes(final ArrNode node,
                                  final Integer nodeVersion,
                                  final Map<Integer, List<ArrDescItem>> nodeItemMap,
                                  final ArrNode sourceNode,
                                  final Map<Integer, List<ArrDescItem>> sourceNodeItemMap,
                                  final ArrRefTemplate refTemplate,
                                  final ArrChange change) {
        List<ArrRefTemplateMapType> refTemplateMapTypes = refTemplateMapTypeRepository.findByRefTemplate(refTemplate);
        Map<Integer, List<ArrRefTemplateMapSpec>> refTemplateMapSpecMap = refTemplateMapSpecRepository.findByRefTemplate(refTemplate).stream()
                .collect(Collectors.groupingBy(s -> s.getRefTemplateMapType().getRefTemplateMapTypeId()));

        if (CollectionUtils.isNotEmpty(refTemplateMapTypes)) {
            for (ArrRefTemplateMapType refTemplateMapType : refTemplateMapTypes) {
                List<ArrRefTemplateMapSpec> refTemplateMapSpecs = refTemplateMapSpecMap.getOrDefault(refTemplateMapType.getRefTemplateMapTypeId(), new ArrayList<>());

                checkMapTypesDataType(refTemplateMapType.getFormItemType(), refTemplateMapType.getToItemType(), refTemplate.getRefTemplateId());

                List<ArrDescItem> sourceNodeItems = sourceNodeItemMap.getOrDefault(refTemplateMapType.getFormItemType().getItemTypeId(), new ArrayList<>());

                if (CollectionUtils.isEmpty(sourceNodeItems) && refTemplateMapType.getFromParentLevel()) {
                    sourceNodeItems = findDescItemsFromParentLevelByItemType(sourceNode, refTemplateMapType.getFormItemType());
                }

                if (CollectionUtils.isNotEmpty(sourceNodeItems)) {
                    List<ArrDescItem> nodeItems = nodeItemMap.getOrDefault(refTemplateMapType.getToItemType().getItemTypeId(), new ArrayList<>());
                    ArrFundVersion fundVersion = getOpenVersionByFundId(node.getFundId());

                    if (CollectionUtils.isEmpty(nodeItems)) {
                        // vytvoření nových itemů
                        List<ArrDescItem> newItems = descriptionItemService.createDescriptionItems(sourceNodeItems, refTemplateMapType, refTemplateMapSpecs);
                        descriptionItemService.createDescriptionItems(newItems, node, fundVersion, change);
                    } else if (sourceNodeItems.size() > 1 || nodeItems.size() > 1) {
                        // smazání a vytvoření
                        descriptionItemService.deleteDescriptionItems(nodeItems, node, fundVersion, change, false);

                        List<ArrDescItem> newItems = descriptionItemService.createDescriptionItems(sourceNodeItems, refTemplateMapType, refTemplateMapSpecs);
                        descriptionItemService.createDescriptionItems(newItems, node, fundVersion, change);
                    } else {
                        // update
                        ArrDescItem sourceItem = sourceNodeItems.get(0);
                        ArrDescItem targetItem = nodeItems.get(0);
                        descriptionItemService.setSpecification(sourceItem, targetItem, refTemplateMapType, refTemplateMapSpecs);
                        descriptionItemService.updateDescriptionItemData(sourceItem, targetItem, refTemplate.getRefTemplateId());
                        descriptionItemService.updateDescriptionItem(targetItem, fundVersion, change);
                    }
                }
            }
        }
    }

    private List<ArrDescItem> findDescItemsFromParentLevelByItemType(ArrNode sourceNode, RulItemType formItemType) {
        ArrLevel sourceLevel = levelRepository.findByNodeAndDeleteChangeIsNull(sourceNode);
        ArrNode parentNode = sourceLevel.getNodeParent();
        if (parentNode != null) {
            List<ArrDescItem> parentItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndItemTypeId(parentNode, formItemType.getItemTypeId());
            if (CollectionUtils.isNotEmpty(parentItems)) {
                return parentItems;
            } else {
                return findDescItemsFromParentLevelByItemType(parentNode, formItemType);
            }
        }
        return null;
    }

    private void checkMapTypesDataType(final RulItemType fromItemType, final RulItemType toItemType, final Integer templateId) {
        DataType fromDataType = DataType.fromCode(fromItemType.getDataType().getCode());
        DataType toDataType = DataType.fromCode(toItemType.getDataType().getCode());

        if (fromDataType == null) {
            throw new IllegalArgumentException("Neznámý kód datového typu " + fromItemType.getDataType().getCode() + " u item typu id " + fromItemType.getItemTypeId());
        }

        if (toDataType == null) {
            throw new IllegalArgumentException("Neznámý kód datového typu " + toItemType.getDataType().getCode() + " u item typu id " + toItemType.getItemTypeId());
        }

        if (fromDataType != toDataType) {
            boolean error = false;
            switch (fromDataType) {
                case UNITID:
                case UNITDATE:
                case DATE:
                case INT:
                case DECIMAL:
                case ENUM:
                    error = (toDataType != DataType.STRING && toDataType != DataType.TEXT);
                    break;
                case STRING:
                    error = toDataType != DataType.TEXT;
                    break;
                default:
                    error = true;
                    break;
            }

            if (error) {
                throw new IllegalArgumentException("Nekompatibilní datové typy " + fromDataType.getName() + "->" +
                        toDataType.getName() + " pro synchronizaci přes šablonu id " + templateId);
            }
        }
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public Set<Integer> findLinkedNodes(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                        final Integer nodeId) {
        return nodeRepository.findLinkedNodes(nodeId);
    }

    public static class Holder<T> {

        private T object;

        public T get() {
            return object;
        }

        public void set(T object) {
            this.object = object;
        }
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
