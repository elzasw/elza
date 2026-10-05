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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.security.Sha256Support;

/**
 * {@link UserService#applyConfiguredPassword}: the password from elza.security.admin.password
 * replaces the password of an existing user, unless the user already has it.
 */
class UserServiceConfiguredPasswordTest {

    private static final String SALT = "kdFss=+4Df_%";
    private static final String USERNAME = "spravce";
    private static final String PASSWORD = "Heslo-123";
    private static final OffsetDateTime OLD_VALID_FROM = OffsetDateTime.parse("2016-01-01T00:00:00+01:00");

    private UserService userService;
    private AuthenticationRepository authenticationRepository;
    private PasswordPolicyService passwordPolicyService;
    private UsrUser user;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        authenticationRepository = mock(AuthenticationRepository.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        ReflectionTestUtils.setField(userService, "authenticationRepository", authenticationRepository);
        ReflectionTestUtils.setField(userService, "passwordPolicyService", passwordPolicyService);
        ReflectionTestUtils.setField(userService, "eventNotificationService", mock(IEventNotificationService.class));
        ReflectionTestUtils.setField(userService, "SALT", SALT);

        user = new UsrUser();
        user.setUserId(7);
        user.setUsername(USERNAME);
    }

    private UsrAuthentication givenPassword(final String authValue) {
        UsrAuthentication authentication = new UsrAuthentication();
        authentication.setAuthenticationId(3);
        authentication.setUser(user);
        authentication.setAuthType(UsrAuthentication.AuthType.PASSWORD);
        authentication.setAuthValue(authValue);
        authentication.setValidFrom(OLD_VALID_FROM);
        authentication.setChangeRequired(true);
        when(authenticationRepository.findByUserAndAuthType(user, UsrAuthentication.AuthType.PASSWORD))
                .thenReturn(authentication);
        return authentication;
    }

    @Test
    void differentPasswordIsReplaced() {
        UsrAuthentication authentication = givenPassword(userService.encodePassword("jine-heslo"));

        assertThat(userService.applyConfiguredPassword(user, PASSWORD)).isTrue();

        verify(passwordPolicyService).validate(PASSWORD);
        verify(authenticationRepository).save(authentication);
        assertThat(userService.matchesPassword(PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
        assertThat(authentication.getValidFrom()).isAfter(OLD_VALID_FROM);
        assertThat(authentication.getChangeRequired()).isFalse();
    }

    @Test
    void samePasswordIsLeftAlone() {
        UsrAuthentication authentication = givenPassword(userService.encodePassword(PASSWORD));

        assertThat(userService.applyConfiguredPassword(user, PASSWORD)).isFalse();

        verify(authenticationRepository, never()).save(any());
        // the validity keeps running
        assertThat(authentication.getValidFrom()).isEqualTo(OLD_VALID_FROM);
    }

    @Test
    void oldHashFormatIsRewritten() {
        UsrAuthentication authentication = givenPassword(Sha256Support.encodePassword(PASSWORD, USERNAME + SALT));

        assertThat(userService.applyConfiguredPassword(user, PASSWORD)).isTrue();

        assertThat(authentication.getAuthValue()).startsWith("{bcrypt}");
    }

    @Test
    void passwordAuthenticationIsAddedWhenMissing() {
        assertThat(userService.applyConfiguredPassword(user, PASSWORD)).isTrue();

        ArgumentCaptor<UsrAuthentication> saved = ArgumentCaptor.forClass(UsrAuthentication.class);
        verify(authenticationRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getAuthType()).isEqualTo(UsrAuthentication.AuthType.PASSWORD);
        assertThat(userService.matchesPassword(PASSWORD, saved.getValue().getAuthValue(), USERNAME)).isTrue();
    }

    @Test
    void weakPasswordIsRejected() {
        givenPassword(userService.encodePassword("jine-heslo"));
        doThrow(new BusinessException("x", UserCode.PASSWORD_POLICY_VIOLATION)).when(passwordPolicyService).validate(any());

        assertThatThrownBy(() -> userService.applyConfiguredPassword(user, "a"))
                .isInstanceOf(BusinessException.class);
        verify(authenticationRepository, never()).save(any());
    }
}
