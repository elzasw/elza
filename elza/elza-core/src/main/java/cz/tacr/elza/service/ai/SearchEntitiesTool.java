package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ArchivalEntityInfo;
import cz.tacr.elza.aiprovider.client.vo.EntityRelationFilter;
import cz.tacr.elza.aiprovider.client.vo.EntitySearchArea;
import cz.tacr.elza.aiprovider.client.vo.SearchEntitiesParams;
import cz.tacr.elza.aiprovider.client.vo.SearchEntitiesResult;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.ApAdvanceSearchFilter;
import cz.tacr.elza.controller.vo.ApSearchArea;
import cz.tacr.elza.controller.vo.ApSearchByRelation;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;

/**
 * Standard {@code searchEntities} tool — search of archival entities (access
 * points), requested by the AI model mid-turn: full text over names/description,
 * optionally constrained to an entity class/type or to entities referencing a
 * given entity (incoming relations). Argument/result shapes are defined by the
 * AI provider contract ({@code SearchEntitiesParams} / {@code SearchEntitiesResult});
 * design notes: {@code elza-development/typespec-ai/investigation-tools.md}.
 *
 * <p>Backed by the same search the registry UI uses: a full-text query runs over
 * the Lucene cached-access-point index, a purely structural query (type and/or
 * relation only) runs as a criteria query. Only {@code NEW} and {@code APPROVED}
 * entities are searched — deleted and replaced ones never surface.
 *
 * <p>The tool runs on the poller thread, outside the request security context,
 * so it enforces the conversation owner's scope read permissions itself: a user
 * with {@code ADMIN}/{@code AP_SCOPE_RD_ALL} searches every scope, anyone else
 * only the scopes their {@code AP_SCOPE_RD} covers. The restriction is applied
 * inside the search query, never post-hoc.
 *
 * <p>The result is a capped list of lightweight identities, best match first,
 * flagged {@code partial} so the model refines the query instead of paging.
 */
@Component
public class SearchEntitiesTool implements AiTool {

    /** Hits returned in total, regardless of the requested limit. */
    static final int MAX_TOTAL_HITS = 50;

    /** Only current, usable entities are searched (settled contract decision). */
    private static final Set<StateApproval> SEARCHED_STATES = EnumSet.of(StateApproval.NEW, StateApproval.APPROVED);

    private final UserService userService;
    private final AccessPointService accessPointService;
    private final AccessPointCacheService accessPointCacheService;
    private final ApCachedAccessPointRepository cachedAccessPointRepository;
    private final ApTypeRepository apTypeRepository;
    private final ScopeRepository scopeRepository;
    private final AiContextResolver aiContextResolver;
    private final StaticDataService staticDataService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public SearchEntitiesTool(final UserService userService,
                              final AccessPointService accessPointService,
                              final AccessPointCacheService accessPointCacheService,
                              final ApCachedAccessPointRepository cachedAccessPointRepository,
                              final ApTypeRepository apTypeRepository,
                              final ScopeRepository scopeRepository,
                              final AiContextResolver aiContextResolver,
                              final StaticDataService staticDataService,
                              final TransactionTemplate transactionTemplate,
                              final ObjectMapper objectMapper) {
        this.userService = userService;
        this.accessPointService = accessPointService;
        this.accessPointCacheService = accessPointCacheService;
        this.cachedAccessPointRepository = cachedAccessPointRepository;
        this.apTypeRepository = apTypeRepository;
        this.scopeRepository = scopeRepository;
        this.aiContextResolver = aiContextResolver;
        this.staticDataService = staticDataService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public StandardToolName name() {
        return StandardToolName.SEARCH_ENTITIES;
    }

    @Override
    public Object execute(final Object arguments, final AiToolContext context) {
        SearchEntitiesParams params = objectMapper.convertValue(arguments, SearchEntitiesParams.class);
        boolean hasFulltext = params != null && StringUtils.isNotBlank(params.getFulltext());
        boolean hasTypeCode = params != null && StringUtils.isNotBlank(params.getTypeCode());
        boolean hasRelation = params != null && params.getRelatedTo() != null;
        if (!hasFulltext && !hasTypeCode && !hasRelation) {
            throw new IllegalArgumentException(
                    "searchEntities requires at least one of: fulltext, typeCode, relatedTo");
        }

        int limit = params.getLimit() != null && params.getLimit() > 0
                ? Math.min(params.getLimit(), MAX_TOTAL_HITS)
                : MAX_TOTAL_HITS;

        // The poller thread has no transaction; the searches and the cache reads
        // (lazy entities) need one.
        return transactionTemplate.execute(status -> {
            Set<Integer> scopeIds = resolveReadableScopeIds(context);
            if (scopeIds.isEmpty()) {
                // The user may read no scope at all — an empty result, not an
                // error: the model adapts, nothing leaks.
                return new SearchEntitiesResult().entities(List.of()).totalCount(0L).partial(false);
            }
            StaticDataProvider sdp = staticDataService.getData();
            Set<Integer> apTypeIds = resolveTypeIds(params.getTypeCode(), sdp);
            ApAdvanceSearchFilter filter = toSearchFilter(params, sdp);

            // The registry-search routing precedent: a full-text query runs over
            // the Lucene index (scored, best match first), a purely structural
            // one as a criteria query (ordered by preferred name).
            if (hasFulltext) {
                return searchByLucene(params, filter, apTypeIds, scopeIds, limit, sdp);
            }
            return searchByCriteria(filter, apTypeIds, scopeIds, limit, sdp);
        });
    }

    /** Full-text search over the cached-access-point index, scored. */
    private SearchEntitiesResult searchByLucene(final SearchEntitiesParams params,
                                                final ApAdvanceSearchFilter filter,
                                                final Set<Integer> apTypeIds, final Set<Integer> scopeIds,
                                                final int limit, final StaticDataProvider sdp) {
        QueryResults<ApCachedAccessPoint> results = cachedAccessPointRepository.findApCachedAccessPointisByQuery(
                params.getFulltext(), filter, apTypeIds, scopeIds, SEARCHED_STATES, null, 0, limit, sdp);
        List<ArchivalEntityInfo> entities = new ArrayList<>(results.getRecords().size());
        for (ApCachedAccessPoint record : results.getRecords()) {
            CachedAccessPoint cap = accessPointCacheService.deserialize(record.getData(), record.getAccessPoint());
            entities.add(aiContextResolver.buildArchivalEntityInfo(cap));
        }
        return new SearchEntitiesResult()
                .entities(entities)
                .totalCount((long) results.getRecordCount())
                .partial(results.getRecordCount() > entities.size());
    }

    /** Structural search (type and/or relation only) as a criteria query. */
    private SearchEntitiesResult searchByCriteria(final ApAdvanceSearchFilter filter,
                                                  final Set<Integer> apTypeIds, final Set<Integer> scopeIds,
                                                  final int limit, final StaticDataProvider sdp) {
        Page<ApState> page = accessPointService.findApAccessPointBySearchFilter(
                filter, apTypeIds, scopeIds, SEARCHED_STATES, null, 0, limit, sdp);
        List<Integer> ids = page.getContent().stream().map(ApState::getAccessPointId).toList();
        Map<Integer, CachedAccessPoint> capById = ids.isEmpty()
                ? Map.of()
                : accessPointCacheService.findCachedAccessPoints(ids).stream()
                        .collect(Collectors.toMap(CachedAccessPoint::getAccessPointId, Function.identity()));
        List<ArchivalEntityInfo> entities = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            CachedAccessPoint cap = capById.get(id);
            if (cap != null) {
                entities.add(aiContextResolver.buildArchivalEntityInfo(cap));
            }
        }
        return new SearchEntitiesResult()
                .entities(entities)
                .totalCount(page.getTotalElements())
                .partial(page.getTotalElements() > entities.size());
    }

    /**
     * The scopes the conversation owner may read ({@code AP_SCOPE_RD}); all
     * scopes for an unrestricted user. May come out empty — the caller answers
     * with an empty result then. The set lands inside the search query, so the
     * permission restriction is never applied post-hoc.
     */
    private Set<Integer> resolveReadableScopeIds(final AiToolContext context) {
        if (context.userId() == null) {
            // the virtual admin account: no user row, full permissions
            return scopeRepository.findAllIds();
        }
        Collection<UserPermission> permissions = userService.getUserPermissions(context.userId());
        boolean unrestricted = permissions.stream()
                .anyMatch(p -> p.getPermission() == Permission.ADMIN
                        || p.getPermission() == Permission.AP_SCOPE_RD_ALL);
        if (unrestricted) {
            return scopeRepository.findAllIds();
        }
        return permissions.stream()
                .filter(p -> p.getPermission() == Permission.AP_SCOPE_RD)
                .flatMap(p -> p.getScopeIds().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Resolves the requested entity class/type code to the id set of the type
     * and all its subtypes (a non-leaf code includes the whole subtree), or
     * {@code null} when no type restriction is requested. An unknown code is an
     * error, not an empty hit list.
     */
    private Set<Integer> resolveTypeIds(final String typeCode, final StaticDataProvider sdp) {
        if (StringUtils.isBlank(typeCode)) {
            return null;
        }
        ApType type = sdp.getApTypeByCode(typeCode.trim());
        if (type == null) {
            throw new IllegalArgumentException("Unknown entity type code: " + typeCode.trim());
        }
        return apTypeRepository.findSubtreeIds(Set.of(type.getApTypeId()));
    }

    /** The contract arguments as Elza's advanced access-point search filter. */
    private ApAdvanceSearchFilter toSearchFilter(final SearchEntitiesParams params, final StaticDataProvider sdp) {
        ApAdvanceSearchFilter filter = new ApAdvanceSearchFilter();
        if (StringUtils.isNotBlank(params.getFulltext())) {
            filter.setSearch(params.getFulltext().trim());
        }
        filter.setArea(toSearchArea(params.getArea()));
        filter.setOnlyMainPart(Boolean.TRUE.equals(params.getOnlyMainPart()));
        if (params.getRelatedTo() != null) {
            filter.setRelFilters(List.of(toRelationFilter(params.getRelatedTo(), sdp)));
        }
        return filter;
    }

    /** The contract search area as Elza's; every name part is the default. */
    private ApSearchArea toSearchArea(final EntitySearchArea area) {
        if (area == null) {
            return ApSearchArea.ALL_NAMES;
        }
        return switch (area) {
            case PREFER_NAMES -> ApSearchArea.PREFER_NAMES;
            case ALL_NAMES -> ApSearchArea.ALL_NAMES;
            case ALL_PARTS -> ApSearchArea.ALL_PARTS;
        };
    }

    /**
     * The contract relation filter ("has a reference to entity X") as Elza's,
     * resolving the optional relation type/spec codes to ids. Without a type the
     * filter matches any reference anywhere in the description (the shared
     * {@code rel_ap_id} index field) — exactly the incoming-relations semantics.
     */
    private ApSearchByRelation toRelationFilter(final EntityRelationFilter relatedTo, final StaticDataProvider sdp) {
        if (relatedTo.getAccessPointId() == null) {
            throw new IllegalArgumentException("relatedTo requires an accessPointId");
        }
        ApSearchByRelation relation = new ApSearchByRelation();
        relation.setCode(relatedTo.getAccessPointId());
        if (StringUtils.isNotBlank(relatedTo.getRelationTypeCode())) {
            ItemType relationType = sdp.getItemTypeByCode(relatedTo.getRelationTypeCode().trim().toUpperCase());
            if (relationType == null) {
                throw new IllegalArgumentException(
                        "Unknown relation type code: " + relatedTo.getRelationTypeCode().trim());
            }
            relation.setRelTypeId(relationType.getItemTypeId());
            if (StringUtils.isNotBlank(relatedTo.getRelationSpecCode())) {
                RulItemSpec spec = relationType.getItemSpecByCode(relatedTo.getRelationSpecCode().trim().toUpperCase());
                if (spec == null) {
                    throw new IllegalArgumentException("Unknown relation specification code: "
                            + relatedTo.getRelationSpecCode().trim());
                }
                relation.setRelSpecId(spec.getItemSpecId());
            }
        } else if (StringUtils.isNotBlank(relatedTo.getRelationSpecCode())) {
            throw new IllegalArgumentException("relationSpecCode requires a relationTypeCode");
        }
        return relation;
    }
}
