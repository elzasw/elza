/**
 * Akce pro splitter.
 */

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Změna velikosti splitteru.
 */
export function splitterResize(leftSize, rightSize) {
    return {
        type: types.GLOBAL_SPLITTER_RESIZE,
        leftSize,
        rightSize,
    };
}
