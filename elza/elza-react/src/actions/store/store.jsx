import * as types from 'actions/constants/ActionTypes';
import {routerNavigate} from 'actions/router.jsx';
import {setFocus} from 'actions/global/focus.jsx';
import {FOCUS_KEYS, URL_ENTITY} from '../../constants.tsx';

export function storeRestoreFromStorage() {
    return (dispatch, getState) => {
        // Načtení z local storage
        if (typeof Storage !== 'undefined') {
            let localStorageData = localStorage.getItem('ELZA-STORE-STATE');
            if (localStorageData) {
                localStorageData = JSON.parse(localStorageData);
            }
            if (localStorageData) {
                console.log('Local storage data', localStorageData);
                dispatch(storeStateDataInit(localStorageData));

                var stateRegion = localStorageData.stateRegion;
                if (stateRegion) {
                    if (stateRegion.arrRegion) {
                        dispatch(storeLoadData('ARR_REGION', stateRegion.arrRegion));
                    }
                    if (stateRegion.fundRegion) {
                        dispatch(storeLoadData('FUND_REGION', stateRegion.fundRegion));
                    }
                    if (stateRegion.app) {
                        dispatch(storeLoadData('APP', stateRegion.app));
                    }
                    if (stateRegion.registryRegionFront && stateRegion.registryRegionFront.length > 0) {
                        dispatch(storeLoadData('REGISTRY_REGION', stateRegion.registryRegionFront[0]));
                    }
                    if (stateRegion.adminRegion) {
                        dispatch(storeLoadData('ADMIN_REGION', stateRegion.adminRegion));
                    }
                }
            }
        }
    };
}

export function storeStateDataInit(storageData) {
    return {
        type: types.STORE_STATE_DATA_INIT,
        storageData,
    };
}

export function storeStateData(data) {
    return {
        type: types.STORE_STATE_DATA,
        ...data,
    };
}

export function storeLoadData(type, data) {
    return (dispatch) => {
        switch (type) {
            case 'APP':
                dispatch(storeLoad({store: 'app', ...data}));
                break;
            case 'REGISTRY_REGION':
                dispatch(storeLoad({store: 'app', registryDetail: {...data}}));
                break;
            case 'ARR_REGION':
                dispatch(storeLoad({arrRegion: data}));
                break;
            case 'ADMIN_REGION':
                dispatch(storeLoad({adminRegion: data}));
                break;
            case 'FUND_REGION':
                dispatch(storeLoad({fundRegion: data}));
                break;
            case 'ARR_REGION_FUND':
                dispatch(storeLoad({arrRegionFund: data}));
                break;
            default:
                break;
        }
    };
}

export function storeLoad(data) {
    return {
        type: types.STORE_LOAD,
        ...data,
    };
}
