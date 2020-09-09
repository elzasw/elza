/**
 * Akce pro správu fulltextu
 *
 * @author Jiří Vaněk
 * @since 22.1.2016
 */
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes';

/**
 * Získání stavu indexování ze serveru.
 * @returns function dispatch
 */
export function getIndexStateFetch() {
    return dispatch => {
        dispatch(getIndexStateRequest());
        return WebApi.getIndexingState().then(json => dispatch(getIndexStateRecieve(json)));
    };
}

/**
 * Získání stavu indexování ze serveru pokud není načtený.
 *
 * @returns {Function}
 */
export function getIndexStateFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.adminRegion.fulltext.fetched && !state.adminRegion.fulltext.isFetching) {
            return dispatch(getIndexStateFetch());
        }
    };
}

/**
 * Akce pro spuštění indexování.
 */
export function reindex() {
    WebApi.reindex();
    return {
        type: types.ADMIN_FULLTEXT_REINDEXING_REQUEST,
    };
}

/**
 * Akce pro získání stavu indexování.
 */
export function getIndexStateRequest() {
    return {
        type: types.ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST,
    };
}

/**
 * Akce po získání seznamu indexování.
 *
 * @return {Object} akce
 */
export function getIndexStateRecieve(json) {
    return {
        type: types.ADMIN_FULLTEXT_REINDEXING_STATE_RECEIVE,
        indexingState: json,
    };
}
