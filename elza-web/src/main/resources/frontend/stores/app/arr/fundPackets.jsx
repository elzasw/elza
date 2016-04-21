import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'
import {consolidateState} from 'components/Utils'

const initialState = {
    filterText: '',
    filterState: 'OPEN',
    selectedIds: [],
    packets: null,
    isFetching: false,
    fetched: false,
    currentDataKey: '',
}

export default function fundPackets(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                selectedIds: [],
                packets: null,
                isFetching: false,
                fetched: false,
                currentDataKey: '',
            }
            break
        case types.STORE_SAVE:
            const {filterText, filterState} = state;
            return {
                filterText,
                filterState,
            }
            break
        case types.CHANGE_PACKETS:
            return {
                ...state,
                currentDataKey: '',
            }
        case types.FUND_PACKETS_REQUEST:
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey
            }

        case types.FUND_PACKETS_RECEIVE:
            return {
                ...state,
                isFetching: false,
                fetched: true,
                packets: action.packets,
            }            
        case types.FUND_PACKETS_CHANGE_SELECTION:
            return {
                ...state,
                selectedIds: action.selectedIds,
            }
        case types.FUND_PACKETS_FILTER:
            switch (action.filterType) {
                case "TEXT":
                    return {
                        ...state,
                        filterText: action.filterText,
                        currentDataKey: '',
                    }            
                case "STATE":
                    return {
                        ...state,
                        filterState: action.filterState,
                        selectedIds: [],
                        currentDataKey: '',
                    }            
            }
        default:
            return state
    }
}
