import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n, AddPartyForm} from 'components/index.jsx';
import {getPartyTypeById} from 'actions/refTables/partyTypes.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {objectById, indexById} from 'stores/app/utils.jsx'
import {DEFAULT_LIST_SIZE} from 'constants'

import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'

export const AREA_PARTY_LIST = 'partyList';
export const AREA_PARTY_DETAIL = 'partyDetail';


export const DEFAULT_PARTY_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;

export const PARTY_TYPE_CODES = {
    GROUP_PARTY: 'GROUP_PARTY',
    PERSON: 'PERSON',
    DYNASTY: 'DYNASTY',
    EVENT: 'EVENT',
};

export const RELATION_CLASS_TYPE_REPEATABILITY = {
    UNIQUE: "UNIQUE",
    MULTIPLE: "MULTIPLE",
};

export const USE_UNITDATE_ENUM = {
    NONE: 'NONE',
    ONE: 'ONE',
    INTERVAL: 'INTERVAL',
};

export const RELATION_CLASS_CODES = {
    RELATION: "R",
    BIRTH: "B",
    EXTINCTION: "E"
};

/**
 * Načtení seznamu osob dle filtru
 *
 * @param filter {Object} - objekt filtru
 * @param versionId int - versionId
 * @param from {number} od kolikáté položky se má posílat seznam - stránkování
 * @param size {number} počet položek v seznamu - velikost jedné stránky
 */
export function partyListFetchIfNeeded(filter, versionId = null, from = 0, size = DEFAULT_PARTY_LIST_MAX_SIZE) {
    return SimpleListActions.fetchIfNeeded(AREA_PARTY_LIST, versionId, () => WebApi.findParty(filter.text, versionId, filter.type, from, size))
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
        dispatch(DetailActions.fetchIfNeeded(AREA_PARTY_DETAIL, id, () => {
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

export function partyUpdate(party) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateParty(party)).then(() => {
            dispatch(partyDetailInvalidate());
            const {app:{partyList}} = getState();
            if (partyList.filteredRows && indexById(partyList.filteredRows, party.id) !== null) {
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

export function partyDelete(partyId) {
    return (dispatch, getState) => {
        WebApi.deleteParty(partyId).then(() => {
            dispatch(partyDetailClear());
            const {app:{partyList}} = getState();
            if (partyList.filteredRows && indexById(partyList.filteredRows, partyId) !== null) {
                dispatch(partyListInvalidate())
            }
        })
    }
}


export function relationCreate(relation) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.createRelation(relation))
            .then(() => {
                dispatch(modalDialogHide());
                dispatch(partyDetailInvalidate());
                const {app:{partyList}} = getState();
                if (partyList.filteredRows && indexById(partyList.filteredRows, relation.partyId) !== null) {
                    dispatch(partyListInvalidate())
                }
            })
            .catch(error => {
                dispatch(clearPartyDetail());
            });
    };
}

export function relationDelete(relationId) {
    return (dispatch, getState) => {
        WebApi.deleteRelation(relationId).then(() => {
            dispatch(partyDetailInvalidate());
            const {app:{partyList, partyDetail}} = getState();
            if (partyList.filteredRows && indexById(partyList.filteredRows, partyDetail.id) !== null) {
                dispatch(partyListInvalidate())
            }
        });
    }
}

export function relationUpdate(relation) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateRelation(relation))
            .then(() => {
                dispatch(modalDialogHide());
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

        dispatch(modalDialogShow(this, label, <AddPartyForm partyType={partyType} showSubmitTypes={showSubmitTypes} versionId={versionId} onSubmitForm={partyAddSubmit.bind(null, callback, dispatch)} />, 'dialog-lg'));
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
        record: {
            '@class': "cz.tacr.elza.controller.vo.RegRecordVO",
            ...other.record
        },
        partyNames : [
            {
                ...newName,
                prefferedName: true,
            }
        ]
    };

    savingApiWrapper(dispatch, WebApi.createParty(party)).then((json) => {
        dispatch(modalDialogHide());
        callback && callback(json, submitType);
    });
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

console.log(normalizeDatation({calendarTypeId:1, value:null}));

