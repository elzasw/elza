package cz.tacr.elza.service.da;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.AipRepositoryCustom.AipVisibility;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;

/**
 * Permissions to an AIP follow the fund it is attached to. An AIP attached to no fund may belong
 * to any of them, so only users with permissions to all funds may work with it.
 *
 * The checks belong to the entry points called by a user. The processing of the digital archive
 * reaches the same work without one, so the work itself checks nothing.
 */
@Component
public class AipAuthorization {

    @Autowired
    private UserService userService;
    @Autowired
    private AipStateRepository aipStateRepository;

    /** The list, the detail, the package and its files. */
    @Transactional(readOnly = true)
    public void checkRead(List<DaAip> aips) {
        check(aips, true);
    }

    /**
     * Actions that change the AIP in ELZA or in the digital archive. The whole request is refused
     * when any of the AIPs is not permitted.
     */
    @Transactional(readOnly = true)
    public void checkArrange(List<DaAip> aips) {
        check(aips, false);
    }

    /** {@link #checkArrange} over the active states the caller has already read. */
    public void checkArrangeStates(Collection<DaAipState> states) {
        checkStates(states, false);
    }

    /** A package brought in by hand is attached to no fund until its references are resolved. */
    public void checkImport() {
        AuthorizationRequest request = AuthorizationRequest.hasPermission(Permission.ADMIN);
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null || !request.matches(userDetail)) {
            throw Authorization.createAccessDeniedException(request.getPermissions());
        }
    }

    public AipVisibility visibility() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null) {
            throw Authorization.createAccessDeniedException(requestFor(null, true).getPermissions());
        }
        if (requestFor(null, true).matches(userDetail)) {
            return AipVisibility.ALL;
        }
        boolean allFunds = AuthorizationRequest.hasPermission(Permission.FUND_RD_ALL).matches(userDetail);
        return new AipVisibility(false, allFunds ? null : userDetail.getId());
    }

    private void check(List<DaAip> aips, boolean read) {
        if (!aips.isEmpty()) {
            checkStates(aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aips), read);
        }
    }

    /** An AIP invalidated by the digital archive has no active state; the action skips it. */
    private void checkStates(Collection<DaAipState> states, boolean read) {
        UserDetail userDetail = userService.getLoggedUserDetail();
        for (DaAipState state : states) {
            AuthorizationRequest request = requestFor(state.getFund(), read);
            if (userDetail == null || !request.matches(userDetail)) {
                throw Authorization.createAccessDeniedException(request.getPermissions());
            }
        }
    }

    private static AuthorizationRequest requestFor(@Nullable ArrFund fund, boolean read) {
        AuthorizationRequest request = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_ADMIN)
                .or(Permission.FUND_ARR_ALL);
        if (fund == null) {
            return request;
        }
        if (read) {
            request.or(Permission.FUND_RD_ALL).or(Permission.FUND_RD, fund.getFundId());
        }
        return request.or(Permission.FUND_ARR, fund.getFundId());
    }
}
