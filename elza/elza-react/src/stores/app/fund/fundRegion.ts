import * as types from 'actions/constants/ActionTypes';
import {isFundTreeAction} from 'actions/arr/fundTree.jsx';
import fundDetail from './fundDetail.jsx';
import {AnyAction} from "redux";

type FundRegionState = {
    fetched: boolean;
    fetching: boolean;
    filterText: string,
    filter: {
        from: number;
        institutionIdentifier?: object | string | number | null;
        [key: string]: any;
    },
    currentDataKey: string,
    funds: any[],
    fundsCount: number,
    fundDetail: ReturnType<typeof fundDetail>;
}

const initialState = {
    fetched: false,
    fetching: false,
    filterText: '',
    filter: {
        from: 0,
    },
    currentDataKey: '',
    funds: [],
    fundsCount: 0,
    fundDetail: fundDetail(),
};

export default function fundRegion(state: FundRegionState = initialState, action: AnyAction = {type: undefined}): FundRegionState {
    if (isFundTreeAction(action) && action.area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
        return {
            ...state,
            fundDetail: fundDetail(state.fundDetail, action),
        };
    }

    switch (action.type) {
        //case types.LOGOUT:
        case types.LOGIN_SUCCESS: {
            if (action.reset) {
                return initialState;
            }
            return state;
        }

        case types.STORE_SAVE:
            /** ingnorujeme typ - jedná se o SAVE state (celý store není potřeba) */
            return {
                fundDetail: fundDetail(state.fundDetail, action),
            } as any;
        case types.STORE_LOAD:
            if (action.fundRegion) {
                return {
                    ...state,
                    fetched: false,
                    fetching: false,
                    filterText: '',
                    currentDataKey: '',
                    funds: [],
                    fundsCount: 0,
                    ...action.fundRegion,
                    fundDetail: fundDetail(action.fundRegion.fundDetail, action),
                };
            } else {
                return state;
            }
        case types.DELETE_FUND:
            return {
                ...state,
                currentDataKey: '',
                fundDetail: fundDetail(state.fundDetail, action),
            };
        case types.CHANGE_APPROVE_VERSION:
        case types.CHANGE_FUND:
        case types.OUTPUT_CHANGES:
            return {
                ...state,
                currentDataKey: '',
                fundDetail: fundDetail(state.fundDetail, action),
            };
        case types.OUTPUT_STATE_CHANGE:
            return {
                ...state,
                fundDetail: fundDetail(state.fundDetail, action),
            };
        case types.FUNDS_SELECT_FUND:
        case types.FUNDS_FUND_DETAIL_REQUEST:
        case types.FUNDS_FUND_DETAIL_RECEIVE:
            return {
                ...state,
                fundDetail: fundDetail(state.fundDetail, action),
            };
        case types.FUNDS_SEARCH:
            return {
                ...state,
                filterText: typeof action.filterText !== 'undefined' ? action.filterText : '',
                filter: {
                    from: 0,
                },
                currentDataKey: '',
            };
        case types.FUNDS_FILTER:
            return {
                ...state,
                filter: {
                    from: typeof action.filter.from !== 'undefined' ? action.filter.from : '',
                    institutionIdentifier: action.filter.institutionIdentifier,
                },
                currentDataKey: '',
            };
        case types.FUNDS_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            };
        case types.FUNDS_RECEIVE:
            return {
                ...state,
                fetching: false,
                fetched: true,
                funds: action.data.funds,
                fundsCount: action.data.totalCount,
            };
        default:
            return state;
    }
}
