import React from 'react';

import {WebApi} from 'actions/index.jsx';
import {Api} from '../../api';
import {SimpleListActions} from 'shared/list';
import {DetailActions} from '../../shared/detail';
import {indexById, storeFromArea} from 'shared/utils';

import {DEFAULT_LIST_SIZE, URL_ENTITY} from '../../constants.tsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {i18n} from 'components/shared';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';
import {AP_VALIDATION} from '../../constants';

export const DEFAULT_REGISTRY_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;
export const AREA_REGISTRY_LIST = 'registryList';
export const AREA_REGISTRY_LAYER_LIST = 'registryLayerList';

const createFilter = values => {
    const extFilters = values.extFilters
        ? values.extFilters.map(f => {
              return {
                  partTypeCode: f.partType ? f.partType.code : null,
                  itemTypeId: f.itemType ? f.itemType.id : null,
                  itemSpecId: f.itemSpec ? f.itemSpec.id : null,
                  value: f.obj ? f.obj.id : f.value,
              };
          })
        : null;
    const relFilters = values.relFilters
        ? values.relFilters.map(f => {
              return {
                  relSpecId: f.itemSpec ? f.itemSpec.id : null,
                  relTypeId: f.itemType ? f.itemType.id : null,
                  code: f.obj ? f.obj.id : null,
              };
          })
        : null;
    return {
        search: values.search,
        area: values.area,
        onlyMainPart: values.onlyMainPart === 'true',
        user: values.user,
        code: values.id,
        creation: values.creation,
        extinction: values.extinction,
        relFilters: relFilters,
        extFilters: extFilters,
    };
};

/**
 * Načtení seznamu rejstříků dle filtru
 *
 * @param from {number} od kolikáté položky se má posílat seznam - stránkování
 * @param size {number} počet položek v seznamu - velikost jedné stránky
 */
export function registryListFetchIfNeeded(from = 0, size = DEFAULT_REGISTRY_LIST_MAX_SIZE) {
    return SimpleListActions.fetchIfNeeded(AREA_REGISTRY_LIST, true, (parent, filter) => {
        const searchFilter = filter.searchFilter ? createFilter(filter.searchFilter) : null;
        return WebApi.findAccessPoint(
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
            undefined,
            undefined,
            filter.revState,
            searchFilter,
        );
    });
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

export const getArchiveEntityUrl = (id) => {
    return `${URL_ENTITY}/${(id == null ? "" : id)}`
}

export function goToAe(history, id, force = false, redirect = true) {
    return dispatch => {
        const result = dispatch(registryDetailFetchIfNeeded(id, force))
        // Zabraneni zmeny adresy v adresnim radku, pokud
        // je RegistryPage v rezimu modalu ( vyber entity
        // pomoci tlacitka v RegistryField )
        if (redirect) {
            history.push(getArchiveEntityUrl(id));
        }
        return result;
    };
}

export function registryDetailFetchIfNeeded(id, force = false) {
    return (dispatch, getState) => {
        return dispatch(
            DetailActions.fetchIfNeeded(
                AREA_REGISTRY_DETAIL,
                id,
                () => {
                    return WebApi.getAccessPoint(id)
                        .then(data => {
                            return data;
                        }).catch((error) => {
                            if(error.status === 404){
                                return null;
                            }
                            throw error;
                        })
                },
                force,
            ),
        );
    };
}

export function registryDetailInvalidate() {
    return DetailActions.invalidate(AREA_REGISTRY_DETAIL, null);
}

export function registryDetailClear() {
    return DetailActions.reset(AREA_REGISTRY_DETAIL);
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
                dispatch(registryListInvalidate());
            }

            if (callback !== null) {
                dispatch(callback);
            }
        });
    };
}

export function registryDelete(id) {
    return (dispatch, getState) => {
        Api.accesspoints.deleteAccessPoint(id).then(() => {
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

export function registryCreateRevision(id, history, select) {
    return (dispatch, getState) => {
        Api.accesspoints.createRevision(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
            const list = storeFromArea(store, AREA_REGISTRY_LIST);
            if (detail.id == id) {
                dispatch(registryDetailInvalidate());
                dispatch(goToAe(history, id, true, !select));
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate());
            }
        });
    };
}

export function registryDeleteRevision(id, history, select) {
    return (dispatch, getState) => {
        Api.accesspoints.deleteRevision(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
            const list = storeFromArea(store, AREA_REGISTRY_LIST);
            if (detail.id == id) {
                dispatch(registryDetailInvalidate());
                dispatch(goToAe(history, id, true, !select));
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate());
            }
        });
    };
}
 
export function registryChangeStateRevision(id, apVersion, revisionState, history, select) {
    return (dispatch, getState) => {

        return Api.accesspoints.accessPointChangeStateRevision(id, revisionState, apVersion).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
            const list = storeFromArea(store, AREA_REGISTRY_LIST);
            if (detail.id == id) {
                dispatch(registryDetailInvalidate());
                dispatch(goToAe(history, id, true, !select));
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate());
            }
        });
    };
}

/**
 * Načtení konfiguračních vrstev pro openlayers mapu
 */
export function layerConfigurationListFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_REGISTRY_LAYER_LIST, null, () => {
        return WebApi.mapLayerConfiguration().then(json => ({rows: json, count: 0}));
    });
}
