import React from 'react';

import {WebApi} from 'actions/index.jsx';
import {SimpleListActions} from 'shared/list';
import {DetailActions} from '../../shared/detail';
import {indexById, storeFromArea} from 'shared/utils';

import {DEFAULT_LIST_SIZE} from '../../constants.tsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {i18n} from 'components/shared';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';
import {addToastrWarning} from '../../components/shared/toastr/ToastrActions.jsx';

export const DEFAULT_REGISTRY_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;
export const AREA_REGISTRY_LIST = 'registryList';

/**
 * Načtení seznamu rejstříků dle filtru
 *
 * @param from {number} od kolikáté položky se má posílat seznam - stránkování
 * @param size {number} počet položek v seznamu - velikost jedné stránky
 */
export function registryListFetchIfNeeded(from = 0, size = DEFAULT_REGISTRY_LIST_MAX_SIZE) {
    return SimpleListActions.fetchIfNeeded(AREA_REGISTRY_LIST, true, (parent, filter) =>
        WebApi.findAccessPoint(
            filter.text,
            filter.registryParentId,
            filter.registryTypeId,
            filter.versionId,
            filter.itemTypeId,
            filter.itemSpecId,
            filter.from,
            size,
            filter.scopeId,
            filter.excludeInvalid,
            filter.state,
        ),
    );
}

/**
 * Filtr osob
 *
 * @param filter {Object} - objekt filtru
 */
export function registryListFilter(filter) {
    return SimpleListActions.filter(AREA_REGISTRY_LIST, filter);
}

/**
 * Invalidace seznamu rejstříků
 */
export function registryListInvalidate() {
    return SimpleListActions.invalidate(AREA_REGISTRY_LIST, null);
}

export const AREA_REGISTRY_DETAIL = 'registryDetail';

export function registryDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        return dispatch(
            DetailActions.fetchIfNeeded(AREA_REGISTRY_DETAIL, id, () => {
                return WebApi.getAccessPoint(id)
                    .then(data => {
                        if (data && data.invalid) {
                            dispatch(addToastrWarning(i18n('registry.invalid.warning')));
                        }
                        return data;
                    })
                    .catch((error) => {
                        dispatch(registryDetailClear());
                        throw error;
                    });
            }),
        );
    };
}

export function registryDetailInvalidate() {
    return DetailActions.invalidate(AREA_REGISTRY_DETAIL, null);
}

export function registryDetailClear() {
    return DetailActions.reset(AREA_REGISTRY_DETAIL);
}

//todo: pryc az se prebinduje v ItemForm

export function registryAdd(versionId, callback, showSubmitTypes = false) {
    alert('Prebindovat v ItemForm');
}

// function registryRecordCreate(callback, data, submitType) {
//     return (dispatch, getState) => {
//         savingApiWrapper(
//             dispatch,
//             data.structured
//                 ? WebApi.confirmStructuredAccessPoint(data.id).then(() => data.structuredObj)
//                 : WebApi.createAccessPoint(
//                       data.name,
//                       data.complement,
//                       data.langaugeCode,
//                       data.description,
//                       data.typeId,
//                       data.scopeId,
//                   ),
//         ).then(json => {
//             dispatch(modalDialogHide());
//             callback && callback(json, submitType);
//         });
//     };
// }

export function registryUpdate(id, typeId, callback = null) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateAccessPoint(id, {typeId})).then(json => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);

            const list = storeFromArea(store, AREA_REGISTRY_LIST);

            if (detail.id == id) {
                dispatch(registryDetailInvalidate());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate());
            }

            if (callback !== null) {
                dispatch(callback);
            }
        });
    };
}

export function apMigrate(id) {
    return (dispatch, getState) => {
        WebApi.migrateAccessPoint(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);

            const list = storeFromArea(store, AREA_REGISTRY_LIST);

            if (detail.id === id) {
                dispatch(registryDetailInvalidate());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate());
            }
        });
    };
}

export function registryDelete(id) {
    return (dispatch, getState) => {
        WebApi.deleteAccessPoint(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
            const list = storeFromArea(store, AREA_REGISTRY_LIST);
            if (detail.id == id) {
                dispatch(registryDetailClear());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate());
            }
        });
    };
}
/* MCV-45365
export function setValidRegistry(id) {
    return (dispatch, getState) => {
        WebApi.setValidRegistry(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
            const list = storeFromArea(store, AREA_REGISTRY_LIST);
            if (detail.id == id) {
                dispatch(registryDetailClear());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate())
            }
        });
    }
}
*/
export function registrySetFolder(recordId) {
    return (dispatch, getState) => {
        return WebApi.getAccessPoint(recordId).then(item => {
            const store = getState();
            const list = storeFromArea(store, AREA_REGISTRY_LIST);

            dispatch(
                registryListFilter({
                    ...list.filter,
                    parents: [{id: item.id, name: item.record}, ...item.parents],
                    typesToRoot: item.typesToRoot,
                    text: null,
                    registryParentId: item.id,
                }),
            );
        });
    };
}
