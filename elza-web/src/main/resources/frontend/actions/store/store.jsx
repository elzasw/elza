import * as types from 'actions/constants/ActionTypes';
import {save} from 'stores/app/AppStore'
import {routerNavigate} from 'actions/router'
import {setFocus} from 'actions/global/focus'

// Globální proměnná pro možnost vypnutí ukládání stavu do local storage
var _storeSaveEnabled = true

export function resetLocalStorage() {
    // Uložení do local storage
    if(typeof(Storage) !== "undefined") {
        _storeSaveEnabled = false
        localStorage.removeItem('ELZA-STORE-STATE');
        location.reload();
    }
}

export function storeRestoreFromStorage() {
    return (dispatch, getState) => {
        // Načtení z local storage
        if(typeof(Storage) !== "undefined") {
            var localStorageData = localStorage.getItem('ELZA-STORE-STATE');
            if (localStorageData) {
                localStorageData = JSON.parse(localStorageData);
            }
            if (localStorageData) {
                //console.log('Local storage data', localStorageData);
                dispatch(storeStateDataInit(localStorageData));

                var stateRegion = localStorageData.stateRegion;
                if (stateRegion) {
                    if (stateRegion.arrRegion) {
                        dispatch(storeLoadData('ARR_REGION', stateRegion.arrRegion, false));
                    }
                    if (stateRegion.fundRegion) {
                        dispatch(storeLoadData('FUND_REGION', stateRegion.fundRegion, false));
                    }
                    if (stateRegion.partyRegionFront && stateRegion.partyRegionFront.length > 0) {
                        dispatch(storeLoadData('PARTY_REGION', stateRegion.partyRegionFront[0], false));
                    }
                    if (stateRegion.registryRegionFront && stateRegion.registryRegionFront.length > 0) {
                        dispatch(storeLoadData('REGISTRY_REGION', stateRegion.registryRegionFront[0], false));
                    }
                }
            }
        }
    }
}

export function storeSave() {
    return (dispatch, getState) => {
        if (_storeSaveEnabled) {
            var store = getState();

            // Načtení dat pro uložení
            var data = save(store);
            //console.log('@@@@storeSave', data);

            // Uložení dat do store - pro zobrazování home stránky a pro uložení dalších inicializačních dat, např. splitter atp.
            dispatch(storeStateData(data))

            // Uložení do local storage
            if(typeof(Storage) !== "undefined") {
                var storeNew = getState();

                var localStorageData = {
                    stateRegion: storeNew.stateRegion,
                    splitter: storeNew.splitter,
                }

                _storeSaveEnabled && localStorage.setItem('ELZA-STORE-STATE', JSON.stringify(localStorageData));
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
            case 'PARTY_REGION':
                dispatch(storeLoad({partyRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/party'));
                    dispatch(setFocus('party', 1, 'list'))

                }
                break;
            case 'REGISTRY_REGION':
                dispatch(storeLoad({registryRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/registry'));
                    dispatch(setFocus('registry', 1, 'list'))
                }
                break;
            case 'ARR_REGION':
                dispatch(storeLoad({arrRegion: data}));
                if (switchView) {
                    dispatch(routerNavigate('/arr'));
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
                    dispatch(setFocus('arr', 1, 'tree'))
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
