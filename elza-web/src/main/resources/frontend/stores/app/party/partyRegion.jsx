import * as types from 'actions/constants/ActionTypes';

import {panel} from './../arr/panel.jsx'
import {consolidateState} from 'components/Utils'

const initialState = {
    dirty: false,
    dirtySearch: false,
    isFetchingSearch: false,
    fetchedSearch: false,
    isFetchingDetail: false,
    fetchedDetail: false,
    selectedId: null,
    filterText: "",
    items: [],
    panel: panel(),
    selectedPartyID : null,
    selectedPartyData: null,
    partyTypes: [],
    gregorianCalendarId: 1,         // id gregoriánského kalendáře - TODO: potřeba ho dovypočíst
}

export default function partyRegion(state = initialState, action) {
    switch (action.type) {
        case types.STORE_LOAD:
            if (!action.partyRegion) {
                return state;
            }

            return {
                ...state,
                isFetchingSearch: false,
                fetchedSearch: false,
                isFetchingDetail: false,
                fetchedDetail: false,
                items: [],
                selectedPartyData: null,
                partyTypes: [],
                ...action.partyRegion
            }
        case types.STORE_SAVE:
            const {selectedPartyData, selectedId, filterText, selectedPartyID} = state;

            var _info
            if (selectedPartyData && selectedPartyData.partyId === selectedPartyID) {
                _info = {name: selectedPartyData.record.record, desc: selectedPartyData.record.characteristics}
            } else {
                _info = null
            }

            return {
                _info,
                selectedId,
                filterText,
                selectedPartyID
            }
        case types.PARTY_FIND_PARTY_REQUEST:
            return Object.assign({}, state, {
                isFetchingSearch: true,
            })
        case types.PARTY_FIND_PARTY_RECEIVE:
            return Object.assign({}, state, {
                dirtySearch: false,
                isFetchingSearch: false,
                fetchedSearch: true,
                items: action.items,
                filterText: action.filterText,
            })
        case types.PARTY_DETAIL_REQUEST:
            return Object.assign({}, state, {
                isFetchingDetail: true,
            })
        case types.PARTY_DETAIL_RECEIVE:
            return Object.assign({}, state, {
                dirty: false,
                isFetchingDetail: false,
                fetchedDetail: true,
                selectedPartyData: action.selectedPartyData,
                selectedPartyID: action.selectedPartyID,
            })
        case types.PARTY_DETAIL_CLEAR:
            return Object.assign({}, state, {
                selectedPartyData: null,
                selectedPartyID: null,
            })
        case types.PARTY_UPDATED:
            var isDirty = action.partyId === state.selectedPartyID;
            console.log("partyUpdate", isDirty, state);

            if (isDirty) {
                return Object.assign({}, state, {
                    dirty: isDirty
                })
            }

            return state;

        case types.PARTY_SELECT:
        case types.PARTY_ARR_RESET:
            var result = {...state};
            result.panel = panel(result.panel, action);
            result.dirty = true;
            return consolidateState(state, result);

        default:
            return state
    }
}
