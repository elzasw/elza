import * as types from 'actions/constants/actionTypes';
import {save} from 'stores/app/AppStore'
import {routerNavigate} from 'actions/router'

export function storeRestoreFromStorage() {
    return (dispatch, getState) => {
        // Načtení z local storage
        if(typeof(Storage) !== "undefined") {
            var localStorageData = localStorage.getItem('ELZA-STORE-STATE');
            if (localStorageData) {
                localStorageData = JSON.parse(localStorageData);
            }
            if (localStorageData) {
                console.log('Local storage data', localStorageData);
                dispatch(storeStateDataInit(localStorageData));

                var stateRegion = localStorageData.stateRegion;
                if (stateRegion.arrRegion) {
                    dispatch(storeLoadData('ARR_REGION', stateRegion.arrRegion, false));
                }
                if (stateRegion.partyRegionFront && stateRegion.partyRegionFront.length > 0) {
                    dispatch(storeLoadData('PARTY_REGION', stateRegion.partyRegionFront[stateRegion.partyRegionFront.length - 1], false));
                }
                if (stateRegion.registryRegionFront && stateRegion.registryRegionFront.length > 0) {
                    dispatch(storeLoadData('REGISTRY_REGION', stateRegion.registryRegionFront[stateRegion.registryRegionFront.length - 1], false));
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
        console.log('@@@@storeSave', data);

        // Uložení dat do store - pro zobrazování home stránky a pro uložení dalších inicializačních dat, např. splitter atp.
        dispatch(storeStateData(data))

        // Uložení do local storage
        if(typeof(Storage) !== "undefined") {
            var storeNew = getState();

            var localStorageData = {
                stateRegion: storeNew.stateRegion,
                splitter: storeNew.splitter,
            }

            localStorage.setItem('ELZA-STORE-STATE', JSON.stringify(localStorageData));
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
