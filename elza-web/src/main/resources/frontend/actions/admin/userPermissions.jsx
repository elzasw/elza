import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail'

export const USER_PERMISSIONS = 'adminRegion.userPermissions';

export function fetchUser(userId) {
    return DetailActions.fetchIfNeeded(USER_PERMISSIONS, "U" + userId, id => WebApi.getUser(id.slice(1)), true);
}

export function fetchGroup(groupId) {
    return DetailActions.fetchIfNeeded(USER_PERMISSIONS, "G" + groupId, id => WebApi.getGroup(id.slice(1)), true);
}
