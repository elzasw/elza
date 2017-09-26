import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail'

export const USER_PERMISSIONS = 'adminRegion.userPermissions';

export function fetch(userId) {
    return DetailActions.fetchIfNeeded(USER_PERMISSIONS, userId, WebApi.getUser, true);
}
