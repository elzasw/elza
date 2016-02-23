/**
 * 
 * Store pro záznam / detailu rejstříku
 *
 **/

import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    dirty: false,
    isFetching: false,
    fetched: false,
    selectedId: null,
    requireReload: false,
    item: null,
    LastUpdated: null,
    variantRecordInternalId: 1
}

export default function registryRegionData(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD:
            if (!action.registryRegion) {
                return state;
            }

            return {
                ...state,
                fetched: false,
                isFetching: false,
                item: null,
                dirty: false,
                selectedId: action.registryRegion.selectedId,
            }
        case types.STORE_SAVE:
            {
                const {selectedId, item, isFetching} = state;

                var _info
                if (item && !isFetching) {
                    _info = {name: item.record, desc: item.characteristics}
                } else {
                    _info = null
                }

                return {
                    selectedId,
                    _info
                }
            }
        case types.REGISTRY_SELECT_REGISTRY:
            if (state.selectedId === action.registry.selectedId){
                return {
                    ...state,
                    isFetching: false,
                    fetched: false,
                }
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
                selectedId: action.registryId,
                isFetching: true,
            });
        case types.REGISTRY_CHANGE_REGISTRY_DETAIL:
            return Object.assign({}, state, {
                fetched: false
            });
        case types.REGISTRY_RECEIVE_REGISTRY_DETAIL:

            return Object.assign({}, state, {
                selectedId: action.selectedId,
                item: action.item,
                dirty: false,
                isFetching: false,
                fetched: true,
                requireReload: false,
                LastUpdated: action.receivedAt
            });
        case types.CHANGE_REGISTRY_UPDATE:
            return Object.assign({}, state, {
                dirty: true
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
                    record.variantRecords[key]['variantRecordInternalId'] = null,
                    record.variantRecords[key]['variantRecordId'] = action.json.variantRecordId;
                    record.variantRecords[key]['version'] = action.json.version;
                    record.variantRecords[key]['record'] = action.json.record;
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
        case types.CHANGE_REGISTRY_UPDATE:
            return Object.assign({}, state, {
                dirty: true
            });
        default:
            return state;
    }
}
