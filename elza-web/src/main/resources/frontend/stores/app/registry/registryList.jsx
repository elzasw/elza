import SimpleListReducer from "shared/list/simple/SimpleListReducer";
import * as types from 'actions/constants/ActionTypes.js';


/**
 * Přetížený simple list reducer
 * @param state
 * @param action
 * @param config
 * @returns {{recordForMove: null}}
 */
export default function reducer(state = undefined, action = {}, config = undefined) {
    switch (action.type) {
        case types.REGISTRY_MOVE_REGISTRY_START: {
            return {
                ...state,
                recordForMove: action.data
            }
        }
        case types.REGISTRY_MOVE_REGISTRY_FINISH:
        case types.REGISTRY_MOVE_REGISTRY_CANCEL: {
            return {
                ...state,
                recordForMove: null,
            }
        }
        default:
            if (state === undefined) {
                return {
                    recordForMove: null,
                    ...SimpleListReducer(state, action, config ? {...config, reducer} : {reducer})
                };
            }
            return SimpleListReducer(state, action, config ? {...config, reducer} : {reducer});
    }
}
