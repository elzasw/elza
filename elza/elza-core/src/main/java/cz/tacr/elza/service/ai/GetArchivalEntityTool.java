package cz.tacr.elza.service.ai;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ArchivalEntity;
import cz.tacr.elza.aiprovider.client.vo.GetArchivalEntityParams;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;

/**
 * Standard {@code getArchivalEntity} tool — fetches one archival entity (access
 * point) in full, requested by the AI model mid-turn, typically to follow a
 * {@code RECORD_REF} reference the sent context only names. Argument/result
 * shapes are defined by the AI provider contract ({@code GetArchivalEntityParams}
 * / {@code ArchivalEntity}); the result is the same payload the client sends as
 * {@code elza.archivalEntity} context, built by {@link AiContextResolver}.
 *
 * <p>The tool runs on the poller thread, outside the request security context,
 * so it enforces the conversation owner's scope read permission itself
 * ({@code AP_SCOPE_RD}). A missing entity and an unreadable one are answered
 * with the same error, so the model cannot probe which entities exist.
 */
@Component
public class GetArchivalEntityTool implements AiTool {

    private final UserService userService;
    private final AccessPointCacheService accessPointCacheService;
    private final ApAccessPointRepository accessPointRepository;
    private final AiContextResolver aiContextResolver;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public GetArchivalEntityTool(final UserService userService,
                                 final AccessPointCacheService accessPointCacheService,
                                 final ApAccessPointRepository accessPointRepository,
                                 final AiContextResolver aiContextResolver,
                                 final TransactionTemplate transactionTemplate,
                                 final ObjectMapper objectMapper) {
        this.userService = userService;
        this.accessPointCacheService = accessPointCacheService;
        this.accessPointRepository = accessPointRepository;
        this.aiContextResolver = aiContextResolver;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public StandardToolName name() {
        return StandardToolName.GET_ARCHIVAL_ENTITY;
    }

    @Override
    public Object execute(final Object arguments, final AiToolContext context) {
        GetArchivalEntityParams params = objectMapper.convertValue(arguments, GetArchivalEntityParams.class);
        boolean hasId = params != null && params.getAccessPointId() != null;
        boolean hasUuid = params != null && StringUtils.isNotBlank(params.getUuid());
        if (hasId == hasUuid) {
            throw new IllegalArgumentException(
                    "getArchivalEntity requires exactly one of accessPointId or uuid");
        }
        // The poller thread has no transaction; the cache read and the reference
        // enrichment (lazy scope/rule-set entities) need one.
        return transactionTemplate.execute(status -> {
            CachedAccessPoint cap = findAccessPoint(params, hasId);
            if (cap == null || cap.getApState() == null
                    || !canReadScope(cap.getApState().getScopeId(), context)) {
                // one message for "missing" and "not readable" — existence must not leak
                throw new IllegalArgumentException("Archival entity not found: "
                        + (hasId ? "accessPointId=" + params.getAccessPointId()
                                 : "uuid=" + params.getUuid().trim()));
            }
            ArchivalEntity entity = aiContextResolver.buildArchivalEntity(cap);
            aiContextResolver.enrichEntityRefs(entity);
            return entity;
        });
    }

    /** Loads the entity from the access-point cache, resolving a UUID to its id first. */
    private CachedAccessPoint findAccessPoint(final GetArchivalEntityParams params, final boolean hasId) {
        Integer accessPointId;
        if (hasId) {
            accessPointId = params.getAccessPointId();
        } else {
            ApAccessPoint accessPoint = accessPointRepository.findAccessPointByUuid(params.getUuid().trim());
            if (accessPoint == null) {
                return null;
            }
            accessPointId = accessPoint.getAccessPointId();
        }
        return accessPointCacheService.findCachedAccessPoint(accessPointId);
    }

    /**
     * True when the conversation owner may read access points in the given scope —
     * the same {@code AP_SCOPE_RD} rule the context resolver applies, evaluated
     * from the user's stored permissions (no security context on the poller
     * thread).
     */
    private boolean canReadScope(final Integer scopeId, final AiToolContext context) {
        if (context.userId() == null) {
            // the virtual admin account: no user row, full permissions
            return true;
        }
        Collection<UserPermission> permissions = userService.getUserPermissions(context.userId());
        return permissions.stream().anyMatch(p -> p.getPermission() == Permission.ADMIN
                || p.getPermission() == Permission.AP_SCOPE_RD_ALL
                || (p.getPermission() == Permission.AP_SCOPE_RD && p.getScopeIds().contains(scopeId)));
    }
}
