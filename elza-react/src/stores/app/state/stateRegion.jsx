import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils';

const initialState = {
    registryRegionFront: [],
    arrRegion: null,
    fundRegion: null,
    adminRegion: null,
    arrRegionFront: [],
};

function updateFront(front, item, index) {
    let result;

    if (index !== null) {
        // je ve frontě, dáme ho na začátek
        var prevItem = front[index];

        var useItem = {...item};

        if (!useItem._info) {
            // nová item nemá info, použijeme info z předchozí - jedná se o případ, kdy např. není ještě detail načten z db
            useItem._info = prevItem._info;
        }

        result = [useItem, ...front.slice(0, index), ...front.slice(index + 1)];
    } else {
        // není ve frontě, přidáme ho tam, ale na začátek
        result = [item, ...front];
    }

    // Pokud máme moc dlouhou frontu, zkrátíme ji
    result = result.slice(0, 5);

    return result;
}

export default function stateRegion(state = initialState, action) {
    switch (action.type) {
        //case types.LOGOUT:
        case types.LOGIN_SUCCESS: {
            if (action.reset) {
                return initialState;
            }
            return state;
        }

        case types.STORE_STATE_DATA_INIT:
            return {
                ...state,
                ...action.storageData.stateRegion,
            };
        case types.STORE_STATE_DATA: {
            const result = {
                ...state,
            };
            if (action.app) {
                result.app = action.app;
                if (action.app.registryDetail && action.app.registryDetail.data) {
                    const index = indexById(result.registryRegionFront, action.app.registryDetail.id);
                    result.registryRegionFront = updateFront(
                        result.registryRegionFront,
                        action.app.registryDetail,
                        index,
                    );
                }
            }

            if (action.fundRegion) {
                result.fundRegion = action.fundRegion;
            }
            if (action.adminRegion) {
                result.adminRegion = action.adminRegion;
            }
            if (action.arrRegion) {
                result.arrRegion = action.arrRegion;

                // Aktivní index dáme do fronty jako poslední, takže bude umístěn na začátek
                const activeIndex = action.arrRegion.activeIndex;
                action.arrRegion.funds.forEach((fundobj, i) => {
                    if (i !== activeIndex) {
                        var index = indexById(result.arrRegionFront, fundobj.versionId, 'versionId');
                        result.arrRegionFront = updateFront(result.arrRegionFront, fundobj, index);
                    }
                });
                if (activeIndex !== null) {
                    const fundobj = action.arrRegion.funds[activeIndex];
                    const index = indexById(result.arrRegionFront, fundobj.versionId, 'versionId');
                    result.arrRegionFront = updateFront(result.arrRegionFront, fundobj, index);
                }
            }

            return result;
        }
        default:
            return state;
    }
}
