import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'
import {i18n} from 'components/shared';
import {addToastrWarning} from '../../components/shared/toastr/ToastrActions.jsx'

export const AREA_SCOPE_LIST = "scopeList";
export const AREA_SCOPE_DETAIL = "scopeDetail";
export const AREA_LANGUAGE_LIST = "languageList";

const dataToRowsHelper = data => ({rows: data, count: data.length});

/**
 * Načtení seznamu tříd rejstříků.
 */
export function scopesListFetchIfNeeded() {
    return (dispatch) => {
        dispatch(SimpleListActions.fetchIfNeeded(AREA_SCOPE_LIST, null, () => {
            return WebApi.getAllScopes().then(dataToRowsHelper);
        }));
    }
}

/**
 * Invalidace seznamu tříd rejstříků.
 */
export function scopeListInvalidate() {
    return (dispatch) => {
        dispatch(SimpleListActions.invalidate(AREA_SCOPE_LIST));
    }
}

/**
 * Načtení detailu scope.
 */
export function scopeDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        dispatch(DetailActions.fetchIfNeeded(AREA_SCOPE_DETAIL, id, () => {
            return WebApi.getScopeWithConnected(id).then((data) => {
                if (data && data.invalid) {
                    dispatch(addToastrWarning(i18n("accesspoint.scope.invalid.warning")));
                }
                return data;
            }).catch(() => dispatch(scopeDetailClear()));
        }));
    }
}

/**
 * Zneplatnění detailu scope.
 */
export function scopeDetailInvalidate() {
    return DetailActions.invalidate(AREA_SCOPE_DETAIL, null)
}

export function scopeDetailClear() {
    return scopeDetailFetchIfNeeded(null);
}

/**
 * Načtení seznamu jazyků.
 */
export function languagesListFetchIfNeeded() {
    return (dispatch) => {
        dispatch(SimpleListActions.fetchIfNeeded(AREA_LANGUAGE_LIST, null, () => {
            return WebApi.getAllLanguages().then(dataToRowsHelper);
        }));
    }
}

/**
 * Invalidace seznamu jazyků.
 */
export function languagesListInvalidate() {
    return (dispatch) => {
        dispatch(SimpleListActions.invalidate(AREA_LANGUAGE_LIST));
    }
}
