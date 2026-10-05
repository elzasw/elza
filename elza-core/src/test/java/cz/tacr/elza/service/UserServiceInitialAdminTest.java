package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.AuthenticationRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.UserRepository;

/**
 * {@link UserService#createInitialAdmin}: the first administrator has no access point,
 * a password checked by the policy and the ADMIN permission.
 */
class UserServiceInitialAdminTest {

    private static final String USERNAME = "spravce";
    private static final String PASSWORD = "Heslo-123";

    private UserService userService;
    private UserRepository userRepository;
    private AuthenticationRepository authenticationRepository;
    private PermissionRepository permissionRepository;
    private PasswordPolicyService passwordPolicyService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        userRepository = mock(UserRepository.class);
        authenticationRepository = mock(AuthenticationRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        when(userRepository.save(any())).thenAnswer(inv -> {
            UsrUser user = inv.getArgument(0);
            user.setUserId(1);
            return user;
        });
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
        ReflectionTestUtils.setField(userService, "authenticationRepository", authenticationRepository);
        ReflectionTestUtils.setField(userService, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(userService, "passwordPolicyService", passwordPolicyService);
        ReflectionTestUtils.setField(userService, "eventNotificationService", mock(IEventNotificationService.class));
        ReflectionTestUtils.setField(userService, "allowDefaultUser", true);
        ReflectionTestUtils.setField(userService, "defaultUsername", "admin");
    }

    @Test
    void createsAdministratorWithoutAccessPoint() {
        UsrUser user = userService.createInitialAdmin(USERNAME, PASSWORD);

        assertThat(user.getUsername()).isEqualTo(USERNAME);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getAccessPoint()).isNull();
        assertThat(user.getCreatedAt()).isNotNull();

        ArgumentCaptor<UsrAuthentication> authentication = ArgumentCaptor.forClass(UsrAuthentication.class);
        verify(authenticationRepository).save(authentication.capture());
        assertThat(authentication.getValue().getAuthType()).isEqualTo(UsrAuthentication.AuthType.PASSWORD);
        assertThat(authentication.getValue().getAuthValue()).startsWith("{bcrypt}");
        assertThat(authentication.getValue().getValidFrom()).isNotNull();
        assertThat(userService.matchesPassword(PASSWORD, authentication.getValue().getAuthValue(), USERNAME)).isTrue();
        verify(passwordPolicyService).validate(PASSWORD);

        ArgumentCaptor<UsrPermission> permission = ArgumentCaptor.forClass(UsrPermission.class);
        verify(permissionRepository).save(permission.capture());
        assertThat(permission.getValue().getPermission()).isEqualTo(UsrPermission.Permission.ADMIN);
        assertThat(permission.getValue().getUser()).isSameAs(user);
    }

    @Test
    void defaultUsernameIsRejected() {
        assertThatThrownBy(() -> userService.createInitialAdmin("Admin", PASSWORD))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getErrorCode()).isEqualTo(UserCode.USERNAME_EXISTS));
        verify(userRepository, never()).save(any());
    }

    @Test
    void defaultUsernameIsAllowedWithoutDefaultUser() {
        ReflectionTestUtils.setField(userService, "allowDefaultUser", false);

        assertThat(userService.createInitialAdmin("admin", PASSWORD).getUsername()).isEqualTo("admin");
    }

    @Test
    void weakPasswordIsRejected() {
        doThrow(new BusinessException("x", UserCode.PASSWORD_POLICY_VIOLATION)).when(passwordPolicyService).validate(any());

        assertThatThrownBy(() -> userService.createInitialAdmin(USERNAME, "a"))
                .isInstanceOf(BusinessException.class);
        verify(authenticationRepository, never()).save(any());
        verify(permissionRepository, never()).save(any());
    }
}
