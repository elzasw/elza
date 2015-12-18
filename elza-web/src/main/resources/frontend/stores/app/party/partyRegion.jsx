import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    selectedId: null,
    filterText: "",
    items: [],
    selectedPartyID : null,
    selectedPartyData: null
}

export default function partyRegion(state = initialState, action) {
    switch (action.type) {
        case types.PARTY_FIND_PARTY_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.PARTY_FIND_PARTY_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                items: action.items,
                filterText: action.filterText,
            })
        case types.PARTY_DETAIL_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.PARTY_DETAIL_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                selectedPartyData: action.selectedPartyData,
                selectedPartyID: action.selectedPartyID,
            })
        default:
            return state
    }
}
