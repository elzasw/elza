/**
 * Akce pro splitter.
 */

import * as types from 'actions/constants/ActionTypes';
import {SPLITTER_AREA_GLOBAL} from 'stores/app/global/splitter.jsx';

/**
 * Změna velikosti splitteru.
 *
 * @param area oblast, pro kterou se rozměry ukládají; stránky bez vlastní oblasti používají globální
 */
export function splitterResize(leftSize, rightSize, area = SPLITTER_AREA_GLOBAL) {
    return {
        type: types.GLOBAL_SPLITTER_RESIZE,
        area,
        leftSize,
        rightSize,
    };
}
