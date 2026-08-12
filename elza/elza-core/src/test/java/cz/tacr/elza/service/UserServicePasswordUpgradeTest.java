package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.security.Sha256Support;

/**
 * {@link UserService#upgradePasswordEncodingIfNeeded}: a legacy SHA-256 hash is
 * re-encoded with the current encoder after a successful login, while the
 * synthetic default-user authentication and up-to-date hashes stay untouched.
 */
class UserServicePasswordUpgradeTest {

    private static final String SALT = "kdFss=+4Df_%";
    private static final String USERNAME = "novak";
    private static final String PASSWORD = "tajneHeslo123";

    private UserService userService;
    private AuthenticationRepository authenticationRepository;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        authenticationRepository = mock(AuthenticationRepository.class);
        ReflectionTestUtils.setField(userService, "authenticationRepository", authenticationRepository);
        ReflectionTestUtils.setField(userService, "SALT", SALT);
    }

    private UsrAuthentication createAuthentication(final Integer authenticationId, final String authValue) {
        UsrUser user = new UsrUser();
        user.setUsername(USERNAME);

        UsrAuthentication authentication = new UsrAuthentication();
        authentication.setAuthenticationId(authenticationId);
        authentication.setUser(user);
        authentication.setAuthType(UsrAuthentication.AuthType.PASSWORD);
        authentication.setAuthValue(authValue);
        return authentication;
    }

    @Test
    void legacyHashIsUpgraded() {
        String legacyHash = Sha256Support.encodePassword(PASSWORD, USERNAME + SALT);
        UsrAuthentication authentication = createAuthentication(1, legacyHash);
        assertThat(userService.matchesPassword(PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();

        userService.upgradePasswordEncodingIfNeeded(authentication, PASSWORD);

        assertThat(authentication.getAuthValue()).startsWith("{bcrypt}");
        assertThat(userService.matchesPassword(PASSWORD, authentication.getAuthValue(), USERNAME)).isTrue();
        verify(authenticationRepository).save(authentication);
    }

    @Test
    void syntheticDefaultUserIsSkipped() {
        String legacyHash = Sha256Support.encodePassword(PASSWORD, USERNAME + SALT);
        UsrAuthentication authentication = createAuthentication(null, legacyHash);

        userService.upgradePasswordEncodingIfNeeded(authentication, PASSWORD);

        assertThat(authentication.getAuthValue()).isEqualTo(legacyHash);
        verify(authenticationRepository, never()).save(authentication);
    }

    @Test
    void currentHashIsNotRewritten() {
        String currentHash = userService.encodePassword(PASSWORD);
        UsrAuthentication authentication = createAuthentication(1, currentHash);

        userService.upgradePasswordEncodingIfNeeded(authentication, PASSWORD);

        assertThat(authentication.getAuthValue()).isEqualTo(currentHash);
        verify(authenticationRepository, never()).save(authentication);
    }
}
