import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'

import {consolidateState} from 'components/Utils'
import {isFundTreeAction} from 'actions/arr/fundTree'
import fundDetail from './fundDetail'

const initialState = {
    list: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null
    },
    detail: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null
    }

}

export default function fundActions(state = initialState, action = {}) {
    switch (action) {
        case types.
            default:
            return state;
    }
}