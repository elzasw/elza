import DetailReducer from "shared/detail/DetailReducer";
import {RESPONSE} from 'shared/detail/DetailActions'

import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components/shared';

const intialState = {
    variantRecordInternalId: 0,
    coordinatesInternalId: 0,
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

/**
 * Přetížený detail reducer
 */
export default function reducer(state = undefined, action = {}, config = undefined) {
    switch (action.type) {
        case types.REGISTRY_VARIANT_RECORD_RECEIVED: {
            const data = {...state.data};
            data.variantRecords.map((variant, key) => {
                if (variant.id == action.item.id && action.item.version>variant.version){
                    data.variantRecords[key] = action.item;
                }
            });
            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_VARIANT_RECORD_CREATE: {
            const data = {...state.data};
            data.variantRecords.push({
                id: null,
                apRecordId: data.id,
                version: 0,
                record:"",
                variantRecordInternalId:state.variantRecordInternalId
            });
            return {
                ...state,
                data,
                variantRecordInternalId: state.variantRecordInternalId+1
            }
        }
        case types.REGISTRY_VARIANT_RECORD_CREATED: {
            const data = {...state.data};

            data.variantRecords.map((variant, key) => {
                if (variant.variantRecordInternalId == action.variantRecordInternalId && !variant.id) {
                    data.variantRecords[key] = {
                        ...data.variantRecords[key],
                        ...action.json,
                        variantRecordInternalId: null
                    };
                }
            });
            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_VARIANT_RECORD_DELETED: {
            const indexForDelete = indexById(state.data.variantRecords, action.variantRecordId);
            if (indexForDelete === null) {
                return state;
            }
            const data = {...state.data};

            data.variantRecords.splice(indexForDelete, 1);

            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED: {
            const indexForDelete = indexById(state.data.variantRecords, action.variantRecordInternalId, 'variantRecordInternalId');
            if (indexForDelete === null) {
                return state;
            }
            const data = {...state.data};

            data.variantRecords.splice(indexForDelete, 1);

            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_RECEIVED: {
            const data = {...state.data};
            data.coordinates.map((variant, key) => {
                if (variant.id == action.item.id && action.item.version > variant.version){
                    data.coordinates[key] = action.item;
                }
            });
            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CHANGE: {
            const index = action.item.id ?
                indexById(state.data.coordinates, action.item.id) :
                indexById(state.data.coordinates, action.item.coordinatesInternalId, 'coordinatesInternalId');
            if (index === null) {
                return state;
            }
            const oldCord = state.data.coordinates[index];
            return {
                ...state,
                data: {
                    ...state.data,
                    coordinates: [
                        ...state.data.coordinates.slice(0,index),
                        {
                            ...validateCoordinate(action.item),
                            oldValue: action.item.oldValue ? action.item.oldValue : {
                                    description: oldCord.description,
                                    value: oldCord.value
                                }
                        },
                        ...state.data.coordinates.slice(index+1)
                    ]
                }
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CREATE: {
            const data = {...state.data};
            data.coordinates.push({
                id: null,
                description: null,
                value: null,
                error: {},
                hasError: false,
                apRecordId: data.id,
                coordinatesInternalId:state.coordinatesInternalId
            });
            return {
                ...state,
                data,
                coordinatesInternalId: state.coordinatesInternalId+1
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_CREATED: {
            const data = {...state.data};

            data.coordinates.map((variant, key) => {
                if (variant.coordinatesInternalId == action.coordinatesInternalId && !variant.id) {
                    data.coordinates[key] = {
                        ...data.coordinates[key],
                        ...action.json,
                        coordinatesInternalId: null
                    };
                }
            });
            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_DELETED: {
            const indexForDelete = indexById(state.data.coordinates, action.coordinatesId);
            if (indexForDelete === null) {
                console.warn("Index for deleting coordinates not found. Action:REGISTRY_RECORD_COORDINATES_DELETED");
                return state;
            }
            const data = {...state.data};

            data.coordinates.splice(indexForDelete, 1);

            return {
                ...state,
                data
            }
        }
        case types.REGISTRY_RECORD_COORDINATES_INTERNAL_DELETED: {
            const indexForDelete = indexById(state.data.coordinates, action.coordinatesInternalId, 'coordinatesInternalId');
            if (indexForDelete === null) {
                console.warn("Index for deleting coordinates not found. Action:REGISTRY_RECORD_COORDINATES_INTERNAL_DELETED");
                return state;
            }
            const data = {...state.data};

            data.coordinates.splice(indexForDelete, 1);

            return {
                ...state,
                data
            }
        }

        case RESPONSE: {
            const newState = DetailReducer(state, action, config ? {...config, reducer} : {reducer});
            if (state.data) {
                if (state.data.variantRecords) {
                    state.data.variantRecords.map((variant) => {
                        if (!variant.id) {
                            newState.data.variantRecords.push(variant);
                        }
                    });
                }
                if (state.data.coordinates) {
                    state.data.coordinates.map((cord) => {
                        if (!cord.id) {
                            newState.data.coordinates.push(cord);
                        }
                    });
                }
            }
            return newState;
        }
        default:
            if (state === undefined) {
                return {
                    ...intialState,
                    ...DetailReducer(state, action, config ? {...config, reducer} : {reducer})
                };
            }
            return DetailReducer(state, action, config ? {...config, reducer} : {reducer});
    }
}
