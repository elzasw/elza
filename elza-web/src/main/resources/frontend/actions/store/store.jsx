import * as types from 'actions/constants/actionTypes';
import {save} from 'stores/app/AppStore'
import {routerNavigate} from 'actions/router'

export function storeRestoreFromStorage() {
    return (dispatch, getState) => {
        // Načtení z local storage
        if(typeof(Storage) !== "undefined") {
            var data = localStorage.getItem('ELZA-STORE-STATE');
            if (data) {
                data = JSON.parse(data);
            }
            if (data) {
                console.log('Local storage data', data);
                dispatch(storeStateDataInit(data));

                if (data.arrRegion) {
                    dispatch(storeLoadData('ARR_REGION', data.arrRegion, false));
                }
                if (data.partyRegionFront && data.partyRegionFront.length > 0) {
                    dispatch(storeLoadData('PARTY_REGION', data.partyRegionFront[data.partyRegionFront.length - 1], false));
                }
                if (data.registryRegionFront && data.registryRegionFront.length > 0) {
                    dispatch(storeLoadData('REGISTRY_REGION', data.registryRegionFront[data.registryRegionFront.length - 1], false));
                }
            }
        }
    }
}

export function storeSave() {
    return (dispatch, getState) => {
        var store = getState();

        // Načtení dat pro uložení
        var data = save(store);

        // Uložení dat do store - pro zobrazování home stránky
        dispatch(storeStateData(data))

        // Uložení do local storage
        if(typeof(Storage) !== "undefined") {
            var storeNew = getState();
            localStorage.setItem('ELZA-STORE-STATE', JSON.stringify(storeNew.stateRegion));
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
            case 'PARTY_REGION':
                dispatch(storeLoad({partyRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/party'));
                }
                break;
            case 'REGISTRY_REGION':
                dispatch(storeLoad({registryRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/registry'));
                }
                break;
            case 'ARR_REGION':
                dispatch(storeLoad({arrRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/arr'));
                }
                break;
            case 'ARR_REGION_FA':
                dispatch(storeLoad({arrRegionFa: data}));
                if (switchView) {
                    dispatch(routerNavigate('/arr'));
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
