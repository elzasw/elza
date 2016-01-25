/**
 * Akce pro správu fulltextu
 *
 * @author Jiří Vaněk
 * @since 22.1.2016
 */
import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes';
 
 /**
 * Získání stavu indexování ze serveru.
 * @returns {dispatch} dispatch
 */
export function getIndexStateFetch() {
    return dispatch => {
        dispatch(getIndexStateRequest())
        return WebApi.getIndexingState().then(json => dispatch(getIndexStateRecieve(json)));
    }
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
    }
}

/**
 * Získání stavu indexování ze serveru pokud není načtený.
 *
 * @returns {Function}
 */
export function actionReindex() {
    return (dispatch, getState) => {
        dispatch(reindex());
        return WebApi.reindex();
    }
}

/**
 * Akce pro spuštění indexování.
 */
export function reindex() {
    return {
        type: types.ADMIN_FULLTEXT_REINDEXING_REQUEST
    }
}

/**
 * Akce pro získání stavu indexování.
 */
export function getIndexStateRequest() {
    return {
        type: types.ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST
    }
}

/**
 * Akce po získání seznamu indexování.
 *
 * @return {Object} akce
 */
export function getIndexStateRecieve(json) {
    return {
        type: types.ADMIN_FULLTEXT_REINDEXING_STATE_RECIEVE,
        indexingState: json
    }
}