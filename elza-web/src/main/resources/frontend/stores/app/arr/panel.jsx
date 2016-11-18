import * as types from 'actions/constants/ActionTypes.js';
import {consolidateState} from 'components/Utils.jsx'

const panelInitialState = {
    id: null,
    name: null,
    versionId: null
};

export function panel(state = panelInitialState, action = {}) {
    switch (action.type) {
        case types.REGISTRY_SELECT:{
            const fund = action.fa;
            if (fund) {
                const result = {
                    ...state,
                    id: fund.id,
                    name: fund.name,
                    versionId: fund.versionId,
                };
                return consolidateState(state, result);
            }
            return state;
        }
        case types.REGISTRY_ARR_RESET: {
            const result = {
                ...state,
                id: null,
                name: null,
                versionId: null,
            };
            return consolidateState(state, result);
        }
        default:
            return state;
    }
}
