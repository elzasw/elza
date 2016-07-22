import * as types from 'actions/constants/ActionTypes.js';

export function selectTab(area, value) {
    return {
        type: types.TAB_SELECT,
        area,
        value
    }
}