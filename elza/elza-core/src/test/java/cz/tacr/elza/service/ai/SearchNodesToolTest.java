package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.FundHits;
import cz.tacr.elza.aiprovider.client.vo.NodeHit;
import cz.tacr.elza.aiprovider.client.vo.SearchNodesResult;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.DescItemField;
import cz.tacr.elza.controller.vo.FieldValueFilter;
import cz.tacr.elza.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.controller.vo.OperationCompareType;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.NodeSearchService;
import cz.tacr.elza.service.NodeSearchService.NodeSearchData;
import cz.tacr.elza.service.UserService;

/**
 * The {@code searchNodes} AI tool: argument mapping to Elza search parameters,
 * enforcement of the conversation owner's read permissions (restriction pushed
 * into the query, empty result when nothing is readable), and the capped,
 * fund-grouped result mapping.
 */
class SearchNodesToolTest {

    private static final int USER_ID = 42;
    private static final int FUND_ID = 12;

    private final UserService userService = mock(UserService.class);
    private final NodeSearchService nodeSearchService = mock(NodeSearchService.class);
    private final ArrangementInternalService arrangementInternalService = mock(ArrangementInternalService.class);
    private final LevelTreeCacheService levelTreeCacheService = mock(LevelTreeCacheService.class);
    private final StaticDataService staticDataService = mock(StaticDataService.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private SearchNodesTool tool;

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        tool = new SearchNodesTool(userService, nodeSearchService, arrangementInternalService,
                                   levelTreeCacheService, staticDataService, 
                                   new TransactionTemplate(txManager), new ObjectMapper());
    }

    @Test
    void restrictsSearchToUsersReadableFundsAndMapsHits() {
        givenPermissions(fundRead(FUND_ID));
        givenSearchFindsNodesInFund(77, 78);

        SearchNodesResult result = (SearchNodesResult) tool.execute(
                Map.of("fulltext", "pivovar"), new AiToolContext(USER_ID));

        ArgumentCaptor<SearchParams> searchParams = ArgumentCaptor.forClass(SearchParams.class);
        ArgumentCaptor<Collection<Integer>> restriction = restrictionCaptor();
        verify(nodeSearchService).nodeSearchData(searchParams.capture(), restriction.capture());
        // The permission restriction is pushed into the query itself.
        assertThat(restriction.getValue()).containsExactly(FUND_ID);
        AbstractFilter filter = searchParams.getValue().getFilters().get(0);
        assertThat(filter).isInstanceOf(MultimatchContainsFilter.class);
        assertThat(((MultimatchContainsFilter) filter).getValue()).isEqualTo("pivovar");

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getPartial()).isFalse();
        FundHits hits = result.getFunds().get(0);
        assertThat(hits.getFundId()).isEqualTo(FUND_ID);
        assertThat(hits.getName()).isEqualTo("Velkostatek Kounice");
        assertThat(hits.getCount()).isEqualTo(2);
        assertThat(hits.getNodes()).extracting(NodeHit::getNodeId).containsExactly(77, 78);
        assertThat(hits.getNodes().get(0).getTitle()).isEqualTo("Node 77");
        assertThat(hits.getNodes().get(0).getReferenceMark()).containsExactly("1", "77");
    }

    @Test
    void intersectsRequestedFundScopeWithReadableFunds() {
        givenPermissions(fundRead(FUND_ID));
        givenSearchFindsNothing();

        tool.execute(Map.of("fulltext", "x", "fundIds", List.of(FUND_ID, 99)), new AiToolContext(USER_ID));

        ArgumentCaptor<Collection<Integer>> restriction = restrictionCaptor();
        verify(nodeSearchService).nodeSearchData(any(), restriction.capture());
        assertThat(restriction.getValue()).containsExactly(FUND_ID);
    }

    @Test
    void answersEmptyWithoutSearchingWhenUserReadsNoFunds() {
        givenPermissions(); // no permissions at all

        SearchNodesResult result = (SearchNodesResult) tool.execute(
                Map.of("fulltext", "x"), new AiToolContext(USER_ID));

        assertThat(result.getFunds()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
        verifyNoInteractions(nodeSearchService);
    }

    @Test
    void adminSearchesUnrestricted() {
        givenPermissions(new UserPermission(Permission.ADMIN));
        givenSearchFindsNothing();

        tool.execute(Map.of("fulltext", "x"), new AiToolContext(USER_ID));

        ArgumentCaptor<Collection<Integer>> restriction = restrictionCaptor();
        verify(nodeSearchService).nodeSearchData(any(), restriction.capture());
        assertThat(restriction.getValue()).isNull();
    }

    @Test
    void virtualAdminAccountSearchesUnrestricted() {
        givenSearchFindsNothing();

        tool.execute(Map.of("fulltext", "x"), new AiToolContext(null));

        ArgumentCaptor<Collection<Integer>> restriction = restrictionCaptor();
        verify(nodeSearchService).nodeSearchData(any(), restriction.capture());
        assertThat(restriction.getValue()).isNull();
        verifyNoInteractions(userService);
    }

    @Test
    void mapsItemConditionsToFieldValueFilters() {
        givenPermissions(fundRead(FUND_ID));
        givenSearchFindsNothing();

        tool.execute(Map.of("itemConditions", List.of(Map.of(
                "type", "ZP2015_UNIT_DATE",
                "operation", "INTERSECT",
                "value", "1930-1939"))), new AiToolContext(USER_ID));

        ArgumentCaptor<SearchParams> searchParams = ArgumentCaptor.forClass(SearchParams.class);
        verify(nodeSearchService).nodeSearchData(searchParams.capture(), any());
        FieldValueFilter filter = (FieldValueFilter) searchParams.getValue().getFilters().get(0);
        DescItemField field = (DescItemField) filter.getField();
        assertThat(field.getTypeCode()).isEqualTo("ZP2015_UNIT_DATE");
        assertThat(filter.getOperation()).isEqualTo(OperationCompareType.INTERSECT);
        assertThat(filter.getValue()).isEqualTo("1930-1939");
    }

    @Test
    void rejectsCallWithoutAnySearchCriteria() {
        assertThatThrownBy(() -> tool.execute(Map.of(), new AiToolContext(USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fulltext");
    }

    @Test
    void capsInlinedHitsPerFundAndFlagsPartial() {
        givenPermissions(fundRead(FUND_ID));
        givenSearchFindsNodesInFund(IntStream.rangeClosed(1, 25).boxed().toArray(Integer[]::new));

        SearchNodesResult result = (SearchNodesResult) tool.execute(
                Map.of("fulltext", "pivovar"), new AiToolContext(USER_ID));

        FundHits hits = result.getFunds().get(0);
        // count still reports every hit in the fund; only the inlined list is capped
        assertThat(hits.getCount()).isEqualTo(25);
        assertThat(hits.getNodes()).hasSize(SearchNodesTool.MAX_HITS_PER_FUND);
        assertThat(result.getPartial()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private void givenPermissions(final UserPermission... permissions) {
        when(userService.getUserPermissions(USER_ID)).thenReturn(Arrays.asList(permissions));
    }

    private static UserPermission fundRead(final Integer... fundIds) {
        UserPermission permission = new UserPermission(Permission.FUND_RD);
        for (Integer fundId : fundIds) {
            permission.addFundId(fundId);
        }
        return permission;
    }

    private void givenSearchFindsNothing() {
        when(nodeSearchService.nodeSearchData(any(), any()))
                .thenReturn(new NodeSearchData(List.of(), 0, false));
    }

    /** One fund ({@link #FUND_ID}) with the given hits; tree order = id order, titles "Node {id}". */
    private void givenSearchFindsNodesInFund(final Integer... nodeIds) {
        List<Integer> ids = new ArrayList<>(Arrays.asList(nodeIds));
        when(nodeSearchService.nodeSearchData(any(), any())).thenReturn(
                new NodeSearchData(List.of(new ArrFundToNodeList(FUND_ID, ids)), ids.size(), false));

        ArrFund fund = new ArrFund();
        fund.setName("Velkostatek Kounice");
        ArrFundVersion version = new ArrFundVersion();
        version.setFund(fund);
        when(arrangementInternalService.getOpenVersionByFundId(FUND_ID)).thenReturn(version);
        when(levelTreeCacheService.sortNodesByTreePosition(any(), eq(version)))
                .thenAnswer(invocation -> new ArrayList<>(invocation.<Collection<Integer>>getArgument(0)));
        when(levelTreeCacheService.getNodesByIds(any(), eq(version))).thenAnswer(invocation -> {
            Collection<Integer> requested = invocation.getArgument(0);
            return requested.stream().map(id -> {
                TreeNodeVO node = new TreeNodeVO();
                node.setId(id);
                node.setName("Node " + id);
                node.setReferenceMark(new String[] { "1", String.valueOf(id) });
                return node;
            }).toList();
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static ArgumentCaptor<Collection<Integer>> restrictionCaptor() {
        return ArgumentCaptor.forClass((Class) Collection.class);
    }
}
