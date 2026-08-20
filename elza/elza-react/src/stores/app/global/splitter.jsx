import * as types from 'actions/constants/ActionTypes';

/** Oblast použitá pro stránky, které nemají vlastní rozměry panelů. */
export const SPLITTER_AREA_GLOBAL = 'global';

export const DEFAULT_SPLITTER_SIZES = {
    leftWidth: 250,
    rightWidth: 150,
};

/** Horní hranice rozumné velikosti panelu - větší uložená hodnota se považuje za poškozenou. */
const MAX_SPLITTER_SIZE = 4000;

const initialState = {
    splitters: {
        [SPLITTER_AREA_GLOBAL]: DEFAULT_SPLITTER_SIZES,
        "AIP": DEFAULT_SPLITTER_SIZES,
        "DAO": DEFAULT_SPLITTER_SIZES,
        "ARR_DATA_GRID": DEFAULT_SPLITTER_SIZES,
        "ARR_MOVEMENTS": DEFAULT_SPLITTER_SIZES,
        "ARR_OUTPUT": DEFAULT_SPLITTER_SIZES,
        "ARR": DEFAULT_SPLITTER_SIZES,
        "ARR_REQUEST": DEFAULT_SPLITTER_SIZES,
        "FUND_ACTION": DEFAULT_SPLITTER_SIZES,
    }
};

/**
 * Uložená velikost panelu, nebo null, pokud je hodnota nepoužitelná.
 */
function validSize(size) {
    if (typeof size !== 'number' || !isFinite(size) || size < 0 || size >= MAX_SPLITTER_SIZE) {
        return null;
    }
    return size;
}

/**
 * Obnovení velikostí panelů z local storage.
 *
 * Ukládá se celý stav, tedy `{splitters: {oblast: {leftWidth, rightWidth}}}`. Starší uložená
 * data mají ale jen jednu sadu rozměrů přímo v korenu (`{leftWidth, rightWidth}`) - ta se
 * použije pro globální oblast. Nepoužitelné hodnoty (chybějící, nečíselné, mimo rozsah) se
 * nahradí výchozími, oblasti bez jediné použitelné hodnoty se ignorují.
 */
function restoreSplitters(state, storedSplitter) {
    const stored = storedSplitter.splitters || {[SPLITTER_AREA_GLOBAL]: storedSplitter};
    const splitters = {...state.splitters};

    Object.keys(stored).forEach(area => {
        const sizes = stored[area];
        if (!sizes || typeof sizes !== 'object') {
            return;
        }

        const leftWidth = validSize(sizes.leftWidth);
        const rightWidth = validSize(sizes.rightWidth);
        if (leftWidth === null && rightWidth === null) {
            return;
        }

        const defaults = splitters[area] || DEFAULT_SPLITTER_SIZES;
        splitters[area] = {
            leftWidth: leftWidth === null ? defaults.leftWidth : leftWidth,
            rightWidth: rightWidth === null ? defaults.rightWidth : rightWidth,
        };
    });

    return {...state, splitters};
}

export default function splitter(state = initialState, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA_INIT:
            if (action.storageData.splitter) {
                return restoreSplitters(state, action.storageData.splitter);
            } else {
                return state;
            }
        case types.GLOBAL_SPLITTER_RESIZE:
            return {
                ...state,
                splitters: {
                    ...state.splitters,
                    [action.area || SPLITTER_AREA_GLOBAL] : {
                        leftWidth: action.leftSize,
                        rightWidth: action.rightSize,
                    }
                }
            };
        default:
            return state;
    }
}
