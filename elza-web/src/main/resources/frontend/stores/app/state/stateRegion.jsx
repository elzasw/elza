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

    if (index !== null) {  // je ve frontě, dáme ho na začátek  
        var prevItem = front[index]

        var useItem = {...item}

        if (!useItem._info) {  // nová item nemá info, použijeme info z předchozí - jedná se o případ, kdy např. není ještě detail načten z db
            useItem._info = prevItem._info
        }

        result = [
            useItem,
            ...front.slice(0, index),
            ...front.slice(index + 1),
        ]
    } else {    // není ve frontě, přidáme ho tam, ale na začátek
        result = [
            item,
            ...front,
        ]
    }

    // Pokud máme moc dlouhou frontu, zkrátíme ji
    result = result.slice(0, 5)

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

                // Aktivní index dáme do fronty jako poslední, takže bude umístěn na začátek
                var activeIndex = action.arrRegion.activeIndex
                action.arrRegion.funds.forEach((fundobj, i) => {
                    if (i !== activeIndex) {
                        var index = indexById(result.arrRegionFront, fundobj.versionId, 'versionId');
                        result.arrRegionFront = updateFront(result.arrRegionFront, fundobj, index);
                    }
                })
                if (activeIndex !== null) {
                    var fundobj = action.arrRegion.funds[activeIndex]
                    var index = indexById(result.arrRegionFront, fundobj.versionId, 'versionId');
                    result.arrRegionFront = updateFront(result.arrRegionFront, fundobj, index);
                }
            }

            return result;
        default:
            return state
    }
}

