/**
 * Akce pro seznam typu záznamů - recordTypes.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refRecordTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.recordTypes.fetched || state.refTables.recordTypes.dirty) && !state.refTables.recordTypes.isFetching) {
            return dispatch(refRecordTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refRecordTypesFetch() {
    return dispatch => {
        dispatch(refRecordTypesRequest())
        return WebApi.getRecordTypes()
            .then(json => {dispatch(refRecordTypesReceive(json))});
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refRecordTypesReceive(json) {
    return {
        type: types.REF_RECORD_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refRecordTypesRequest() {
    return {
        type: types.REF_RECORD_TYPES_REQUEST
    }
}

// funkce, ktera rekurzivne převede strom strop typu zaznamovych hesel na seznam těch, ktere odpovidaji zadanému typu osoby
function getRecordTypes(types, partyTypeId){
    var options = [];
    if(types != null && types.length>0){
        for(var i=0; i<types.length; i++){
            if(types[i].partyTypeId == partyTypeId){
                console.log(types[i].partyTypeId);
               options[options.length] = types[i]; 
            };

            var childrens = this.getRecordTypes(types[i].children, partyTypeId);
            for(var j=0; j<childrens.length; j++){
                options[options.length] = childrens[j]; 
            }
        }
    }
    return options;
}
