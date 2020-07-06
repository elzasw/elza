package cz.tacr.elza.ws.core.v1;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.PartyService;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.Permission;
import cz.tacr.elza.ws.types.v1.PermissionsForUser;
import cz.tacr.elza.ws.types.v1.User;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "UserService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
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

    @Override
    @Transactional
    public void removeUser(String removeUser) throws CoreServiceException {
        //userService.re

    }

    @Override
    @Transactional
    public void createUser(User createUser) throws CoreServiceException {
        try {
            // Find person
            String srcPersonId = createUser.getPersonId();
            ApAccessPoint ap;
            if (srcPersonId.length() == 36) {
                ap = accessPointService.getAccessPointByUuid(srcPersonId);
            } else {
                ap = accessPointService.getAccessPoint(Integer.valueOf(srcPersonId));
            }
            ParParty party = partyService.findParPartyByAccessPoint(ap);
            if (party == null) {
                throw new ObjectNotFoundException("Party not foud", BaseCode.ID_NOT_EXIST).set("id", srcPersonId);
            }

            Map<UsrAuthentication.AuthType, String> userAttrs = Collections.emptyMap();
            UsrUser user = userService.createUser(createUser.getUsername(), userAttrs, party.getPartyId());

            if (createUser.getPermList() != null) {
                addPermissions(user, createUser.getPermList().getPerm());
            }
        } catch (Exception e) {
            logger.error("Failed to create user: {}", e.getMessage(), e);
            throw prepareException("Failed to create user", e);
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

    private CoreServiceException prepareException(String msg, Exception e) {
        ErrorDescription ed = new ErrorDescription();
        ed.setUserMessage(msg);
        ed.setDetail(e.getMessage());
        return new CoreServiceException(msg, ed, e);
    }

    @Override
    @Transactional
    public void addPermissions(PermissionsForUser addPermissions) throws CoreServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    @Transactional
    public void removePermissions(PermissionsForUser removePermissions) throws CoreServiceException {
        // TODO Auto-generated method stub

    }

}
