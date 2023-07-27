package cz.tacr.elza.controller;

import java.security.Principal;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<AdminInfo> adminInfo() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if(userDetail==null) {
            throw new AccessDeniedException("User not authorized.", null);
        }
        
        AdminInfo ai = new AdminInfo();
        
        AuthorizationRequest arFundRead = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_ADMIN)
                .or(Permission.FUND_ARR_ALL)
                .or(Permission.FUND_RD_ALL)
                .or(Permission.FUND_RD);
        if (arFundRead.matches(userDetail)) {
            // read fund stats
            ArrangementStats arrStats = arrangementService.getStats();
            ai.setFunds(arrStats.getFundCount());
            ai.setLevels(arrStats.getLevelCount());
        }
        
        AuthorizationRequest arRead = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.AP_SCOPE_RD_ALL)
                .or(Permission.AP_SCOPE_RD);
        if (arRead.matches(userDetail)) {
            AccessPointStats apStats = accessPointService.getStats();
            ai.setAccessPoints(apStats.getValidAccessPointCount());
        }

        UserStats userStats = userService.getStats();
        ai.setUsers(userStats.getActiveUserCount());

        List<Principal> sessions = clientOutboundChannelExecutor.getPrincipals();
        ai.setLoggedUsers(sessions.size());

        return new ResponseEntity<>(ai, HttpStatus.OK);
    }

    @Override
    @AuthMethod(permission = { UsrPermission.Permission.ADMIN })
    public ResponseEntity<LoggedUsers> adminLoggedUsers() {

        LoggedUsers lus = new LoggedUsers();
        List<Principal> sessions = clientOutboundChannelExecutor.getPrincipals();
        for(Principal session: sessions) {
            LoggedUser lu = new LoggedUser();
            lu.setUser(session.getName());
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