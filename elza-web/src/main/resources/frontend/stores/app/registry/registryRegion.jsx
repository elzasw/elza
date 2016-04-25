/**
 * 
 * Store pro seznamy rejstříků
 *
 **/
import * as types from 'actions/constants/ActionTypes';
import registryRegionData from './registryRegionData';
import {panel} from './../arr/panel.jsx'
import {consolidateState} from 'components/Utils'
import {valuesEquals} from 'components/Utils.jsx'

const initialState = {
    dirty: false,
    isFetching: false,
    fetched: false,
    selectedId: null,
    recordForMove: null,
    isReloadingRegistry: false,
    filterText: null,
    registryRegionData: registryRegionData(undefined, {type: ''}),
    panel: panel(),
    registryParentId: null,
    registryTypesId: null,
    parents: [],
    typesToRoot: [],
    records: [],
    countRecords: 0
};

export default function registryRegion(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD: {
            if (!action.registryRegion) {
                return state;
            }

            return {
                ...state,
                isFetching: false,
                fetched: false,
                dirty: true,
                recordForMove: null,
                isReloadingRegistry: false,
                ///registryRegionData({selectedId: action.registryRegion.selectedId}, action),
                panel: panel(),
                registryParentId: null,
                registryTypesId: null,
                partyTypes: [],
                parents: [],
                typesToRoot: [],
                records: [],
                countRecords: 0,
                ...action.registryRegion,
                registryRegionData: registryRegionData(action.registryRegion.registryRegionData, action)
            }
        }
        case types.STORE_SAVE: {
            const {selectedId, filterText} = state;

            /*const _info = state.registryRegionData && state.registryRegionData.item && state.registryRegionData.selectedId === selectedId ? {
                name: state.registryRegionData.item.record,
                desc: state.registryRegionData.item.characteristics
            } : null;*/

            return {
                selectedId,
                filterText,
                registryRegionData: registryRegionData(state.registryRegionData, action)
                //_info
            }
        }
        case types.REGISTRY_RECORD_SELECT: {
            return {
                ...state,
                selectedId: action.registry.selectedId,
                registryRegionData: registryRegionData(state.registryRegionData, action)
            }
        }
        case types.REGISTRY_LIST_REQUEST: {
            return {
                ...state,
                isFetching: true
            }
        }
        case types.REGISTRY_RECORD_SEARCH: {
            return {
                    ...state,
                    filterText: action.registry.filterText === '' ? null : action.registry.filterText,
                    fetched: false
            }
        }
        case types.REGISTRY_PARENT_RECORD_CHANGED: {
            return {
                ...state,
                registryParentId: action.registry.registryParentId,
                parents: action.registry.parents,
                registryTypesId: action.registry.registryTypesId,
                typesToRoot: action.registry.typesToRoot,
                filterText: action.registry.filterText,
                fetched: false
            }
        }
        case types.REGISTRY_LIST_RECEIVE: {
            if (!valuesEquals(state.filterText, action.search) || state.registryParentId !== action.registryParentId){
                return state;
            }
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                records: action.records,
                countRecords: action.countRecords,
                lastUpdated: action.receivedAt
            }
        }
        case types.REGISTRY_CHANGED_TYPES_ID: {
            return {
                ...state,
                registryTypesId: action.registryTypesId,
                fetched: false
            }
        }
        case types.REGISTRY_RECORD_REMOVE: {
            return {
                ...state,
                selectedId: null,
                fetched: false
            }
        }
        case types.REGISTRY_MOVE_REGISTRY_START: {
            return {
                ...state,
                recordForMove: state.registryRegionData.item
            }
        }
        case types.REGISTRY_MOVE_REGISTRY_FINISH: {
            return {
                ...state,
                recordForMove: null,
                dirty: true
            }
        }
        case types.REGISTRY_MOVE_REGISTRY_CANCEL: {
            return {
                ...state,
                recordForMove: null
            }
        }
        case types.REGISTRY_PARENT_RECORD_UNSET: {
            return {
                ...state,
                registryParentId: null,
                parents: [],
                typesToRoot: [],
                fetched: false
            }
        }
        case types.REGISTRY_CLEAR_SEARCH: {
            return {
                ...state,
                filterText: null,
                fetched: false
            }
        }
        case types.CHANGE_REGISTRY_UPDATE: {
            return {
                ...state,
                dirty: true
            }
        }
        case types.REGISTRY_SELECT: {
            const result = {
                ...state,
                panel: panel(state.panel, action),
                dirty: true,
                filterText: null,
                selectedId: action.recordId
            };

            return consolidateState(state, result);
        }
        case types.REGISTRY_ARR_RESET: {
            const result = {
                ...state,
                panel: panel(state.panel, action),
                fetched: false,
                filterText: null,
                selectedId: null
            };

            return consolidateState(state, result);
        }
        case types.REGISTRY_RECORD_UPDATED:
        case types.REGISTRY_RECORD_COORDINATES_CREATE:
        case types.REGISTRY_RECORD_COORDINATES_CREATED:
        case types.REGISTRY_RECORD_COORDINATES_CHANGE:
        case types.REGISTRY_RECORD_COORDINATES_DELETED:
        case types.REGISTRY_RECORD_COORDINATES_INTERNAL_DELETED:
        case types.REGISTRY_RECORD_COORDINATES_RECEIVED:
        case types.REGISTRY_VARIANT_RECORD_RECEIVED:
        case types.REGISTRY_VARIANT_RECORD_CREATE:
        case types.REGISTRY_VARIANT_RECORD_CREATED:
        case types.REGISTRY_VARIANT_RECORD_DELETED:
        case types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED:
        case types.REGISTRY_RECORD_NOTE_UPDATED:
        case types.REGISTRY_RECORD_DETAIL_CHANGE:
        case types.REGISTRY_RECORD_DETAIL_RECEIVE:
        case types.REGISTRY_RECORD_DETAIL_REQUEST: {
            return {
                ...state,
                registryRegionData:registryRegionData(state.registryRegionData, action)
            }
        }
        default:
            return state
    }
}
