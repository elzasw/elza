/**
 * 
 * Store pro záznam / detailu rejstříku
 *
 **/

import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components'

const initialState = {
    coordinatesInternalId: 1,
    coordinates: [],
    dirty: false,
    isFetching: false,
    fetched: false,
    selectedId: null,
    requireReload: false,
    item: null,
    LastUpdated: null,
    variantRecordInternalId: 1,
    variantRecords: []
};


function validateCoordinate(coordinate) {
    const newCord = {...coordinate, error: {value:null}, hasError: false};
    if (newCord.value) {
        if (newCord.value.indexOf("POINT") === 0) {
            let left = newCord.value.indexOf('(') + 1;
            let right = newCord.value.indexOf(')');
            if ((right - left) == 0) {
                newCord.error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            let data = newCord.value.substr(left, newCord.value.indexOf(')') - left).split(' ');
            if (newCord.value === '' || newCord.value === ' ' || data.length != 2 || data[0] == null || data[0] == '' || data[1] == null || data[1] == '') {
                newCord.error.value = i18n("subNodeForm.errorPointCoordinates");
            } else {
                newCord.error.value = null;
            }
        }
    } else {
        newCord.error.value = i18n('subNodeForm.validate.value.notEmpty');
    }
    if (newCord.error.value) {
        newCord.hasError = true;
    }
    return newCord;
}

export default function registryRegionData(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD: {
            return {
                ...initialState,
                ...state,
                fetched: false,
                isFetching: false,
                item: null,
                dirty: false
            }
        }
        case types.STORE_SAVE: {
            const {selectedId, item, isFetching} = state;

            const _info = item && !isFetching ? {name: item.record, desc: item.characteristics} : null;

            return {
                selectedId,
                _info
            }
        }
        case types.REGISTRY_RECORD_SELECT: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                selectedId: action.registry.selectedId,
                item: action.registry
            }
        }
        case types.REGISTRY_RECORD_DETAIL_REQUEST: {
            return {
                ...state,
                selectedId: action.registryId,
                isFetching: true
            }
        }
        case types.REGISTRY_RECORD_DETAIL_CHANGE: {
            return {
                ...state,
                fetched: false
            }
        }
        case types.REGISTRY_RECORD_DETAIL_RECEIVE: {
            return {
                ...state,
                selectedId: action.selectedId,
                item: action.item,
                dirty: false,
                isFetching: false,
                fetched: true,
                requireReload: false,
                LastUpdated: action.receivedAt
            }
        }
        case types.CHANGE_REGISTRY_UPDATE: {
            return {
                ...state,
                dirty: true
            }
        }
        case types.REGISTRY_RECORD_UPDATED: {
            return {
                ...state,
                item: action.json
            }
        }
        case types.REGISTRY_VARIANT_RECORD_RECEIVED: {
            const record = {...state.item};
            record.variantRecords.map((variant, key) => {
                if (variant.variantRecordId == action.item.variantRecordId && action.item.version>variant.version){
                    record.variantRecords[key] = action.item;
                }
            });
            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_VARIANT_RECORD_CREATE: {
            const record = {...state.item};
            record.variantRecords.push({
                variantRecordId: null,
                regRecordId: record.recordId,
                version:0,
                record:null,
                variantRecordInternalId:state.variantRecordInternalId
            });
            return {
                ...state,
                item: record,
                variantRecordInternalId: state.variantRecordInternalId+1
            }
        }
        case types.REGISTRY_VARIANT_RECORD_CREATED: {
            const record = {...state.item};
            
            record.variantRecords.map((variant, key) => {
                if (variant.variantRecordInternalId == action.variantRecordInternalId && !variant.variantRecordId) {
                    record.variantRecords[key] = {
                        ...record.variantRecords[key],
                        ...action.json,
                        variantRecordInternalId: null,
                    };
                }
            });
            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_VARIANT_RECORD_DELETED: {
            const indexForDelete = indexById(state.item.variantRecords, action.variantRecordId, 'variantRecordId');
            if (indexForDelete === null) {
                return state;
            }
            const record = {...state.item};

            record.variantRecords.splice(indexForDelete, 1);

            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED: {
            const indexForDelete = indexById(state.item.variantRecords, action.variantRecordInternalId, 'variantRecordInternalId');
            if (indexForDelete === null) {
                return state;
            }
            const record = {...state.item};

            record.variantRecords.splice(indexForDelete, 1);

            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_RECEIVED: {
            const record = {...state.item};
            record.coordinates.map((variant, key) => {
                if (variant.coordinatesId == action.item.coordinatesId && action.item.version>variant.version){
                    record.coordinates[key] = action.item;
                }
            });
            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CHANGE: {
            const index = action.item.coordinatesId ?
                indexById(state.item.coordinates, action.item.coordinatesId, 'coordinatesId') :
                indexById(state.item.coordinates, action.item.coordinatesInternalId, 'coordinatesInternalId');
            if (index === null) {
                return state;
            }
            return {
                ...state,
                item: {
                    ...state.item,
                    coordinates: [
                        ...state.item.coordinates.slice(0,index),
                        validateCoordinate(action.item),
                        ...state.item.coordinates.slice(index+1)
                    ]
                }
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CREATE: {
            const record = {...state.item};
            console.log(record);
            record.coordinates.push({
                coordinatesId: null,
                description: null,
                value: null,
                error: {},
                hasError: false,
                regRecordId: record.recordId,
                coordinatesInternalId:state.coordinatesInternalId
            });
            return {
                ...state,
                item: record,
                coordinatesInternalId: state.coordinatesInternalId+1
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CREATED: {
            const record = {...state.item};
            
            record.coordinates.map((variant, key) => {
                if (variant.coordinatesInternalId == action.coordinatesInternalId && !variant.coordinatesId) {
                    record.coordinates[key] = {
                        ...record.coordinates[key],
                        ...action.json,
                        coordinatesInternalId: null
                    };
                }
            });
            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_DELETED: {
            const indexForDelete = indexById(state.item.coordinates, action.coordinatesId, 'coordinatesId');
            if (indexForDelete === null) {
                return state;
            }
            const record = {...state.item};

            record.coordinates.splice(indexForDelete, 1);

            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_INTERNAL_DELETED: {
            const indexForDelete = indexById(state.item.coordinates, action.coordinatesInternalId, 'coordinatesInternalId');
            if (indexForDelete === null) {
                return state;
            }
            const record = {...state.item};

            record.coordinates.splice(indexForDelete, 1);

            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_RECORD_NOTE_UPDATED: {
            return {
                ...state,
                item: action.json
            }
        }
        default:
            return state;
    }
}
