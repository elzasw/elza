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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AuthenticationRepository;

/**
 * Password change and recovery login in {@link UserService}.
 */
class UserServicePasswordChangeTest {

    private static final String USERNAME = "novak";
    private static final String PASSWORD = "tajneHeslo123";
    private static final String NEW_PASSWORD = "noveHeslo456";
    private static final String RECOVERY_PASSWORD = "obnova-789";
    private static final OffsetDateTime OLD_VALID_FROM = OffsetDateTime.parse("2016-01-01T00:00:00+01:00");

    private UserService userService;
    private AuthenticationRepository authenticationRepository;
    private PasswordPolicyService passwordPolicyService;
    private UsrUser user;
    private UsrAuthentication authentication;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        authenticationRepository = mock(AuthenticationRepository.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        ReflectionTestUtils.setField(userService, "authenticationRepository", authenticationRepository);
        ReflectionTestUtils.setField(userService, "passwordPolicyService", passwordPolicyService);
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

    private void configureRecovery(final String username, final String password) {
        ReflectionTestUtils.setField(userService, "recoveryUsername", username);
        ReflectionTestUtils.setField(userService, "recoveryPassword", password);
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

    @Test
    void recoveryPasswordIsAcceptedOnlyWhenConfigured() {
        assertThat(userService.isRecoveryPassword(USERNAME, RECOVERY_PASSWORD)).isFalse();

        configureRecovery(USERNAME, null);
        assertThat(userService.isRecoveryPassword(USERNAME, RECOVERY_PASSWORD)).isFalse();

        configureRecovery(USERNAME, RECOVERY_PASSWORD);
        assertThat(userService.isRecoveryPassword(USERNAME, RECOVERY_PASSWORD)).isTrue();
        assertThat(userService.isRecoveryPassword(USERNAME, "jine")).isFalse();
        assertThat(userService.isRecoveryPassword("jiny", RECOVERY_PASSWORD)).isFalse();
        assertThat(userService.matchesPassword(RECOVERY_PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
        // the user's own password keeps working
        assertThat(userService.matchesPassword(PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
    }

    @Test
    void recoveryNeverAppliesToDefaultUser() {
        configureRecovery("admin", RECOVERY_PASSWORD);
        assertThat(userService.isRecoveryPassword("admin", RECOVERY_PASSWORD)).isFalse();
    }

    @Test
    void recoveryPasswordIsAcceptedAsOldPassword() {
        configureRecovery(USERNAME, RECOVERY_PASSWORD);

        userService.changePassword(user, RECOVERY_PASSWORD, NEW_PASSWORD);

        assertThat(authentication.getAuthValue()).startsWith("{bcrypt}");
        assertThat(userService.matchesPassword(NEW_PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
    }
}
