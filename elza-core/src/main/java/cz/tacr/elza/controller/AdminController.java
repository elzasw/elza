package cz.tacr.elza.controller;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

import cz.tacr.elza.controller.vo.AdminCopyPermissionParams;
import cz.tacr.elza.controller.vo.AdminInfo;
import cz.tacr.elza.controller.vo.ApiKeyInfo;
import cz.tacr.elza.controller.vo.LoggedUser;
import cz.tacr.elza.controller.vo.LoggedUsers;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.UsrApiKey;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.apikey.ApiKeyMapper;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AccessPointService.AccessPointStats;
import cz.tacr.elza.service.ApiKeyService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ArrangementService.ArrangementStats;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.UserService.UserStats;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.websocket.WebSocketThreadPoolTaskExecutor;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/api/v1")
public class AdminController implements AdminApi {

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    @Qualifier("clientOutboundChannelExecutor")
    private WebSocketThreadPoolTaskExecutor clientOutboundChannelExecutor;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private SiemAuditLogger siemAuditLogger;

    @Override
    @Transactional
    public ResponseEntity<Void> adminCopyPermissions(@ApiParam(value = "ID of target user", required = true) @PathVariable("userId") Integer userId,
                                                     @ApiParam(value = "", required = true) @Valid @RequestBody AdminCopyPermissionParams adminCopyPermissionParams) {

        userService.copyPermissions(userId, adminCopyPermissionParams.getFromUserId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AdminInfo> adminInfo() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if(userDetail==null) {
            throw new AccessDeniedException("User not authorized.", Collections.emptyList());
        }

        AdminInfo ai = new AdminInfo();

        AuthorizationRequest arFundRead = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_ADMIN)
                .or(Permission.FUND_ARR_ALL)
                .or(Permission.FUND_RD_ALL);
        if (arFundRead.matches(userDetail)) {
            // read fund stats
            ArrangementStats arrStats = arrangementService.getStats();
            ai.setFunds(arrStats.getFundCount());
            ai.setLevels(arrStats.getLevelCount());
        }

        AuthorizationRequest arRead = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.AP_SCOPE_RD_ALL);
        if (arRead.matches(userDetail)) {
            AccessPointStats apStats = accessPointService.getStats();
            ai.setAccessPoints(apStats.getValidAccessPointCount());
        }

        UserStats userStats = userService.getStats();
        ai.setUsers(userStats.getActiveUserCount());

        Collection<WebSocketSession> sessions = clientOutboundChannelExecutor.getSessions();
        ai.setLoggedUsers(sessions.size());

        return new ResponseEntity<>(ai, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<LoggedUsers> adminLoggedUsers() {

    	// Check permissions - only Admins are allowed or users managing another user or group
    	boolean isAdmin = false;
    	boolean userControl = false, groupControl = false;
    	if (!userService.hasPermission(Permission.ADMIN)) {
    		if(userService.hasPermission(Permission.USER_CONTROL_ENTITY)) {
    			userControl = true;
    		}
    		if(userService.hasPermission(Permission.GROUP_CONTROL_ENTITY)) {
    			groupControl = true;
    		}
    	} else {
    		isAdmin = true;
    	}
    	if(!isAdmin && !userControl && !groupControl) {
			Permission[] perms = { UsrPermission.Permission.ADMIN, 
			        UsrPermission.Permission.USER_CONTROL_ENTITY,
			        UsrPermission.Permission.GROUP_CONTROL_ENTITY };    		
    		throw new AccessDeniedException("Missing permissions: " + Arrays.toString(perms), perms);
    	}

        LoggedUsers lus = new LoggedUsers();
        Collection<WebSocketSession> sessions = clientOutboundChannelExecutor.getSessions();
        for (WebSocketSession session : sessions) {
            InetSocketAddress remoteAddr = session.getRemoteAddress();

            Principal principal = session.getPrincipal();
            if(principal==null) {
            	continue;
            }

            Authentication auth = (Authentication) principal;
	        UserDetail userDetail = (UserDetail) auth.getDetails();

	        // if not admin - add only managed users
            if(!isAdmin) {
            	if(!userService.hasPermission(UsrPermission.Permission.USER_CONTROL_ENTITY, userDetail.getId())) {
            		continue;
            	}
            }

            LoggedUser lu = new LoggedUser();
            if (remoteAddr != null) {
                lu.setRemoteAddr(remoteAddr.toString());
            }
	
	        lu.setUserId(userDetail.getId());
	        lu.setUser(userDetail.getUsername());
	        lus.addUsersItem(lu);
        }        

        lus.setTotalCount(sessions.size());
        return new ResponseEntity<>(lus, HttpStatus.OK);
    }

    /**
     * Vytvoření chybějících záznamů v arr_cached_node
     */
    @Override
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    @Transactional
    public ResponseEntity<Void> adminSyncNodeCache() {
        nodeCacheService.syncCache();
        return ResponseEntity.ok().build();
    }

    /**
     * Invalidation of arr_inhibited_item records whose source desc item
     * no longer lies on an ancestor node
     */
    @Override
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    @Transactional
    public ResponseEntity<Integer> adminDeleteInvalidInhibitedItems() {
        int count = arrangementService.cleanupOrphanedInhibitedItems();
        return ResponseEntity.ok(count);
    }

    @Override
    @AuthMethod(permission = { UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITY })
    public ResponseEntity<List<ApiKeyInfo>> adminListUserApiKeys(
            @AuthParam(type = AuthParam.Type.USER) Integer userId) {
        userService.requireInteractiveAuth();
        List<ApiKeyInfo> result = apiKeyService.listByUserId(userId).stream()
                .map(ApiKeyMapper::toInfo)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    @AuthMethod(permission = { UsrPermission.Permission.USR_PERM, UsrPermission.Permission.USER_CONTROL_ENTITY })
    public ResponseEntity<Void> adminRevokeUserApiKey(
            @AuthParam(type = AuthParam.Type.USER) Integer userId,
            Integer id) {
        userService.requireInteractiveAuth();
        UsrApiKey key = apiKeyService.getRequired(id);
        // The key must belong to the addressed user, otherwise the URL is treated as pointing at
        // a non-existent resource (a mismatch is a client bug, not a security event).
        if (!key.getUser().getUserId().equals(userId)) {
            throw new ObjectNotFoundException("API key not found", BaseCode.ID_NOT_EXIST)
                    .set(BaseCode.PARAM_PROPERTY, "id");
        }

        // actor is null for the built-in admin (no usr_user row); the audit still records who
        // acted using the login username from UserDetail.
        UsrUser actor = userService.getLoggedUser();
        UserDetail actorDetail = userService.getLoggedUserDetail();
        String actorName = actorDetail != null ? actorDetail.getUsername() : "unknown";

        boolean alreadyRevoked = key.getRevokedDate() != null;
        apiKeyService.revoke(id, actor);
        if (!alreadyRevoked) {
            siemAuditLogger.apiKeyRevoked(actorName, key.getUser().getUsername(), key.getKeyId());
        }
        return ResponseEntity.ok().build();
    }

}
