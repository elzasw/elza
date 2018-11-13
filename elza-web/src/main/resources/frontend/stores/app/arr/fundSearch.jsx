import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components/shared';
import * as types from './../../../actions/constants/ActionTypes.js';

const initialState = {
    fulltext: '',
    funds: [],
    isFetching: false,
    fetched: false,
    currentDataKey: ''
};

const initialFundState = {
    expand: false,
    isFetching: false,
    fetched: false,
    nodes: []
};

export default function fundSearch(state = initialState, action = {}) {
    switch (action.type) {
        case types.FUND_SEARCH_FULLTEXT_CHANGE: {
            if (action.fulltext === '') {
                return initialState;
            } else {
                return {
                    ...state,
                    fulltext: action.fulltext
                };
            }
        }
        case types.FUND_SEARCH_FULLTEXT_REQUEST: {
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.fulltext,
            }
        }
        case types.FUND_SEARCH_FULLTEXT_RECEIVE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                funds: action.funds.map(fund => {
                    return {
                        ...initialFundState,
                        ...fund
                    }
                })
            };
        }
        case types.FUND_SEARCH_EXPAND_FUND: {
            return state; // TODO
        }
        case types.FUND_SEARCH_COLLAPSE_FUND: {
            return state; // TODO
        }
        case types.FUND_SEARCH_FUND_REQUEST: {
            return state; // TODO
        }
        case types.FUND_SEARCH_FUND_RECEIVE:{
            return state; // TODO
        }
        default:
            return state
    }
}
