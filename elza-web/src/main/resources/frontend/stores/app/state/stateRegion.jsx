import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    partyRegionFront: [],
    registryRegionFront: [],
    arrRegion: null,
    arrRegionFront: [],
}

function updateFront(front, item, index) {
    var result;

    if (index !== null) {    // není ve frontě, přidáme ho tam
        result = [
            ...front.slice(0, index),
            ...front.slice(index + 1),
            item
        ]
    } else {
        result = [...front, item]
    }

    if (result.length > 20) {   // pokud máme moc dlouhou frontu, zkrátíme ji
        result = [...front.slice(1, result.length)]
    }

    return result;
}

export default function stateRegion(state = initialState, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA_INIT:
            return {
                ...state,
                ...action.storageData.stateRegion
            }
        case types.STORE_STATE_DATA:
            var result = {
                ...state,
            }

            if (action.partyRegion) {
                var index = indexById(result.partyRegionFront, action.partyRegion.selectedPartyID, 'selectedPartyID');
                result.partyRegionFront = updateFront(result.partyRegionFront, action.partyRegion, index);
            }
            if (action.registryRegion) {
                var index = indexById(result.registryRegionFront, action.registryRegion.selectedId, 'selectedId');
                result.registryRegionFront = updateFront(result.registryRegionFront, action.registryRegion, index);
            }
            if (action.arrRegion) {
                result.arrRegion = action.arrRegion

                action.arrRegion.fas.map(faobj => {
                    var index = indexById(result.arrRegionFront, faobj.versionId, 'versionId');
                    result.arrRegionFront = updateFront(result.arrRegionFront, faobj, index);
                })
            }

            return result;
        default:
            return state
    }
}

