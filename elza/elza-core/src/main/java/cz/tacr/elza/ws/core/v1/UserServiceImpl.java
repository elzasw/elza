package cz.tacr.elza.ws.core.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.PermissionRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.ws.types.v1.Permission;
import cz.tacr.elza.ws.types.v1.PermissionsForUser;
import cz.tacr.elza.ws.types.v1.SetUserState;
import cz.tacr.elza.ws.types.v1.User;

@Component
@jakarta.jws.WebService(serviceName = "CoreService", portName = "UserService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.UserService")
public class UserServiceImpl implements UserService {

    final private static Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    cz.tacr.elza.service.UserService userService;

    @Autowired
    PartyService partyService;

    @Autowired
    PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void setUserState(SetUserState setUserState) throws CoreServiceException {
        try {
            UsrUser user = userService.findByUsername(setUserState.getUsername());
            Validate.notNull(user, "User not found: %s", setUserState.getUsername());
            boolean nextState;
            switch (setUserState.getState()) {
            case ACTIVE:
                nextState = true;
                break;
            case INACTIVE:
                nextState = false;
                break;
            default:
                throw new IllegalStateException("Unexpected state: " + setUserState.getState());
            }
            user = userService.changeActive(user, nextState);
        } catch (Exception e) {
            logger.error("Failed to remove user: {}", e.toString(), e);
            throw WSHelper.prepareException("Failed to remove user", e);
        }
    }

    @Override
    @Transactional
    public void createUser(User createUser) throws CoreServiceException {
        try {
            // Find person
            String srcPersonId = createUser.getPersonId();
            Validate.notNull(srcPersonId, "personId is null");

            ApAccessPoint ap;
            if (srcPersonId.length() == 36) {
                ap = accessPointService.getAccessPointByUuid(srcPersonId);
            } else {
                ap = accessPointService.getAccessPoint(Integer.valueOf(srcPersonId));
            }

            Map<UsrAuthentication.AuthType, String> userAttrs = Collections.emptyMap();
            UsrUser user = userService.createUser(createUser.getUsername(), userAttrs, ap.getAccessPointId());

            if (createUser.getPermList() != null) {
                addPermissions(user, createUser.getPermList().getPerm());
            }
        } catch (Exception e) {
            logger.error("Failed to create user: {}", e.toString(), e);
            throw WSHelper.prepareException("Failed to create user", e);
        }

    }

    private void addPermissions(final UsrUser user, List<Permission> permList) {
        List<UsrPermission> permissions = permList.stream().map(p -> preparePermission(p)).collect(Collectors.toList());

        userService.addUserPermission(user, permissions, true);
    }

    private UsrPermission preparePermission(Permission p) {
        UsrPermission up = new UsrPermission();
        switch (p.getPermType()) {
        case ADMIN:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.ADMIN);
            break;
        case AP_CONFIRM_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.AP_CONFIRM_ALL);
            break;
        case AP_EDIT_CONFIRMED_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.AP_EDIT_CONFIRMED_ALL);
            break;
        case AP_SCOPE_RD_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.AP_SCOPE_RD_ALL);
            break;
        case AP_SCOPE_WR_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.AP_SCOPE_WR_ALL);
            break;
        case FUND_ADMIN:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_ADMIN);
            break;
        case FUND_ARR_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_ARR_ALL);
            break;
        case FUND_BA_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_BA_ALL);
            break;
        case FUND_CREATE:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_CREATE);
            break;
        case FUND_EXPORT_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_EXPORT_ALL);
            break;
        case FUND_ISSUE_ADMIN_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_ISSUE_ADMIN_ALL);
            break;
        case FUND_OUTPUT_WR_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_OUTPUT_WR_ALL);
            break;
        case FUND_RD_ALL:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_RD_ALL);
            break;
        case FUND_VER_WR:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.FUND_VER_WR);
            break;
        case USR_PERM:
            up.setPermission(cz.tacr.elza.domain.UsrPermission.Permission.USR_PERM);
            break;
        default:
            throw new IllegalStateException("Unsupported permission, type: " + p.getPermType());
        }
        return up;
    }

    @Override
    @Transactional
    public void addPermissions(PermissionsForUser addPermissions) throws CoreServiceException {
        try {
            List<Permission> permList = addPermissions.getPermList().getPerm();
            List<UsrPermission> permissions = permList.stream().map(p -> preparePermission(p)).collect(Collectors
                    .toList());

            UsrUser user = userService.findByUsername(addPermissions.getUsername());
            Validate.notNull(user, "User not found: %s", addPermissions.getUsername());
            
            List<UsrPermission> addPermissionsList = new ArrayList<>(permissions.size());
            // prevent duplicated permissions
            List<UsrPermission> dbPerms = permissionRepository.findByUserOrderByPermissionIdAsc(user);            
            outter: for (UsrPermission srcPerm : permissions) {
                for (UsrPermission dbPerm : dbPerms) {
                    // check if permission found
                    if (srcPerm.isSamePermission(dbPerm)) {
                        logger.info("Permission already exists: {}", srcPerm.getPermission());
                        // skip already existing items
                        continue outter;
                        /*
                        throw new BusinessException("Permission already exists", BaseCode.PROPERTY_IS_INVALID)
                                .set("permission", srcPerm.getPermission())
                                .set("fundId", srcPerm.getFundId())
                                .set("nodeId", srcPerm.getNodeId())
                                .set("scopeId", srcPerm.getScopeId())
                                .set("issueListId", srcPerm.getIssueListId());
                                */
                    }
                }
                addPermissionsList.add(srcPerm);
            }

            userService.addUserPermission(user, addPermissionsList, true);
        } catch (Exception e) {
            logger.error("Failed to add permissions: {}", e.toString(), e);
            throw WSHelper.prepareException("Failed to add permissions", e);
        }

    }

    @Override
    @Transactional
    public void removePermissions(PermissionsForUser removePermissions) throws CoreServiceException {
        try {
            List<Permission> permList = removePermissions.getPermList().getPerm();
            List<UsrPermission> permissions = permList.stream().map(p -> preparePermission(p)).collect(Collectors
                    .toList());

            UsrUser user = userService.findByUsername(removePermissions.getUsername());
            Validate.notNull(user, "User not found: %s", removePermissions.getUsername());

            List<UsrPermission> dbPerms = permissionRepository.findByUserOrderByPermissionIdAsc(user);

            List<UsrPermission> removePermList = new ArrayList<>(permissions.size());
            // prepare dbPerms to be deleted
            outer: for (UsrPermission srcPerm : permissions) {
                for (UsrPermission dbPerm : dbPerms) {
                    // check if permission found
                    if (srcPerm.isSamePermission(dbPerm)) {
                        removePermList.add(dbPerm);
                        continue outer;
                    }
                }
                logger.info("Permission not found, permission: {}", srcPerm.getPermission());
                /*
                throw new BusinessException("Permission not found", BaseCode.PROPERTY_NOT_EXIST)
                        .set("permission", srcPerm.getPermission())
                        .set("fundId", srcPerm.getFundId())
                        .set("nodeId", srcPerm.getNodeId())
                        .set("scopeId", srcPerm.getScopeId())
                        .set("issueListId", srcPerm.getIssueListId());
                        */
            }

            userService.deleteUserPermission(user, removePermList);
        } catch (Exception e) {
            logger.error("Failed to remove permissions: {}", e.toString(), e);
            throw WSHelper.prepareException("Failed to remove permissions", e);
        }

    }

}
