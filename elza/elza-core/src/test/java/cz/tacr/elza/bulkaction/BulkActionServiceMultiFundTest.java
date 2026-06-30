package cz.tacr.elza.bulkaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import cz.tacr.elza.controller.vo.FundsActionGroupResult;
import cz.tacr.elza.controller.vo.MultiFundActionResult;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrFundsChange;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.repository.BulkActionNodeRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.FundsChangeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.UserService;

/**
 * Pure unit test for the multi-fund methods of {@link BulkActionService}
 * ({@code groupFundsByRuleSet} a {@code queueMulti}). All collaborators are
 * mocked, so no Spring context or database is needed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkActionServiceMultiFundTest {

    private static final String ACTION_CODE = "ContentMigration";

    @Mock
    private FundVersionRepository fundVersionRepository;
    @Mock
    private UserService userService;
    @Mock
    private ActionRepository actionRepository;
    @Mock
    private BulkActionConfigManager bulkActionConfigManager;
    @Mock
    private FundsChangeRepository fundsChangeRepository;
    @Mock
    private BulkActionRunRepository bulkActionRepository;
    @Mock
    private ArrangementInternalService arrangementInternalService;
    @Mock
    private AsyncRequestService asyncRequestService;
    @Mock
    private NodeRepository nodeRepository;
    @Mock
    private BulkActionNodeRepository bulkActionNodeRepository;

    @InjectMocks
    private BulkActionService service;

    private RulRuleSet ruleSet(int id, String code, String name) {
        RulRuleSet rs = new RulRuleSet();
        rs.setRuleSetId(id);
        rs.setCode(code);
        rs.setName(name);
        return rs;
    }

    private ArrFundVersion openVersion(int fundId, int versionId, RulRuleSet rs) {
        ArrFund fund = new ArrFund();
        fund.setFundId(fundId);
        ArrFundVersion v = new ArrFundVersion();
        v.setFundVersionId(versionId);
        v.setFund(fund); // also sets fundId
        v.setRuleSet(rs);
        ArrNode root = new ArrNode();
        root.setNodeId(versionId * 1000);
        v.setRootNode(root);
        return v;
    }

    /** Builds a {@link RulAction} mock; must be created before being passed into another {@code when(...)}. */
    private RulAction action(String code) {
        RulAction a = mock(RulAction.class);
        when(a.getCode()).thenReturn(code);
        return a;
    }

    private BulkActionConfig config(String code) {
        BulkActionConfig c = mock(BulkActionConfig.class);
        when(c.getCode()).thenReturn(code);
        when(c.getName()).thenReturn(code);
        return c;
    }

    @Test
    void groupFundsByRuleSet_splitsByRuleSetSkipsNoOpenVersionAndExcludesPersistentSort() {
        RulRuleSet rsA = ruleSet(100, "ZP2015", "Pravidla ZP2015");
        RulAction actionMig = action(ACTION_CODE);
        RulAction actionSort = action(BulkActionService.PERSISTENT_SORT_CODE);
        BulkActionConfig configMig = config(ACTION_CODE);
        BulkActionConfig configSort = config(BulkActionService.PERSISTENT_SORT_CODE);

        // funds 1 and 2 have open versions in rule set A; fund 3 has none -> skipped
        when(fundVersionRepository.findByFundIdsAndLockChangeIsNull(any()))
                .thenReturn(Arrays.asList(openVersion(1, 10, rsA), openVersion(2, 20, rsA)));
        when(actionRepository.findByRuleSet(rsA)).thenReturn(Arrays.asList(actionMig, actionSort));
        when(bulkActionConfigManager.get(ACTION_CODE)).thenReturn(configMig);
        when(bulkActionConfigManager.get(BulkActionService.PERSISTENT_SORT_CODE)).thenReturn(configSort);

        FundsActionGroupResult result = service.groupFundsByRuleSet(Arrays.asList(1, 2, 3), null);

        assertThat(result.getGroups()).hasSize(1);
        assertThat(result.getGroups().get(0).getRuleSetCode()).isEqualTo("ZP2015");
        assertThat(result.getGroups().get(0).getFundCount()).isEqualTo(2);
        assertThat(result.getGroups().get(0).getFundVersionIds()).containsExactly(10, 20);
        // persistent sort requires run-time config -> excluded in multi-fund mode
        assertThat(result.getGroups().get(0).getActions()).hasSize(1);
        assertThat(result.getGroups().get(0).getActions().get(0).getCode()).isEqualTo(ACTION_CODE);

        assertThat(result.getSkipped()).hasSize(1);
        assertThat(result.getSkipped().get(0).getId()).isEqualTo(3);
        assertThat(result.getSkipped().get(0).getReason()).isEqualTo(BulkActionService.SKIP_NO_OPEN_VERSION);
    }

    @Test
    void groupFundsByRuleSet_byFilter_resolvesFundsFromSearch() {
        RulRuleSet rsA = ruleSet(100, "ZP2015", "Pravidla ZP2015");
        RulAction actionMig = action(ACTION_CODE);
        BulkActionConfig configMig = config(ACTION_CODE);
        ArrFund fund = new ArrFund();
        fund.setFundId(7);
        FilteredResult<ArrFund> filtered = new FilteredResult<>(0, 10, 1, List.of(fund));

        when(userService.findFundsWithPermissions(eq("abc"), eq(0), any())).thenReturn(filtered);
        when(fundVersionRepository.findByFundIdsAndLockChangeIsNull(any()))
                .thenReturn(List.of(openVersion(7, 70, rsA)));
        when(actionRepository.findByRuleSet(rsA)).thenReturn(List.of(actionMig));
        when(bulkActionConfigManager.get(ACTION_CODE)).thenReturn(configMig);

        FundsActionGroupResult result = service.groupFundsByRuleSet(null, "abc");

        assertThat(result.getGroups()).hasSize(1);
        assertThat(result.getGroups().get(0).getFundVersionIds()).containsExactly(70);
    }

    @Test
    void queueMulti_allInvalid_createsNoFundsChangeAndReportsSkips() {
        RulRuleSet rsOther = ruleSet(200, "OTHER", "Jiná pravidla");
        RulAction otherAction = action("SomethingElse");

        // version 10 has no open version (absent from the batch load); version 20 is in a
        // rule set that does not contain the action
        when(fundVersionRepository.findAllById(any())).thenReturn(List.of(openVersion(20, 20, rsOther)));
        when(actionRepository.findByRuleSet(rsOther)).thenReturn(List.of(otherAction));

        MultiFundActionResult result = service.queueMulti(1, ACTION_CODE, Arrays.asList(10, 20));

        assertThat(result.getQueuedCount()).isEqualTo(0);
        assertThat(result.getFundsChangeId()).isNull();
        assertThat(result.getSkipped()).hasSize(2);
        assertThat(result.getSkipped()).anySatisfy(s -> {
            assertThat(s.getId()).isEqualTo(10);
            assertThat(s.getReason()).isEqualTo(BulkActionService.SKIP_NO_OPEN_VERSION);
        });
        assertThat(result.getSkipped()).anySatisfy(s -> {
            assertThat(s.getId()).isEqualTo(20);
            assertThat(s.getReason()).isEqualTo(BulkActionService.SKIP_ACTION_NOT_IN_RULESET);
        });
        verify(fundsChangeRepository, never()).save(any());
        verify(asyncRequestService, never()).enqueue(any(ArrFundVersion.class), any(ArrBulkActionRun.class));
    }

    @Test
    void queueMulti_fansOutOnePerFundAndLinksThemToOneFundsChange() {
        RulRuleSet rs = ruleSet(100, "ZP2015", "Pravidla ZP2015");
        RulAction actionMig = action(ACTION_CODE);
        BulkActionConfig configMig = config(ACTION_CODE);
        ArrFundVersion v10 = openVersion(10, 10, rs);
        ArrFundVersion v20 = openVersion(20, 20, rs);

        when(fundVersionRepository.findAllById(any())).thenReturn(Arrays.asList(v10, v20));
        when(fundVersionRepository.findById(10)).thenReturn(Optional.of(v10));
        when(fundVersionRepository.findById(20)).thenReturn(Optional.of(v20));
        when(fundVersionRepository.getOneCheckExist(10)).thenReturn(v10);
        when(fundVersionRepository.getOneCheckExist(20)).thenReturn(v20);
        when(actionRepository.findByRuleSet(rs)).thenReturn(List.of(actionMig));
        when(bulkActionConfigManager.get(ACTION_CODE)).thenReturn(configMig);
        when(arrangementInternalService.createChange(any())).thenReturn(new ArrChange());
        when(nodeRepository.findById(any())).thenAnswer(inv -> {
            ArrNode n = new ArrNode();
            n.setNodeId(inv.getArgument(0));
            return Optional.of(n);
        });
        // the saved funds-change gets an id
        when(fundsChangeRepository.save(any(ArrFundsChange.class))).thenAnswer(inv -> {
            ArrFundsChange fc = inv.getArgument(0);
            fc.setFundsChangeId(99);
            return fc;
        });

        MultiFundActionResult result = service.queueMulti(1, ACTION_CODE, Arrays.asList(10, 20));

        assertThat(result.getQueuedCount()).isEqualTo(2);
        assertThat(result.getFundsChangeId()).isEqualTo(99);
        assertThat(result.getSkipped()).isEmpty();

        // exactly one funds-change row for the whole batch
        verify(fundsChangeRepository, times(1)).save(any(ArrFundsChange.class));

        // one queued run per fund, all linked to the same funds-change
        ArgumentCaptor<ArrBulkActionRun> runCaptor = ArgumentCaptor.forClass(ArrBulkActionRun.class);
        verify(asyncRequestService, times(2)).enqueue(any(ArrFundVersion.class), runCaptor.capture());
        assertThat(runCaptor.getAllValues()).hasSize(2);
        assertThat(runCaptor.getAllValues()).allSatisfy(run -> {
            assertThat(run.getBulkActionCode()).isEqualTo(ACTION_CODE);
            assertThat(run.getFundsChange()).isNotNull();
            assertThat(run.getFundsChange().getFundsChangeId()).isEqualTo(99);
        });
    }
}
