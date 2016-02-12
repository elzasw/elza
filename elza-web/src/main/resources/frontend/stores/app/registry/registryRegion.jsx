/**
 * 
 * Store pro seznamy rejstříků
 *
 **/
import * as types from 'actions/constants/ActionTypes';
import registryRegionData from './registryRegionData';
import {panel} from './../arr/panel.jsx'
import {consolidateState} from 'components/Utils'

const initialState = {
    dirty: false,
    isFetching: false,
    fetched: false,
    selectedId: null,
    recordForMove: null,
    isReloadingRegistry: false,
    filterText: null,
    registryRegionData: undefined,
    panel: panel(),
    registryParentId: null,
    registryTypesId: null,
    parents: [],
    typesToRoot: [],
    records: [],
    countRecords: 0,
}

export default function registryRegion(state = initialState, action = {}) {
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
                registryRegionData: undefined,
                partyTypes: [],
                records: [],
                countRecords: 0,
                ...action.registryRegion
            }
        case types.STORE_SAVE:
            {
                const {registryRegionData, isFetching, fetched, selectedId, filterText, registryParentId, registryTypesId, parents, typesToRoot} = state;

                var _info
                if (registryRegionData && registryRegionData.item.recordId === selectedId) {
                    _info = {name: registryRegionData.item.record, desc: registryRegionData.item.characteristics, childs:registryRegionData.item.childs, registerTypeId: registryRegionData.item.registerTypeId}
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
                registryRegionData: registryRegionData(state.registryRegionData, action)
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
                registryTypesId: action.registry.registryTypesId,
                typesToRoot: action.registry.typesToRoot,
                filterText: action.registry.filterText,
                fetched: false
            })
        case types.REGISTRY_RECEIVE_REGISTRY_LIST:
            if (state.filterText !== action.search || state.registryParentId !== action.registryParentId){
                return state;
            }
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
                recordForMove: state.registryRegionData.item
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

        case types.REGISTRY_SELECT:
            var result = {...state};
            result.panel = panel(result.panel, action);
            result.dirty = true;
            result.filterText = null;
            result.selectedId = action.recordId;
            return consolidateState(state, result);

        case types.REGISTRY_ARR_RESET:
            var result = {...state};
            result.panel = panel(result.panel, action);
            result.fetched = false;
            result.filterText = null;
            result.selectedId = null;
            return consolidateState(state, result);

        default:
            return state
    }
}
