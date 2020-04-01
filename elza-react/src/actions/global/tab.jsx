import * as types from 'actions/constants/ActionTypes.js';

/**
 * Uloží otevřený tab oblasti
 *
 * @param area
 * @param value
 * @returns {{type: *, area: *, value: *}}
 */
export function selectTab(area, value) {
    return {
        type: types.TAB_SELECT,
        area,
        value,
    };
}
