package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.ArchivalEntity;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;

/**
 * The {@code getArchivalEntity} tool: argument validation (exactly one
 * identifier), id/uuid resolution, scope read permission enforced from the
 * conversation owner's stored permissions, and the indistinguishable answer for
 * a missing vs. an unreadable entity.
 */
class GetArchivalEntityToolTest {

    private static final int AP_ID = 100;
    private static final int SCOPE_ID = 5;
    private static final int USER_ID = 42;
    private static final String UUID = "0f6dc4a2-0000-0000-0000-000000000001";

    private final UserService userService = mock(UserService.class);
    private final AccessPointCacheService cacheService = mock(AccessPointCacheService.class);
    private final ApAccessPointRepository accessPointRepository = mock(ApAccessPointRepository.class);
    private final AiContextResolver contextResolver = mock(AiContextResolver.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    private final GetArchivalEntityTool tool = new GetArchivalEntityTool(userService, cacheService,
            accessPointRepository, contextResolver, transactionTemplate, new ObjectMapper());

    @BeforeEach
    void wire() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation
                .<TransactionCallback<Object>>getArgument(0)
                .doInTransaction(mock(TransactionStatus.class)));
    }

    @Test
    void resolvesByAccessPointId() {
        CachedAccessPoint cap = cachedAccessPoint();
        when(cacheService.findCachedAccessPoint(AP_ID)).thenReturn(cap);
        grantScopeRead();
        ArchivalEntity entity = new ArchivalEntity().accessPointId(AP_ID);
        when(contextResolver.buildArchivalEntity(cap)).thenReturn(entity);

        Object result = tool.execute(Map.of("accessPointId", AP_ID), new AiToolContext(USER_ID));

        assertThat(result).isSameAs(entity);
        verify(contextResolver).enrichEntityRefs(entity);
    }

    @Test
    void resolvesByUuid() {
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setAccessPointId(AP_ID);
        when(accessPointRepository.findAccessPointByUuid(UUID)).thenReturn(accessPoint);
        CachedAccessPoint cap = cachedAccessPoint();
        when(cacheService.findCachedAccessPoint(AP_ID)).thenReturn(cap);
        grantScopeRead();
        ArchivalEntity entity = new ArchivalEntity().accessPointId(AP_ID).uuid(UUID);
        when(contextResolver.buildArchivalEntity(cap)).thenReturn(entity);

        Object result = tool.execute(Map.of("uuid", UUID), new AiToolContext(USER_ID));

        assertThat(result).isSameAs(entity);
    }

    @Test
    void requiresExactlyOneIdentifier() {
        assertThatThrownBy(() -> tool.execute(Map.of(), new AiToolContext(USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
        assertThatThrownBy(() -> tool.execute(Map.of("accessPointId", AP_ID, "uuid", UUID),
                new AiToolContext(USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void missingAndUnreadableEntityAnswerAlike() {
        // Missing: nothing cached under the id.
        assertThatThrownBy(() -> tool.execute(Map.of("accessPointId", AP_ID), new AiToolContext(USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Archival entity not found: accessPointId=" + AP_ID);

        // Unreadable: entity exists, but the owner has no read permission for its
        // scope — same message, so the model cannot probe which entities exist.
        when(cacheService.findCachedAccessPoint(AP_ID)).thenReturn(cachedAccessPoint());
        when(userService.getUserPermissions(USER_ID))
                .thenReturn(List.of(scopePermission(Permission.AP_SCOPE_RD, SCOPE_ID + 1)));
        assertThatThrownBy(() -> tool.execute(Map.of("accessPointId", AP_ID), new AiToolContext(USER_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Archival entity not found: accessPointId=" + AP_ID);
        verify(contextResolver, never()).buildArchivalEntity(any());
    }

    @Test
    void virtualAdminReadsWithoutPermissionLookup() {
        CachedAccessPoint cap = cachedAccessPoint();
        when(cacheService.findCachedAccessPoint(AP_ID)).thenReturn(cap);
        ArchivalEntity entity = new ArchivalEntity().accessPointId(AP_ID);
        when(contextResolver.buildArchivalEntity(cap)).thenReturn(entity);

        Object result = tool.execute(Map.of("accessPointId", AP_ID), new AiToolContext(null));

        assertThat(result).isSameAs(entity);
        verify(userService, never()).getUserPermissions(any());
    }

    // --- builders -----------------------------------------------------------

    private CachedAccessPoint cachedAccessPoint() {
        ApScope scope = new ApScope();
        scope.setScopeId(SCOPE_ID);
        ApState state = new ApState();
        state.setScope(scope);
        CachedAccessPoint cap = new CachedAccessPoint();
        cap.setAccessPointId(AP_ID);
        cap.setApState(state);
        return cap;
    }

    private void grantScopeRead() {
        when(userService.getUserPermissions(USER_ID))
                .thenReturn(List.of(scopePermission(Permission.AP_SCOPE_RD, SCOPE_ID)));
    }

    private UserPermission scopePermission(final Permission permission, final int scopeId) {
        UserPermission userPermission = new UserPermission(permission);
        userPermission.addScopeId(scopeId);
        return userPermission;
    }
}
