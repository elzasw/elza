import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail'

export const ENTITY_PERMISSIONS = 'adminRegion.entityPermissions';
export const ENTITIES_PERMISSIONS_BY_FUND = 'adminRegion.entitiesPermissionsByFund';

export function fetchUser(userId) {
    return DetailActions.fetchIfNeeded(ENTITY_PERMISSIONS, "U" + userId, id => WebApi.getUser(id.slice(1)), true);
}

export function fetchGroup(groupId) {
    return DetailActions.fetchIfNeeded(ENTITY_PERMISSIONS, "G" + groupId, id => WebApi.getGroup(id.slice(1)), true);
}

export function fetchUsersByFund(fundId) {
    return DetailActions.fetchIfNeeded(ENTITIES_PERMISSIONS_BY_FUND, "U" + fundId, id => WebApi.findUsersPermissionsByFund(id.slice(1)), true);
}

export function fetchGroupsByFund(fundId) {
    return DetailActions.fetchIfNeeded(ENTITIES_PERMISSIONS_BY_FUND, "G" + fundId, id => WebApi.findGroupsPermissionsByFund(id.slice(1)), true);
}
