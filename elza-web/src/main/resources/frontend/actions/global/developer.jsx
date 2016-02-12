import * as types from 'actions/constants/ActionTypes';

export function developerSet(enabled) {
    return {
        type: types.DEVELOPER_SET,
        enabled
    }
}
