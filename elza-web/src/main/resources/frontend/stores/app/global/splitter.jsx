import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    leftWidth: 250,
    rightWidth: 150,
}

export default function splitter(state = initialState, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA_INIT:
            if (action.storageData.splitter) {
                return {
                    ...state,
                    leftWidth: action.storageData.splitter.leftWidth,
                    rightWidth: action.storageData.splitter.rightWidth,
                }
            } else {
                return state;
            }
        case types.GLOBAL_SPLITTER_RESIZE:
            return {
                ...state,
                leftWidth: action.leftSize,
                rightWidth: action.rightSize,
            }
        default:
            return state
    }
}

