package cz.tacr.elza.bulkaction;


import static cz.tacr.elza.repository.ExceptionThrow.bulkAction;
import static cz.tacr.elza.repository.ExceptionThrow.node;
import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.BulkAction;
import cz.tacr.elza.controller.vo.FundsActionGroup;
import cz.tacr.elza.controller.vo.FundsActionGroupResult;
import cz.tacr.elza.controller.vo.FundsActionSkipped;
import cz.tacr.elza.controller.vo.MultiFundActionResult;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrBulkActionNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrFundsChange;
import cz.tacr.elza.domain.FundsChangeType;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.FundsChangeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;

/**
 * Serviska pro obsluhu hromadných akcí.
 *
 */
@Service
public class BulkActionService {

    /**
     * Počet hromadných akcí v listu MAX_BULK_ACTIONS_LIST.
     */
    public static final int MAX_BULK_ACTIONS_LIST = 100;

    public static final String PERSISTENT_SORT_CODE = "PERZISTENTNI_RAZENI";

    /**
     * Horní mez počtu fondů zpracovaných při výběru "všechny odpovídající filtru".
     */
    public static final int MAX_MULTI_FUND_FUNDS = 5000;

    /** Důvod přeskočení: fond nemá otevřenou verzi. */
    public static final String SKIP_NO_OPEN_VERSION = "NO_OPEN_VERSION";

    /** Důvod přeskočení: akce nepatří do pravidel fondu. */
    public static final String SKIP_ACTION_NOT_IN_RULESET = "ACTION_NOT_IN_RULESET";

    /** Velikost bloku pro dávkové načítání fondů/verzí přes IN (ochrana před obřím IN i N+1). */
    public static final int FUND_BATCH_SIZE = 1000;

    private final static Logger logger = LoggerFactory.getLogger(BulkActionService.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ApplicationContext appCtx;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private BulkActionConfigManager bulkActionConfigManager;

    @Autowired
    private BulkActionRunRepository bulkActionRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private BulkActionNodeRepository bulkActionNodeRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private FundsChangeRepository fundsChangeRepository;

    /**
     * Uložení hromadné akce z klienta
     *
     * @param userId         identfikátor uživatele, který spustil hromadnou akci
     * @param bulkActionCode Kod hromadné akce
     * @param fundVersionId  identifikátor verze archivní pomůcky - je také vstupním uzlem
     * @return objekt hromadné akce
     */
    public ArrBulkActionRun queue(final Integer userId, final String bulkActionCode, final Integer fundVersionId) {
        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));
        return queue(userId, bulkActionCode, fundVersionId, Collections.singletonList(version.getRootNode().getNodeId()), null);
    }

    /**
     * Uložení hromadné akce z klienta
     *
     * @param userId         identfikátor uživatele, který spustil hromadnou akci
     * @param bulkActionCode Kod hromadné akce
     * @param fundVersionId  identifikátor verze archivní pomůcky
     * @param inputNodeIds   seznam vstupních uzlů (podstromů AS)
     * @param runConfig      dodatečné nastavení běhu hromadné akce
     * @return objekt hromadné akce
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    public ArrBulkActionRun queue(final Integer userId,
                                  final String bulkActionCode,
                                  @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                  final List<Integer> inputNodeIds,
                                  final Object runConfig) {
        return queueInternal(userId, bulkActionCode, fundVersionId, inputNodeIds, runConfig, null);
    }

    /**
     * Interní uložení hromadné akce. Volá se z {@link #queue} (jednofondová akce)
     * i z {@link #queueMulti} (vícefondová akce), kde se předává seskupení {@code fundsChange}.
     */
    private ArrBulkActionRun queueInternal(final Integer userId,
                                           final String bulkActionCode,
                                           final Integer fundVersionId,
                                           final List<Integer> inputNodeIds,
                                           final Object runConfig,
                                           final ArrFundsChange fundsChange) {
        Assert.notNull(bulkActionCode, "Musí být vyplněn kód hromadné akce");
        Assert.isTrue(StringUtils.isNotBlank(bulkActionCode), "Musí být vyplněn kód hromadné akce");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notEmpty(inputNodeIds, "Musí být vyplněna alespoň jedna JP");

        ArrBulkActionRun bulkActionRun = new ArrBulkActionRun();

        bulkActionRun.setChange(arrangementInternalService.createChange(ArrChange.Type.BULK_ACTION));
        bulkActionRun.setBulkActionCode(bulkActionCode);
        bulkActionRun.setUserId(userId);
        bulkActionRun.setFundsChange(fundsChange);
        ArrFundVersion arrFundVersion = new ArrFundVersion();

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(fundVersionId);

        RulRuleSet ruleSet = version.getRuleSet();
        List<RulAction> byRulPackage = actionRepository.findByRuleSet(ruleSet);
        if (byRulPackage.stream().noneMatch(i -> i.getCode().equals(bulkActionCode))) {
            throw new BusinessException("Hromadná akce nepatří do stejných pravidel jako pravidla verze AP.", PackageCode.OTHER_PACKAGE)
                    .set("code", bulkActionCode)
                    .set("ruleSet", ruleSet.getCode());
        }

        arrFundVersion.setFundVersionId(fundVersionId);
        bulkActionRun.setFundVersion(arrFundVersion);
        bulkActionRun.setDatePlanned(new Date());

        if (runConfig != null) {
            try {
                bulkActionRun.setConfig(objectMapper.writeValueAsString(runConfig));
            } catch (JsonProcessingException e) {
                throw new SystemException("Problém při převodu na JSON", e, BaseCode.JSON_PARSE);
            }
        }
        storeBulkActionRun(bulkActionRun);

        List<ArrBulkActionNode> bulkActionNodes = new ArrayList<>(inputNodeIds.size());
        for (Integer nodeId : inputNodeIds) {
            ArrBulkActionNode bulkActionNode = new ArrBulkActionNode();
            ArrNode arrNode = nodeRepository.findById(nodeId)
                    .orElseThrow(node(nodeId));
            bulkActionNode.setNode(arrNode);
            bulkActionNode.setBulkActionRun(bulkActionRun);
            bulkActionNodes.add(bulkActionNode);
        }
        bulkActionRun.setArrBulkActionNodes(bulkActionNodes);
        storeBulkActionNodes(bulkActionNodes);
        asyncRequestService.enqueue(bulkActionRun.getFundVersion(), bulkActionRun);
        return bulkActionRun;
    }

    /**
     * Spustí (zařadí) hromadnou akci nad více archivními soubory najednou.
     *
     * Vytvoří jedno seskupení {@link ArrFundsChange} a pro každou platnou verzi fondu
     * naplánuje samostatný běh {@link ArrBulkActionRun} (přes existující asynchronní frontu).
     * Akce běží vždy nad celým fondem (kořenový uzel). Fondy, do jejichž pravidel akce
     * nepatří nebo které nemají otevřenou verzi, jsou přeskočeny a vráceny ve výsledku.
     *
     * @param userId         uživatel spouštějící akci
     * @param bulkActionCode kód hromadné akce
     * @param fundVersionIds identifikátory otevřených verzí fondů
     * @return výsledek se seskupením a přeskočenými fondy
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL})
    public MultiFundActionResult queueMulti(final Integer userId,
                                            final String bulkActionCode,
                                            final List<Integer> fundVersionIds) {
        Assert.isTrue(StringUtils.isNotBlank(bulkActionCode), "Musí být vyplněn kód hromadné akce");
        Assert.notEmpty(fundVersionIds, "Musí být vybrán alespoň jeden archivní soubor");

        // Validace předem (jen čtení), aby přeskočené fondy nevytvářely žádné záznamy.
        // Verze se načítají dávkově (IN po blocích), aby výběr nad mnoha fondy negeneroval N+1 dotazů.
        Map<Integer, ArrFundVersion> versionsById = new HashMap<>();
        for (List<Integer> chunk : ListUtils.partition(fundVersionIds, FUND_BATCH_SIZE)) {
            for (ArrFundVersion version : fundVersionRepository.findAllById(chunk)) {
                versionsById.put(version.getFundVersionId(), version);
            }
        }

        // Příslušnost akce k pravidlům se vyhodnotí jen jednou pro každá pravidla.
        Map<Integer, Boolean> actionInRuleSet = new HashMap<>();
        List<ArrFundVersion> validVersions = new ArrayList<>();
        List<FundsActionSkipped> skipped = new ArrayList<>();
        for (Integer fundVersionId : fundVersionIds) {
            ArrFundVersion version = versionsById.get(fundVersionId);
            if (version == null) {
                skipped.add(new FundsActionSkipped(fundVersionId, SKIP_NO_OPEN_VERSION));
            } else if (!actionInRuleSet.computeIfAbsent(version.getRuleSet().getRuleSetId(),
                    id -> isActionInRuleSet(version.getRuleSet(), bulkActionCode))) {
                skipped.add(new FundsActionSkipped(fundVersionId, SKIP_ACTION_NOT_IN_RULESET));
            } else {
                validVersions.add(version);
            }
        }

        MultiFundActionResult result = new MultiFundActionResult(validVersions.size(), skipped);

        if (validVersions.isEmpty()) {
            return result;
        }

        ArrFundsChange fundsChange = fundsChangeRepository
                .save(ArrFundsChange.create(FundsChangeType.BULK_ACTION, userId, new Date()));

        for (ArrFundVersion version : validVersions) {
            queueInternal(userId, bulkActionCode, version.getFundVersionId(),
                    Collections.singletonList(version.getRootNode().getNodeId()), null, fundsChange);
        }

        result.setFundsChangeId(fundsChange.getFundsChangeId());
        return result;
    }

    /**
     * Rozdělí vybrané archivní soubory podle pravidel (rule set) a ke každé skupině přidá
     * seznam hromadných akcí, které je nad ní možné spustit ve vícefondovém režimu.
     *
     * @param fundIds explicitně vybrané fondy (mají přednost), nebo {@code null}
     * @param search  filtr pro výběr všech odpovídajících fondů (pokud nejsou zadány {@code fundIds})
     * @return skupiny fondů podle pravidel a přeskočené fondy
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL})
    public FundsActionGroupResult groupFundsByRuleSet(final List<Integer> fundIds, final String search) {
        List<Integer> targetFundIds;
        if (!CollectionUtils.isEmpty(fundIds)) {
            targetFundIds = new ArrayList<>(new LinkedHashSet<>(fundIds));
        } else {
            FilteredResult<ArrFund> funds = userService.findFundsWithPermissions(search, 0, MAX_MULTI_FUND_FUNDS);
            if (funds.getTotalCount() > funds.getList().size()) {
                logger.warn("Vícefondová akce: filtru odpovídá {} fondů, použito jen {} (limit {}).",
                        funds.getTotalCount(), funds.getList().size(), MAX_MULTI_FUND_FUNDS);
            }
            targetFundIds = funds.getList().stream().map(fund -> fund.getFundId()).collect(Collectors.toList());
        }

        // Otevřené verze se načítají dávkově (IN po blocích), aby výběr nad mnoha fondy
        // negeneroval N+1 dotazů ani jeden obří IN.
        List<ArrFundVersion> versions = new ArrayList<>();
        for (List<Integer> chunk : ListUtils.partition(targetFundIds, FUND_BATCH_SIZE)) {
            versions.addAll(fundVersionRepository.findByFundIdsAndLockChangeIsNull(chunk));
        }

        Map<Integer, FundsActionGroup> groupsByRuleSet = new LinkedHashMap<>();
        Map<Integer, RulRuleSet> ruleSetById = new HashMap<>();
        Set<Integer> resolvedFundIds = new HashSet<>();

        for (ArrFundVersion version : versions) {
            resolvedFundIds.add(version.getFundId());
            RulRuleSet ruleSet = version.getRuleSet();
            ruleSetById.putIfAbsent(ruleSet.getRuleSetId(), ruleSet);
            FundsActionGroup group = groupsByRuleSet.computeIfAbsent(ruleSet.getRuleSetId(), id ->
                    new FundsActionGroup()
                            .ruleSetId(ruleSet.getRuleSetId())
                            .ruleSetCode(ruleSet.getCode())
                            .ruleSetName(ruleSet.getName())
                            .fundCount(0)
                            .fundVersionIds(new ArrayList<>())
                            .actions(new ArrayList<>()));
            group.getFundVersionIds().add(version.getFundVersionId());
        }

        for (FundsActionGroup group : groupsByRuleSet.values()) {
            group.setFundCount(group.getFundVersionIds().size());
            group.setActions(getMultiFundActions(ruleSetById.get(group.getRuleSetId())));
        }

        // Fondy bez otevřené verze (nevrácené dávkovým dotazem) jsou přeskočeny.
        List<FundsActionSkipped> skipped = new ArrayList<>();
        for (Integer fundId : targetFundIds) {
            if (!resolvedFundIds.contains(fundId)) {
                skipped.add(new FundsActionSkipped(fundId, SKIP_NO_OPEN_VERSION));
            }
        }

        return new FundsActionGroupResult(new ArrayList<>(groupsByRuleSet.values()), skipped);
    }

    /**
     * Vrátí per-fond běhy patřící do jednoho vícefondového seskupení.
     *
     * @param fundsChangeId id seskupení (arr_funds_change)
     * @return list běhů hromadné akce
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL})
    public List<ArrBulkActionRun> getRunsByFundsChange(final Integer fundsChangeId) {
        Assert.notNull(fundsChangeId, "Identifikátor seskupení musí být vyplněn");
        return bulkActionRepository.findByFundsChangeId(fundsChangeId);
    }

    /**
     * Hromadné akce dostupné pro daná pravidla a podporované ve vícefondovém režimu.
     * Akce vyžadující běhovou konfiguraci (např. perzistentní řazení) jsou vynechány.
     */
    private List<BulkAction> getMultiFundActions(final RulRuleSet ruleSet) {
        List<BulkAction> actions = new ArrayList<>();
        for (RulAction action : actionRepository.findByRuleSet(ruleSet)) {
            BulkActionConfig config = bulkActionConfigManager.get(action.getCode());
            if (config == null || PERSISTENT_SORT_CODE.equals(config.getCode())) {
                continue;
            }
            actions.add(new BulkAction()
                    .code(config.getCode())
                    .name(config.getName())
                    .description(config.getDescription())
                    .fastAction(false));
        }
        return actions;
    }

    private boolean isActionInRuleSet(final RulRuleSet ruleSet, final String bulkActionCode) {
        return actionRepository.findByRuleSet(ruleSet).stream()
                .anyMatch(action -> action.getCode().equals(bulkActionCode));
    }

    /**
     * Zvaliduje uzel v nové transakci.
     *
     * @param faLevelId     id uzlu
     * @param fundVersionId id verze
     * @return výsledek validace
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public ArrNodeConformityExt setConformityInfoInNewTransaction(final Integer faLevelId, final Integer fundVersionId) {
        return ruleService.setConformityInfo(faLevelId, fundVersionId);
    }

    /// Operace s repositories, getry atd..

    /**
     * Vrací seznam stavů hromadných akcí podle verze archivní pomůcky.
     * <p>
     * - hledá se v seznamu úloh i v databázi
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam stavů hromadných akcí
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<ArrBulkActionRun> getAllArrBulkActionRun(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        return bulkActionRepository.findByFundVersionIdPageable(fundVersionId, PageRequest.of(0, MAX_BULK_ACTIONS_LIST));
    }

    /**
     * Získání informace o hromadný akce.
     *
     * @param bulkActionRunId   identifikátor hromadné akce
     * @return hromadná akce
     */
    public ArrBulkActionRun getArrBulkActionRun(final Integer bulkActionRunId) {
        Assert.notNull(bulkActionRunId, "Identifikátor běhu hromadné akce musí být vyplněn");
        ArrBulkActionRun bulkActionRun = bulkActionRepository.findById(bulkActionRunId)
                .orElseThrow(bulkAction(bulkActionRunId));
        checkAuthBA(bulkActionRun.getFundVersion());
        return bulkActionRun;
    }

    /**
     * Pomocná metoda pro zjištění oprávnění na AS.
     *
     * @param fundVersion verze AS
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL, UsrPermission.Permission.FUND_BA})
    private void checkAuthBA(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        // pomocná metoda na ověření
    }

    /**
     * Vrací seznam nastavení hromadných akcí podle verze archivní pomůcky.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam nastavení hromadných akcí
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD})
    public List<BulkActionConfig> getBulkActions(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        List<RulAction> ruleActions = actionRepository.findByRuleSet(version.getRuleSet());
        List<BulkActionConfig> configs = new ArrayList<>(ruleActions.size());

        for (RulAction action : ruleActions) {
            BulkActionConfig config = bulkActionConfigManager.get(action.getCode());
            configs.add(config);
        }

        return configs;
    }

    /**
     * Store bulk action run.
     *
     * @param bulkActionRun the bulk action run
     */
    @Transactional(TxType.MANDATORY)
    public void storeBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        if (bulkActionRun.getBulkActionRunId() == null) {
            BulkActionConfig bulkActionConfigOrig = bulkActionConfigManager.get(bulkActionRun.getBulkActionCode());

            if (bulkActionConfigOrig == null) {
                throw new IllegalArgumentException("Hromadná akce neexistuje!");
            }

            Integer fundVersionId = bulkActionRun.getFundVersion().getFundVersionId();
            ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                    .orElseThrow(version(fundVersionId));

            if (version == null) {
                throw new IllegalArgumentException("Verze archivní pomůcky neexistuje!");
            }

            if (version.getLockChange() != null) {
                throw new IllegalArgumentException("Verze archivní pomůcky je uzamčená!");
            }

            bulkActionRun.setFundVersion(version);
        }

        bulkActionRepository.save(bulkActionRun);
    }

    /**
     * Uloží uzly hromadné akce
     *
     * @param bulkActionNodes the bulk action nodes
     */
    public void storeBulkActionNodes(final List<ArrBulkActionNode> bulkActionNodes) {
        bulkActionNodeRepository.saveAll(bulkActionNodes);
    }

    /**
     * Searches latest finished bulk actions for specified node ids.
     */
    public List<ArrBulkActionRun> findFinishedBulkActionsByNodeIds(ArrFundVersion fundVersion, Collection<Integer> nodeIds) {
        return bulkActionRepository.findBulkActionsByNodes(fundVersion.getFundVersionId(), nodeIds, State.FINISHED);
    }

    /**
     * Searches latest executions of bulk actions for specified node ids.
     */
    public List<ArrBulkActionRun> findBulkActionsByNodeIds(ArrFundVersion fundVersion, Collection<Integer> nodeIds) {
        return bulkActionRepository.findBulkActionsByNodes(fundVersion.getFundVersionId(), nodeIds, null);
    }

    /**
     * Vyhledá hromadnou akci podle kódu.
     *
     * @param code  kód hromadné akce
     * @return hromadná akce
     */
    public RulAction getBulkActionByCode(final String code) {
        String fileName = RulAction.getFileNameFromCode(code);
        return actionRepository.findOneByFilename(fileName);
    }

    /**
     * Vyhledá hromadnou akci podle kódu.
     *
     * @param codes  kód hromadné akce
     * @return hromadná akce
     */
    public List<RulAction> getBulkActionByCodes(final List<String> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return Collections.emptyList();
        }
        List<String> fileNames = codes.stream().map(RulAction::getFileNameFromCode).collect(Collectors.toList());
        return actionRepository.findByFilenameIn(fileNames);
    }

    public List<RulAction> getRecommendedActions(RulOutputType outputType) {
        return actionRepository.findByRecommendedActionOutputType(outputType);
    }

    public SecurityContext createSecurityContext(ArrBulkActionRun bulkActionRun) {

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();

        // read user from db
        String username = null, encodePassword = null;

        UserDetail userDetail = userService.createUserDetail(bulkActionRun.getUserId());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, encodePassword,
                null);
        auth.setDetails(userDetail);
        ctx.setAuthentication(auth);

        return ctx;
    }
}
