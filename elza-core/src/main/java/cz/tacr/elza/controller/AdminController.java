package cz.tacr.elza.controller;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collection;

import jakarta.transaction.Transactional;
import javax.validation.Valid;

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
import cz.tacr.elza.controller.vo.LoggedUser;
import cz.tacr.elza.controller.vo.LoggedUsers;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AccessPointService.AccessPointStats;
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
            throw new AccessDeniedException("User not authorized.", null);
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
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    public ResponseEntity<LoggedUsers> adminLoggedUsers() {

        LoggedUsers lus = new LoggedUsers();
        Collection<WebSocketSession> sessions = clientOutboundChannelExecutor.getSessions();
        for (WebSocketSession session : sessions) {
            InetSocketAddress remoteAddr = session.getRemoteAddress();

            LoggedUser lu = new LoggedUser();
            if (remoteAddr != null) {
                lu.setRemoteAddr(remoteAddr.toString());
            }
            Principal principal = session.getPrincipal();
            Authentication auth = (Authentication) principal;
            UserDetail userDetail = (UserDetail) auth.getDetails();

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

}
