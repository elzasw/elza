package cz.tacr.elza.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.GroupUserRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.PermissionAction;
import cz.tacr.elza.security.UserDetail;

/**
 * SIEM log events of changes of permissions and group membership in {@link UserService}.
 */
class UserServicePermissionAuditTest {

    private static final String ADMIN = "admin";
    private static final String USERNAME = "novak";
    private static final String GROUP_CODE = "editors";

    private UserService userService;
    private PermissionRepository permissionRepository;
    private GroupUserRepository groupUserRepository;
    private SiemAuditLogger siemAuditLogger;
    private UsrUser user;
    private UsrGroup group;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        permissionRepository = mock(PermissionRepository.class);
        groupUserRepository = mock(GroupUserRepository.class);
        siemAuditLogger = mock(SiemAuditLogger.class);
        ReflectionTestUtils.setField(userService, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(userService, "groupUserRepository", groupUserRepository);
        ReflectionTestUtils.setField(userService, "userRepository", mock(UserRepository.class));
        ReflectionTestUtils.setField(userService, "eventNotificationService", mock(IEventNotificationService.class));
        ReflectionTestUtils.setField(userService, "siemAuditLogger", siemAuditLogger);
        when(permissionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        user = new UsrUser();
        user.setUserId(10);
        user.setUsername(USERNAME);

        group = new UsrGroup();
        group.setGroupId(3);
        group.setCode(GROUP_CODE);

        // logged administrator allowed to change any permission; no id, so no permission cache lookup
        UserDetail adminDetail = mock(UserDetail.class);
        when(adminDetail.getId()).thenReturn(null);
        when(adminDetail.hasPermission(UsrPermission.Permission.USR_PERM)).thenReturn(true);
        var auth = new UsernamePasswordAuthenticationToken(ADMIN, null);
        auth.setDetails(adminDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // invalidateCache() registers an after-commit synchronization
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    private static UsrPermission permission(final UsrPermission.Permission type) {
        UsrPermission permission = new UsrPermission();
        permission.setPermission(type);
        return permission;
    }

    @Test
    void grantedGlobalPermissionIsLogged() {
        userService.addUserPermission(user, new ArrayList<>(List.of(permission(UsrPermission.Permission.FUND_RD_ALL))), true);
        verify(siemAuditLogger).permissionChanged(ADMIN, PermissionAction.GRANT, USERNAME, null, "FUND_RD_ALL", null);
    }

    @Test
    void grantedFundPermissionHasTarget() {
        ArrFund fund = new ArrFund();
        fund.setFundId(123);
        UsrPermission permission = permission(UsrPermission.Permission.FUND_RD);
        permission.setFund(fund);

        userService.addGroupPermission(group, new ArrayList<>(List.of(permission)), true);

        verify(siemAuditLogger).permissionChanged(ADMIN, PermissionAction.GRANT, null, GROUP_CODE, "FUND_RD",
                                                  "fund:123");
    }

    @Test
    void revokedPermissionIsLogged() {
        UsrPermission existing = permission(UsrPermission.Permission.FUND_RD_ALL);
        existing.setPermissionId(5);
        existing.setUser(user);
        when(permissionRepository.findByUserOrderByPermissionIdAsc(user)).thenReturn(List.of(existing));
        UsrPermission toDelete = permission(UsrPermission.Permission.FUND_RD_ALL);
        toDelete.setPermissionId(5);

        userService.deleteUserPermission(user, List.of(toDelete));

        verify(siemAuditLogger).permissionChanged(ADMIN, PermissionAction.REVOKE, USERNAME, null, "FUND_RD_ALL", null);
    }

    @Test
    void groupMembershipIsLogged() {
        when(groupUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        userService.joinGroup(user, group);
        verify(siemAuditLogger).groupMemberAdded(ADMIN, USERNAME, GROUP_CODE);

        when(groupUserRepository.findByGroupAndUser(group, user)).thenReturn(List.of(new UsrGroupUser()));
        userService.leaveGroup(group, user);
        verify(siemAuditLogger).groupMemberRemoved(ADMIN, USERNAME, GROUP_CODE);
    }
}
