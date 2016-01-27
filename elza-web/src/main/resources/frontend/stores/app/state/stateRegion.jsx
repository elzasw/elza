import * as types from 'actions/constants/actionTypes';

const initialState = {
    partyRegion: null,
    registryRegion: null,
    arrRegion: null,
    faData: {},
}

export default function partyRegion(state = initialState, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA:
            var result = {
                ...state,
            }

            if (action.partyRegion) {
                result.partyRegion = action.partyRegion
            }
            if (action.registryRegion) {
                result.registryRegion = action.registryRegion
            }
            if (action.arrRegion) {
                result.arrRegion = action.arrRegion
                result.faData = {...result.faData}
                action.arrRegion.fas.map(faobj => {
                    result.faData[faobj.versionId] = {...faobj};
                })
            }

            return result;
        default:
            return state
    }
}

