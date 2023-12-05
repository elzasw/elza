package cz.tacr.elza.bulkaction;


import static cz.tacr.elza.repository.ExceptionThrow.bulkAction;
import static cz.tacr.elza.repository.ExceptionThrow.node;
import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.lang.StringUtils;
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

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrBulkActionNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
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
import cz.tacr.elza.repository.FundVersionRepository;
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
        Assert.notNull(bulkActionCode, "Musí být vyplněn kód hromadné akce");
        Assert.isTrue(StringUtils.isNotBlank(bulkActionCode), "Musí být vyplněn kód hromadné akce");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notEmpty(inputNodeIds, "Musí být vyplněna alespoň jedna JP");

        ArrBulkActionRun bulkActionRun = new ArrBulkActionRun();

        bulkActionRun.setChange(arrangementInternalService.createChange(ArrChange.Type.BULK_ACTION));
        bulkActionRun.setBulkActionCode(bulkActionCode);
        bulkActionRun.setUserId(userId);
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
