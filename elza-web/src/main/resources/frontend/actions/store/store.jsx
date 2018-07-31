import * as types from 'actions/constants/ActionTypes.js';
import {routerNavigate} from 'actions/router.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import {FOCUS_KEYS} from "../../constants.tsx";

export function storeRestoreFromStorage() {
    return (dispatch, getState) => {
        // Načtení z local storage
        if(typeof(Storage) !== "undefined") {
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
                        dispatch(storeLoadData('ARR_REGION', stateRegion.arrRegion, false));
                    }
                    if (stateRegion.fundRegion) {
                        dispatch(storeLoadData('FUND_REGION', stateRegion.fundRegion, false));
                    }
                    if (stateRegion.app) {
                        dispatch(storeLoadData('APP', stateRegion.app, false));
                    }
                    if (stateRegion.registryRegionFront && stateRegion.registryRegionFront.length > 0) {
                        dispatch(storeLoadData('REGISTRY_REGION', stateRegion.registryRegionFront[0], false));
                    }
                    if (stateRegion.adminRegion) {
                        dispatch(storeLoadData('ADMIN_REGION', stateRegion.adminRegion, false));
                    }
                }
            }
        }
    }
}



export function storeStateDataInit(storageData) {
    return {
        type: types.STORE_STATE_DATA_INIT,
        storageData
    }
}
export function storeStateData(data) {
    return {
        type: types.STORE_STATE_DATA,
        ...data
    }
}
export function storeLoadData(type, data, switchView = true) {
    return (dispatch, getState) => {
        switch (type) {
            case 'APP':
            case 'PARTY_REGION':
                dispatch(storeLoad({store:'app', ...data}));
                if (switchView) {
                    if (data.partyDetail) {
                        dispatch(routerNavigate('/party'));
                        dispatch(setFocus(FOCUS_KEYS.PARTY, 1, 'list'))
                    }
                }
                break;
            case 'REGISTRY_REGION':
                dispatch(storeLoad({store:'app', ...data}));
                if (switchView) {
                    dispatch(routerNavigate('/registry'));
                    dispatch(setFocus(FOCUS_KEYS.REGISTRY, 1, 'list'))
                }
                break;
            case 'ARR_REGION':
                dispatch(storeLoad({arrRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/arr'));
                }
                break;
            case 'ADMIN_REGION':
                dispatch(storeLoad({adminRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/admin'));
                }
                break;
            case 'FUND_REGION':
                dispatch(storeLoad({fundRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/fund'));
                }
                break;
            case 'ARR_REGION_FUND':
                dispatch(storeLoad({arrRegionFund: data}));
                if (switchView) {
                    dispatch(routerNavigate('/arr'));
                    dispatch(setFocus(FOCUS_KEYS.ARR, 1, 'tree'))
                }
                break;
        }
    }
}

export function storeLoad(data) {
    return {
        type: types.STORE_LOAD,
        ...data
    }
}
