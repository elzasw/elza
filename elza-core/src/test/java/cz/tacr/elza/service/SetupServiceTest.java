package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.OverrideProperties;
import cz.tacr.elza.security.OverrideProperties.OverrideUser;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.ConfigAdminAction;

/**
 * {@link SetupService}: the first administrator is created only while no user exists;
 * the users from elza.security.override are applied at every startup.
 */
class SetupServiceTest {

    private static final String USERNAME = "spravce";
    private static final String PASSWORD = "Heslo-123";

    private SetupService setupService;
    private UserRepository userRepository;
    private UserService userService;
    private PasswordPolicyService passwordPolicyService;
    private SiemAuditLogger siemAuditLogger;
    private OverrideProperties overrideProperties;

    @BeforeEach
    void setUp() {
        setupService = new SetupService();
        userRepository = mock(UserRepository.class);
        userService = mock(UserService.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        siemAuditLogger = mock(SiemAuditLogger.class);
        overrideProperties = new OverrideProperties();
        ReflectionTestUtils.setField(setupService, "userRepository", userRepository);
        ReflectionTestUtils.setField(setupService, "userService", userService);
        ReflectionTestUtils.setField(setupService, "passwordPolicyService", passwordPolicyService);
        ReflectionTestUtils.setField(setupService, "txManager", mock(PlatformTransactionManager.class));
        ReflectionTestUtils.setField(setupService, "siemAuditLogger", siemAuditLogger);
        ReflectionTestUtils.setField(setupService, "overrideProperties", overrideProperties);
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
        verify(siemAuditLogger).initialAdminCreated(USERNAME);
    }

    @Test
    void setupIsClosedOnceAUserExists() {
        when(userRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> setupService.createAdmin(USERNAME, PASSWORD))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getErrorCode()).isEqualTo(UserCode.SETUP_NOT_AVAILABLE));
        verify(userService, never()).createInitialAdmin(any(), any());
    }

    private void configureOverride(final String... usernameAndPassword) {
        List<OverrideUser> users = new ArrayList<>();
        for (int i = 0; i < usernameAndPassword.length; i += 2) {
            OverrideUser user = new OverrideUser();
            user.setUsername(usernameAndPassword[i]);
            user.setPassword(usernameAndPassword[i + 1]);
            users.add(user);
        }
        overrideProperties.setOverride(users);
    }

    private UsrUser givenUser(final String username, final boolean active) {
        UsrUser user = new UsrUser();
        user.setUsername(username);
        user.setActive(active);
        when(userRepository.findByUsername(username)).thenReturn(user);
        return user;
    }

    @Test
    void missingOverrideUserIsCreatedWithoutPassword() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(null);
        configureOverride(" " + USERNAME + " ", PASSWORD);

        setupService.applyOverrideFromConfiguration();

        verify(userService).createInitialAdmin(eq(USERNAME), isNull());
        verify(siemAuditLogger).overrideActive(USERNAME);
        verify(siemAuditLogger).configAdminApplied(USERNAME, ConfigAdminAction.USER_CREATED);
    }

    @Test
    void existingActiveOverrideUserIsNotChanged() {
        givenUser(USERNAME, true);
        configureOverride(USERNAME, PASSWORD);

        setupService.applyOverrideFromConfiguration();

        verify(userService, never()).createInitialAdmin(any(), any());
        verify(userRepository, never()).save(any());
        verify(siemAuditLogger).overrideActive(USERNAME);
        verify(siemAuditLogger, never()).configAdminApplied(any(), any());
    }

    @Test
    void inactiveOverrideUserIsActivated() {
        UsrUser user = givenUser(USERNAME, false);
        configureOverride(USERNAME, PASSWORD);

        setupService.applyOverrideFromConfiguration();

        assertThat(user.getActive()).isTrue();
        verify(userRepository).save(user);
        verify(siemAuditLogger).configAdminApplied(USERNAME, ConfigAdminAction.USER_ACTIVATED);
    }

    @Test
    void allOverrideUsersAreApplied() {
        givenUser(USERNAME, true);
        when(userRepository.findByUsername("druhy")).thenReturn(null);
        configureOverride(USERNAME, PASSWORD, "druhy", "Jine-Heslo-1");

        setupService.applyOverrideFromConfiguration();

        verify(siemAuditLogger).overrideActive(USERNAME);
        verify(siemAuditLogger).overrideActive("druhy");
        verify(userService).createInitialAdmin(eq("druhy"), isNull());
    }

    @Test
    void failureOfOneOverrideUserDoesNotStopTheOthers() {
        when(userRepository.findByUsername(any())).thenReturn(null);
        when(userService.createInitialAdmin(eq("admin"), any()))
                .thenThrow(new BusinessException("x", UserCode.USERNAME_EXISTS));
        configureOverride("admin", PASSWORD, USERNAME, PASSWORD);

        setupService.applyOverrideFromConfiguration();

        verify(siemAuditLogger).configAdminFailed("admin", "x");
        verify(siemAuditLogger).configAdminApplied(USERNAME, ConfigAdminAction.USER_CREATED);
    }

    @Test
    void weakOverridePasswordIsOnlyReported() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(null);
        doThrow(new BusinessException("x", UserCode.PASSWORD_POLICY_VIOLATION)).when(passwordPolicyService).validate(any());
        configureOverride(USERNAME, "a");

        setupService.applyOverrideFromConfiguration();

        verify(userService).createInitialAdmin(eq(USERNAME), isNull());
        verify(siemAuditLogger, never()).configAdminFailed(any(), any());
    }

    @Test
    void removedAdminConfigurationStopsTheStartup() {
        ReflectionTestUtils.setField(setupService, "legacyAdminUsername", USERNAME);

        assertThatThrownBy(() -> setupService.applyOverrideFromConfiguration())
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void incompleteOrDuplicateOverrideStopsTheStartup() {
        configureOverride(USERNAME, null);
        assertThatThrownBy(() -> setupService.applyOverrideFromConfiguration())
                .isInstanceOf(IllegalStateException.class);

        configureOverride(USERNAME, PASSWORD, " " + USERNAME, PASSWORD);
        assertThatThrownBy(() -> setupService.applyOverrideFromConfiguration())
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).findByUsername(any());
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
