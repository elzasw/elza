import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n, AddPartyForm} from 'components/index.jsx';
import {getPartyTypeById} from 'actions/refTables/partyTypes.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {objectById} from 'stores/app/utils.jsx'

import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'

export const AREA_PARTY_LIST = 'partyList';
export const AREA_PARTY_DETAIL = 'partyDetail';

export const PARTY_TYPE_CODES = {
    GROUP_PARTY: 'GROUP_PARTY',
    PERSON: 'PERSON',
    DYNASTY: 'DYNASTY',
    EVENT: 'EVENT',
};

/**
 * Načtení seznamu osob dle filtru
 *
 * @param filter {Object} - objekt filtru
 * @param versionId int - versionId
 */
export function partyListFetchIfNeeded(filter, versionId = null) {
    return SimpleListActions.fetchIfNeeded(AREA_PARTY_LIST, versionId, () => WebApi.findParty(filter.text, versionId, filter.type))
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
    return DetailActions.fetchIfNeeded(AREA_PARTY_DETAIL, id, () => WebApi.getParty(id));
}

export function partyDetailInvalidate() {
    return DetailActions.invalidate(AREA_PARTY_DETAIL, null)
}

export function partyDetail() {
    return DetailActions.invalidate(AREA_PARTY_DETAIL, null)
}

export function partyDetailClear() {
    return partyDetailFetchIfNeeded(null);
}

export function partyUpdate(party) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.updateParty(party)).then(() => {
            dispatch(partyDetailInvalidate());
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
    return dispatch => {
        WebApi.deleteParty(partyId).then(() => {
            dispatch(partyDetailClear());
            dispatch(partyListInvalidate());
        })
    }
}


export function relationCreate(relation) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.createRelation(relation))
            .then(() => {
                dispatch(modalDialogHide());
                dispatch(partyDetailInvalidate());
            });
    };
}

export function relationDelete(relationId) {
    return dispatch => {
        WebApi.deleteRelation(relationId).then(() => {
            dispatch(partyDetailInvalidate());
        });
    }
}

export function relationUpdate(relation) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.updateRelation(relation))
            .then(() => {
                dispatch(modalDialogHide());
                dispatch(partyDetailInvalidate());
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

        dispatch(modalDialogShow(this, label, <AddPartyForm partyType={partyType} showSubmitTypes={showSubmitTypes} versionId={versionId} onSubmitForm={partyAddSubmit.bind(null, callback, dispatch)} />));
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
    const party = {
        '@type': PARTY_CLASS_BY_TYPE[data.partyType.code],
        ...other,
        record: {
            '@class': "cz.tacr.elza.controller.vo.RegRecordVO",
            ...other.record
        },
        partyNames : [
            {
                ...prefferedName,
                prefferedName: true,
            }
        ]
    };

    savingApiWrapper(dispatch, WebApi.createParty(party)).then((json) => {
        dispatch(modalDialogHide());
        callback && callback(json, submitType);
    });
}
