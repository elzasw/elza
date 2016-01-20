
import * as types from 'actions/constants/actionTypes';


export function lockDescItemType(nodeId, descItemTypeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_LOCK,
        nodeId: nodeId,
        descItemTypeId: descItemTypeId
    }
}

export function unlockDescItemType(nodeId, descItemTypeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_UNLOCK,
        nodeId: nodeId,
        descItemTypeId: descItemTypeId
    }
}

export function unlockAllDescItemType(nodeId) {
    return {
        type: types.NODE_DESC_ITEM_TYPE_UNLOCK_ALL,
        nodeId: nodeId
    }
}