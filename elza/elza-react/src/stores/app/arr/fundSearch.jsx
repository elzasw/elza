import { indexById } from 'stores/app/utils';
import * as types from './../../../actions/constants/ActionTypes';
import { /* getDataKey, */ generateDataKey } from 'actions/arr/fundSearch';

const initialState = {
    fulltext: '',
    filters: [],
    isIdSearch: false,
    funds: [],
    isFetching: false,
    fetched: false,
    currentDataKey: '',
    totalCount: 0,
    partialResult: false,
};

const initialFundState = {
    expanded: false,
    isFetching: false,
    fetched: false,
    nodes: [],
};

export default function fundSearch(state = initialState, action = {}) {
    let index;
    switch (action.type) {
        case types.FUND_SEARCH_FULLTEXT_CHANGE: {
            if (action.fulltext === '' && !action.filters) {
                return {
                    ...initialState,
                    isIdSearch: state.isIdSearch,
                };
            } else {
                return {
                    ...state,
                    fulltext: action.fulltext != undefined ? action.fulltext : state.fulltext,
                    filters: action.filters || state.filters,
                    isIdSearch: action.isIdSearch != undefined ? action.isIdSearch : state.isIdSearch,
                };
            }
        }
        case types.FUND_SEARCH_FULLTEXT_REQUEST: {
            return {
                ...state,
                isFetching: true,
                currentDataKey: generateDataKey(state.filters),
            };
        }
        case types.FUND_SEARCH_FULLTEXT_RECEIVE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                funds: action.funds.map(fund => {
                    return {
                        ...initialFundState,
                        ...fund,
                    };
                }),
                partialResult: action.partialResult,
                totalCount: action.totalCount,
            };
        }
        case types.FUND_SEARCH_EXPAND_FUND: {
            index = indexById(state.funds, action.fund.id);
            const newFunds = [...state.funds];
            const { fund } = action;

            Object.assign(newFunds[index], { ...fund, expanded: !fund.expanded });

            return {
                ...state,
                funds: newFunds,
            };
        }
        case types.FUND_SEARCH_FUND_REQUEST: {
            index = indexById(state.funds, action.fund.id);
            const newFunds = [...state.funds];

            Object.assign(newFunds[index], { ...action.fund, isFetching: true });

            return {
                ...state,
                funds: newFunds,
            };
        }
        case types.FUND_SEARCH_FUND_RECEIVE: {
            index = indexById(state.funds, action.fund.id);
            const newFunds = [...state.funds];

            Object.assign(newFunds[index], { ...action.fund, isFetching: false, fetched: true, nodes: action.nodes });

            return {
                ...state,
                funds: newFunds,
            };
        }
        default:
            return state;
    }
}
