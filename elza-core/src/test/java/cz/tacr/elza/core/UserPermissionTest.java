package cz.tacr.elza.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;

public class UserPermissionTest extends AbstractTest {

    @Autowired
    ScopeRepository scopeRepository;

    @Autowired
    ApTypeRepository typeRepository;

    @Autowired
    ApStateRepository stateRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PackageRepository packageRepository;

    @Autowired
    ApChangeRepository changeRepository;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    ApAccessPointRepository accessPointRepository;

    @Autowired
    UserService userService;

    @Autowired
    AccessPointService accessPointService;

    @Test
    @Transactional
    public void testGetNextStates() {
        ApAccessPoint accessPoint = createApAccessPoint();
        ApState state = createApState(accessPoint, StateApproval.NEW);

        // 1. zakládání a změny nových
        UsrUser user1 = createUser("u1", accessPoint);
        addPermission(user1, UsrPermission.Permission.AP_SCOPE_WR_ALL);
        authorizeAsUser(user1);

        // NEW -> TO_APPROVE, TO_AMEND
        List<StateApproval> states = accessPointService.getNextStates(state);
        Assert.assertTrue(states.size() == 3);
        Assert.assertTrue(states.contains(StateApproval.NEW));
        Assert.assertTrue(states.contains(StateApproval.TO_APPROVE));
        Assert.assertTrue(states.contains(StateApproval.TO_AMEND));

        // TO_APPROVE -> NEW, TO_AMEND
        state.setStateApproval(StateApproval.TO_APPROVE);
        states = accessPointService.getNextStates(state);
        Assert.assertTrue(states.size() == 2);
        Assert.assertTrue(states.contains(StateApproval.NEW));
        Assert.assertTrue(states.contains(StateApproval.TO_AMEND));

        // TO_AMEND -> NEW, TO_APPROVE
        state.setStateApproval(StateApproval.TO_AMEND);
        states = accessPointService.getNextStates(state);
        Assert.assertTrue(states.size() == 3);
        Assert.assertTrue(states.contains(StateApproval.TO_AMEND));
        Assert.assertTrue(states.contains(StateApproval.NEW));
        Assert.assertTrue(states.contains(StateApproval.TO_APPROVE));

        // 2. schvalování
        UsrUser user2 = createUser("u2", accessPoint);
        addPermission(user2, UsrPermission.Permission.AP_CONFIRM_ALL);
        authorizeAsUser(user2);

        // TO_APPROVE -> APPROVED
        state.setStateApproval(StateApproval.TO_APPROVE);
        states = accessPointService.getNextStates(state);
        Assert.assertTrue(states.size() == 1);
        Assert.assertTrue(states.contains(StateApproval.APPROVED));

        // 3. změna schválených
        UsrUser user3 = createUser("u3", accessPoint);
        addPermission(user3, UsrPermission.Permission.AP_EDIT_CONFIRMED_ALL);
        authorizeAsUser(user3);

        // APPROVED -> APPROVED
        state.setStateApproval(StateApproval.APPROVED);
        states = accessPointService.getNextStates(state);
        Assert.assertTrue(states.size() == 1);
        Assert.assertTrue(states.contains(StateApproval.APPROVED));
    }

    private void addPermission(UsrUser user, UsrPermission.Permission permission) {
        UsrPermission usrPermission = new UsrPermission();
        usrPermission.setUser(user);
        usrPermission.setPermission(permission);
        permissionRepository.save(usrPermission);
    }

    private UsrUser createUser(String userName, ApAccessPoint accessPoint) {
        UsrUser user = new UsrUser();
        user.setAccessPoint(accessPoint);
        user.setUsername(userName);
        user.setActive(true);
        return userRepository.save(user);
    }

    private ApState createApState(ApAccessPoint accessPoint, StateApproval state) {
        ApScope scope = new ApScope();
        scope.setScopeId(1);
        scope.setCode("GLOBAL");
        scope.setName("globální");
        scopeRepository.save(scope);

        List<ApType> types = typeRepository.findAll();

        ApChange createChange = changeRepository.save(new ApChange());

        ApState apState = new ApState();
        apState.setStateId(1);
        apState.setAccessPoint(accessPoint);
        apState.setScope(scope);
        apState.setApType(types.get(0));
        apState.setCreateChange(createChange);
        apState.setStateApproval(state);
        return stateRepository.save(apState);
    }

    private ApAccessPoint createApAccessPoint() {
        ApAccessPoint accessPoint = new ApAccessPoint();
        //accessPoint.setAccessPointId(1); // https://github.com/spring-projects/spring-boot/issues/37126
        accessPoint.setUuid(UUID.randomUUID().toString());
        accessPoint.setState(ApStateEnum.OK);
        accessPoint.setLastUpdate(LocalDateTime.now());
        return accessPointRepository.save(accessPoint);
    }

    private void authorizeAsUser(UsrUser user) {
        UserDetail userDetail = userService.createUserDetail(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

}
