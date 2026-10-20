package cz.tacr.elza.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

/**
 * {@link PasswordAutheticationProvider}: the need-change-password flag, the password from
 * elza.security.override and the method and reason of a login in the SIEM log.
 */
class PasswordAutheticationProviderTest {

    private static final String USERNAME = "novak";
    private static final String PASSWORD = "tajneHeslo123";
    private static final String OVERRIDE_PASSWORD = "z-konfigurace";

    private UserService userService;
    private SiemAuditLogger siemAuditLogger;
    private PasswordAutheticationProvider provider;
    private UsrAuthentication usrAuthentication;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        siemAuditLogger = mock(SiemAuditLogger.class);
        provider = new PasswordAutheticationProvider(userService, mock(PlatformTransactionManager.class),
                                                     siemAuditLogger);

        UsrUser user = new UsrUser();
        user.setUserId(10);
        user.setUsername(USERNAME);
        user.setActive(true);

        usrAuthentication = new UsrAuthentication();
        usrAuthentication.setAuthenticationId(1);
        usrAuthentication.setAuthType(UsrAuthentication.AuthType.PASSWORD);
        usrAuthentication.setAuthValue("{bcrypt}hash");

        when(userService.findByUsername(USERNAME)).thenReturn(user);
        when(userService.findAuthentication(user, UsrAuthentication.AuthType.PASSWORD)).thenReturn(usrAuthentication);
        when(userService.matchesPassword(PASSWORD, "{bcrypt}hash", USERNAME)).thenReturn(true);
        when(userService.createAuthentication(user)).thenAnswer(inv -> {
            var token = new UsernamePasswordAuthenticationToken(USERNAME, StringUtils.EMPTY, null);
            token.setDetails(new UserDetail(user, Collections.emptyList(), null,
                                            List.of(UsrAuthentication.AuthType.PASSWORD)));
            return token;
        });
    }

    private UserDetail login() {
        Authentication result = provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD));
        return (UserDetail) result.getDetails();
    }

    @Test
    void validPasswordNeedsNoChange() {
        assertThat(login().isNeedChangePassword()).isFalse();
        verify(userService).upgradePasswordEncodingIfNeeded(usrAuthentication, PASSWORD);
    }

    @Test
    void expiredPasswordNeedsChange() {
        when(userService.needsPasswordChange(usrAuthentication)).thenReturn(true);
        assertThat(login().isNeedChangePassword()).isTrue();
    }

    @Test
    void wrongPasswordIsLoggedAsInvalidPassword() {
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(siemAuditLogger).loginFailed(USERNAME, null, AuthenticationType.PASSWORD, "INVALID_PASSWORD", null);
    }

    @Test
    void unknownUserIsLoggedAsInvalidUsername() {
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("nobody", PASSWORD)))
                .isInstanceOf(UsernameNotFoundException.class);
        verify(siemAuditLogger).loginFailed("nobody", null, AuthenticationType.PASSWORD, "INVALID_USERNAME", null);
    }

    @Test
    void overridePasswordIsAcceptedWithoutChange() {
        when(userService.matchesOverridePassword(USERNAME, OVERRIDE_PASSWORD)).thenReturn(true);
        when(userService.needsPasswordChange(usrAuthentication)).thenReturn(true);

        Authentication result = provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME,
                                                                                               OVERRIDE_PASSWORD));

        assertThat(((UserDetail) result.getDetails()).isNeedChangePassword()).isFalse();
        verify(userService, never()).upgradePasswordEncodingIfNeeded(any(), any());
        verify(siemAuditLogger).loginSuccess(USERNAME, null, AuthenticationType.OVERRIDE);
    }

    @Test
    void ownPasswordOfOverrideUserIsLoggedAsPassword() {
        when(userService.isOverrideUser(USERNAME)).thenReturn(true);

        login();

        verify(siemAuditLogger).loginSuccess(USERNAME, null, AuthenticationType.PASSWORD);
    }

    @Test
    void overrideUserWithoutPasswordRowLogsIn() {
        when(userService.findAuthentication(any(UsrUser.class), eq(UsrAuthentication.AuthType.PASSWORD)))
                .thenReturn(null);
        when(userService.isOverrideUser(USERNAME)).thenReturn(true);
        when(userService.matchesOverridePassword(USERNAME, OVERRIDE_PASSWORD)).thenReturn(true);

        provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, OVERRIDE_PASSWORD));

        verify(siemAuditLogger).loginSuccess(USERNAME, null, AuthenticationType.OVERRIDE);
    }

    @Test
    void wrongPasswordOfOverrideUserWithoutPasswordRowIsInvalidPassword() {
        when(userService.findAuthentication(any(UsrUser.class), eq(UsrAuthentication.AuthType.PASSWORD)))
                .thenReturn(null);
        when(userService.isOverrideUser(USERNAME)).thenReturn(true);

        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(siemAuditLogger).loginFailed(USERNAME, null, AuthenticationType.PASSWORD, "INVALID_PASSWORD", null);
    }
}
