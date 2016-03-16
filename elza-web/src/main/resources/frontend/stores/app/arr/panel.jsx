import * as types from 'actions/constants/ActionTypes';
import {consolidateState} from 'components/Utils'

const panelInitialState = {
    id: null,
    name: null,
    versionId: null
}

export function panel(state = panelInitialState, action = {}) {
    switch (action.type) {

        case types.PARTY_SELECT:
        case types.REGISTRY_SELECT:
            var result = {...state};
            var fund = action.fa;
            if (fund != null) {
                result.id = fund.fundId;
                result.name = fund.name;
                result.versionId = fund.id;
            }
            return consolidateState(state, result);

        case types.REGISTRY_ARR_RESET:
        case types.PARTY_ARR_RESET:
            var result = {...state};
            result.id = null;
            result.name = null;
            result.versionId = null;
            return consolidateState(state, result);

        default:
            return state;
    }
}
