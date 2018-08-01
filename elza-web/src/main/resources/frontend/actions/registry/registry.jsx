import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'
import {storeFromArea, indexById} from 'shared/utils'

import {DEFAULT_LIST_SIZE, MODAL_DIALOG_VARIANT} from '../../constants.tsx'
export const DEFAULT_REGISTRY_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;
export const AREA_REGISTRY_LIST = "registryList";
import * as types from 'actions/constants/ActionTypes.js';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {i18n} from 'components/shared';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess,addToastrDanger, addToastrWarning} from '../../components/shared/toastr/ToastrActions.jsx'


let AddRegistryForm;
import('../../components/registry/AddRegistryForm').then((a) => {
    AddRegistryForm = a.default;
});
//import AddRegistryForm from "../../components/registry/AddRegistryForm";



/**
 * Načtení seznamu rejstříků dle filtru
 *
 * @param from {number} od kolikáté položky se má posílat seznam - stránkování
 * @param size {number} počet položek v seznamu - velikost jedné stránky
 */
export function registryListFetchIfNeeded(from = 0, size = DEFAULT_REGISTRY_LIST_MAX_SIZE) {
    return SimpleListActions.fetchIfNeeded(AREA_REGISTRY_LIST, true, (parent, filter) => WebApi.findRegistry(filter.text, filter.registryParentId, filter.registryTypeId, filter.versionId, filter.itemSpecId, filter.from, size, filter.scopeId, filter.excludeInvalid));
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


/**
 * TODO Rejstříky ve verzi 16 nemají žádnou možnost pro move (hierarchie)
 *
 * @param parentRecordId
 * @returns {Function}
 */
export function registryMove(parentRecordId) {
    return (dispatch, getState) => {
        const store = getState();
        const list = storeFromArea(store, AREA_REGISTRY_LIST);
        if (!list.recordForMove) {
            console.error('Not selected record for move');
            return;
        }

        WebApi.getAccessPoint(list.recordForMove.id).then((data) => {
            // TODO compel AP
            savingApiWrapper(dispatch, WebApi.updateAccessPoint(list.recordForMove.id, {...data, parentRecordId})).then(json => {
                dispatch(registryMoveFinish());
                dispatch(registryListInvalidate());
            });
        });
    }
}


export function registryMoveStart(data) {

    return {
        type: types.REGISTRY_MOVE_REGISTRY_START,
        area: AREA_REGISTRY_LIST,
        data
    }
}

export function registryMoveCancel() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_CANCEL,
        area: AREA_REGISTRY_LIST,
    }
}


export function registryMoveFinish() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_FINISH,
        area: AREA_REGISTRY_LIST,
    }
}





export const AREA_REGISTRY_DETAIL = "registryDetail";

export function registryDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        dispatch(DetailActions.fetchIfNeeded(AREA_REGISTRY_DETAIL, id, () => {
            return WebApi.getAccessPoint(id).then((data) => {
                if (data && data.invalid) {
                    dispatch(addToastrWarning(i18n("registry.invalid.warning")));
                }
                return data;
            }).catch(() => dispatch(registryDetailClear()));
        }));
    }
}

export function registryDetailInvalidate() {
    return DetailActions.invalidate(AREA_REGISTRY_DETAIL, null)
}

export function registryDetailClear() {
    return registryDetailFetchIfNeeded(null);
}


export function registryAdd(parentId, versionId, callback, parentName = '', showSubmitTypes = false) {
    return (dispatch) => {
        const title = parentId && parentName ? i18n('registry.addRegistryFor', parentName.substr(0, 40)) : i18n('registry.addRegistry');
        dispatch(modalDialogShow(this, title,
            <AddRegistryForm
                versionId={versionId}
                showSubmitTypes={showSubmitTypes}
                onSubmitForm={(data, submitType) => (dispatch(registryRecordCreate(parentId, callback, data, submitType)))}
                parentRecordId={parentId}
            />
            )
        )

    }
}

function registryRecordCreate(parentId, callback, data, submitType) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, (
            data.structured ?
                WebApi.confirmStructuredAccessPoint(data.id).then(() => data.structuredObj) :
                WebApi.createAccessPoint(data.name, data.complement, data.langaugeCode, data.description, data.typeId, data.scopeId)
        )).then(json => {
            dispatch(modalDialogHide());
            callback && callback(json, submitType);
        });
    }
}

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
                dispatch(registryListInvalidate())
            }

            if (callback !== null) {
                dispatch(callback);
            }
        });
    }
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
                dispatch(registryListInvalidate())
            }
        });
    }
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

            dispatch(registryListFilter({
                ...list.filter,
                parents: [
                    {id: item.id, name: item.record},
                    ...item.parents
                ],
                typesToRoot: item.typesToRoot,
                text: null,
                registryParentId: item.id,
            }));
        });
    }
}


/// Variant registry


/**
 * @deprecated
 * @param data
 * @returns {Function}
 */
export function registryVariantUpdate(data) {
    return (dispatch, getState) => {
        const store = getState();
        const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
        let needFetch = false;
        detail.data.names.map(variant => {
            if (variant.id == data.id && variant.record !== data.record) {
                needFetch = true;
            }
        });
        if (needFetch === true) {
            return savingApiWrapper(dispatch, WebApi.updateAccessPointName(-1, data)).then(json => {
                dispatch(receiveRegistryVariantRecord(json));
            });
        }
    }
}

export function receiveRegistryVariantRecord(json) {
    return {
        item: json,
        type: types.REGISTRY_VARIANT_RECORD_RECEIVED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryVariantAddRow() {
    return{
        type: types.REGISTRY_VARIANT_RECORD_CREATE,
        area: AREA_REGISTRY_DETAIL
    }
}

/**
 * @deprecated
 */
export function registryVariantCreate(data, variantRecordInternalId) {
    return (dispatch) => {
        savingApiWrapper(dispatch, WebApi.createAccessPointName(-1 ,data)).then(json => {
            dispatch(registryVariantCreated(json, variantRecordInternalId));
        });
    }
}

export function registryVariantCreated(json, variantRecordInternalId) {
    return {
        json,
        variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_CREATED,
        area: AREA_REGISTRY_DETAIL
    }
}

/**
 * @deprecated
 */
export function registryVariantDelete(variantRecordId){
    return (dispatch) => {
        WebApi.deleteAccessPointName(-1, variantRecordId).then(json => {
            dispatch(registryVariantDeleted(variantRecordId));
        });
    }
}

export function registryVariantDeleted(variantRecordId) {
    return {
        variantRecordId,
        type: types.REGISTRY_VARIANT_RECORD_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryVariantInternalDelete(variantRecordInternalId) {
    return {
        variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}


/// Coordinates
export function registryCoordinatesUpdate(data) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateRegCoordinates(data)).then(json => {
            dispatch(registryCoordinatesReceive(json));
        });
    }
}

export function registryCoordinatesReceive(json) {
    return {
        item: json,
        type: types.REGISTRY_RECORD_COORDINATES_RECEIVED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesAddRow() {
    return {
        type: types.REGISTRY_RECORD_COORDINATES_CREATE,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesCreate(data, coordinatesId) {
    return (dispatch) => {
        savingApiWrapper(dispatch, WebApi.createRegCoordinates(data)).then(json => {
            dispatch(registryCoordinatesCreated(json, coordinatesId));
        });
    }
}

export function registryCoordinatesCreated(json, coordinatesInternalId) {
    return {
        json,
        coordinatesInternalId,
        type: types.REGISTRY_RECORD_COORDINATES_CREATED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesDelete(coordinatesId) {
    return (dispatch) => {
        WebApi.deleteRegCoordinates(coordinatesId).then(json => {
            dispatch(registryCoordinatesDeleted(coordinatesId));
        });
    }
}

export function registryCoordinatesDeleted(coordinatesId) {
    return {
        coordinatesId,
        type: types.REGISTRY_RECORD_COORDINATES_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesInternalDelete(coordinatesInternalId) {
    return {
        coordinatesInternalId,
        type: types.REGISTRY_RECORD_COORDINATES_INTERNAL_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesChange(item) {
    return {
        item,
        type: types.REGISTRY_RECORD_COORDINATES_CHANGE,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesUpload(file, apRecordId) {
    return (dispatch) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('apRecordId', apRecordId);
        savingApiWrapper(dispatch, WebApi.regCoordinatesImport(formData)).then(() => {
            dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successCoordinates')));
        }).catch(() => {
            dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorCoordinates')));
        });
    }
}
