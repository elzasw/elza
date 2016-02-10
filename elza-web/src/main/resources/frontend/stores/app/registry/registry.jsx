/**
 * 
 * Store pro seznamy rejstříků
 *
 **/
import * as types from 'actions/constants/ActionTypes';
import registryData from './registryData';

const initialState = {
    dirty: false,
    isFetching: false,
    fetched: false,
    selectedId: null,
    recordForMove: null,
    isReloadingRegistry: false,
    filterText: null,
    registryData: undefined,
    registryParentId: null,
    registryTypesId: null,
    parents: [],
    typesToRoot: [],
    records: [],
    countRecords: 0,
}

export default function registry(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD:
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
                registryData: undefined,
                partyTypes: [],
                records: [],
                countRecords: 0,
                ...action.registryRegion
            }
        case types.STORE_SAVE:
            {
                const {registryData, isFetching, fetched, selectedId, filterText, registryParentId, registryTypesId, parents, typesToRoot} = state;

                var _info
                if (registryData && registryData.item.recordId === selectedId) {
                    _info = {name: registryData.item.record, desc: registryData.item.characteristics, childs:registryData.item.childs, registerTypeId: registryData.item.registerTypeId}
                } else {
                    _info = null
                }

                return {
                    selectedId,
                    filterText,
                    parents,
                    typesToRoot,
                    registryParentId,
                    registryTypesId,
                    _info
                }
            }
        case types.REGISTRY_SELECT_REGISTRY:
            return Object.assign({}, state, {
                selectedId: action.registry.selectedId,
                registryData: registryData(state.registryData, action)
            })
        case types.REGISTRY_REQUEST_REGISTRY_LIST:
            return Object.assign({}, state, {
                isFetching: true
            })
        case types.REGISTRY_SEARCH_REGISTRY:
            if (action.registry.filterText === '')
                action.registry.filterText = null;
            return Object.assign({}, state, {
                filterText: action.registry.filterText,
                // registryParentId: null, v nové verzy DD se filtruje pod aktuálním rodičem
                fetched: false
            })
            // {...state, filterText: action.registry.filterText, registryParentId: null, fetched: false}

        case types.REGISTRY_CHANGED_PARENT_REGISTRY:
            return Object.assign({}, state, {
                registryParentId: action.registry.registryParentId,
                parents: action.registry.parents,
                typesToRoot: action.registry.typesToRoot,
                filterText: action.registry.filterText,
                fetched: false
            })
        case types.REGISTRY_RECEIVE_REGISTRY_LIST:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                records: action.records,
                countRecords: action.countRecords,
                lastUpdated: action.receivedAt
            })
        case types.REGISTRY_CHANGED_TYPES_ID:
            return Object.assign({}, state, {
                registryTypesId: action.registryTypesId,
                fetched: false
            })
        case types.REGISTRY_REMOVE_REGISTRY:
            return Object.assign({}, state, {
                selectedId: null,
                fetched: false
            })
        case types.REGISTRY_MOVE_REGISTRY_START:
            return Object.assign({}, state, {
                recordForMove: state.registryData.item
            })
        case types.REGISTRY_MOVE_REGISTRY_FINISH:
            return Object.assign({}, state, {
                recordForMove: null,
                dirty: true
            })
        case types.REGISTRY_MOVE_REGISTRY_CANCEL:
            return Object.assign({}, state, {
                recordForMove: null
            })
        case types.REGISTRY_UNSET_PARENT:
            return Object.assign({}, state, {
                registryParentId: null,
                parents: [],
                typesToRoot: [],
                fetched: false
            })
        case types.REGISTRY_CLEAR_SEARCH:
            return Object.assign({}, state, {
                filterText: null,
                fetched: false
            })
        case types.CHANGE_REGISTRY_UPDATE:
            return Object.assign({}, state, {
                dirty: true
            })
        default:
            return state
    }
}
