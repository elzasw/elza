/**
 * 
 * Store pro záznam / detailu rejstříku
 *
 **/

import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components/index.jsx';

const initialState = {
    currentDataKey: null,
    coordinatesInternalId: 1,
    coordinates: [],
    isFetching: false,
    fetched: false,
    selectedId: null,
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
                currentDataKey: null
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
                fetched: false,
                selectedId: action.registry.selectedId,
                currentDataKey: action.registry.selectedId,
                item: {
                    selectedId: action.registry.selectedId
                }
            }
        }
        case types.REGISTRY_RECORD_DETAIL_REQUEST: {
            return {
                ...state,
                currentDataKey: action.dataKey,
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
            if (action.item.id !== state.currentDataKey) {
                return state;
            }
            const newState = {
                ...state,
                selectedId: action.selectedId,
                item: action.item,
                isFetching: false,
                fetched: true,
                lastUpdated: action.receivedAt
            };
            if (state.item) {
                if (state.item.variantRecords) {
                    state.item.variantRecords.map((variant) => {
                        if (!variant.id) {
                            newState.item.variantRecords.push(variant);
                        }
                    });
                }
                if (state.item.coordinates) {
                    state.item.coordinates.map((cord) => {
                        /*
                        Kod pro částečnou změnu přes přenačtení
                        if (cord.id) {
                            const index = indexById(newState.item.coordinates, cord.id);
                            if (index && cord.oldValue) {
                                const newCord = newState.item.coordinates[index];
                                if (cord.oldValue.description === cord.description && cord.oldValue.value !== cord.value && cord.description !== newCord.description) {
                                    newState.item.coordinates[index].value = cord.value;
                                } else if (cord.oldValue.value === cord.value && cord.oldValue.description !== cord.description && cord.value !== newCord.value) {
                                    newState.item.coordinates[index].description = cord.description;
                                }
                            }
                        } else {
                        */
                        if (!cord.id) {
                            newState.item.coordinates.push(cord);
                        }
                    });
                }
            }
            return newState;
        }
        case types.CHANGE_REGISTRY_UPDATE: {
            if (state.currentDataKey != action.changedIds[0]) {
                return state;
            }
            return {
                ...state,
                currentDataKey: null
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
                if (variant.id == action.item.id && action.item.version>variant.version){
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
                id: null,
                regRecordId: record.id,
                version:0,
                record:"",
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
                if (variant.variantRecordInternalId == action.variantRecordInternalId && !variant.id) {
                    record.variantRecords[key] = {
                        ...record.variantRecords[key],
                        ...action.json,
                        variantRecordInternalId: null
                    };
                }
            });
            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_VARIANT_RECORD_DELETED: {
            const indexForDelete = indexById(state.item.variantRecords, action.variantRecordId);
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
                if (variant.id == action.item.id && action.item.version>variant.version){
                    record.coordinates[key] = action.item;
                }
            });
            return {
                ...state,
                item: record
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CHANGE: {
            const index = action.item.id ?
                indexById(state.item.coordinates, action.item.id) :
                indexById(state.item.coordinates, action.item.coordinatesInternalId, 'coordinatesInternalId');
            if (index === null) {
                return state;
            }
            const oldCord = state.item.coordinates[index];
            return {
                ...state,
                item: {
                    ...state.item,
                    coordinates: [
                        ...state.item.coordinates.slice(0,index),
                        {
                            ...validateCoordinate(action.item),
                            oldValue: action.item.oldValue ? action.item.oldValue : {
                                description: oldCord.description,
                                value: oldCord.value
                            }
                        },
                        ...state.item.coordinates.slice(index+1)
                    ]
                }
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CREATE: {
            const record = {...state.item};
            record.coordinates.push({
                id: null,
                description: null,
                value: null,
                error: {},
                hasError: false,
                regRecordId: record.id,
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
                if (variant.coordinatesInternalId == action.coordinatesInternalId && !variant.id) {
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
            const indexForDelete = indexById(state.item.coordinates, action.coordinatesId);
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
