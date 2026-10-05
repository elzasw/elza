package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.ConfigAdminAction;

/**
 * {@link SetupService}: the first administrator is created only while no user exists.
 */
class SetupServiceTest {

    private static final String USERNAME = "spravce";
    private static final String PASSWORD = "Heslo-123";

    private SetupService setupService;
    private UserRepository userRepository;
    private UserService userService;
    private SiemAuditLogger siemAuditLogger;

    @BeforeEach
    void setUp() {
        setupService = new SetupService();
        userRepository = mock(UserRepository.class);
        userService = mock(UserService.class);
        siemAuditLogger = mock(SiemAuditLogger.class);
        ReflectionTestUtils.setField(setupService, "userRepository", userRepository);
        ReflectionTestUtils.setField(setupService, "userService", userService);
        ReflectionTestUtils.setField(setupService, "txManager", mock(PlatformTransactionManager.class));
        ReflectionTestUtils.setField(setupService, "siemAuditLogger", siemAuditLogger);
    }

    @Test
    void setupIsRequiredOnlyWithoutUsers() {
        when(userRepository.count()).thenReturn(0L);
        assertThat(setupService.isSetupRequired()).isTrue();

        when(userRepository.count()).thenReturn(1L);
        assertThat(setupService.isSetupRequired()).isFalse();
    }

    @Test
    void adminIsCreated() {
        when(userRepository.count()).thenReturn(0L);

        setupService.createAdmin(" " + USERNAME + " ", PASSWORD);

        verify(userService).createInitialAdmin(USERNAME, PASSWORD);
    }

    @Test
    void setupIsClosedOnceAUserExists() {
        when(userRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> setupService.createAdmin(USERNAME, PASSWORD))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getErrorCode()).isEqualTo(UserCode.SETUP_NOT_AVAILABLE));
        verify(userService, never()).createInitialAdmin(any(), any());
    }

    private void configureAdmin(final String username, final String password) {
        ReflectionTestUtils.setField(setupService, "configAdminUsername", username);
        ReflectionTestUtils.setField(setupService, "configAdminPassword", password);
    }

    @Test
    void missingConfiguredAdminIsCreatedEvenWhenOtherUsersExist() {
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.findByUsername(USERNAME)).thenReturn(null);
        configureAdmin(" " + USERNAME + " ", PASSWORD);

        setupService.applyAdminFromConfiguration();

        verify(userService).createInitialAdmin(USERNAME, PASSWORD);
        verify(userService, never()).applyConfiguredPassword(any(), any());
        verify(siemAuditLogger).configAdminApplied(USERNAME, ConfigAdminAction.USER_CREATED);
    }

    private UsrUser givenUser(final boolean active) {
        UsrUser user = new UsrUser();
        user.setUsername(USERNAME);
        user.setActive(active);
        when(userRepository.findByUsername(USERNAME)).thenReturn(user);
        return user;
    }

    @Test
    void existingConfiguredAdminGetsThePassword() {
        UsrUser user = givenUser(true);
        when(userService.applyConfiguredPassword(user, PASSWORD)).thenReturn(true);
        configureAdmin(USERNAME, PASSWORD);

        setupService.applyAdminFromConfiguration();

        verify(userService).applyConfiguredPassword(user, PASSWORD);
        verify(userService, never()).createInitialAdmin(any(), any());
        // an active user is not written
        verify(userRepository, never()).save(any());
        verify(siemAuditLogger).configAdminApplied(USERNAME, ConfigAdminAction.PASSWORD_SET);
        verify(siemAuditLogger, never()).configAdminApplied(USERNAME, ConfigAdminAction.USER_ACTIVATED);
    }

    @Test
    void inactiveConfiguredAdminIsActivated() {
        UsrUser user = givenUser(false);
        configureAdmin(USERNAME, PASSWORD);

        setupService.applyAdminFromConfiguration();

        assertThat(user.getActive()).isTrue();
        verify(userRepository).save(user);
        verify(userService).applyConfiguredPassword(user, PASSWORD);
        verify(siemAuditLogger).configAdminApplied(USERNAME, ConfigAdminAction.USER_ACTIVATED);
        // the password was already the configured one
        verify(siemAuditLogger, never()).configAdminApplied(USERNAME, ConfigAdminAction.PASSWORD_SET);
    }

    @Test
    void unchangedConfiguredAdminWritesNoAuditEvent() {
        givenUser(true);
        configureAdmin(USERNAME, PASSWORD);

        setupService.applyAdminFromConfiguration();

        verify(siemAuditLogger, never()).configAdminApplied(any(), any());
        verify(siemAuditLogger, never()).configAdminFailed(any(), any());
    }

    @Test
    void incompleteConfigurationIsIgnored() {
        configureAdmin(USERNAME, null);
        setupService.applyAdminFromConfiguration();
        configureAdmin(" ", PASSWORD);
        setupService.applyAdminFromConfiguration();

        verify(userRepository, never()).findByUsername(any());
        verify(userService, never()).createInitialAdmin(any(), any());
        verify(userService, never()).applyConfiguredPassword(any(), any());
    }

    @Test
    void failureOfConfiguredAdminDoesNotStopTheStartup() {
        configureAdmin("admin", PASSWORD);
        when(userService.createInitialAdmin(any(), any()))
                .thenThrow(new BusinessException("x", UserCode.USERNAME_EXISTS));

        setupService.applyAdminFromConfiguration();

        verify(userService).createInitialAdmin("admin", PASSWORD);
        verify(siemAuditLogger).configAdminFailed("admin", "x");
        verify(siemAuditLogger, never()).configAdminApplied(any(), any());
    }

    @Test
    void usernameIsRequired() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> setupService.createAdmin("  ", PASSWORD))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getErrorCode()).isEqualTo(BaseCode.PROPERTY_NOT_EXIST));
        verify(userService, never()).createInitialAdmin(any(), any());
    }
}
