package cz.tacr.elza.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;

/**
 * {@link PasswordAutheticationProvider}: the need-change-password flag and the recovery login.
 */
class PasswordAutheticationProviderTest {

    private static final String USERNAME = "novak";
    private static final String PASSWORD = "tajneHeslo123";

    private UserService userService;
    private PasswordAutheticationProvider provider;
    private UsrAuthentication usrAuthentication;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        provider = new PasswordAutheticationProvider(userService, mock(PlatformTransactionManager.class),
                                                     mock(SiemAuditLogger.class));

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
    void recoveryLoginNeedsChangeAndDoesNotStorePassword() {
        when(userService.isRecoveryPassword(USERNAME, PASSWORD)).thenReturn(true);

        assertThat(login().isNeedChangePassword()).isTrue();
        verify(userService, never()).upgradePasswordEncodingIfNeeded(any(), any());
    }
}
