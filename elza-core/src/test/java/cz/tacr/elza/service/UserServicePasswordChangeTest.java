package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.security.OverrideProperties;
import cz.tacr.elza.security.OverrideProperties.OverrideUser;
import cz.tacr.elza.security.SiemAuditLogger;

/**
 * Password change (including its SIEM log events) in {@link UserService}.
 */
class UserServicePasswordChangeTest {

    private static final String USERNAME = "novak";
    private static final String PASSWORD = "tajneHeslo123";
    private static final String NEW_PASSWORD = "noveHeslo456";
    private static final String OVERRIDE_PASSWORD = "z-konfigurace";
    private static final OffsetDateTime OLD_VALID_FROM = OffsetDateTime.parse("2016-01-01T00:00:00+01:00");

    private UserService userService;
    private AuthenticationRepository authenticationRepository;
    private PasswordPolicyService passwordPolicyService;
    private SiemAuditLogger siemAuditLogger;
    private OverrideProperties overrideProperties;
    private UsrUser user;
    private UsrAuthentication authentication;

    @BeforeEach
    void setUp() {
        // other tests leave an authentication on the thread; these run without a logged user
        SecurityContextHolder.clearContext();
        userService = new UserService();
        authenticationRepository = mock(AuthenticationRepository.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        siemAuditLogger = mock(SiemAuditLogger.class);
        ReflectionTestUtils.setField(userService, "authenticationRepository", authenticationRepository);
        ReflectionTestUtils.setField(userService, "passwordPolicyService", passwordPolicyService);
        ReflectionTestUtils.setField(userService, "siemAuditLogger", siemAuditLogger);
        overrideProperties = new OverrideProperties();
        ReflectionTestUtils.setField(userService, "overrideProperties", overrideProperties);
        ReflectionTestUtils.setField(userService, "eventNotificationService", mock(IEventNotificationService.class));
        ReflectionTestUtils.setField(userService, "allowDefaultUser", true);
        ReflectionTestUtils.setField(userService, "defaultUsername", "admin");

        user = new UsrUser();
        user.setUserId(10);
        user.setUsername(USERNAME);

        authentication = new UsrAuthentication();
        authentication.setAuthenticationId(1);
        authentication.setUser(user);
        authentication.setAuthType(UsrAuthentication.AuthType.PASSWORD);
        authentication.setAuthValue(userService.encodePassword(PASSWORD));
        authentication.setValidFrom(OLD_VALID_FROM);
        when(authenticationRepository.findByUserAndAuthType(user, UsrAuthentication.AuthType.PASSWORD))
                .thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void selfChangeClearsChangeRequiredAndRefreshesValidFrom() {
        authentication.setChangeRequired(true);
        authentication.setNeverExpire(true);

        userService.changePassword(user, PASSWORD, NEW_PASSWORD);

        verify(passwordPolicyService).validate(NEW_PASSWORD);
        verify(authenticationRepository).save(authentication);
        assertThat(userService.matchesPassword(NEW_PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
        assertThat(authentication.getChangeRequired()).isFalse();
        assertThat(authentication.getNeverExpire()).isTrue();
        assertThat(authentication.getValidFrom()).isAfter(OLD_VALID_FROM);
    }

    @Test
    void adminChangeSetsFlags() {
        userService.changePassword(user, NEW_PASSWORD, true, true);

        assertThat(authentication.getChangeRequired()).isTrue();
        assertThat(authentication.getNeverExpire()).isTrue();

        userService.changePassword(user, NEW_PASSWORD, null, false);

        assertThat(authentication.getChangeRequired()).isFalse();
        assertThat(authentication.getNeverExpire()).isFalse();
    }

    @Test
    void policyViolationKeepsPassword() {
        String authValue = authentication.getAuthValue();
        doThrow(new BusinessException("x", BaseCode.INVALID_STATE)).when(passwordPolicyService).validate(any());

        assertThatThrownBy(() -> userService.changePassword(user, PASSWORD, "a"))
                .isInstanceOf(BusinessException.class);

        assertThat(authentication.getAuthValue()).isEqualTo(authValue);
        verify(authenticationRepository, never()).save(any());
    }

    @Test
    void defaultUserPasswordCannotBeChanged() {
        UsrUser admin = new UsrUser();
        admin.setUsername("admin");
        ReflectionTestUtils.setField(userService, "defaultPassword", userService.encodePassword(PASSWORD));

        assertThatThrownBy(() -> userService.changePassword(admin, NEW_PASSWORD, null, null))
                .isInstanceOf(BusinessException.class);
        verify(authenticationRepository, never()).save(any());
    }

    // no security context in these tests, so the actor is null

    @Test
    void selfChangeIsLoggedToSiem() {
        userService.changePassword(user, PASSWORD, NEW_PASSWORD);

        verify(siemAuditLogger).passwordChanged(null, USERNAME);
    }

    @Test
    void adminChangeIsLoggedToSiem() {
        userService.changePassword(user, NEW_PASSWORD, null, null);

        verify(siemAuditLogger).passwordChanged(null, USERNAME);
    }

    @Test
    void wrongOldPasswordIsLoggedToSiem() {
        assertThatThrownBy(() -> userService.changePassword(user, "spatne", NEW_PASSWORD))
                .isInstanceOf(BusinessException.class);

        verify(siemAuditLogger).passwordChangeFailed(USERNAME, "OLD_PASSWORD_MISMATCH");
        verify(siemAuditLogger, never()).passwordChanged(any(), any());
        verify(authenticationRepository, never()).save(any());
    }

    private void configureOverride() {
        OverrideUser overrideUser = new OverrideUser();
        overrideUser.setUsername(USERNAME);
        overrideUser.setPassword(OVERRIDE_PASSWORD);
        overrideProperties.setOverride(List.of(overrideUser));
    }

    @Test
    void overridePasswordIsAcceptedAsOldPassword() {
        configureOverride();

        userService.changePassword(user, OVERRIDE_PASSWORD, NEW_PASSWORD);

        verify(authenticationRepository).save(authentication);
        assertThat(userService.matchesPassword(NEW_PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
    }

    @Test
    void overrideUserWithoutPasswordRowGetsOne() {
        configureOverride();
        when(authenticationRepository.findByUserAndAuthType(user, UsrAuthentication.AuthType.PASSWORD))
                .thenReturn(null);

        userService.changePassword(user, OVERRIDE_PASSWORD, NEW_PASSWORD);

        ArgumentCaptor<UsrAuthentication> saved = ArgumentCaptor.forClass(UsrAuthentication.class);
        verify(authenticationRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getAuthType()).isEqualTo(UsrAuthentication.AuthType.PASSWORD);
        assertThat(userService.matchesPassword(NEW_PASSWORD, saved.getValue().getAuthValue(), USERNAME)).isTrue();
    }

    @Test
    void userWithoutPasswordRowOutsideOverrideCannotChangePassword() {
        when(authenticationRepository.findByUserAndAuthType(user, UsrAuthentication.AuthType.PASSWORD))
                .thenReturn(null);

        assertThatThrownBy(() -> userService.changePassword(user, OVERRIDE_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(BusinessException.class);
        verify(authenticationRepository, never()).save(any());
    }
}
