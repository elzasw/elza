import * as types from 'actions/constants/ActionTypes.js';
import {i18n} from 'components/shared';
import {indexById} from "stores/app/utils.jsx";

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    data: [],
    currentDataKey: null,
};

export default function subNodeDaos(state = initialState, action = {}) {
    switch (action.type) {

        case types.FUND_SUB_NODE_DAOS_REQUEST:
            return {
                ...state,
                currentDataKey: action.dataKey
            };

        case types.FUND_SUB_NODE_DAOS_RECEIVE:
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                data: action.data
            };

        // ------ WebSocket -----
        case types.CHANGE_DAOS:
            return {
                ...state,currentDataKey: false
            };

        default:
            return state
    }
}

