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

import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.BulkAction;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.FundsActionGroup;
import cz.tacr.elza.controller.vo.FundsActionGroupResult;
import cz.tacr.elza.controller.vo.FundsActionSkipped;
import cz.tacr.elza.controller.vo.MultiFundActionResult;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrBulkActionNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrFundsChange;
import cz.tacr.elza.domain.FundsChangeType;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
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
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.FundsChangeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
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

    /** Důvod přeskočení: fond nemá otevřenou verzi. */
    public static final String SKIP_NO_OPEN_VERSION = "NO_OPEN_VERSION";

    /** Velikost bloku pro dávkové načítání fondů/verzí přes IN (ochrana před obřím IN i N+1). */
    public static final int FUND_BATCH_SIZE = 1000;

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

    @Autowired
    private ArrangementService arrangementService;

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
     * Výběr fondů se předává buď explicitně ({@code fundIds}), nebo filtrem ({@code filters}),
     * který se vyhodnocuje až zde na serveru — na klienta se identifikátory fondů nikdy
     * nestahují. Akce se spustí jen nad fondy, jejichž otevřená verze používá pravidla
     * {@code ruleSetId} (skupina zvolená v dialogu); fondy jiných pravidel se nespouští.
     *
     * Vytvoří jedno seskupení {@link ArrFundsChange} a pro každý fond naplánuje samostatný
     * běh {@link ArrBulkActionRun} (přes existující asynchronní frontu). Akce běží vždy
     * nad celým fondem (kořenový uzel).
     *
     * @param userId         uživatel spouštějící akci
     * @param bulkActionCode kód hromadné akce
     * @param ruleSetId      identifikátor pravidel zvolené skupiny
     * @param fundIds        identifikátory vybraných fondů, nebo {@code null}
     * @param filters        filtr fondů (stejný jako u /fund/search), použije se bez {@code fundIds}
     * @return výsledek se seskupením a přeskočenými fondy
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL})
    public MultiFundActionResult queueMulti(final Integer userId,
                                            final String bulkActionCode,
                                            final Integer ruleSetId,
                                            final List<Integer> fundIds,
                                            final List<AbstractFilter> filters) {
        Assert.isTrue(StringUtils.isNotBlank(bulkActionCode), "Musí být vyplněn kód hromadné akce");
        Assert.notNull(ruleSetId, "Musí být vyplněn identifikátor pravidel");

        List<FundsActionSkipped> skipped = new ArrayList<>();
        List<ArrFundVersion> versions = resolveOpenVersions(fundIds, filters, skipped);

        // fondy jiných pravidel nejsou chyba — uživatel v dialogu zvolil jednu skupinu
        List<ArrFundVersion> validVersions = versions.stream()
                .filter(version -> ruleSetId.equals(version.getRuleSet().getRuleSetId()))
                .collect(Collectors.toList());

        MultiFundActionResult result = new MultiFundActionResult(validVersions.size(), skipped);

        if (validVersions.isEmpty()) {
            return result;
        }

        RulRuleSet ruleSet = validVersions.get(0).getRuleSet();
        if (!isActionInRuleSet(ruleSet, bulkActionCode)) {
            throw new BusinessException("Hromadná akce nepatří do zvolených pravidel.", PackageCode.OTHER_PACKAGE)
                    .set("code", bulkActionCode)
                    .set("ruleSet", ruleSet.getCode());
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
     * Vrací pouze počty fondů ve skupinách — identifikátory se na klienta nepředávají,
     * spuštění akce se odkazuje na stejný výběr ({@code fundIds}/{@code filters}).
     *
     * @param fundIds explicitně vybrané fondy (mají přednost), nebo {@code null}
     * @param filters filtr fondů (stejný jako u /fund/search); prázdný znamená všechny fondy
     * @return skupiny fondů podle pravidel a přeskočené fondy
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_BA_ALL})
    public FundsActionGroupResult groupFundsByRuleSet(final List<Integer> fundIds, final List<AbstractFilter> filters) {
        List<FundsActionSkipped> skipped = new ArrayList<>();
        List<ArrFundVersion> versions = resolveOpenVersions(fundIds, filters, skipped);

        Map<Integer, FundsActionGroup> groupsByRuleSet = new LinkedHashMap<>();
        Map<Integer, RulRuleSet> ruleSetById = new HashMap<>();
        Map<Integer, Integer> fundCounts = new HashMap<>();

        for (ArrFundVersion version : versions) {
            RulRuleSet ruleSet = version.getRuleSet();
            ruleSetById.putIfAbsent(ruleSet.getRuleSetId(), ruleSet);
            fundCounts.merge(ruleSet.getRuleSetId(), 1, Integer::sum);
            groupsByRuleSet.computeIfAbsent(ruleSet.getRuleSetId(), id ->
                    new FundsActionGroup()
                            .ruleSetId(ruleSet.getRuleSetId())
                            .ruleSetCode(ruleSet.getCode())
                            .ruleSetName(ruleSet.getName())
                            .fundCount(0)
                            .actions(new ArrayList<>()));
        }

        for (FundsActionGroup group : groupsByRuleSet.values()) {
            group.setFundCount(fundCounts.get(group.getRuleSetId()));
            group.setActions(getMultiFundActions(ruleSetById.get(group.getRuleSetId())));
        }

        return new FundsActionGroupResult(new ArrayList<>(groupsByRuleSet.values()), skipped);
    }

    /**
     * Vyhodnotí výběr fondů na otevřené verze. Buď podle explicitních identifikátorů
     * (dávkové IN po blocích), nebo podle filtru vyhodnoceného na serveru (stránkovaně,
     * aby dotaz nikdy nepřekročil limity databáze ani nevyžadoval stažení id na klienta).
     * Fondy bez otevřené verze jsou u explicitního výběru hlášeny v {@code skipped};
     * filtr je nevrací vůbec (hledá jen v otevřených verzích).
     */
    private List<ArrFundVersion> resolveOpenVersions(final List<Integer> fundIds,
                                                     final List<AbstractFilter> filters,
                                                     final List<FundsActionSkipped> skipped) {
        if (!CollectionUtils.isEmpty(fundIds)) {
            List<Integer> targetFundIds = new ArrayList<>(new LinkedHashSet<>(fundIds));
            List<ArrFundVersion> versions = new ArrayList<>();
            for (List<Integer> chunk : ListUtils.partition(targetFundIds, FUND_BATCH_SIZE)) {
                versions.addAll(fundVersionRepository.findByFundIdsAndLockChangeIsNull(chunk));
            }
            Set<Integer> resolvedFundIds = new HashSet<>();
            for (ArrFundVersion version : versions) {
                resolvedFundIds.add(version.getFundId());
            }
            for (Integer fundId : targetFundIds) {
                if (!resolvedFundIds.contains(fundId)) {
                    skipped.add(new FundsActionSkipped(fundId, SKIP_NO_OPEN_VERSION));
                }
            }
            return versions;
        }

        List<ArrFundVersion> versions = new ArrayList<>();
        int offset = 0;
        while (true) {
            SearchParams searchParams = new SearchParams();
            searchParams.setFilters(filters != null ? filters : Collections.emptyList());
            searchParams.setOffset(offset);
            searchParams.setSize(FUND_BATCH_SIZE);
            List<ArrFundVersion> page = arrangementService.findFundsBySearchParams(searchParams)
                    .getFundVersionList();
            versions.addAll(page);
            if (page.size() < FUND_BATCH_SIZE) {
                return versions;
            }
            offset += FUND_BATCH_SIZE;
        }
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

    /**
     * Recommended bulk actions for the given output, each paired with the most
     * recent run on the output's nodes (VO of the run) or, when no such run
     * exists, a stub VO carrying only the action code. Order follows the list
     * of recommended actions of the output's type.
     *
     * @param output output whose recommended actions are queried
     * @return one VO per recommended action of the output's type
     */
    public List<BulkActionRunVO> getRecommendedBulkActionsForOutput(ArrOutput output) {
        List<RulAction> recommendedActions = getRecommendedActions(output.getOutputType());
        if (recommendedActions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> nodeIds = output.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null)
                .map(ArrNodeOutput::getNodeId)
                .collect(Collectors.toSet());
        List<ArrBulkActionRun> runs = nodeIds.isEmpty()
                ? Collections.emptyList()
                : findBulkActionsByNodeIds(
                        arrangementService.getOpenVersionByFundId(output.getFund().getFundId()),
                        nodeIds);
        runs.sort((a, b) -> b.getChange().getChangeId() - a.getChange().getChangeId());

        List<BulkActionRunVO> result = new ArrayList<>(recommendedActions.size());
        for (RulAction action : recommendedActions) {
            String code = action.getCode();
            ArrBulkActionRun latest = runs.stream()
                    .filter(r -> code.equals(r.getBulkActionCode()))
                    .findFirst().orElse(null);
            if (latest != null) {
                result.add(BulkActionRunVO.newInstance(latest));
            } else {
                BulkActionRunVO stub = new BulkActionRunVO();
                stub.setCode(code);
                result.add(stub);
            }
        }
        return result;
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
