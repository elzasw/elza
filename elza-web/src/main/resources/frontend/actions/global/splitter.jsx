import * as types from 'actions/constants/actionTypes';

export function splitterResize(leftSize, rightSize) {
    return {
        type: types.GLOBAL_SPLITTER_RESIZE,
        leftSize,
        rightSize
    }
}
