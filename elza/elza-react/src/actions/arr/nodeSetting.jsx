/**
 * Akce pro různá nastavení pro konkrétní uzel, např. zamykání atributů atp.
 */

import * as types from 'actions/constants/ActionTypes.js';

export function isNodeSettingsAction(action) {
    switch (action.type) {
        case types.NODE_DESC_ITEM_TYPE_LOCK:
        case types.NODE_DESC_ITEM_TYPE_UNLOCK:
        case types.NODE_DESC_ITEM_TYPE_COPY_ALL:
        case types.NODE_DESC_ITEM_TYPE_COPY:
        case types.NODE_DESC_ITEM_TYPE_NOCOPY:
            return true;
        default:
            return false;
    }
}

/**
 * Zamčení atributu.
 * @param {int} nodeId id uzlu
 * @param {int} descItemTypeId id atributu
 */
export function lockDescItemType(nodeId, descItemTypeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_LOCK,
        nodeId: nodeId,
        descItemTypeId: descItemTypeId,
    };
}

/**
 * kopírování všech položek atributu.
 * @param {int} nodeId id uzlu
 * @param {int} descItemTypeId id atributu
 */
export function toggleCopyAllDescItemType(nodeId, descItemTypeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_COPY_ALL,
        nodeId: nodeId,
    };
}

/**
 * Odemčení všech atributů pro konkrétní uzel.
 * @param {int} nodeId id uzlu
 */
export function unlockAllDescItemType(nodeId) {
    return {
        //type: types.NODE_DESC_ITEM_TYPE_UNLOCK_ALL,
        nodeId: nodeId,
    };
}

/**
 * Odemčení všech atributů pro konkrétní uzel.
 * @param {int} nodeId id uzlu
 */
export function unlockDescItemType(nodeId, descItemTypeId) {
    return {
        //type: types.NODE_DESC_ITEM_TYPE_UNLOCK_ALL,
        nodeId: nodeId,
    };
}

/**
 * Zapnutí opakovaného kopírování hodnoty atributu.
 * @param {int} nodeId id uzlu
 * @param {int} descItemTypeId id atributu
 */
export function copyDescItemType(nodeId, descItemTypeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_COPY,
        nodeId: nodeId,
        descItemTypeId: descItemTypeId,
    };
}

/**
 * Vypnutí opakovaného kopírování hodnoty atributu.
 * @param {int} nodeId id uzlu
 * @param {int} descItemTypeId id atributu
 */
export function nocopyDescItemType(nodeId, descItemTypeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_NOCOPY,
        nodeId: nodeId,
        descItemTypeId: descItemTypeId,
    };
}
