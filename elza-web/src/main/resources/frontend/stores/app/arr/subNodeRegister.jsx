import * as types from 'actions/constants/actionTypes';
import {i18n} from 'components'
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    versionId: null,
    nodeId: null,
    data: null,
}

export default function subNodeRegister(state = initialState, action) {
    switch (action.type) {

        case types.FA_SUB_NODE_REGISTER_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })

        case types.FA_SUB_NODE_REGISTER_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                data: action.data,
            })

        default:
            return state
    }
}

