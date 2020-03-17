import * as types from 'actions/constants/ActionTypes.js';
import fundTree from './../arr/fundTree.jsx';
import {isFundTreeAction} from 'actions/arr/fundTree.jsx';

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    fundTree: fundTree(undefined, {type: ''}),
};

export default function fundDetail(state = initialState, action = {}) {
    if (isFundTreeAction(action) && action.area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
        return {
            ...state,
            fundTree: fundTree(state.fundTree, action),
        };
    }

    switch (action.type) {
        case types.STORE_SAVE:
            const {id} = state;
            return {
                id,
                fundTree: fundTree(state.fundTree, action),
            };
        case types.STORE_LOAD:
            return {
                ...state,
                fetched: false,
                fetching: false,
                currentDataKey: '',
                fundTree: fundTree(state.fundTree, action),
            };
        case types.DELETE_FUND:
            if (state.id === action.fundId) {
                return {
                    ...state,
                    id: null,
                    currentDataKey: '',
                };
            } else {
                return state;
            }
        case types.CHANGE_APPROVE_VERSION:
        case types.CHANGE_FUND:
            if (state.id === action.fundId) {
                return {
                    ...state,
                    currentDataKey: '',
                };
            } else {
                return state;
            }
        case types.OUTPUT_CHANGES: {
            return {
                ...state,
                currentDataKey: '',
            };
        }
        case types.OUTPUT_STATE_CHANGE: {
            if (action.versionId === state.versionId) {
                return {
                    ...state,
                    currentDataKey: '',
                };
            } else {
                return state;
            }
        }
        case types.FUNDS_SELECT_FUND:
            if (state.id !== action.id) {
                return {
                    ...state,
                    id: action.id,
                    currentDataKey: '',
                    fetched: false,
                    fundTree: fundTree(undefined, {type: ''}),
                };
            } else {
                return state;
            }
        case types.FUNDS_FUND_DETAIL_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            };
        case types.FUNDS_FUND_DETAIL_RECEIVE:
            return {
                ...state,
                ...action.data,
                fetching: false,
                fetched: true,
            };
        default:
            return state;
    }
}
