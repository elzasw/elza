import * as types from 'actions/constants/ActionTypes';

const DEFAULT_SPLITTER_SIZES = {
    leftWidth: 250,
    rightWidth: 150,
}

const initialState = {
    splitters: {
        global: {
            leftWidth: 250,
            rightWidth: 150,
        },
        "AIP": DEFAULT_SPLITTER_SIZES,
        "DAO": DEFAULT_SPLITTER_SIZES,
        "ARR_DATA_GRID": DEFAULT_SPLITTER_SIZES,
        "ARR_MOVEMENTS": DEFAULT_SPLITTER_SIZES,
        "ARR_OUTPUT": DEFAULT_SPLITTER_SIZES,
        "ARR": DEFAULT_SPLITTER_SIZES,
        "ARR_REQUEST": DEFAULT_SPLITTER_SIZES,
        "FUND_ACTION": DEFAULT_SPLITTER_SIZES,
    }
};


export default function splitter(state = initialState, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA_INIT:
            if (action.storageData.splitter) {
                const resultState = {
                    ...state,
                    splitters: {
                        ...state.splitters,
                        global: {
                            leftWidth: action.storageData.splitter.leftWidth,
                            rightWidth: action.storageData.splitter.rightWidth,
                        }
                    }
                };
                if (
                    typeof resultState.splitters.global.leftWidth == 'undefined' ||
                    resultState.splitters.global.leftWidth < 0 ||
                    resultState.splitters.global.leftWidth >= 4000
                ) {
                    resultState.splitters.global.leftWidth = initialState.splitters.global.leftWidth;
                }
                if (
                    typeof resultState.splitters.global.rightWidth == 'undefined' ||
                    resultState.splitters.global.rightWidth < 0 ||
                    resultState.splitters.global.rightWidth >= 4000
                ) {
                    resultState.splitters.global.rightWidth = initialState.splitters.global.rightWidth;
                }
                return resultState;
            } else {
                return state;
            }
        case types.GLOBAL_SPLITTER_RESIZE:
            return {
                ...state,
                splitters: {
                    ...state.splitters,
                    [action.area] : {
                        leftWidth: action.leftSize,
                        rightWidth: action.rightSize,
                    }
                }
            };
        default:
            return state;
    }
}
