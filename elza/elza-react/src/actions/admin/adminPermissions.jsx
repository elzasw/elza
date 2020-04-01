import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail';
import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';

export const ENTITY_PERMISSIONS = 'adminRegion.entityPermissions';
export const USERS_PERMISSIONS_BY_FUND = 'adminRegion.usersPermissionsByFund';
export const GROUPS_PERMISSIONS_BY_FUND = 'adminRegion.groupsPermissionsByFund';

export const ALL_ID = 'ALL_ID';

export function fetchUser(userId) {
    return DetailActions.fetchIfNeeded(ENTITY_PERMISSIONS, 'U' + userId, id => WebApi.getUser(id.slice(1)), true);
}

export function fetchGroup(groupId) {
    return DetailActions.fetchIfNeeded(ENTITY_PERMISSIONS, 'G' + groupId, id => WebApi.getGroup(id.slice(1)), true);
}

export function fetchUsersByFund(fundId) {
    if (fundId === ALL_ID) {
        return SimpleListActions.fetchIfNeeded(
            USERS_PERMISSIONS_BY_FUND,
            fundId,
            (id, filter) => WebApi.findUsersPermissionsByFundAll(),
            true,
        );
    } else {
        return SimpleListActions.fetchIfNeeded(
            USERS_PERMISSIONS_BY_FUND,
            fundId,
            (id, filter) => WebApi.findUsersPermissionsByFund(id),
            true,
        );
    }
}

export function changeUsersForFund(fundId, users) {
    return SimpleListActions.setData(USERS_PERMISSIONS_BY_FUND, fundId, users);
}

export function fetchGroupsByFund(fundId) {
    if (fundId === ALL_ID) {
        return SimpleListActions.fetchIfNeeded(
            GROUPS_PERMISSIONS_BY_FUND,
            fundId,
            (id, filter) => WebApi.findGroupsPermissionsByFundAll(),
            true,
        );
    } else {
        return SimpleListActions.fetchIfNeeded(
            GROUPS_PERMISSIONS_BY_FUND,
            fundId,
            (id, filter) => WebApi.findGroupsPermissionsByFund(id),
            true,
        );
    }
}

export function changeGroupsForFund(fundId, groups) {
    return SimpleListActions.setData(GROUPS_PERMISSIONS_BY_FUND, fundId, groups);
}
