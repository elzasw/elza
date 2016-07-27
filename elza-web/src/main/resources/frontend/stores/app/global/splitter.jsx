import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    leftWidth: 250,
    rightWidth: 150,
}

export default function splitter(state = initialState, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA_INIT:
            if (action.storageData.splitter) {
                const resultState = {
                    ...state,
                    leftWidth: action.storageData.splitter.leftWidth,
                    rightWidth: action.storageData.splitter.rightWidth,
                }
                if (typeof resultState.leftWidth == "undefined" || resultState.leftWidth <0 || resultState.leftWidth >= 4000) {
                    resultState.leftWidth = initialState.leftWidth;
                }
                if (typeof resultState.rightWidth == "undefined" || resultState.rightWidth <0 || resultState.rightWidth >= 4000) {
                    resultState.rightWidth = initialState.rightWidth;
                }
                return resultState;
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

