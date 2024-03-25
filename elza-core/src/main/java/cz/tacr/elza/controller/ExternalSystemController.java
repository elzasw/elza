package cz.tacr.elza.controller;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.ExtSystemProperty;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/api/v1")
public class ExternalSystemController implements ExternalsystemsApi {

    @Autowired
    ExternalSystemService extSystemService;

    @Autowired
    UserService userService;

    final UsrPermission.Permission reqPermissions[] = { UsrPermission.Permission.ADMIN,
            UsrPermission.Permission.AP_EXTERNAL_WR };

    @Override
    @Transactional
    public ResponseEntity<List<ExtSystemProperty>> externalSystemAllProperties(Integer extSystemId, Integer userId) {

        UserDetail loggedDetail = userService.getLoggedUserDetail();

        if (loggedDetail == null) {
            throw new AccessDeniedException("Not logged", reqPermissions);
        }
        if (!loggedDetail.hasPermission(UsrPermission.Permission.ADMIN)) {
            if (!loggedDetail.hasPermission(UsrPermission.Permission.AP_EXTERNAL_WR)) {
                throw new AccessDeniedException("Cannot list externernal system properties", reqPermissions);
            }
            if (userId == null) {
                // without admin perms only own properties might be listed
                userId = loggedDetail.getId();
            }
        }

        // List<ExtSystemProperty> properties = extSystemService.findAllProperties(extSystemId, userId);
        List<ExtSystemProperty> properties = extSystemService.findUserProperties(extSystemId, userId);

        return ResponseEntity.ok(properties);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> externalSystemStoreProperties(@ApiParam(value = "request body", required = true) @Valid @RequestBody List<ExtSystemProperty> extSystemProperties) {
        UserDetail loggedDetail = userService.getLoggedUserDetail();

        if (loggedDetail == null) {
            throw new AccessDeniedException("Not logged", reqPermissions);
        }

        Validate.notNull(extSystemProperties, "ExtSystemProperty shouldn't be null");

        boolean checkPermsission = false;
        if (!loggedDetail.hasPermission(UsrPermission.Permission.ADMIN)) {
            if (!loggedDetail.hasPermission(UsrPermission.Permission.AP_EXTERNAL_WR)) {
                throw new AccessDeniedException("Cannot list externernal system properties", reqPermissions);
            }
            checkPermsission = true;
        }

        for (ExtSystemProperty extSystemProperty : extSystemProperties) {
            if (checkPermsission) {
                // without admin perms only own properties might be set
                if (extSystemProperty.getUserId() == null ||
                        !extSystemProperty.getUserId().equals(loggedDetail.getId())) {
                    throw new AccessDeniedException("User can set permissions only for himself.", reqPermissions);
                }
            }
            Validate.notNull(extSystemProperty.getExtSystemId(),
                             "ExtSystemProperty.externalSystemId shouldn't be null");
            Validate.notNull(extSystemProperty.getName(), "ExtSystemProperty.name shouldn't be null");
            Validate.notNull(extSystemProperty.getValue(), "ExtSystemProperty.value shouldn't be null");

            ApExternalSystem extSystem = extSystemService.findApExternalSystemById(extSystemProperty.getExtSystemId());
            UsrUser user = userService.getUserInternal(extSystemProperty.getUserId());

            extSystemService.storeProperty(extSystem, user, extSystemProperty);
        }


        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> externalSystemDeleteProperties(@ApiParam(value = "external system property id", required = true) @Valid @RequestBody List<Integer> extSysPropertyIds) {
        UserDetail loggedDetail = userService.getLoggedUserDetail();

        if (loggedDetail == null) {
            throw new AccessDeniedException("Not logged", reqPermissions);
        }
        
        // set flag if permission for each property should be checked
        boolean checkPermsission = false;
        if (!loggedDetail.hasPermission(UsrPermission.Permission.ADMIN)) {
            if (!loggedDetail.hasPermission(UsrPermission.Permission.AP_EXTERNAL_WR)) {
                throw new AccessDeniedException("Cannot change externernal system properties", reqPermissions);
            } else {
                checkPermsission = true;
            }
        }

        for (Integer extSysPropertyId : extSysPropertyIds) {

            SysExternalSystemProperty dbProp = extSystemService.getProperty(extSysPropertyId);

            if (checkPermsission) {
                // without admin perms only own properties might be set
                if (dbProp.getUserId() == null ||
                        !dbProp.getUserId().equals(loggedDetail.getId())) {
                    throw new AccessDeniedException("User has no permissions to delete this property.", reqPermissions);
                }
            }

            extSystemService.deleteProperty(extSysPropertyId);
        }

        return ResponseEntity.ok().build();
    }
}
