import {INIT_REDUCER} from './SharedActions';

/**
 * Společný store pro různá data, např. pro globální čísleníky pro fieldy atp. Převážně využíván např. fieldy z shared (jako StatField atp.).
 */
const initialState = {
    stores: {}, // mapa název reduceru na jeho store
    reducer: shared,
};

export default function shared(state = initialState, action = {}) {
    switch (action.type) {
        case INIT_REDUCER: {
            let area = action.area;
            return {
                ...state,
                stores: {
                    ...state.stores,
                    [area]: action.store,
                },
            };
        }
        default:
            if (action.area) {
                let store = state.stores[action.area];
                if (store) {
                    let newStore = store.reducer(store, action);
                    return {
                        ...state,
                        stores: {
                            ...state.stores,
                            [action.area]: newStore,
                        },
                    };
                } else {
                    return state;
                }
            } else {
                return state;
            }
    }
}
