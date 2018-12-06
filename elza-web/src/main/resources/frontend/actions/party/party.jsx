import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n} from 'components/shared';
import {getPartyTypeById} from 'actions/refTables/partyTypes.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {storeFromArea, indexById, objectById} from 'shared/utils'


import {DEFAULT_LIST_SIZE, MODAL_DIALOG_VARIANT} from '../../constants.tsx'

import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'
import AddPartyForm from "../../components/party/AddPartyForm";

import {PARTY_TYPE_CODES} from '../../constants.tsx'

export const AREA_PARTY_LIST = 'partyList';
export const AREA_PARTY_DETAIL = 'partyDetail';


export const DEFAULT_PARTY_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;

export const RELATION_CLASS_TYPE_REPEATABILITY = {
    UNIQUE: "UNIQUE",
    MULTIPLE: "MULTIPLE",
};

export const USE_UNITDATE_ENUM = {
    NONE: 'NONE',
    ONE: 'ONE',
    INTERVAL: 'INTERVAL',
};


/**
 * Načtení seznamu osob dle filtru
 *
 * @param versionId int - versionId
 * @param from {number} od kolikáté položky se má posílat seznam - stránkování
 * @param size {number} počet položek v seznamu - velikost jedné stránky
 */
export function partyListFetchIfNeeded(versionId = null, from = 0, size = DEFAULT_PARTY_LIST_MAX_SIZE, scopeId = null) {
    return SimpleListActions.fetchIfNeeded(AREA_PARTY_LIST, versionId, (parent, filter) => WebApi.findParty(filter.text, versionId, filter.type, filter.itemSpecId, filter.from, size, filter.scopeId, filter.excludeInvalid))
}

/**
 * Filtr osob
 *
 * @param filter {Object} - objekt filtru
 */
export function partyListFilter(filter) {
    return SimpleListActions.filter(AREA_PARTY_LIST, filter);
}

/**
 * Invalidace seznamu osob
 */
export function partyListInvalidate() {
    return SimpleListActions.invalidate(AREA_PARTY_LIST, null);
}

export function partyDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        return dispatch(DetailActions.fetchIfNeeded(AREA_PARTY_DETAIL, id, () => {
            return WebApi.getParty(id).catch(()=>dispatch(partyDetailClear()));
        }));
    }
}

export function partyDetailInvalidate() {
    return DetailActions.invalidate(AREA_PARTY_DETAIL, null)
}

export function partyDetailClear() {
    return partyDetailFetchIfNeeded(null);
}

export function partyUpdate(obj) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateParty(obj)).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_PARTY_DETAIL);
            const list = storeFromArea(store, AREA_PARTY_LIST);
            if (detail.id == obj.id) {
                dispatch(partyDetailInvalidate());
            }

            if (list.filteredRows && indexById(list.filteredRows, obj.id) !== null) {
                dispatch(partyListInvalidate())
            }
        });
    }
}


export function partyCreate(party) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.createParty(party))
            .then(newParty => {
                dispatch(modalDialogHide());
                dispatch(partyDetailFetchIfNeeded(newParty.id));
            });
    }
}

export function partyDelete(id) {
    return (dispatch, getState) => {
        WebApi.deleteParty(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_PARTY_DETAIL);
            const list = storeFromArea(store, AREA_PARTY_LIST);
            if (detail.id == id) {
                dispatch(partyDetailClear());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(partyListInvalidate())
            }
        })
    }
}
/* MCV-45365
export function setValidParty(id) {
    return (dispatch, getState) => {
        WebApi.setValidParty(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_PARTY_DETAIL);
            const list = storeFromArea(store, AREA_PARTY_LIST);
            if (detail.id == id) {
                dispatch(partyDetailClear());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(partyListInvalidate())
            }
        })
    }
}
*/

export function relationCreate(relation) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.createRelation(relation))
            .then(() => {
                dispatch(partyDetailInvalidate());
                const {app:{partyList}} = getState();
                if (partyList.filteredRows && indexById(partyList.filteredRows, relation.partyId) !== null) {
                    dispatch(partyListInvalidate())
                }
            })
            .catch(error => {
                dispatch(partyDetailClear());
            });
    };
}

export function relationDelete(relationId) {
    return (dispatch, getState) => {
        WebApi.deleteRelation(relationId).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_PARTY_DETAIL);
            const list = storeFromArea(store, AREA_PARTY_LIST);
            dispatch(partyDetailInvalidate());

            if (list.filteredRows && indexById(list.filteredRows, detail.id) !== null) {
                dispatch(partyListInvalidate())
            }
        });
    }
}

export function relationUpdate(relation) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateRelation(relation))
            .then(() => {
                dispatch(partyDetailInvalidate());
                const {app:{partyList}} = getState();
                if (partyList.filteredRows && indexById(partyList.filteredRows, relation.partyId) !== null) {
                    dispatch(partyListInvalidate())
                }
            });
    };
}


export function partyAdd(partyTypeId, versionId, callback, showSubmitTypes = false) {
    return (dispatch, getState) => {
        const {refTables: {partyTypes}} = getState();
        const partyType = objectById(partyTypes.items, partyTypeId);

        let label;
        if (Object.keys(PARTY_TYPE_CODES).indexOf(partyType.code) !== -1) {
            label = i18n('party.create.title.' + partyType.code);
        } else {
            label = i18n('party.addParty');
        }

        dispatch(modalDialogShow(
            this,
            label,
            <AddPartyForm
                partyType={partyType}
                showSubmitTypes={showSubmitTypes}
                versionId={versionId}
                onSubmitForm={partyAddSubmit.bind(null, callback, dispatch)} />
        ));
    }
}

export const PARTY_CLASS_BY_TYPE = {
    [PARTY_TYPE_CODES.PERSON]: '.ParPersonVO',
    [PARTY_TYPE_CODES.DYNASTY]: '.ParDynastyVO',
    [PARTY_TYPE_CODES.GROUP_PARTY]: '.ParPartyGroupVO',
    [PARTY_TYPE_CODES.EVENT]: '.ParEventVO',
};

function partyAddSubmit(callback, dispatch, submitType, data) {
    const {prefferedName, ...other} = data;
    const newName = normalizeNameObject(prefferedName);
    const party = {
        '@class': PARTY_CLASS_BY_TYPE[data.partyType.code],
        ...other,
        accessPoint: {
            '@class': "cz.tacr.elza.controller.vo.ApAccessPointVO",
            ...other.accessPoint
        },
        partyNames : [
            {
                ...newName,
                prefferedName: true,
            }
        ]
    };
    const promise = savingApiWrapper(dispatch, WebApi.createParty(party));
    promise.then((json) => {
        callback && callback(json, submitType);
    });
    return promise;
}


const removeUndefined = (obj) => {
    for (let key in obj ) {
        if (obj.hasOwnProperty(key)) {
            if (obj[key] === undefined || obj[key] === null) {
                delete obj[key];
            }
        }
    }
    return obj;
};

export const normalizeNameObject = (obj) => {
    if (!obj) {
        return null;
    }

    obj.validFrom = normalizeDatation(obj.validFrom);
    obj.validTo = normalizeDatation(obj.validTo);


    ['mainPart', 'otherPart', 'degreeBefore', 'degreeAfter'].each(i => {
        if (obj[i]) {
            obj[i] = obj[i].trim();
            if (obj[i].length == 0) {
                obj[i] = null;
            }
        }
    });

    return obj;
};

export const normalizeDatation = (obj) => {
    if (!obj) {
        return null;
    }

    if (obj.value != null && obj.value.trim().length === 0) {
        obj.value = null;
    }
    if ((obj.value !== null && obj.value !== undefined) || (obj.textDate !== null && obj.textDate !== undefined) || (obj.note !== null && obj.note !== undefined)) {
        return obj
    }
    return null;
};
