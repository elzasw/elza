import {save} from 'stores/AppStore.jsx'
import {storeStateData} from './store.jsx'

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

export function storeSave() {
    return (dispatch, getState) => {
        if (_storeSaveEnabled) {
            var store = getState();

            // Načtení dat pro uložení
            var data = save(store);
            //console.log('@@@@storeSave', data);

            // Uložení dat do store - pro zobrazování home stránky a pro uložení dalších inicializačních dat, např. splitter atp.
            dispatch(storeStateData(data));

            // Uložení do local storage
            if(typeof(Storage) !== "undefined") {
                var storeNew = getState();

                const localStorageData = {
                    stateRegion: storeNew.stateRegion,
                    splitter: storeNew.splitter,
                };

                _storeSaveEnabled && localStorage.setItem('ELZA-STORE-STATE', JSON.stringify(localStorageData));
            }
        }
    }
}