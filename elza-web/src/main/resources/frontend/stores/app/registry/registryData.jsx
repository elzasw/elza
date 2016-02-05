/**
 * 
 * Store pro záznam / detailu rejstříku
 * 
 **/ 

import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    isFetching: false,
    fetched: false,
    selectedId: null,
    requireReload: false,
    item: null,
    LastUpdated: null,
    variantRecordInternalId: 1
}

export default function registryData(state = initialState, action = {}) {
    switch (action.type) {
        case types.REGISTRY_SELECT_REGISTRY:
            if (state.selectedId === action.registry.selectedId){
                return state;
            }
            else{
                return Object.assign({}, state, {
                    isFetching: false,
                    fetched: false,
                    item: action.registry
                });
            }
        case types.REGISTRY_REQUEST_REGISTRY_DETAIL:
            return Object.assign({}, state, {
                isFetching: true,
                fetched: false
            });
        case types.REGISTRY_CHANGE_REGISTRY_DETAIL:
            return Object.assign({}, state, {
                fetched: false
            });
        case types.REGISTRY_RECEIVE_REGISTRY_DETAIL:

            return Object.assign({}, state, {
                selectedId: action.selectedId,
                item: action.item,
                isFetching: false,
                fetched: true,
                requireReload: false,
                LastUpdated: action.receivedAt
            });
        case types.REGISTRY_RECORD_UPDATED:
            return Object.assign({}, state, {
                item: action.json
            });
        case types.REGISTRY_VARIANT_RECORD_RECIVED:
            var record = Object.assign({}, state.item);
            record.variantRecords.map((variant, key) => {
                if (variant.variantRecordId == action.item.variantRecordId && action.item.version>variant.version){
                    record.variantRecords[key] = action.item;
                }
            });
            return Object.assign({}, state, {
                item: record
            });
        case types.REGISTRY_VARIANT_RECORD_ADD_NEW_CLEAN:
            var record = Object.assign({}, state.item);
            record.variantRecords.push({
                variantRecordId: null,
                regRecordId: state.item.regRecordId,
                version:0,
                record:null,
                variantRecordInternalId:state.variantRecordInternalId
            });
            return Object.assign({}, state, {
                item: record,
                variantRecordInternalId: state.variantRecordInternalId+1
            });
        case types.REGISTRY_VARIANT_RECORD_INSERTED:
            var record = Object.assign({}, state.item);
            record.variantRecords.map((variant, key) => {
                if (variant.variantRecordInternalId == action.variantRecordInternalId && !variant.variantRecordId ){
                    record.variantRecords[key]['variantRecordId'] = action.json.variantRecordId;
                    record.variantRecords[key]['version'] = action.json.version;
                }
            });
            return Object.assign({}, state, {
                item: record
            });
        case types.REGISTRY_VARIANT_RECORD_DELETED:
            var record = Object.assign({}, state.item);
            var indexForDelete = indexById(record.variantRecords, action.variantRecordId, 'variantRecordId');;

            record.variantRecords.splice(indexForDelete, 1);

            return Object.assign({}, state, {
                item: record
            });
        case types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED:
            var record = Object.assign({}, state.item);
            var indexForDelete = indexById(record.variantRecords, action.variantRecordInternalId, 'variantRecordInternalId');

            record.variantRecords.splice(indexForDelete, 1);

            return Object.assign({}, state, {
                item: record
            });
        case types.REGISTRY_RECORD_NOTE_UPDATED:
            return Object.assign({}, state, {
                item: action.json
            });
        default:
            return state;
    }
}
