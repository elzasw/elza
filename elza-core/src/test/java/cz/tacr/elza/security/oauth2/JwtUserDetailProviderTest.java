package cz.tacr.elza.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.oauth2.OAuth2Properties.PermProperties;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;

/**
 * Login with a bearer token: which users are accepted or created and when their permissions are
 * synchronized.
 */
class JwtUserDetailProviderTest {

    private static final String TOKEN = "token";

    private JwtDecoder jwtDecoder;
    private UserService userService;
    private AccessPointService apService;
    private ItemTypeRepository itemTypeRepository;
    private SiemAuditLogger siemAuditLogger;
    private OAuth2Properties properties;
    private UserDetail userDetail;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        userService = mock(UserService.class);
        apService = mock(AccessPointService.class);
        itemTypeRepository = mock(ItemTypeRepository.class);
        siemAuditLogger = mock(SiemAuditLogger.class);
        properties = new OAuth2Properties();
        userDetail = mock(UserDetail.class);

        when(userService.createSecurityContextSystem()).thenReturn(SecurityContextHolder.createEmptyContext());
        when(userService.createUserDetail(any(UsrUser.class))).thenReturn(userDetail);
    }

    private JwtUserDetailProvider provider() {
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new JwtUserDetailProvider(jwtDecoder, txManager, userService, apService, itemTypeRepository,
                properties, siemAuditLogger);
    }

    private void tokenWithClaims(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .claims(c -> c.putAll(claims))
                .build();
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwt);
    }

    private Authentication login() {
        return provider().authenticate(new BearerTokenAuthenticationToken(TOKEN));
    }

    private static UsrUser user(int userId, String username) {
        UsrUser user = new UsrUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setActive(true);
        return user;
    }

    private static PermProperties mapping(String authority, String scope, String... permissions) {
        PermProperties props = new PermProperties();
        props.setAuthority(authority);
        props.setScope(scope);
        props.setPermissions(List.of(permissions));
        return props;
    }

    private static ApScope scope(int scopeId) {
        ApScope scope = new ApScope();
        scope.setScopeId(scopeId);
        return scope;
    }

    @Test
    void permissionsOfExistingUserAreKeptWhenMappingIsNotConfigured() {
        tokenWithClaims(Map.of("sub", "jdoe", "authorities", List.of("ELZA_ADMIN")));
        when(userService.findByUsername("jdoe")).thenReturn(user(1, "jdoe"));

        Authentication result = login();

        assertSame(userDetail, result.getDetails());
        verify(userService, never()).changeUserPermission(any(), any());
    }

    @Test
    void permissionsAreSynchronizedFromMappedAuthorities() {
        properties.setPermissions(List.of(
                mapping("ELZA_ADMIN", null, "ADMIN"),
                mapping("READER_A", "SCOPE_A", "AP_SCOPE_RD"),
                mapping("READER_B", "SCOPE_B", "AP_SCOPE_RD")));
        when(apService.getApScope("SCOPE_A")).thenReturn(scope(10));
        when(apService.getApScope("SCOPE_B")).thenReturn(scope(20));
        tokenWithClaims(Map.of("sub", "jdoe", "authorities", List.of("READER_A", "READER_B", "UNKNOWN")));
        UsrUser user = user(1, "jdoe");
        when(userService.findByUsername("jdoe")).thenReturn(user);

        login();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UsrPermission>> perms = ArgumentCaptor.forClass(List.class);
        verify(userService).changeUserPermission(eq(user), perms.capture());
        List<Integer> scopeIds = perms.getValue().stream().map(UsrPermission::getScopeId).toList();
        assertEquals(List.of(10, 20), scopeIds);
        perms.getValue().forEach(p -> assertEquals(Permission.AP_SCOPE_RD, p.getPermission()));
    }

    @Test
    void emptyMappingRemovesDirectPermissions() {
        properties.setPermissions(List.of());
        tokenWithClaims(Map.of("sub", "jdoe"));
        UsrUser user = user(1, "jdoe");
        when(userService.findByUsername("jdoe")).thenReturn(user);

        login();

        verify(userService).changeUserPermission(user, List.of());
    }

    @Test
    void unknownUserIsRejectedWhenCreationIsDisabled() {
        properties.setCreateUsers(false);
        tokenWithClaims(Map.of("sub", "jdoe", "name", "John Doe"));

        assertThrows(UsernameNotFoundException.class, this::login);

        verify(userService, never()).createUser(anyString(), any(), any());
        verify(siemAuditLogger).loginFailed("jdoe", null, AuthenticationType.JWT, "INVALID_USERNAME", null);
    }

    @Test
    void unknownUserIsCreatedWithUserNameAsFallbackDisplayName() {
        ApScope userScope = scope(5);
        when(apService.getApScope("JWT_USERS")).thenReturn(userScope);
        ApType apType = new ApType();
        when(apService.getType("PERSON_INDIVIDUAL")).thenReturn(apType);
        RulItemType nmMain = new RulItemType();
        nmMain.setItemTypeId(77);
        when(itemTypeRepository.findOneByCode("NM_MAIN")).thenReturn(nmMain);
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setAccessPointId(99);
        ApState state = new ApState();
        state.setAccessPoint(accessPoint);
        when(apService.createAccessPoint(eq(userScope), eq(apType), any(ApPartFormVO.class))).thenReturn(state);
        when(userService.createUser(eq("jdoe"), isNull(), eq(99))).thenReturn(user(1, "jdoe"));
        tokenWithClaims(Map.of("sub", "jdoe"));

        login();

        ArgumentCaptor<ApPartFormVO> partForm = ArgumentCaptor.forClass(ApPartFormVO.class);
        verify(apService).createAccessPoint(eq(userScope), eq(apType), partForm.capture());
        ApItemStringVO name = (ApItemStringVO) partForm.getValue().getItems().get(0);
        assertEquals("jdoe", name.getValue());
    }

    @Test
    void inactiveUserIsRejectedAsLockedNotAsServerError() {
        tokenWithClaims(Map.of("sub", "jdoe"));
        when(userService.findByUsername("jdoe")).thenReturn(user(1, "jdoe"));
        when(userService.createUserDetail(any(UsrUser.class))).thenThrow(new LockedException("User is not active"));

        assertThrows(LockedException.class, this::login);

        verify(siemAuditLogger).loginFailed("jdoe", null, AuthenticationType.JWT, "INACTIVE_USER", null);
    }

    @Test
    void claimNamesAreConfigurable() {
        properties.setUsernameClaim("preferred_username");
        properties.setAuthoritiesClaim("roles");
        properties.setPermissions(List.of(mapping("elza-admin", null, "ADMIN")));
        tokenWithClaims(Map.of("sub", "f81d4fae-7dec", "preferred_username", "jdoe",
                "roles", List.of("elza-admin")));
        UsrUser user = user(1, "jdoe");
        when(userService.findByUsername("jdoe")).thenReturn(user);

        Authentication result = login();

        assertEquals("jdoe", result.getName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UsrPermission>> perms = ArgumentCaptor.forClass(List.class);
        verify(userService).changeUserPermission(eq(user), perms.capture());
        assertEquals(List.of(Permission.ADMIN), perms.getValue().stream().map(UsrPermission::getPermission).toList());
    }

    @Test
    void tokenWithoutUserNameIsRejected() {
        properties.setUsernameClaim("preferred_username");
        tokenWithClaims(Map.of("sub", "jdoe"));

        assertThrows(InvalidBearerTokenException.class, this::login);

        verify(userService, never()).findByUsername(anyString());
    }
}
