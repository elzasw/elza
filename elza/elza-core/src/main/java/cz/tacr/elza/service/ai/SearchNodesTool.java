package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.FundHits;
import cz.tacr.elza.aiprovider.client.vo.ItemCondition;
import cz.tacr.elza.aiprovider.client.vo.NodeHit;
import cz.tacr.elza.aiprovider.client.vo.SearchNodesParams;
import cz.tacr.elza.aiprovider.client.vo.SearchNodesResult;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.DescItemField;
import cz.tacr.elza.controller.vo.FieldType;
import cz.tacr.elza.controller.vo.FieldValueFilter;
import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.controller.vo.LogicalFilter;
import cz.tacr.elza.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.controller.vo.OperationCompareType;
import cz.tacr.elza.controller.vo.OperationLogicalType;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataService;
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
 * Standard {@code searchNodes} tool — cross-fund search of description units,
 * requested by the AI model mid-turn. Argument/result shapes are defined by the
 * AI provider contract ({@code SearchNodesParams} / {@code SearchNodesResult});
 * design notes: {@code elza-development/typespec-ai/node-search.md}.
 *
 * <p>The tool runs on the poller thread, outside the request security context,
 * so it enforces the conversation owner's read permissions itself: a user with
 * {@code ADMIN}/{@code FUND_RD_ALL} searches unrestricted, anyone else only the
 * funds their {@code FUND_RD} covers; a {@code fundIds} scope requested by the
 * model is intersected with that. The restriction is applied inside the search
 * query (the {@code fundId} index field), never post-hoc.
 *
 * <p>The result is capped (total hits, hits per fund, funds listed) and flags
 * {@code partial} so the model refines the query instead of paging.
 */
@Component
public class SearchNodesTool implements AiTool {

    /** Hits returned in total, regardless of the requested limit. */
    static final int MAX_TOTAL_HITS = 50;

    /** Hits inlined per fund; the fund's {@code count} still reports all of them. */
    static final int MAX_HITS_PER_FUND = 10;

    /** Funds listed in the result. */
    static final int MAX_FUNDS_LISTED = 20;

    private final UserService userService;
    private final NodeSearchService nodeSearchService;
    private final ArrangementInternalService arrangementInternalService;
    private final LevelTreeCacheService levelTreeCacheService;
    private final StaticDataService staticDataService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public SearchNodesTool(final UserService userService,
                           final NodeSearchService nodeSearchService,
                           final ArrangementInternalService arrangementInternalService,
                           final LevelTreeCacheService levelTreeCacheService,
                           final StaticDataService staticDataService,
                           final TransactionTemplate transactionTemplate,
                           final ObjectMapper objectMapper) {
        this.userService = userService;
        this.nodeSearchService = nodeSearchService;
        this.arrangementInternalService = arrangementInternalService;
        this.levelTreeCacheService = levelTreeCacheService;
        this.staticDataService = staticDataService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public StandardToolName name() {
        return StandardToolName.SEARCH_NODES;
    }

    @Override
    public Object execute(final Object arguments, final AiToolContext context) {
        SearchNodesParams params = objectMapper.convertValue(arguments, SearchNodesParams.class);
        boolean hasFulltext = params != null && StringUtils.isNotBlank(params.getFulltext());
        boolean hasConditions = params != null && CollectionUtils.isNotEmpty(params.getItemConditions());
        boolean hasEntityRef = params != null && params.getReferencesEntityId() != null;
        if (!hasFulltext && !hasConditions && !hasEntityRef) {
            throw new IllegalArgumentException(
                    "searchNodes requires at least one of: fulltext, itemConditions, referencesEntityId");
        }

        Collection<Integer> restriction = resolveRestriction(params.getFundIds(), context);
        if (restriction != null && restriction.isEmpty()) {
            // The user may read none of the requested funds (or none at all) —
            // an empty result, not an error: the model adapts, nothing leaks.
            return new SearchNodesResult().funds(List.of()).totalCount(0L).partial(false);
        }

        int totalLimit = params.getLimit() != null && params.getLimit() > 0
                ? Math.min(params.getLimit(), MAX_TOTAL_HITS)
                : MAX_TOTAL_HITS;

        // The poller thread has no transaction; the search (Hibernate Search +
        // tree-cache reads with lazy entities) and the static-data lookups need one.
        return transactionTemplate.execute(status -> {
            AbstractFilter entityRefFilter = null;
            if (params.getReferencesEntityId() != null) {
                entityRefFilter = createEntityRefFilter(params.getReferencesEntityId());
                if (entityRefFilter == null) {
                    // no reference item type exists — nothing can reference an entity
                    return new SearchNodesResult().funds(List.of()).totalCount(0L).partial(false);
                }
            }
            SearchParams searchParams = toSearchParams(params, entityRefFilter);
            NodeSearchData data = nodeSearchService.nodeSearchData(searchParams, restriction);
            return toResult(data, totalLimit);
        });
    }

    /**
     * The condition "some item of the level references the entity" as an OR over
     * every {@code RECORD_REF} item type — a reference item condition with a
     * numeric value matches by entity id, so the existing search vocabulary
     * covers the type-agnostic reference match without a dedicated field.
     * {@code null} when the system defines no reference item type (nothing can
     * match then).
     */
    private AbstractFilter createEntityRefFilter(final Integer accessPointId) {
        List<AbstractFilter> refFilters = new ArrayList<>();
        for (ItemType itemType : staticDataService.getData().getItemTypes()) {
            if (itemType.getDataType() == DataType.RECORD_REF) {
                DescItemField field = new DescItemField();
                field.setFieldType(FieldType.DESC_ITEM);
                field.setTypeCode(itemType.getCode());
                FieldValueFilter filter = new FieldValueFilter();
                filter.setFilterType(FilterType.FIELD_VALUE);
                filter.setField(field);
                filter.setOperation(OperationCompareType.EQ);
                filter.setValue(String.valueOf(accessPointId));
                refFilters.add(filter);
            }
        }
        if (refFilters.isEmpty()) {
            return null;
        }
        LogicalFilter anyReference = new LogicalFilter();
        anyReference.setFilterType(FilterType.LOGICAL);
        anyReference.setOperation(OperationLogicalType.OR);
        anyReference.setFilters(refFilters);
        return anyReference;
    }

    /**
     * The fund set the search must stay within: {@code null} = unrestricted
     * (admin), otherwise the user's readable funds intersected with the
     * requested scope. May come out empty — the caller answers with an empty
     * result then.
     */
    private Collection<Integer> resolveRestriction(final List<Integer> requestedFundIds,
                                                   final AiToolContext context) {
        boolean unrestricted;
        Set<Integer> readable;
        if (context.userId() == null) {
            // the virtual admin account: no user row, full permissions
            unrestricted = true;
            readable = Set.of();
        } else {
            Collection<UserPermission> permissions = userService.getUserPermissions(context.userId());
            unrestricted = permissions.stream()
                    .anyMatch(p -> p.getPermission() == Permission.ADMIN
                            || p.getPermission() == Permission.FUND_RD_ALL);
            readable = permissions.stream()
                    .filter(p -> p.getPermission() == Permission.FUND_RD)
                    .flatMap(p -> p.getFundIds().stream())
                    .collect(Collectors.toSet());
        }
        if (unrestricted) {
            return CollectionUtils.isEmpty(requestedFundIds) ? null : new HashSet<>(requestedFundIds);
        }
        if (CollectionUtils.isEmpty(requestedFundIds)) {
            return readable;
        }
        Set<Integer> scoped = new HashSet<>(requestedFundIds);
        scoped.retainAll(readable);
        return scoped;
    }

    /** The contract arguments as Elza search parameters (all conditions ANDed). */
    private SearchParams toSearchParams(final SearchNodesParams params, final AbstractFilter entityRefFilter) {
        List<AbstractFilter> filters = new ArrayList<>();
        if (entityRefFilter != null) {
            filters.add(entityRefFilter);
        }
        if (StringUtils.isNotBlank(params.getFulltext())) {
            MultimatchContainsFilter fulltext = new MultimatchContainsFilter();
            fulltext.setFilterType(FilterType.CONTAINS);
            fulltext.setValue(params.getFulltext().trim());
            filters.add(fulltext);
        }
        if (params.getItemConditions() != null) {
            for (ItemCondition condition : params.getItemConditions()) {
                if (StringUtils.isBlank(condition.getType()) || condition.getOperation() == null) {
                    throw new IllegalArgumentException(
                            "Each itemCondition requires a 'type' (item-type code) and an 'operation'");
                }
                DescItemField field = new DescItemField();
                field.setFieldType(FieldType.DESC_ITEM);
                field.setTypeCode(condition.getType());
                field.setSpecCode(condition.getSpec());
                FieldValueFilter filter = new FieldValueFilter();
                filter.setFilterType(FilterType.FIELD_VALUE);
                filter.setField(field);
                // the contract operations are a subset of Elza's, names aligned
                filter.setOperation(OperationCompareType.valueOf(condition.getOperation().name()));
                filter.setValue(condition.getValue());
                filters.add(filter);
            }
        }
        SearchParams searchParams = new SearchParams();
        searchParams.setFilters(filters);
        return searchParams;
    }

    /**
     * The search outcome as the contract result: funds ordered by hit count,
     * each with its first hits in tree order (title + reference designation),
     * capped per fund and in total; {@code partial} reports any truncation.
     */
    private SearchNodesResult toResult(final NodeSearchData data, final int totalLimit) {
        List<ArrFundToNodeList> funds = new ArrayList<>(data.fundToNodeLists());
        funds.sort(Comparator.comparingInt(ArrFundToNodeList::getNodeCount).reversed());

        boolean partial = data.partialResult();
        SearchNodesResult result = new SearchNodesResult()
                .funds(new ArrayList<>())
                .totalCount(data.totalCount());
        int remaining = totalLimit;
        for (ArrFundToNodeList fund : funds) {
            if (result.getFunds().size() >= MAX_FUNDS_LISTED || remaining <= 0) {
                partial = true;
                break;
            }
            ArrFundVersion version = arrangementInternalService.getOpenVersionByFundId(fund.getFundId());
            if (version == null) {
                continue; // fund without an open version — nothing presentable
            }
            List<Integer> sorted = levelTreeCacheService.sortNodesByTreePosition(fund.getNodeIdList(), version);
            int take = Math.min(Math.min(MAX_HITS_PER_FUND, remaining), sorted.size());
            List<TreeNodeVO> nodes = levelTreeCacheService.getNodesByIds(sorted.subList(0, take), version);

            FundHits hits = new FundHits()
                    .fundId(fund.getFundId())
                    .name(version.getFund().getName())
                    .count(fund.getNodeCount())
                    .nodes(new ArrayList<>());
            for (TreeNodeVO node : nodes) {
                NodeHit hit = new NodeHit()
                        .nodeId(node.getId())
                        .title(node.getName());
                if (node.getReferenceMark() != null) {
                    hit.referenceMark(Arrays.asList(node.getReferenceMark()));
                }
                hits.addNodesItem(hit);
            }
            if (hits.getNodes().size() < fund.getNodeCount()) {
                partial = true;
            }
            remaining -= hits.getNodes().size();
            result.addFundsItem(hits);
        }
        return result.partial(partial);
    }
}
