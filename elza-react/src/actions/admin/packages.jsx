/**
 * Akce pro správu importovaných balíčků
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import {WebApi} from 'actions/index.jsx';
import {i18n} from 'components/shared';
import * as types from 'actions/constants/ActionTypes';
import * as outputFilters from '../refTables/outputFilters';
import * as exportFilters from '../refTables/exportFilters';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';

/**
 * Získání seznamu importovaných balíčků ze serveru.
 * @returns {Function} dispatch
 */
export function getPackagesFetch() {
    return dispatch => {
        dispatch(getPackagesRequest());
        return WebApi.getPackages().then(json => dispatch(getPackagesReceive(json)));
    };
}

/**
 * Získání seznamu importovaných balíčků ze serveru pokud nejsou načtené.
 *
 * @returns {Function}
 */
export function getPackagesFetchIfNeeded() {
    return (dispatch, getState) => {
        const {
            adminRegion: {
                packages: {fetched, dirty, isFetching},
            },
        } = getState();
        if ((!fetched || dirty) && !isFetching) {
            return dispatch(getPackagesFetch());
        }
    };
}

/**
 * Smazání balíčku.
 *
 * @param {string} code - kód mazaného balíčku.
 * @returns {dispatch} dispatch
 */
export function deletePackage(code) {
    return dispatch => {
        dispatch(deletePackageRequest(code));
        return WebApi.deletePackage(code)
            .then(json => dispatch(deletePackageReceive(code)))
            .then(json => {
                dispatch(
                    addToastrSuccess(
                        i18n('admin.packages.message.delete.title'),
                        i18n('admin.packages.message.delete.message', code),
                    ),
                );
            });
    };
}

/**
 * Import balíčku.
 *
 * @returns {dispatch} dispatch
 */
export function importPackage(data) {
    return dispatch => {
        dispatch(importPackageRequest());
        return savingApiWrapper(dispatch, WebApi.importPackage(data))
            .then(json => dispatch(importPackageReceive()))
            .then(json => {
                dispatch(outputFilters.invalidate());
                dispatch(exportFilters.invalidate());
                dispatch(
                    addToastrSuccess(
                        i18n('admin.packages.message.import.title'),
                        i18n('admin.packages.message.import.message'),
                    ),
                );
            });
    };
}

/**
 * Akce pro získání seznamu importovaných balíčků.
 */
export function getPackagesRequest() {
    return {
        type: types.ADMIN_PACKAGES_REQUEST,
    };
}

/**
 * Akce po získání seznamu importovaných balíčků.
 *
 * @return {Object} akce
 */
export function getPackagesReceive(json) {
    return {
        type: types.ADMIN_PACKAGES_RECEIVE,
        items: json,
    };
}

/**
 * Akce pro smazání balíčku.
 *
 * @param {string} code - kód mazaného balíčku.
 * @return {Object} akce
 */
export function deletePackageRequest(code) {
    return {
        type: types.ADMIN_PACKAGES_DELETE_REQUEST,
        code: code,
    };
}

/**
 * Akce po smazání balíčku.
 *
 * @param {string} code - kód smazaného balíčku.
 * @return {Object} akce
 */
export function deletePackageReceive(code) {
    return {
        type: types.ADMIN_PACKAGES_DELETE_RECEIVE,
        code: code,
    };
}

/**
 * Akce pro import balíčku.
 *
 * @return {Object} akce
 */
export function importPackageRequest() {
    return {
        type: types.ADMIN_PACKAGES_IMPORT_REQUEST,
    };
}

/**
 * Akce po importu balíčku.
 *
 * @return {Object} akce
 */
export function importPackageReceive() {
    return {
        type: types.ADMIN_PACKAGES_IMPORT_RECEIVE,
    };
}
