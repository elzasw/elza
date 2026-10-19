package cz.tacr.elza.service;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.SiemAuditLogger;

/**
 * Activation, deactivation and renaming of a user in {@link UserService} and their SIEM log events.
 */
class UserServiceUserLifecycleTest {

    private static final String USERNAME = "novak";

    private UserService userService;
    private SiemAuditLogger siemAuditLogger;
    private UsrUser user;

    @BeforeEach
    void setUp() {
        // other tests leave an authentication on the thread; these run without a logged user
        SecurityContextHolder.clearContext();
        userService = new UserService();
        siemAuditLogger = mock(SiemAuditLogger.class);
        ReflectionTestUtils.setField(userService, "userRepository", mock(UserRepository.class));
        ReflectionTestUtils.setField(userService, "authenticationRepository", mock(AuthenticationRepository.class));
        ReflectionTestUtils.setField(userService, "eventNotificationService", mock(IEventNotificationService.class));
        ReflectionTestUtils.setField(userService, "siemAuditLogger", siemAuditLogger);

        user = new UsrUser();
        user.setUserId(10);
        user.setUsername(USERNAME);
        user.setActive(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // no security context in these tests, so the actor is null

    @Test
    void deactivationIsLoggedToSiem() {
        userService.changeActive(user, false);

        verify(siemAuditLogger).userActiveChanged(null, USERNAME, false);
    }

    @Test
    void activationIsLoggedToSiem() {
        user.setActive(false);

        userService.changeActive(user, true);

        verify(siemAuditLogger).userActiveChanged(null, USERNAME, true);
    }

    @Test
    void unchangedStateIsNotLogged() {
        userService.changeActive(user, true);

        verify(siemAuditLogger, never()).userActiveChanged(any(), any(), anyBoolean());
    }

    // the user has no authentication, so a rename needs no new password

    @Test
    void renameIsLoggedToSiem() {
        userService.changeUser(user, null, "novy", Map.of());

        verify(siemAuditLogger).userRenamed(null, USERNAME, "novy");
    }

    @Test
    void unchangedUsernameIsNotLogged() {
        userService.changeUser(user, null, USERNAME, Map.of());

        verify(siemAuditLogger, never()).userRenamed(any(), any(), any());
    }
}
