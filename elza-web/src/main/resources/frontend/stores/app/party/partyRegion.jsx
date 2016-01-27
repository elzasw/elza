import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetchingSearch: false,
    fetchedSearch: false,
    isFetchingDetail: false,
    fetchedDetail: false,
    selectedId: null,
    filterText: "",
    items: [],
    selectedPartyID : null,
    selectedPartyData: null,
    partyTypes: [],
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
            const {selectedId, filterText, selectedPartyID} = state;
            return {
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
                isFetchingDetail: false,
                fetchedDetail: true,
                selectedPartyData: action.selectedPartyData,
                selectedPartyID: action.selectedPartyID,
            })
        case types.PARTY_DELETED:
            return Object.assign({}, state, {
                selectedPartyData: null,
                selectedPartyID: null,
            })
        default:
            return state
    }
}
