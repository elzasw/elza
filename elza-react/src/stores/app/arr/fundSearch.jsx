import { indexById } from 'stores/app/utils.jsx';
import * as types from './../../../actions/constants/ActionTypes.js';

const initialState = {
    fulltext: '',
    funds: [],
    isFetching: false,
    fetched: false,
    currentDataKey: '',
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
            if (action.fulltext === '') {
                return initialState;
            } else {
                return {
                    ...state,
                    fulltext: action.fulltext,
                };
            }
        }
        case types.FUND_SEARCH_FULLTEXT_REQUEST: {
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.fulltext,
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

            Object.assign(
                newFunds[index],
                { ...action.fund, isFetching: false, fetched: true, nodes: action.nodes },
            );

            return {
                ...state,
                funds: newFunds,
            };
        }
        default:
            return state;
    }
}
